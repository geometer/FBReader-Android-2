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

package org.geometerplus.android.fbreader.bookmark;

import java.util.List;

import android.app.Activity;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import org.geometerplus.fbreader.book.*;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public final class SelectStyleFragment extends ListFragment implements IBookCollection.Listener<Book> {
	volatile BookCollectionShadow Collection;
	private StyleListAdapter myStylesAdapter;

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);

		Collection = ((EditBookmarkActivity)activity).Collection;

		Collection.bindToService(activity, new Runnable() {
			public void run() {
				final List<HighlightingStyle> styles = Collection.highlightingStyles();
				if (styles.isEmpty()) {
					activity.finish();
					return;
				}
				myStylesAdapter = new StyleListAdapter(SelectStyleFragment.this, styles);
				setListAdapter(myStylesAdapter);
				Collection.addListener(SelectStyleFragment.this);
			}
		});
	}

	@Override
	public void onDetach() {
		if (Collection != null) {
			Collection.removeListener(this);
			Collection = null;
		}
		super.onDetach();
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		final StyleListAdapter adapter = (StyleListAdapter)getListAdapter();
		adapter.onItemClick(listView, view, position, id);
	}

	// method from IBookCollection.Listener
	public void onBookEvent(BookEvent event, Book book) {
		if (event == BookEvent.BookmarkStyleChanged) {
			myStylesAdapter.setStyleList(Collection.highlightingStyles());
		}
	}

	// method from IBookCollection.Listener
	public void onBuildEvent(IBookCollection.Status status) {
	}

	Bookmark getBookmark() {
		return ((EditBookmarkActivity)getActivity()).Bookmark;
	}
}
