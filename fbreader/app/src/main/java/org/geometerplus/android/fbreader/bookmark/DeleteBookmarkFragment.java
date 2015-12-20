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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.Button;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public final class DeleteBookmarkFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		if (container == null) {
			return null;
		}
		final View view = inflater.inflate(R.layout.edit_bookmark_delete, container, false);
		final Button deleteButton = (Button)view.findViewById(R.id.edit_bookmark_delete_button);
		deleteButton.setText(EditBookmarkActivity.Resource.getResource("deleteBookmark").getValue());
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final EditBookmarkActivity activity = (EditBookmarkActivity)getActivity();
				if (activity == null) {
					return;
				}
				activity.Collection.bindToService(activity, new Runnable() {
					public void run() {
						activity.Collection.deleteBookmark(activity.Bookmark);
						activity.finish();
					}
				});
			}
		});
		return view;
	}
}
