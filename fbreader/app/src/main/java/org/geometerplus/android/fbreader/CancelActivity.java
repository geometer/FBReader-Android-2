/*
 * Copyright (C) 2010-2017 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader;

import java.util.List;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import android.view.*;

import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.core.options.Config;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public class CancelActivity extends ListActivity {
	public static String ACTIONS_KEY = "fbreader.cancel.actions";

	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private final List<CancelMenuHelper.ActionDescription> myActions =
		new ArrayList<CancelMenuHelper.ActionDescription>();

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		myActions.clear();
		final Intent intent = getIntent();
		final List<CancelMenuHelper.ActionDescription> actions = intent != null
			? (List<CancelMenuHelper.ActionDescription>)intent.getSerializableExtra(ACTIONS_KEY)
			: null;
		if (actions != null) {
			myActions.addAll(actions);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!myActions.isEmpty()) {
			final ActionListAdapter adapter = new ActionListAdapter(myActions);
			setListAdapter(adapter);
			getListView().setOnItemClickListener(adapter);
		} else {
			myCollection.bindToService(this, new Runnable() {
				public void run() {
					myActions.addAll(new CancelMenuHelper().getActionsList(myCollection));
					final ActionListAdapter adapter = new ActionListAdapter(myActions);
					setListAdapter(adapter);
					getListView().setOnItemClickListener(adapter);
				}
			});
		}
	}

	@Override
	protected void onDestroy() {
		myCollection.unbind();
		super.onDestroy();
	}

	private class ActionListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
		private final List<CancelMenuHelper.ActionDescription> myActions;

		ActionListAdapter(List<CancelMenuHelper.ActionDescription> actions) {
			myActions = new ArrayList<CancelMenuHelper.ActionDescription>(actions);
		}

		public final int getCount() {
			return myActions.size();
		}

		public final CancelMenuHelper.ActionDescription getItem(int position) {
			return myActions.get(position);
		}

		public final long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, final ViewGroup parent) {
			final View view = convertView != null
				? convertView
				: LayoutInflater.from(parent.getContext()).inflate(R.layout.cancel_item, parent, false);
			final CancelMenuHelper.ActionDescription item = getItem(position);
			final TextView titleView = ViewUtil.findTextView(view, R.id.cancel_item_title);
			final TextView summaryView = ViewUtil.findTextView(view, R.id.cancel_item_summary);
			final String title = item.Title;
			final String summary = item.Summary;
			titleView.setText(title);
			if (summary != null) {
				summaryView.setVisibility(View.VISIBLE);
				summaryView.setText(summary);
				titleView.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				));
			} else {
				summaryView.setVisibility(View.GONE);
				titleView.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT
				));
			}
			return view;
		}

		public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final Intent data = new Intent();
			final CancelMenuHelper.ActionDescription item = getItem(position);
			data.putExtra(FBReaderIntents.Key.TYPE, item.Type.name());
			if (item instanceof CancelMenuHelper.BookmarkDescription) {
				FBReaderIntents.putBookmarkExtra(
					data, ((CancelMenuHelper.BookmarkDescription)item).getBookmark()
				);
			}
			setResult(RESULT_FIRST_USER, data);
			finish();
		}
	}
}
