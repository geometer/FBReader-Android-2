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

package org.fbreader.util.android;

import java.util.ArrayList;
import java.util.List;

import android.widget.SectionIndexer;

public class LinearIndexer implements SectionIndexer {
	private static class Section {
		final String Title;
		final int FirstIndex;

		Section(String title, int firstIndex) {
			Title = title;
			FirstIndex = firstIndex;
		}

		@Override
		public String toString() {
			return Title;
		}
	}

	private final List<Section> mySections = new ArrayList<Section>();
	private int myCount;
	private volatile Object[] myCache;

	public synchronized void reset() {
		mySections.clear();
		myCount = 0;
		myCache = null;
	}

	public synchronized void addElement(String title) {
		if (mySections.isEmpty() || !title.equals(mySections.get(mySections.size() - 1).Title)) {
			mySections.add(new Section(title, myCount));
			myCache = null;
		}
		++myCount;
	}

	public int getPositionForSection(int sectionIndex) {
		try {
			return mySections.get(sectionIndex).FirstIndex;
		} catch (Exception e) {
			return 0;
		}
	}

	public int getSectionForPosition(int positionIndex) {
		try {
			for (int i = 0; i < mySections.size(); ++i) {
				if (mySections.get(i).FirstIndex <= positionIndex) {
					return i;
				}
			}
			return mySections.size() - 1;
		} catch (Exception e) {
			return 0;
		}
	}

	public synchronized Object[] getSections() {
		if (myCache == null) {
			myCache = mySections.toArray();
		}
		return myCache;
	}
}
