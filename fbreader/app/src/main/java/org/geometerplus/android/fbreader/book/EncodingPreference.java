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

import java.util.*;

import org.geometerplus.zlibrary.core.encodings.Encoding;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.formats.*;

import org.geometerplus.android.fbreader.preferences.SingleChoicePreference;

class EncodingPreference extends SingleChoicePreference implements BookInfoPreference {
	private final PluginCollection myPluginCollection;
	private final Book myBook;

	EncodingPreference(EditBookInfoActivity activity, ZLResource resource, Book book) {
		super(activity, resource);
		myPluginCollection = PluginCollection.Instance(Paths.systemInfo(activity));
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
	public void updateView() {
		notifyChanged();
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
