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

package org.geometerplus.android.fbreader.book;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.preference.*;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.book.Book;

import org.geometerplus.android.fbreader.FBReaderUtil;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.OrientationUtil;

import org.fbreader.md.MDSettingsActivity;

public class EditBookInfoActivity extends MDSettingsActivity {
	final static int EDIT_TAGS_REQUEST_CODE = 1;
	final static int EDIT_AUTHORS_REQUEST_CODE = 2;

	private class EditBookInfoFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle bundle) {
			super.onCreate(bundle);

			myScreen = getPreferenceManager().createPreferenceScreen(EditBookInfoActivity.this);
			setPreferenceScreen(myScreen);

			Book = FBReaderIntents.getBookExtra(getIntent(), Collection);

			if (Book == null) {
				finish();
				return;
			}

			FBReaderUtil.setBookTitle(EditBookInfoActivity.this, Book);

			Collection.bindToService(EditBookInfoActivity.this, new Runnable() {
				public void run() {
					if (myInitialized) {
						return;
					}
					myInitialized = true;

					addPreference(new BookTitlePreference(EditBookInfoActivity.this, Resource, "title", Book));
					myEditAuthorsPreference =
						new EditAuthorsPreference(EditBookInfoActivity.this, Resource, "authors");
					addPreference(myEditAuthorsPreference);
					myEditTagsPreference =
						new EditTagsPreference(EditBookInfoActivity.this, Resource, "tags");
					addPreference(myEditTagsPreference);
					addPreference(new BookLanguagePreference(EditBookInfoActivity.this, Resource.getResource("language"), Book));
					addPreference(new EncodingPreference(EditBookInfoActivity.this, Resource.getResource("encoding"), Book));
				}
			});
		}

		@Override
		public void onDestroy() {
			Collection.unbind();
			super.onDestroy();
		}
	}

	private PreferenceScreen myScreen;
	final ZLResource Resource = ZLResource.resource("BookInfo");

	final BookCollectionShadow Collection = new BookCollectionShadow();
	private volatile boolean myInitialized;

	private EditTagsPreference myEditTagsPreference;
	private EditAuthorsPreference myEditAuthorsPreference;
	Book Book;

	public void addPreference(Preference preference) {
		myScreen.addPreference(preference);
	}

	@Override
	protected PreferenceFragment preferenceFragment() {
		return new EditBookInfoFragment();
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));
	}

	@Override
	protected void onStart() {
		super.onStart();
		OrientationUtil.setOrientation(this, getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		OrientationUtil.setOrientation(this, intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case EDIT_TAGS_REQUEST_CODE:
				myEditTagsPreference.updateSummary();
				break;
			case EDIT_AUTHORS_REQUEST_CODE:
				myEditAuthorsPreference.updateSummary();
				break;
		}
	}

	void saveBook() {
		Collection.bindToService(this, new Runnable() {
			public void run() {
				Collection.saveBook(Book);
			}
		});
	}
}
