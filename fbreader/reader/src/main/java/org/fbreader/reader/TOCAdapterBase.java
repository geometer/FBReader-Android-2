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

package org.fbreader.reader;

import java.util.HashSet;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import org.fbreader.util.android.DrawableUtil;

public abstract class TOCAdapterBase extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
	private final ListView myParent;
	private final TOCTreeBase<?> Root;
	private TOCTreeBase<?>[] myItems;
	private final HashSet<TOCTreeBase<?>> myOpenItems = new HashSet<TOCTreeBase<?>>();

	protected TOCAdapterBase(ListView parent, TOCTreeBase<?> root) {
		myParent = parent;
		Root = root;
		myItems = new TOCTreeBase[root.getSize() - 1];
		myOpenItems.add(root);

		parent.setAdapter(this);
		parent.setOnItemClickListener(this);
		parent.setOnItemLongClickListener(this);
	}

	protected final void openTree(TOCTreeBase<?> tree) {
		if (tree == null) {
			return;
		}
		while (!myOpenItems.contains(tree)) {
			myOpenItems.add(tree);
			tree = tree.Parent;
		}
	}

	public final void expandOrCollapseTree(TOCTreeBase<?> tree) {
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

	public final boolean isOpen(TOCTreeBase<?> tree) {
		return myOpenItems.contains(tree);
	}

	public final void selectItem(TOCTreeBase<?> tree) {
		if (tree == null) {
			return;
		}
		openTree(tree.Parent);
		int index = 0;
		while (true) {
			TOCTreeBase<?> parent = tree.Parent;
			if (parent == null) {
				break;
			}
			for (TOCTreeBase<?> sibling : parent.subtrees()) {
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

	private int getCount(TOCTreeBase<?> tree) {
		int count = 1;
		if (isOpen(tree)) {
			for (TOCTreeBase<?> subtree : tree.subtrees()) {
				count += getCount(subtree);
			}
		}
		return count;
	}

	public final int getCount() {
		return getCount(Root) - 1;
	}

	private final int indexByPosition(int position, TOCTreeBase<?> tree) {
		if (position == 0) {
			return 0;
		}
		--position;
		int index = 1;
		for (TOCTreeBase<?> subtree : tree.subtrees()) {
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

	public final TOCTreeBase<?> getItem(int position) {
		final int index = indexByPosition(position + 1, Root) - 1;
		TOCTreeBase<?> item = myItems[index];
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

	protected boolean runTreeItem(TOCTreeBase<?> tree) {
		if (!tree.hasChildren()) {
			return false;
		}
		expandOrCollapseTree(tree);
		return true;
	}

	public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		runTreeItem(getItem(position));
	}

	protected final void setIcon(ImageView imageView, TOCTreeBase<?> tree) {
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
