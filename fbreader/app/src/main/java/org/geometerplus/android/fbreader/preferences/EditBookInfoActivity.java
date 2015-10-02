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

package org.geometerplus.android.fbreader.preferences;

import java.util.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.*;

import org.geometerplus.zlibrary.core.encodings.Encoding;
import org.geometerplus.zlibrary.core.language.Language;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.formats.*;

import org.geometerplus.android.fbreader.FBReaderUtil;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.OrientationUtil;

import org.fbreader.md.MDEditTextPreference;
import org.fbreader.md.MDSettingsActivity;
import org.geometerplus.zlibrary.ui.android.R;

class BookTitlePreference extends MDEditTextPreference {
	private final Book myBook;

	BookTitlePreference(Context context, ZLResource rootResource, String resourceKey, Book book) {
		super(context);

		setTitle(rootResource.getResource(resourceKey).getValue());
		myBook = book;
	}

	@Override
	protected String positiveButtonText() {
		return ZLResource.resource("dialog").getResource("button").getResource("ok").getValue();
	}

	@Override
	protected void setValue(String value) {
		if (!value.equals(getValue())) {
			myBook.setTitle(value);
			((EditBookInfoActivity)getContext()).saveBook();
		}
	}

	@Override
	protected final String getValue() {
		return myBook.getTitle();
	}
}

class BookLanguagePreference extends LanguagePreference {
	private final Book myBook;

	private static List<Language> languages() {
		final TreeSet<Language> set = new TreeSet<Language>();
		for (String code : ZLTextHyphenator.Instance().languageCodes()) {
			set.add(new Language(code));
		}
		set.add(new Language(Language.OTHER_CODE));
		return new ArrayList<Language>(set);
	}

	BookLanguagePreference(Context context, ZLResource resource, Book book) {
		super(context, resource, languages());
		myBook = book;
	}

	@Override
	protected String currentValue() {
		final String language = myBook.getLanguage();
		if (language != null) {
			for (String l : values()) {
				if (language.equals(l)) {
					return language;
				}
			}
		}
		return Language.OTHER_CODE;
	}

	@Override
	protected void onValueSelected(int index, String code) {
		myBook.setLanguage(code.length() > 0 ? code : null);
		((EditBookInfoActivity)getContext()).saveBook();
	}
}

class EncodingPreference extends SingleChoicePreference {
	private final PluginCollection myPluginCollection;
	private final Book myBook;

	EncodingPreference(Context context, ZLResource resource, Book book) {
		super(context, resource);
		myPluginCollection = PluginCollection.Instance(Paths.systemInfo(context));
		myBook = book;

		final FormatPlugin plugin;
		try {
			plugin = BookUtil.getPlugin(myPluginCollection, book);
		} catch (BookReadingException e) {
			return;
		}

		final List<Encoding> encodings =
			new ArrayList<Encoding>(plugin.supportedEncodings().encodings());
		Collections.sort(encodings, new Comparator<Encoding>() {
			public int compare(Encoding e1, Encoding e2) {
				return e1.DisplayName.compareTo(e2.DisplayName);
			}
		});
		final String[] codes = new String[encodings.size()];
		final String[] names = new String[encodings.size()];
		int index = 0;
		for (Encoding e : encodings) {
			//addItem(e.Family, e.Name, e.DisplayName);
			codes[index] = e.Name;
			names[index] = e.DisplayName;
			++index;
		}
		setLists(codes, names);

		if (encodings.size() == 1) {
			setEnabled(false);
		}
	}

	@Override
	protected String currentValue() {
		final String[] codes = values();
		if (codes.length > 1) {
			final String bookEncoding = BookUtil.getEncoding(myBook, myPluginCollection);
			if (bookEncoding != null) {
				return bookEncoding.toLowerCase();
			}
		}
		return codes[0];
	}

	@Override
	protected void onValueSelected(int index, String value) {
		if (!value.equalsIgnoreCase(BookUtil.getEncoding(myBook, myPluginCollection))) {
			myBook.setEncoding(value);
			((EditBookInfoActivity)getContext()).saveBook();
		}
	}
}

class EditTagsPreference extends Preference {
	protected final ZLResource myResource;
	private final Book myBook;

	EditTagsPreference(Context context, ZLResource rootResource, String resourceKey, Book book) {
		super(context);
		myBook = book;
		myResource = rootResource.getResource(resourceKey);
		setTitle(myResource.getValue());
		setSummary(book.tagsString(", "));
	}

	void saveTags(final ArrayList<String> tags) {
		if(tags.size() == 0)
			return;

		myBook.removeAllTags();
		for (String t : tags) {
			myBook.addTag(t);
		}
		((EditBookInfoActivity)getContext()).saveBook();
	}

	@Override
	protected void onClick() {
		Intent intent = new Intent(getContext(), EditTagsDialogActivity.class);
		intent.putExtra(EditListDialogActivity.Key.ACTIVITY_TITLE, myResource.getValue());
		ArrayList<String> tags = new ArrayList<String>();
		for (Tag tag : myBook.tags()) {
			tags.add(tag.Name);
		}
		ArrayList<String> allTags = new ArrayList<String>();
		for (Tag tag : ((EditBookInfoActivity)getContext()).tags()) {
			allTags.add(tag.Name);
		}
		intent.putExtra(EditListDialogActivity.Key.LIST, tags);
		intent.putExtra(EditListDialogActivity.Key.ALL_ITEMS_LIST, allTags);
		((EditBookInfoActivity)getContext()).startActivityForResult(intent, EditTagsDialogActivity.REQ_CODE);
	}
}

class EditAuthorsPreference extends Preference {
	protected final ZLResource myResource;
	private final Book myBook;

	EditAuthorsPreference(Context context, ZLResource rootResource, String resourceKey, Book book) {
		super(context);
		myBook = book;
		myResource = rootResource.getResource(resourceKey);
		setTitle(myResource.getValue());
		setSummary(book.authorsString(", "));
	}

	void saveAuthors(final ArrayList<String> authors) {
		if (authors.size() == 0) {
			return;
		}

		myBook.removeAllAuthors();
		for (String a : authors) {
			myBook.addAuthor(a);
		}
		((EditBookInfoActivity)getContext()).saveBook();
	}

	@Override
	protected void onClick() {
		final Intent intent = new Intent(getContext(), EditAuthorsDialogActivity.class);
		intent.putExtra(EditListDialogActivity.Key.ACTIVITY_TITLE, myResource.getValue());
		final ArrayList<String> authors = new ArrayList<String>();
		for (Author author : myBook.authors()) {
			authors.add(author.DisplayName);
		}
		((EditBookInfoActivity)getContext()).saveBook();
		final ArrayList<String> allAuthors = new ArrayList<String>();
		for (Author author : ((EditBookInfoActivity)getContext()).authors()) {
			allAuthors.add(author.DisplayName);
		}
		intent.putExtra(EditListDialogActivity.Key.LIST, authors);
		intent.putExtra(EditListDialogActivity.Key.ALL_ITEMS_LIST, allAuthors);
		((EditBookInfoActivity)getContext()).startActivityForResult(intent, EditAuthorsDialogActivity.REQ_CODE);
	}
}

public class EditBookInfoActivity extends MDSettingsActivity {
	private class EditBookInfoFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle bundle) {
			super.onCreate(bundle);

			myScreen = getPreferenceManager().createPreferenceScreen(EditBookInfoActivity.this);
			setPreferenceScreen(myScreen);

			myBook = FBReaderIntents.getBookExtra(getIntent(), myCollection);

			if (myBook == null) {
				finish();
				return;
			}

			FBReaderUtil.setBookTitle(EditBookInfoActivity.this, myBook);

			myCollection.bindToService(EditBookInfoActivity.this, new Runnable() {
				public void run() {
					if (myInitialized) {
						return;
					}
					myInitialized = true;

					addPreference(new BookTitlePreference(EditBookInfoActivity.this, Resource, "title", myBook));
					myEditAuthorsPreference = (EditAuthorsPreference)addPreference(new EditAuthorsPreference(EditBookInfoActivity.this, Resource, "authors", myBook));
					myEditTagsPreference = (EditTagsPreference)addPreference(new EditTagsPreference(EditBookInfoActivity.this, Resource, "tags", myBook));
					addPreference(new BookLanguagePreference(EditBookInfoActivity.this, Resource.getResource("language"), myBook));
					addPreference(new EncodingPreference(EditBookInfoActivity.this, Resource.getResource("encoding"), myBook));
				}
			});
		}

		@Override
		public void onDestroy() {
			myCollection.unbind();
			super.onDestroy();
		}
	}

	private PreferenceScreen myScreen;
	final ZLResource Resource = ZLResource.resource("BookInfo");

	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private volatile boolean myInitialized;

	private EditTagsPreference myEditTagsPreference;
	private EditAuthorsPreference myEditAuthorsPreference;
	private Book myBook;

	public Preference addPreference(Preference preference) {
		myScreen.addPreference(preference);
		return preference;
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
	protected void onActivityResult(int reqCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (reqCode) {
				case EditTagsDialogActivity.REQ_CODE:
					myEditTagsPreference.saveTags(data.getStringArrayListExtra(EditListDialogActivity.Key.LIST));
					break;
				case EditAuthorsDialogActivity.REQ_CODE:
					myEditAuthorsPreference.saveAuthors(data.getStringArrayListExtra(EditListDialogActivity.Key.LIST));
					break;
			}
		}
	}

	void saveBook() {
		myCollection.bindToService(this, new Runnable() {
			public void run() {
				myCollection.saveBook(myBook);
			}
		});
	}

	List<Author> authors() {
		return myCollection.authors();
	}

	List<Tag> tags() {
		return myCollection.tags();
	}
}
