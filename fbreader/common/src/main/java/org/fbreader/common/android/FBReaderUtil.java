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
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import org.fbreader.md.MDActivity;
import org.fbreader.md.MDAlertDialogBuilder;

import org.geometerplus.zlibrary.core.resources.ZLResource;

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
}
