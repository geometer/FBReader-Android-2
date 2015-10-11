/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import org.fbreader.md.MDAlertDialogBuilder;
import org.fbreader.md.MDListActivity;
import org.fbreader.util.android.DrawableUtil;
import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.book.Author;
import org.geometerplus.fbreader.book.Book;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

import org.geometerplus.zlibrary.ui.android.R;

public class EditAuthorsDialogActivity extends MDListActivity {
	private final ZLResource myResource = ZLResource.resource("dialog").getResource("editAuthors");
	private final ZLResource myButtonResource = ZLResource.resource("dialog").getResource("button");

	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private volatile Book myBook;
	private final List<Author> myAuthors = new ArrayList<Author>();

	@Override
	protected int layoutId() {
		return R.layout.edit_authors;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(myResource.getValue());

		myBook = FBReaderIntents.getBookExtra(getIntent(), myCollection);
		if (myBook == null) {
			finish();
		}
		myAuthors.clear();
		myAuthors.addAll(myBook.authors());

		myCollection.bindToService(this, new Runnable() {
			public void run() {
				// TODO: collect all authors list (?)
			}
		});

		final Button okButton = (Button)findViewById(R.id.md_single_button);
		okButton.setText(myButtonResource.getResource("ok").getValue());
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// TODO: save data
			}
		});

		setListAdapter(new AuthorsAdapter());
	}

	@Override
	public void onDestroy() {
		myCollection.unbind();
		super.onDestroy();
	}

	private void updateAuthor(String text, Author oldAuthor) {
		// TODO: implement
	}

	private Drawable myDeleteIcon;
	private Drawable deleteIcon() {
		if (myDeleteIcon == null) {
			myDeleteIcon = DrawableUtil.tintedDrawable(
				this, R.drawable.ic_button_delete, R.color.text_primary
			);
		}
		return myDeleteIcon;
	}

	private class AuthorsAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position) != null ? 0 : 1;
		}

		@Override
		public int getCount() {
			return myAuthors.size() + 1;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Author getItem(int position) {
			if (position == myAuthors.size()) {
				return null;
			}
			return myAuthors.get(position);
		}

		@Override
		public View getView(final int position, View view, ViewGroup parent) {
			final Author author = getItem(position);
			if (view == null) {
				final int id = author != null
					? R.layout.edit_authors_item : R.layout.edit_authors_add_item;
				view = getLayoutInflater().inflate(id, parent, false);
			}

			if (author != null) {
				ViewUtil.setSubviewText(view, R.id.edit_authors_item_title, author.DisplayName);
				final ImageView deleteButton =
					ViewUtil.findImageView(view, R.id.edit_authors_item_delete);
				deleteButton.setImageDrawable(deleteIcon());
				deleteButton.setVisibility(myAuthors.size() > 1 ? View.VISIBLE : View.GONE);
				deleteButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(final View v) {
						myAuthors.remove(position);
						notifyDataSetChanged();
					}
				});
			} else {
				ViewUtil.findImageView(view, R.id.edit_authors_add_item_icon)
					.setImageDrawable(DrawableUtil.tintedDrawable(
						EditAuthorsDialogActivity.this,
						R.drawable.ic_button_add_author,
						R.color.text_primary
					));
				ViewUtil.setSubviewText(
					view,
					R.id.edit_authors_add_item_text,
					myResource.getResource("addAuthor").getValue()
				);
			}

			return view;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			final Author author = getItem(position);
			final View root = getLayoutInflater().inflate(R.layout.text_autocomplete_dialog, null);
			final AutoCompleteTextView edit =
				(AutoCompleteTextView)root.findViewById(R.id.text_autocomplete_dialog_edit);
			final String key = author == null ? "addAuthor" : "editAuthor";

			final AlertDialog dialog = new MDAlertDialogBuilder(EditAuthorsDialogActivity.this)
				.setView(root)
				.setTitle(myResource.getResource(key).getValue())
				.setPositiveButton(
					myButtonResource.getResource("ok").getValue(),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							updateAuthor(edit.getText().toString(), author);
						}
					}
				)
				.create();
			// TODO: disable ok button on invalid input
			dialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface di) {
					if (author != null) {
						edit.setText(author.DisplayName);
						edit.setSelection(author.DisplayName.length());
					}
					edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						public boolean onEditorAction(TextView self, int actionId, KeyEvent event) {
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								updateAuthor(edit.getText().toString(), author);
								dialog.dismiss();
							}
							return true;
						}
					});
					final List<Author> allAuthors = myCollection.authors();
					final TreeSet<String> names = new TreeSet<String>();
					for (Author a : allAuthors) {
						names.add(a.DisplayName);
					}
					edit.setAdapter(new ArrayAdapter<String>(
						EditAuthorsDialogActivity.this,
						android.R.layout.simple_dropdown_item_1line,
						new ArrayList<String>(names)
					));
					edit.requestFocus();
				}
			});
			dialog.show();
		}
	}
}
