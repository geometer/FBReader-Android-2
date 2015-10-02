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

package org.geometerplus.android.fbreader.preferences;

import java.util.ArrayList;

import android.content.*;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.android.util.ContextMenuDialog;

import org.fbreader.md.MDAlertDialogBuilder;
import org.fbreader.md.MDListActivity;

public abstract class EditListDialogActivity extends MDListActivity {
	public static final int REQ_CODE = 001;
	public interface Key {
		final String LIST					= "edit_list.list";
		final String ALL_ITEMS_LIST			= "edit_list.all_items_list";
		final String ACTIVITY_TITLE         = "edit_list.title";
	}

	protected ArrayList<String> myEditList;
	protected ZLResource myResource;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		myEditList = intent.getStringArrayListExtra(Key.LIST);
		setTitle(intent.getStringExtra(Key.ACTIVITY_TITLE));
		setResult(RESULT_CANCELED);
	}

	protected void parseUIElements() {
		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		final Button okButton = (Button)findViewById(R.id.edit_dialog_button_ok);
		if (okButton != null) {
			okButton.setText(buttonResource.getResource("ok").getValue());
			okButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					setResult(RESULT_OK, new Intent().putExtra(Key.LIST, myEditList));
					finish();
				}
			});
		}
		final Button cancelButton = (Button)findViewById(R.id.edit_dialog_button_cancel);
		if (cancelButton != null) {
			cancelButton.setText(buttonResource.getResource("cancel").getValue());
			cancelButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					setResult(RESULT_CANCELED);
					finish();
				}
			});
		}
	}

	protected void showItemRemoveDialog(final int index) {
		if(index < 0 || myResource == null)
			return;

		final ZLResource resource = myResource.getResource("removeDialog");
		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		new MDAlertDialogBuilder(EditListDialogActivity.this)
			.setCancelable(false)
			.setTitle(resource.getValue())
			.setMessage(resource.getResource("message").getValue().replace("%s", myEditList.get(index)))
			.setPositiveButton(buttonResource.getResource("yes").getValue(), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					myEditList.remove(index);
					if(getListAdapter() != null)
						((BaseAdapter)getListAdapter()).notifyDataSetChanged();
				}
			})
			.setNegativeButton(buttonResource.getResource("cancel").getValue(), null)
			.create().show();
	}

	protected void onClick(final int position) {
		final ContextMenuDialog dialog = new ContextMenuDialog() {
			@Override
			protected String getTitle() {
				return myEditList.get(position);
			}

			@Override
			protected void onItemClick(long itemId) {
				switch ((int)itemId) {
					case 0:
						editItem(position);
						break;
					case 1:
						deleteItem(position);
						break;
				}
			}
		};

		final ZLResource resource = ZLResource.resource("dialog").getResource("editList");
		dialog.addItem(0, resource, "edit");
		dialog.addItem(1, resource, "remove");

		dialog.show(this);
	}

	protected void onLongClick(int position) {
		//can be overriden in children
	}

	abstract protected void editItem(int position);

	protected void deleteItem(int position) {
		showItemRemoveDialog(position);
	}

	protected class EditListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener,AdapterView.OnItemLongClickListener {
		@Override
		public int getCount() {
			return myEditList.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public String getItem(int position) {
			return myEditList.get(position);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final View view = convertView != null
				? convertView
				: LayoutInflater.from(EditListDialogActivity.this).inflate(R.layout.edit_list_dialog_item, parent, false);

			((TextView)view.findViewById(R.id.edit_item_title)).setText(getItem(position));
			return view;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			onClick(position);
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			onLongClick(position);
			return true;
		}
	}
}
