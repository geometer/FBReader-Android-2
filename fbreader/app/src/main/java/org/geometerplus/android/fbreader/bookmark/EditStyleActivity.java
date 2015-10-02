/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.bookmark;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Window;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.util.ZLColor;

import org.geometerplus.fbreader.book.BookmarkUtil;
import org.geometerplus.fbreader.book.HighlightingStyle;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.fbreader.md.MDEditTextPreference;
import org.fbreader.md.MDSettingsActivity;
import org.geometerplus.android.fbreader.preferences.*;
import org.geometerplus.zlibrary.ui.android.R;

public class EditStyleActivity extends MDSettingsActivity {
	static final String STYLE_ID_KEY = "style.id";

	private final ZLResource myRootResource = ZLResource.resource("editStyle");
	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private HighlightingStyle myStyle;
	private BgColorPreference myBgColorPreference;

	@Override
	protected PreferenceFragment preferenceFragment() {
		return new EditStyleFragment();
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));
		setTitleVisibility(false);
	}

	private class EditStyleFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle bundle) {
			super.onCreate(bundle);

			final PreferenceScreen screen =
				getPreferenceManager().createPreferenceScreen(EditStyleActivity.this);
			setPreferenceScreen(screen);

			myCollection.bindToService(EditStyleActivity.this, new Runnable() {
				public void run() {
					myStyle = myCollection.getHighlightingStyle(getIntent().getIntExtra(STYLE_ID_KEY, -1));
					if (myStyle == null) {
						finish();
						return;
					}
					screen.addPreference(new NamePreference());
					screen.addPreference(new InvisiblePreference());
					myBgColorPreference = new BgColorPreference();
					screen.addPreference(myBgColorPreference);
				}
			});
		}

		@Override
		public void onDestroy() {
			myCollection.unbind();
			super.onDestroy();
		}
	}

	private class NamePreference extends MDEditTextPreference {
		NamePreference() {
			super(EditStyleActivity.this);
			setTitle(myRootResource.getResource("name").getValue());
		}

		@Override
		protected String positiveButtonText() {
			return ZLResource.resource("dialog").getResource("button").getResource("ok").getValue();
		}

		@Override
		protected final String getValue() {
			return BookmarkUtil.getStyleName(myStyle);
		}

		@Override
		protected void setValue(String value) {
			if (!value.equals(getValue())) {
				BookmarkUtil.setStyleName(myStyle, value);
				myCollection.saveHighlightingStyle(myStyle);
			}
		}
	}

	private class InvisiblePreference extends ZLCheckBoxPreference {
		private ZLColor mySavedBgColor;

		InvisiblePreference() {
			super(EditStyleActivity.this, myRootResource.getResource("invisible"));
			setChecked(myStyle.getBackgroundColor() == null);
		}

		@Override
		protected void onClick() {
			super.onClick();
			if (isChecked()) {
				mySavedBgColor = myStyle.getBackgroundColor();
				myStyle.setBackgroundColor(null);
				myBgColorPreference.setEnabled(false);
			} else {
				myStyle.setBackgroundColor(
					mySavedBgColor != null ? mySavedBgColor : new ZLColor(127, 127, 127)
				);
				myBgColorPreference.setEnabled(true);
			}
			myCollection.saveHighlightingStyle(myStyle);
		}
	}

	private class BgColorPreference extends ColorPreference {
		BgColorPreference() {
			super(EditStyleActivity.this);
			setEnabled(getSavedColor() != null);
		}

		@Override
		public String getTitle() {
			return myRootResource.getResource("bgColor").getValue();
		}

		@Override
		protected ZLColor getSavedColor() {
			return myStyle.getBackgroundColor();
		}

		@Override
		protected void saveColor(ZLColor color) {
			myStyle.setBackgroundColor(color);
			myCollection.saveHighlightingStyle(myStyle);
		}
	}
}
