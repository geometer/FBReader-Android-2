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

package org.geometerplus.android.fbreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.view.View;

import com.github.johnpersano.supertoasts.SuperActivityToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.Bookmark;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.util.OrientationUtil;

import org.fbreader.md.MDActivity;

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

	public static void showBookmarkToast(final FBReaderMainActivity activity, final Bookmark bookmark) {
		final SuperActivityToast toast =
			new SuperActivityToast(activity, SuperToast.Type.BUTTON);
		toast.setText(bookmark.getText());
		toast.setDuration(SuperToast.Duration.EXTRA_LONG);
		toast.setButtonIcon(
			android.R.drawable.ic_menu_edit, 0,
			ZLResource.resource("dialog").getResource("button").getResource("edit").getValue()
		);
		toast.setOnClickWrapper(new OnClickWrapper("bkmk", new SuperToast.OnClickListener() {
			@Override
			public void onClick(View view, Parcelable token) {
				final Intent intent =
					FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.EDIT_BOOKMARK);
				FBReaderIntents.putBookmarkExtra(intent, bookmark);
				OrientationUtil.startActivity(activity, intent);
			}
		}));
		activity.showToast(toast);
	}
}
