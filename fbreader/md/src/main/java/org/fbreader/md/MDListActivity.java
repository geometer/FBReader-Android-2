/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.fbreader.md;

import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class MDListActivity extends MDActivity {
	private ListView myListView;

	@Override
	protected int layoutId() {
		return R.layout.md_list_activity;
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		myListView = (ListView)findViewById(android.R.id.list);
	}

	public final void setListAdapter(ListAdapter adapter) {
		myListView.setAdapter(adapter);
		if (adapter instanceof ListView.OnItemClickListener) {
			myListView.setOnItemClickListener((ListView.OnItemClickListener)adapter);
		}
	}

	public final ListAdapter getListAdapter() {
		return myListView.getAdapter();
	}

	public final ListView getListView() {
		return myListView;
	}
}
