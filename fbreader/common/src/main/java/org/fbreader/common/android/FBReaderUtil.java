/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.fbreader.common.android;

import java.io.*;

import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import org.fbreader.md.MDActivity;
import org.fbreader.md.MDAlertDialogBuilder;
import org.fbreader.util.IOUtil;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.filesystem.ZLPhysicalFile;
import org.geometerplus.zlibrary.core.filetypes.FileType;
import org.geometerplus.zlibrary.core.filetypes.FileTypeCollection;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.util.MimeType;

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;

import org.geometerplus.fbreader.book.Book;

import org.fbreader.common.R;

public abstract class FBReaderUtil {
	public static ZLAndroidLibrary getZLibrary(Activity activity) {
		return ((ZLAndroidApplication)activity.getApplication()).library();
	}

	public static void ensureFullscreen(Activity activity, View view) {
		if (view == null) {
			return;
		}

		final ZLAndroidLibrary zlibrary = getZLibrary(activity);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
				&& zlibrary.EnableFullscreenModeOption.getValue()) {
			view.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE |
				View.SYSTEM_UI_FLAG_IMMERSIVE |
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
				View.SYSTEM_UI_FLAG_FULLSCREEN |
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			);
		} else if (zlibrary.DisableButtonLightsOption.getValue()) {
			view.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE
			);
		}
	}

	public static void setBookTitle(MDActivity activity, Book book) {
		if (book != null) {
			activity.setTitleAndSubtitle(book.getTitle(), book.authorsString(", "));
		}
	}

	private static InputStream assetsStream(MDActivity activity, String name, String locale) {
		try {
			return activity.getResources().getAssets().open(name + "/" + locale + ".html");
		} catch (IOException e) {
			return null;
		}
	}

	private static String fromResourceFile(MDActivity activity, String name) {
		final StringBuffer buffer = new StringBuffer();

		BufferedReader reader = null;
		try {
			final Locale locale = Locale.getDefault();
			InputStream is = assetsStream(activity, name, ZLResource.getLanguage());
			if (is == null) {
				is = assetsStream(activity, name, locale.getLanguage() + "_" + locale.getCountry());
			}
			if (is == null) {
				is = assetsStream(activity, name, locale.getLanguage());
			}
			if (is == null) {
				is = assetsStream(activity, name, "en");
			}
			reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				buffer.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			// ignore
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
			}
		}

		return buffer.toString();
	}

	public static void showHtmlDialog(MDActivity activity, String title, String fileName) {
		final TextView textView =
			(TextView)activity.getLayoutInflater().inflate(R.layout.text_dialog, null);
		textView.setText(Html.fromHtml(fromResourceFile(activity, "data/" + fileName)));
		textView.setMovementMethod(new LinkMovementMethod());
		new MDAlertDialogBuilder(activity)
			.setTitle(title)
			.setView(textView)
			.create().show();
	}

	public static void shareBook(MDActivity activity, Book book) {
		if (book == null) {
			return;
		}

		try {
			final ZLPhysicalFile file = ZLFile.createFileByPath(book.getPath()).getPhysicalFile();
			if (file == null) {
				return;
			}
			final CharSequence sharedFrom =
				Html.fromHtml(ZLResource.resource("sharing").getResource("sharedFrom").getValue());
			final File origFile = file.javaFile();

			final File shareDir = new File(activity.getCacheDir(), "books");
			shareDir.mkdirs();

			String name = null;
			final MimeType mime = FileTypeCollection.Instance.mimeType(file);
			if (mime != null) {
				final FileType type = FileTypeCollection.Instance.typeForMime(mime);
				if (type != null) {
					name = book.getTitle() + "." + type.defaultExtension(mime);
				}
			}
			final File toShare = IOUtil.copyToDir(file.javaFile(), shareDir, name);
			if (toShare == null) {
				// TODO: show toast
				return;
			}
			final Uri uri = FileProvider.getUriForFile(
				activity, activity.getString(R.string.file_provider_authority), toShare
			);
			activity.startActivity(
				new Intent(Intent.ACTION_SEND)
					.setType(FileTypeCollection.Instance.rawMimeType(file).Name)
					.putExtra(Intent.EXTRA_SUBJECT, book.getTitle())
					.putExtra(Intent.EXTRA_TEXT, sharedFrom)
					.putExtra(Intent.EXTRA_STREAM, uri)
					.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			);
		} catch (ActivityNotFoundException e) {
			// TODO: show toast
		}
	}
}
