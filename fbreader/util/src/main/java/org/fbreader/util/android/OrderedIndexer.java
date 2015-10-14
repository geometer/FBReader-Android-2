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

import java.util.*;

import android.widget.SectionIndexer;

public class OrderedIndexer implements SectionIndexer {
	private static class Section implements Comparable<Section> {
		final String Title;
		int Count;

		Section(String title) {
			Title = title;
			Count = 1;
		}

		@Override
		public String toString() {
			return Title;
		}

		@Override
		public int compareTo(Section section) {
			return Title.compareTo(section.Title);
		}
	}

	private final List<Section> mySections = new ArrayList<Section>();
	private volatile Object[] myCache;

	public synchronized void reset() {
		mySections.clear();
		myCache = null;
	}

	public synchronized void addLabel(String title) {
		final Section section = new Section(title);
		final int index = Collections.binarySearch(mySections, section);
		if (index >= 0) {
			mySections.get(index).Count += 1;
		} else {
			mySections.add(- index - 1, section);
			myCache = null;
		}
	}

	public synchronized void removeLabel(String title) {
		final Section section = new Section(title);
		final int index = Collections.binarySearch(mySections, section);
		if (index >= 0) {
			final Section existing = mySections.get(index);
			if (existing.Count > 1) {
				existing.Count -= 1;
			} else {
				mySections.remove(index);
				myCache = null;
			}
		}
	}

	public synchronized int getPositionForSection(int sectionIndex) {
		int position = 0;
		int index = 0;
		for (Section section : mySections) {
			index += 1;
			if (index > sectionIndex) {
				break;
			}
			position += section.Count;
		}
		return position;
	}

	public int getSectionForPosition(int positionIndex) {
		int position = 0;
		int index = 0;
		for (Section section : mySections) {
			position += section.Count;
			if (position > positionIndex) {
				break;
			}
			index += 1;
		}
		return index;
	}

	public synchronized Object[] getSections() {
		if (myCache == null) {
			myCache = mySections.toArray();
		}
		return myCache;
	}
}
