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

package org.geometerplus.android.util;

import java.util.ArrayList;

import android.content.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import org.fbreader.md.MDAlertDialogBuilder;
import org.fbreader.reader.android.UIMessageUtil;
import org.fbreader.util.android.DrawableUtil;
import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.R;

public class FolderListDialogActivity extends EditableListDialogActivity {
	interface Key {
		String FOLDER_LIST            = "folder_list.folder_list";
		String ACTIVITY_TITLE         = "folder_list.title";
		String CHOOSER_TITLE          = "folder_list.chooser_title";
		String WRITABLE_FOLDERS_ONLY  = "folder_list.writable_folders_only";
	}

	private ArrayList<String> myFolderList;
	private String myChooserTitle;
	private boolean myChooseWritableDirectoriesOnly;
	private ZLResource myResource;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		myFolderList = intent.getStringArrayListExtra(Key.FOLDER_LIST);
		setTitle(intent.getStringExtra(Key.ACTIVITY_TITLE));
		myChooserTitle = intent.getStringExtra(Key.CHOOSER_TITLE);
		myChooseWritableDirectoriesOnly = intent.getBooleanExtra(Key.WRITABLE_FOLDERS_ONLY, true);
		myResource = ZLResource.resource("dialog").getResource("folderList");

		final Button okButton = (Button)findViewById(R.id.md_single_button);
		okButton.setText(myButtonResource.getResource("ok").getValue());
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_OK, new Intent().putExtra(Key.FOLDER_LIST, myFolderList));
				finish();
			}
		});

		final DirectoriesAdapter adapter = new DirectoriesAdapter();
		setListAdapter(adapter);
		getListView().setOnItemClickListener(adapter);

		setResult(RESULT_CANCELED);
	}

	@Override
	protected void onActivityResult(int index, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			final String path = FileChooserUtil.folderPathFromData(data);
			final int existing = myFolderList.indexOf(path);
			if (existing == -1) {
				if (index < myFolderList.size()) {
					myFolderList.set(index, path);
				} else {
					myFolderList.add(path);
				}
				((DirectoriesAdapter)getListAdapter()).notifyDataSetChanged();
			} else if (existing != index) {
				UIMessageUtil.showMessageText(
					this, myResource.getResource("duplicate").getValue().replace("%s", path)
				);
			}
		}
	}

	private void showItemRemoveDialog(final int index) {
		final ZLResource resource = myResource.getResource("removeDialog");
		new MDAlertDialogBuilder(FolderListDialogActivity.this)
			.setCancelable(false)
			.setTitle(resource.getValue())
			.setMessage(resource.getResource("message").getValue().replace("%s", myFolderList.get(index)))
			.setPositiveButton(myButtonResource.getResource("yes").getValue(), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					myFolderList.remove(index);
					((DirectoriesAdapter)getListAdapter()).notifyDataSetChanged();
				}
			})
			.create().show();
	}

	private class DirectoriesAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
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
			return myFolderList.size() + 1;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public String getItem(int position) {
			return position < myFolderList.size() ? myFolderList.get(position) : null;
		}

		@Override
		public View getView(final int position, View view, ViewGroup parent) {
			final String dir = getItem(position);
			if (view == null) {
				final int id = dir != null
					? R.layout.editable_list_item : R.layout.editable_list_add_item;
				view = getLayoutInflater().inflate(id, parent, false);
			}

			if (dir != null) {
				ViewUtil.setSubviewText(view, R.id.editable_list_item_title, dir);
				final ImageView deleteButton =
					ViewUtil.findImageView(view, R.id.editable_list_item_delete);
				deleteButton.setVisibility(myFolderList.size() > 1 ? View.VISIBLE : View.GONE);
				deleteButton.setImageDrawable(deleteIcon());
				deleteButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(final View v) {
						showItemRemoveDialog(position);
					}
				});
			} else {
				ViewUtil.findImageView(view, R.id.editable_list_add_item_icon)
					.setImageDrawable(DrawableUtil.tintedDrawable(
						FolderListDialogActivity.this,
						R.drawable.ic_button_add,
						R.color.text_primary
					));
				ViewUtil.setSubviewText(
					view,
					R.id.editable_list_add_item_text,
					myResource.getResource("addFolder").getValue()
				);
			}

			return view;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			FileChooserUtil.runDirectoryChooser(
				FolderListDialogActivity.this,
				position,
				myChooserTitle,
				position < myFolderList.size() ? myFolderList.get(position) : "/",
				myChooseWritableDirectoriesOnly
			);
		}
	}
}
