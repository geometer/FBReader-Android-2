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
import android.view.Menu;
import android.view.MenuItem;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.formats.PluginCollection;

import org.geometerplus.android.fbreader.FBReaderUtil;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.OrientationUtil;

import org.fbreader.md.MDSettingsActivity;

public class EditBookInfoActivity extends MDSettingsActivity implements MenuItem.OnMenuItemClickListener, IBookCollection.Listener<Book> {
	private class EditBookInfoFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle bundle) {
			super.onCreate(bundle);

			myScreen = getPreferenceManager().createPreferenceScreen(EditBookInfoActivity.this);
			setPreferenceScreen(myScreen);

			Book = FBReaderIntents.getBookExtra(getIntent(), myCollection);

			if (Book == null) {
				finish();
				return;
			}

			FBReaderUtil.setBookTitle(EditBookInfoActivity.this, Book);
			addPreference(new BookTitlePreference(EditBookInfoActivity.this, Resource, "title", Book));
			addPreference(new EditAuthorsPreference(EditBookInfoActivity.this, Resource, "authors"));
			addPreference(new EditTagsPreference(EditBookInfoActivity.this, Resource, "tags"));
			addPreference(new BookLanguagePreference(EditBookInfoActivity.this, Resource.getResource("language"), Book));
			addPreference(new EncodingPreference(EditBookInfoActivity.this, Resource.getResource("encoding"), Book));
		}
	}

	private PreferenceScreen myScreen;
	final ZLResource Resource = ZLResource.resource("BookInfo");

	private final BookCollectionShadow myCollection = new BookCollectionShadow();

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

		myCollection.bindToService(this, new Runnable() {
			public void run() {
				updateBookInfo(myCollection.getBookById(Book.getId()));
			}
		});
		myCollection.addListener(this);
	}

	@Override
	public void onStop() {
		myCollection.removeListener(this);
		myCollection.unbind();
		super.onStop();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		OrientationUtil.setOrientation(this, intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		final MenuItem item = menu.add(
			ZLResource.resource("dialog").getResource("button").getResource("reloadInfo").getValue()
		);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		item.setOnMenuItemClickListener(this);
		return true;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if (Book != null) {
			BookUtil.reloadInfoFromFile(
				Book, PluginCollection.Instance(Paths.systemInfo(this))
			);
			updateBookInfo(Book);
			saveBook();
		}
		return true;
	}

	void saveBook() {
		myCollection.bindToService(this, new Runnable() {
			public void run() {
				myCollection.saveBook(Book);
			}
		});
	}

	private void updateBookInfo(Book book) {
		if (book == null || !myCollection.sameBook(book, Book)) {
			return;
		}

		Book.updateFrom(book);
		FBReaderUtil.setBookTitle(EditBookInfoActivity.this, Book);
		for (int i = myScreen.getPreferenceCount() - 1; i >= 0; --i) {
			final Preference pref = myScreen.getPreference(i);
			if (pref instanceof BookInfoPreference) {
				((BookInfoPreference)pref).updateView();
			}
		}
	}

	public void onBookEvent(BookEvent event, Book book) {
		if (event == BookEvent.Updated && myCollection.sameBook(book, Book)) {
			updateBookInfo(book);
		}
	}

	public void onBuildEvent(IBookCollection.Status status) {
	}
}
