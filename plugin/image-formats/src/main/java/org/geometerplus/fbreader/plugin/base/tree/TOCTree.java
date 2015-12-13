/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
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

import org.fbreader.reader.TOCTreeBase;

import android.os.Bundle;
import android.util.Log;

public class TOCTree extends TOCTreeBase<TOCTree> {
	private Reference myReference;

	public TOCTree() {
		this(null, null);
	}

	public TOCTree(TOCTree parent, String text) {
		super(parent, text);
	}

	public Reference getReference() {
		return myReference;
	}

	public void setReference(int reference) {
		myReference = new Reference(reference);
	}

	public static class Reference {
		public final int PageNum;

		public Reference(final int pageNum) {
			PageNum = pageNum;
		}
	}
	
	public static final String TREE_KEY = "TrEe";
	public static final String TEXT_KEY = "TeXt";
	public static final String POSITION_KEY = "PoSiTiOn";
	public static final String NEXT_KEY = "NeXt";
	public static final String CHILD_KEY = "ChIlD";
	
	public static void writeToBundle(Bundle b, TOCTree t) {
		if (t != null) {
			b.putInt(TREE_KEY + "root", t.hashCode());
			writeToBundle(b, t, null);
		}
	}
	
	private static void writeToBundle(Bundle b, TOCTree t, TOCTree next) {
		String key = TREE_KEY + t.hashCode() + "/";
		Log.e("BUNDLE", "writing: " + key);
		Log.e("BUNDLE", "title: " + ((t.Text != null) ? t.Text : "null"));
		b.putInt(key + POSITION_KEY, t.myReference != null ? t.myReference.PageNum : -1);
		b.putString(key + TEXT_KEY, t.Text != null ? t.Text : "null");
		if (next != null) {
			b.putInt(key + NEXT_KEY, next.hashCode());
			Log.e("BUNDLE", "nextkey: " + key + NEXT_KEY);
			Log.e("BUNDLE", "nextkey: " + next.hashCode());
		}
		if (t.hasChildren()) {
			b.putInt(key + CHILD_KEY, t.subtrees().get(0).hashCode());
			for (int i = 0; i < t.subtrees().size(); ++i) {
				writeToBundle(b, t.subtrees().get(i), i < (t.subtrees().size() - 1) ? t.subtrees().get(i + 1) : null);
			}
		}
	}
	
	public static TOCTree readFromBundle(Bundle b) {
		TOCTree root = new TOCTree();
		int hashcode = b.getInt(TREE_KEY + "root");
		readFromBundle(b, hashcode, root);
		return root.subtrees().get(0);
	}
	
	private static void readFromBundle(Bundle b, int hashcode, TOCTree parent) {
		String key = TREE_KEY + hashcode + "/";
		Log.e("BUNDLE", "reading: " + key);
		final TOCTree t = new TOCTree(parent, b.getString(key + TEXT_KEY));
		Log.e("BUNDLE", "title: " + (t.Text != null ? t.Text : "null"));
		t.setReference(b.getInt(key + POSITION_KEY));
		String nkey = key + NEXT_KEY;
		boolean hasNext = b.containsKey(nkey);
		while (hasNext) {
			readFromBundle(b, b.getInt(nkey), parent);
			String tkey = TREE_KEY + b.getInt(nkey) + "/";
			nkey = tkey + NEXT_KEY;
			hasNext = b.containsKey(nkey + NEXT_KEY);
		}
		boolean hasChild = b.containsKey(key + CHILD_KEY);
		if (hasChild) {
			readFromBundle(b, b.getInt(key + CHILD_KEY), t);
		}
	}
	
	
}
