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

package org.geometerplus.fbreader.plugin.base.tree;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;

import org.fbreader.common.android.FBActivity;
import org.fbreader.common.android.FBReaderUtil;
import org.fbreader.plugin.format.base.R;
import org.fbreader.reader.TOCTree;
import org.fbreader.reader.TOCAdapterBase;
import org.fbreader.reader.android.ContextMenuDialog;
import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

public class TOCActivity extends FBActivity {
	private TOCAdapter myAdapter;
	private TOCTree mySelectedItem;

	// see TODO comment in onCreate
	private PluginView myPluginView;

	@Override
	protected int layoutId() {
		return R.layout.toc;
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		// TODO: send data in intent instead
		final ViewHolder holder = ViewHolder.getInstance();
		myPluginView = holder.getView();

		FBReaderUtil.setBookTitle(this, holder.getCurrentBook());

		final TOCTree root = myPluginView.getTOCTree();
		myAdapter = new TOCAdapter((ListView)findViewById(R.id.toc_list), root);
		TOCTree treeToSelect = myPluginView.getCurrentTOCElement();
		myAdapter.selectItem(treeToSelect);
		mySelectedItem = treeToSelect;
	}

	private static final int PROCESS_TREE_ITEM_ID = 0;
	private static final int READ_BOOK_ITEM_ID = 1;

	private final class TOCAdapter extends TOCAdapterBase {
		TOCAdapter(ListView view, TOCTree root) {
			super(view, root);
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			final TOCTree tree = getItem(position);
			if (!tree.hasChildren()) {
				return false;
			}

			final ContextMenuDialog dialog = new ContextMenuDialog() {
				@Override
				protected String getTitle() {
					return tree.Text;
				}

				@Override
				protected void onItemClick(long itemId) {
					switch ((int)itemId) {
						case PROCESS_TREE_ITEM_ID:
							myAdapter.runTreeItem(tree);
							break;
						case READ_BOOK_ITEM_ID:
							myAdapter.openBookText(tree);
							break;
					}
				}
			};

			final ZLResource resource = ZLResource.resource("tocView");
			dialog.addItem(
				PROCESS_TREE_ITEM_ID,
				resource,
				isOpen(tree) ? "collapseTree" : "expandTree"
			);
			if (tree.Reference != null && tree.Reference != -1) {
				dialog.addItem(READ_BOOK_ITEM_ID, resource, "readText");
			}
			dialog.show(TOCActivity.this);
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View view = (convertView != null) ? convertView :
				LayoutInflater.from(parent.getContext()).inflate(R.layout.toc_tree_item, parent, false);
			final TOCTree tree = getItem(position);
			view.setBackgroundColor(tree == mySelectedItem ? 0xff808080 : 0);
			setIcon(ViewUtil.findImageView(view, R.id.toc_tree_item_icon), tree);
			ViewUtil.setSubviewText(view, R.id.toc_tree_item_text, tree.Text);
			final int pageNo = tree.Reference != null ? tree.Reference : -1;
			ViewUtil.setSubviewText(
				view,
				R.id.toc_tree_item_pageno,
				pageNo != -1 ? String.valueOf(pageNo + 1) : ""
			);
			return view;
		}

		void openBookText(TOCTree tree) {
			if (tree.Reference != null && tree.Reference != -1) {
				finish();
				myPluginView.gotoPage(tree.Reference, false);
			}
		}

		@Override
		protected boolean runTreeItem(TOCTree tree) {
			if (super.runTreeItem(tree)) {
				return true;
			}
			openBookText(tree);
			return true;
		}
	}
}
