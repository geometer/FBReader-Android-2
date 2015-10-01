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

package org.geometerplus.android.util;

import java.util.List;
import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.fbreader.common.R;
import org.fbreader.md.MDAlertDialogBuilder;

public abstract class ContextMenuDialog extends BaseAdapter implements DialogInterface.OnClickListener {
	public static class Item {
		final int Id;
		final String Title;

		Item(int id, String title) {
			Id = id;
			Title = title;
		}
	}

	private final List<Item> myItems = new ArrayList<Item>();

	public final void addItem(int id, String title) {
		myItems.add(new Item(id, title));
	}

	public final void addItem(int id, ZLResource resource, String resourceId) {
		addItem(id, resource.getResource(resourceId).getValue());
	}

	protected String getTitle() {
		return null;
	}

	protected abstract void onItemClick(long itemId);

	public final void show(Context context) {
		new MDAlertDialogBuilder(context)
			.setTitle(getTitle())
			.setAdapter(this, this)
			.create().show();
	}

	@Override
	public final int getCount() {
		return myItems.size();
	}

	@Override
	public final Item getItem(int position) {
		return myItems.get(position);
	}

	@Override
	public final long getItemId(int position) {
		return getItem(position).Id;
	}

	@Override
	public final View getView(int position, View convertView, final ViewGroup parent) {
		final View view = convertView != null
			? convertView
			: LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item, parent, false);
		final TextView titleView = (TextView)view.findViewById(R.id.menu_item_title);
		titleView.setText(getItem(position).Title);
		return view;
	}

	@Override
	public final void onClick(DialogInterface dialog, int which) {
		onItemClick(getItemId(which));
	}
}
