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

import java.util.*;

import android.os.Bundle;
import android.util.Log;

public final class TOCTree implements Iterable<TOCTree> {
	private int mySize = 1;
	public final TOCTree Parent;
	public final int Level;
	private volatile List<TOCTree> mySubtrees;

	public final String Text;
	public final Integer Reference;

	public TOCTree() {
		this(null, null, null);
	}

	public TOCTree(TOCTree parent, String text, Integer reference) {
		Parent = parent;
		if (parent != null) {
			Level = parent.Level + 1;
			parent.addSubtree(this);
		} else {
			Level = 0;
		}
		Text = text != null ? trim(text) : null;
		Reference = reference;
	}

	public final int getSize() {
		return mySize;
	}

	public final boolean hasChildren() {
		return mySubtrees != null && !mySubtrees.isEmpty();
	}

	public List<TOCTree> subtrees() {
		if (mySubtrees == null) {
			return Collections.emptyList();
		}
		synchronized (mySubtrees) {
			return new ArrayList<TOCTree>(mySubtrees);
		}
	}

	public synchronized final TOCTree getTreeByParagraphNumber(int index) {
		if (index < 0 || index >= mySize) {
			// TODO: throw an exception?
			return null;
		}
		if (index == 0) {
			return this;
		}
		--index;
		if (mySubtrees != null) {
			synchronized (mySubtrees) {
				for (TOCTree subtree : mySubtrees) {
					if (subtree.mySize <= index) {
						index -= subtree.mySize;
					} else {
						return subtree.getTreeByParagraphNumber(index);
					}
				}
			}
		}
		throw new RuntimeException("That's impossible!!!");
	}

	synchronized final void addSubtree(TOCTree subtree) {
		if (mySubtrees == null) {
			mySubtrees = Collections.synchronizedList(new ArrayList<TOCTree>());
		}
		synchronized (mySubtrees) {
			mySubtrees.add(subtree);
			for (TOCTree parent = this; parent != null; parent = parent.Parent) {
				parent.mySize += 1;
			}
		}
	}

	public final TreeIterator iterator() {
		return new TreeIterator(Integer.MAX_VALUE);
	}

	public final Iterable<TOCTree> allSubtrees(final int maxLevel) {
		return new Iterable<TOCTree>() {
			public TreeIterator iterator() {
				return new TreeIterator(maxLevel);
			}
		};
	}

	private class TreeIterator implements Iterator<TOCTree> {
		private TOCTree myCurrentElement = TOCTree.this;
		private final LinkedList<Integer> myIndexStack = new LinkedList<Integer>();
		private final int myMaxLevel;

		TreeIterator(int maxLevel) {
			myMaxLevel = maxLevel;
		}

		public boolean hasNext() {
			return myCurrentElement != null;
		}

		public TOCTree next() {
			final TOCTree element = myCurrentElement;
			if (element.hasChildren() && element.Level < myMaxLevel) {
				myCurrentElement = element.mySubtrees.get(0);
				myIndexStack.add(0);
			} else {
				TOCTree parent = element;
				while (!myIndexStack.isEmpty()) {
					final int index = myIndexStack.removeLast() + 1;
					parent = parent.Parent;
					synchronized (parent.mySubtrees) {
						if (parent.mySubtrees.size() > index) {
							myCurrentElement = parent.mySubtrees.get(index);
							myIndexStack.add(index);
							break;
						}
					}
				}
				if (myIndexStack.isEmpty()) {
					myCurrentElement = null;
				}
			}
			return element;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	// faster replacement for
	// return text.trim().replaceAll("[\t ]+", " ");
	private static String trim(String text) {
		final char[] data = text.toCharArray();
		int count = 0;
		int shift = 0;
		boolean changed = false;
		char space = ' ';
		for (int i = 0; i < data.length; ++i) {
			final char ch = data[i];
			if (ch == ' ' || ch == '\t') {
				++count;
				space = ch;
			} else {
				if (count > 0) {
					if (count == i) {
						shift += count;
						changed = true;
					} else {
						shift += count - 1;
						if (shift > 0 || space == '\t') {
							data[i - shift - 1] = ' ';
							changed = true;
						}
					}
					count = 0;
				}
				if (shift > 0) {
					data[i - shift] = data[i];
				}
			}
		}
		if (count > 0) {
			changed = true;
			shift += count;
		}
		return changed ? new String(data, 0, data.length - shift) : text;
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
		b.putInt(key + POSITION_KEY, t.Reference != null ? t.Reference : -1);
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
		final TOCTree t = new TOCTree(parent, b.getString(key + TEXT_KEY), b.getInt(key + POSITION_KEY));
		Log.e("BUNDLE", "title: " + (t.Text != null ? t.Text : "null"));
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
