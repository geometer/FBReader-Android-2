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

import android.content.Context;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.book.Book;

import org.fbreader.md.MDEditTextPreference;

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
