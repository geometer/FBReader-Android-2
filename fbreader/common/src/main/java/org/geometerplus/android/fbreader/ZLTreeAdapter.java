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

package org.geometerplus.android.fbreader;

import java.util.HashSet;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import org.fbreader.util.android.DrawableUtil;

import org.geometerplus.zlibrary.core.tree.ZLTree;

import org.fbreader.common.R;

public abstract class ZLTreeAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
	private final ListView myParent;
	private final ZLTree<?> Root;
	private ZLTree<?>[] myItems;
	private final HashSet<ZLTree<?>> myOpenItems = new HashSet<ZLTree<?>>();

	protected ZLTreeAdapter(ListView parent, ZLTree<?> root) {
		myParent = parent;
		Root = root;
		myItems = new ZLTree[root.getSize() - 1];
		myOpenItems.add(root);

		parent.setAdapter(this);
		parent.setOnItemClickListener(this);
		parent.setOnItemLongClickListener(this);
	}

	protected final void openTree(ZLTree<?> tree) {
		if (tree == null) {
			return;
		}
		while (!myOpenItems.contains(tree)) {
			myOpenItems.add(tree);
			tree = tree.Parent;
		}
	}

	public final void expandOrCollapseTree(ZLTree<?> tree) {
		if (!tree.hasChildren()) {
			return;
		}
		if (isOpen(tree)) {
			myOpenItems.remove(tree);
		} else {
			myOpenItems.add(tree);
		}
		//myParent.invalidateViews();
		//myParent.requestLayout();
		notifyDataSetChanged();
	}

	public final boolean isOpen(ZLTree<?> tree) {
		return myOpenItems.contains(tree);
	}

	public final void selectItem(ZLTree<?> tree) {
		if (tree == null) {
			return;
		}
		openTree(tree.Parent);
		int index = 0;
		while (true) {
			ZLTree<?> parent = tree.Parent;
			if (parent == null) {
				break;
			}
			for (ZLTree<?> sibling : parent.subtrees()) {
				if (sibling == tree) {
					break;
				}
				index += getCount(sibling);
			}
			tree = parent;
			++index;
		}
		if (index > 0) {
			myParent.setSelection(index - 1);
		}
		myParent.invalidateViews();
	}

	private int getCount(ZLTree<?> tree) {
		int count = 1;
		if (isOpen(tree)) {
			for (ZLTree<?> subtree : tree.subtrees()) {
				count += getCount(subtree);
			}
		}
		return count;
	}

	public final int getCount() {
		return getCount(Root) - 1;
	}

	private final int indexByPosition(int position, ZLTree<?> tree) {
		if (position == 0) {
			return 0;
		}
		--position;
		int index = 1;
		for (ZLTree<?> subtree : tree.subtrees()) {
			int count = getCount(subtree);
			if (count <= position) {
				position -= count;
				index += subtree.getSize();
			} else {
				return index + indexByPosition(position, subtree);
			}
		}
		throw new RuntimeException("That's impossible!!!");
	}

	public final ZLTree<?> getItem(int position) {
		final int index = indexByPosition(position + 1, Root) - 1;
		ZLTree<?> item = myItems[index];
		if (item == null) {
			item = Root.getTreeByParagraphNumber(index + 1);
			myItems[index] = item;
		}
		return item;
	}

	public final boolean areAllItemsEnabled() {
		return true;
	}

	public final boolean isEnabled(int position) {
		return true;
	}

	public final long getItemId(int position) {
		return indexByPosition(position + 1, Root);
	}

	protected boolean runTreeItem(ZLTree<?> tree) {
		if (!tree.hasChildren()) {
			return false;
		}
		expandOrCollapseTree(tree);
		return true;
	}

	public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		runTreeItem(getItem(position));
	}

	protected final void setIcon(ImageView imageView, ZLTree<?> tree) {
		final Context context = myParent.getContext();
		if (tree.hasChildren()) {
			imageView.setImageDrawable(DrawableUtil.tintedDrawable(
				context,
				isOpen(tree) ? R.drawable.ic_button_minus_small : R.drawable.ic_button_plus_small,
				R.color.text_primary
			));
		} else {
			imageView.setImageDrawable(null);
		}
		final DisplayMetrics dm = context.getResources().getDisplayMetrics();
		final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, dm),
			RelativeLayout.LayoutParams.MATCH_PARENT
		);
		params.setMargins(
			(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15 * (tree.Level - 1), dm),
			0, 0, 0
		);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		imageView.setLayoutParams(params);
	}
}
