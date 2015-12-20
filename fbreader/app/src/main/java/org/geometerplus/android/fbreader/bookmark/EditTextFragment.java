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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.book.Bookmark;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public final class EditTextFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		final EditBookmarkActivity activity = (EditBookmarkActivity)getActivity();

		if (activity == null || container == null) {
			return null;
		}
		final Bookmark bookmark = activity.Bookmark;
		if (bookmark == null) {
			return null;
		}
		final BookCollectionShadow collection = activity.Collection;

		final View view = inflater.inflate(R.layout.edit_bookmark_text, container, false);

		final EditText editor = (EditText)view.findViewById(R.id.edit_bookmark_text);
		editor.setText(bookmark.getText());
		final int len = editor.getText().length();
		editor.setSelection(len, len);

		final Button saveTextButton = (Button)view.findViewById(R.id.edit_bookmark_save_text_button);
		saveTextButton.setEnabled(false);
		saveTextButton.setText(EditBookmarkActivity.Resource.getResource("saveText").getValue());
		saveTextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				collection.bindToService(getActivity(), new Runnable() {
					public void run() {
						bookmark.setText(editor.getText().toString());
						collection.saveBookmark(bookmark);
						saveTextButton.setEnabled(false);
					}
				});
			}
		});
		editor.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence sequence, int start, int before, int count) {
				final String originalText = bookmark.getText();
				saveTextButton.setEnabled(!originalText.equals(editor.getText().toString()));
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		return view;
	}
}
