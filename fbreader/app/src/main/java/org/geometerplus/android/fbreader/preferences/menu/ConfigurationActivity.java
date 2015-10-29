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

package org.geometerplus.android.fbreader.preferences.menu;

import java.util.*;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.mobeta.android.dslv.DragSortListView;

import org.fbreader.md.MDListActivity;
import org.fbreader.util.android.DrawableUtil;
import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.android.fbreader.MenuData;
import org.geometerplus.android.fbreader.api.MenuNode;

public class ConfigurationActivity extends MDListActivity {
	private static final ZLResource Resource = ZLResource.resource("Preferences").getResource("menu");

	private final List<Item> myAllItems = new ArrayList<Item>();

	@Override
	protected int layoutId() {
		return R.layout.menu_configure_view;
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setTitle(Resource.getValue());
	}

	@Override
	protected void onStart() {
		super.onStart();

		myAllItems.clear();

		final Set<String> usedIds = new HashSet<String>();
		for (MenuData.Location location : MenuData.Location.values()) {
			myAllItems.add(new SectionItem(location));
			for (MenuNode node : MenuData.topLevelNodes(location)) {
				final String id = MenuData.code(node);
				if (usedIds.contains(id)) {
					continue;
				}
				usedIds.add(id);
				myAllItems.add(new MenuNodeItem(location, node, id));
			}
		}

		setListAdapter(new MenuListAdapter());
	}

	private static abstract class Item {
		private MenuData.Location Location;

		public Item(MenuData.Location location) {
			Location = location;
		}
	}

	private static class SectionItem extends Item {
		private final String Title;

		public SectionItem(MenuData.Location location) {
			super(location);
			Title = Resource.getResource(location.resourceKey()).getValue();
		}
	}

	private static class MenuNodeItem extends Item {
		private final MenuNode Node;
		private final String Id;

		public MenuNodeItem(MenuData.Location location, MenuNode node, String id) {
			super(location);
			Node = node;
			Id = id;
		}

		public String getTitle() {
			return ZLResource.resource("menu").getResource(Id).getValue();
		}
	}

	private class MenuListAdapter extends ArrayAdapter<Item> implements DragSortListView.DropListener, DragSortListView.RemoveListener {
		public MenuListAdapter() {
			super(ConfigurationActivity.this, R.layout.menu_configure_item, myAllItems);
		}

		private void saveChanges() {
			final HashMap<MenuData.Location,Integer> sizes =
				new HashMap<MenuData.Location,Integer>();
			for (int i = 0; i < getCount(); ++i) {
				final Item item = getItem(i);
				if (!(item instanceof MenuNodeItem)) {
					continue;
				}
				Integer index = sizes.get(item.Location);
				if (index == null) {
					index = 0;
				}
				sizes.put(item.Location, index + 1);
				MenuData.nodeOption(((MenuNodeItem)item).Id)
					.setValue(item.Location.StartIndex + index);
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position) instanceof SectionItem ? 0 : 1;
		}

		@Override
		public View getView(int position, View view, final ViewGroup parent) {
			final Item item = getItem(position);

			if (item instanceof SectionItem) {
				if (view == null) {
					view = getLayoutInflater().inflate(R.layout.menu_configure_section_head, null);
				}
				ViewUtil.setSubviewText(
					view, R.id.menu_configure_section_head_title, ((SectionItem)item).Title
				);
			} else /* if (item instanceof MenuNodeItem) */ {
				if (view == null) {
					view = getLayoutInflater().inflate(R.layout.menu_configure_item, null);
				}

				final MenuNodeItem menuItem = (MenuNodeItem)item;

				final TextView titleView =
					ViewUtil.findTextView(view, R.id.menu_configure_item_title);
				titleView.setText(menuItem.getTitle());

				final ImageView iconView =
					ViewUtil.findImageView(view, R.id.menu_configure_item_icon);
				iconView.setImageDrawable(DrawableUtil.tintedDrawable(
					ConfigurationActivity.this, menuItem.Node.IconId, R.color.text_primary
				));

				final ImageView dragIconView =
					ViewUtil.findImageView(view, R.id.menu_configure_item_drag_icon);
				dragIconView.setImageDrawable(DrawableUtil.tintedDrawable(
					ConfigurationActivity.this, R.drawable.ic_button_drag_large, R.color.text_primary
				));
			}
			return view;
		}

		// method from DragSortListView.DropListener
		public void drop(int from, int to) {
			to = Math.max(to, 1);
			if (from == to) {
				return;
			}

			final Item item = getItem(from);
			if (!(item instanceof MenuNodeItem)) {
				return;
			}
			final MenuNodeItem menuNodeItem = (MenuNodeItem)item;

			final Item toItem = getItem(from < to ? to : to - 1);
			if (!MenuData.locationGroup(menuNodeItem.Id).contains(toItem.Location)) {
				return;
			}

			remove(item);
			insert(item, to);
			item.Location = toItem.Location;
			((DragSortListView)getListView()).moveCheckState(from, to);
			saveChanges();
		}

		// method from DragSortListView.RemoveListener
		public void remove(int which) {
			final Item item = getItem(which);
			if (item instanceof MenuNodeItem) {
				remove(item);
				((DragSortListView)getListView()).removeCheckState(which);
			}
		}
	}
}
