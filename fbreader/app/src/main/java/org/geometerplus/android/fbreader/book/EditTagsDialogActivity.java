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

import android.content.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import org.fbreader.md.MDAlertDialogBuilder;
import org.fbreader.util.android.DrawableUtil;
import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.Tag;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.EditableListDialogActivity;

import org.geometerplus.zlibrary.ui.android.R;

public class EditTagsDialogActivity extends EditableListDialogActivity {
	private final ZLResource myResource = ZLResource.resource("dialog").getResource("editTags");

	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private volatile Book myBook;
	private final List<Tag> myTags = new ArrayList<Tag>();
	private final List<Tag> myAllTags = Collections.synchronizedList(new ArrayList<Tag>());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(myResource.getValue());

		myBook = FBReaderIntents.getBookExtra(getIntent(), myCollection);
		if (myBook == null) {
			finish();
		}
		myTags.clear();
		myTags.addAll(myBook.tags());

		myCollection.bindToService(this, new Runnable() {
			public void run() {
				myAllTags.clear();
				myAllTags.addAll(myCollection.tags());
			}
		});

		final Button okButton = (Button)findViewById(R.id.md_single_button);
		okButton.setText(myButtonResource.getResource("ok").getValue());
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				myBook.removeAllTags();
				for (Tag t : myTags) {
					myBook.addTag(t);
				}
				myCollection.saveBook(myBook);
				finish();
			}
		});

		setListAdapter(new TagsAdapter());
	}

	@Override
	public void onDestroy() {
		myCollection.unbind();
		super.onDestroy();
	}

	private class TagsAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
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
			return myTags.size() + 1;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Tag getItem(int position) {
			if (position == myTags.size()) {
				return null;
			}
			return myTags.get(position);
		}

		@Override
		public View getView(final int position, View view, ViewGroup parent) {
			final Tag tag = getItem(position);
			if (view == null) {
				final int id = tag != null
					? R.layout.editable_list_item : R.layout.editable_list_add_item;
				view = getLayoutInflater().inflate(id, parent, false);
			}

			if (tag != null) {
				ViewUtil.setSubviewText(view, R.id.editable_list_item_title, tag.Name);
				final ImageView deleteButton =
					ViewUtil.findImageView(view, R.id.editable_list_item_delete);
				deleteButton.setImageDrawable(deleteIcon());
				deleteButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(final View v) {
						showRemoveTagDialog(position, tag);
					}
				});
			} else {
				ViewUtil.findImageView(view, R.id.editable_list_add_item_icon)
					.setImageDrawable(DrawableUtil.tintedDrawable(
						EditTagsDialogActivity.this,
						R.drawable.ic_button_add,
						R.color.text_primary
					));
				ViewUtil.setSubviewText(
					view,
					R.id.editable_list_add_item_text,
					myResource.getResource("addTag").getValue()
				);
			}

			return view;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			final Tag tag = getItem(position);
			final View root = getLayoutInflater().inflate(R.layout.text_autocomplete_dialog, null);
			final AutoCompleteTextView edit =
				(AutoCompleteTextView)root.findViewById(R.id.text_autocomplete_dialog_edit);
			final String key = tag == null ? "addTag" : "editTag";

			final AlertDialog dialog = new MDAlertDialogBuilder(EditTagsDialogActivity.this)
				.setView(root)
				.setTitle(myResource.getResource(key).getValue())
				.setPositiveButton(
					myButtonResource.getResource("ok").getValue(),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							updateTag(edit.getText().toString(), tag);
						}
					}
				)
				.create();
			// TODO: disable ok button on invalid input
			dialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface di) {
					if (tag != null) {
						edit.setText(tag.Name);
						edit.setSelection(tag.Name.length());
					}
					edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						public boolean onEditorAction(TextView self, int actionId, KeyEvent event) {
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								updateTag(edit.getText().toString(), tag);
								dialog.dismiss();
							}
							return true;
						}
					});
					final TreeSet<String> names = new TreeSet<String>();
					synchronized (myAllTags) {
						for (Tag t : myAllTags) {
							names.add(t.Name);
						}
					}
					edit.setAdapter(new ArrayAdapter<String>(
						EditTagsDialogActivity.this,
						android.R.layout.simple_dropdown_item_1line,
						new ArrayList<String>(names)
					));
					edit.requestFocus();
				}
			});
			dialog.show();
		}

		private void updateTag(String text, Tag oldTag) {
			if (text == null) {
				return;
			}
			text = text.trim();
			if (text.length() == 0) {
				return;
			}
			text.replaceAll("\\s+", " ");

			Tag newTag = null;
			synchronized (myAllTags) {
				for (Tag t : myAllTags) {
					if (text.equals(t.Name)) {
						newTag = t;
					}
				}
			}
			if (newTag == null) {
				newTag = Tag.getTag(text.split("/"));
			}
			if (newTag == null || newTag.equals(oldTag)) {
				return;
			}
			if (oldTag != null) {
				final int position = myTags.indexOf(oldTag);
				if (position == -1) {
					return;
				}
				if (myTags.contains(newTag)) {
					myTags.remove(position);
				} else {
					myTags.set(position, newTag);
				}
			} else {
				if (!myTags.contains(newTag)) {
					myTags.add(newTag);
				}
			}
			notifyDataSetChanged();
		}

		private void showRemoveTagDialog(final int position, Tag tag) {
			final ZLResource resource = myResource.getResource("removeTag");
			new MDAlertDialogBuilder(EditTagsDialogActivity.this)
				.setTitle(resource.getValue())
				.setMessage(resource.getResource("message").getValue().replace("%s", tag.Name))
				.setPositiveButton(
					myButtonResource.getResource("yes").getValue(),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							myTags.remove(position);
							notifyDataSetChanged();
						}
					}
				)
				.create().show();
		}
	}
}
