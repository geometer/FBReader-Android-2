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

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.view.*;
import android.widget.*;

import yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView;

import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.book.*;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

final class StyleListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
	private final SelectStyleFragment myFragment;
	private final List<HighlightingStyle> myStyles;

	StyleListAdapter(SelectStyleFragment fragment, List<HighlightingStyle> styles) {
		myFragment = fragment;
		myStyles = new ArrayList<HighlightingStyle>(styles);
	}

	public synchronized void setStyleList(List<HighlightingStyle> styles) {
		myStyles.clear();
		myStyles.addAll(styles);
		notifyDataSetChanged();
	}

	public final synchronized int getCount() {
		return myStyles.size();
	}

	public final synchronized HighlightingStyle getItem(int position) {
		return myStyles.get(position);
	}

	public final long getItemId(int position) {
		return position;
	}

	public final synchronized View getView(int position, View convertView, final ViewGroup parent) {
		final View view = convertView != null
			? convertView
			: LayoutInflater.from(parent.getContext()).inflate(R.layout.style_item, parent, false);
		final HighlightingStyle style = getItem(position);

		final CheckBox checkBox = (CheckBox)ViewUtil.findView(view, R.id.style_item_checkbox);
		final AmbilWarnaPrefWidgetView colorView =
			(AmbilWarnaPrefWidgetView)ViewUtil.findView(view, R.id.style_item_color);
		final TextView titleView = ViewUtil.findTextView(view, R.id.style_item_title);
		final Button button = (Button)ViewUtil.findView(view, R.id.style_item_edit_button);

		checkBox.setChecked(style.Id == myFragment.getBookmark().getStyleId());

		colorView.setVisibility(View.VISIBLE);
		BookmarksUtil.setupColorView(colorView, style);

		titleView.setText(BookmarkUtil.getStyleName(style));

		button.setVisibility(View.VISIBLE);
		button.setText(EditBookmarkActivity.Resource.getResource("editStyle").getValue());
		button.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				myFragment.getActivity().startActivity(
					new Intent(myFragment.getActivity(), EditStyleActivity.class)
						.putExtra(EditStyleActivity.STYLE_ID_KEY, style.Id)
				);
			}
		});

		return view;
	}

	public final synchronized void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final HighlightingStyle style = getItem(position);
		final Bookmark bookmark = myFragment.getBookmark();
		final BookCollectionShadow collection = myFragment.Collection;
		collection.bindToService(myFragment.getActivity(), new Runnable() {
			public void run() {
				bookmark.setStyleId(style.Id);
				collection.setDefaultHighlightingStyleId(style.Id);
				collection.saveBookmark(bookmark);
			}
		});
		notifyDataSetChanged();
	}
}
