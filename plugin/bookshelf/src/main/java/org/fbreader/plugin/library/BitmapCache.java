/*
 * Copyright (C) 2010-2014 Geometer Plus <contact@geometerplus.com>
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

package org.fbreader.plugin.library;

import java.util.*;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.SparseArray;

class BitmapCache {
	static class Container {
		final Bitmap Bitmap;

		private Container(Bitmap bitmap) {
			Bitmap = bitmap;
		}

		@TargetApi(Build.VERSION_CODES.KITKAT)
		private int sizeKitkat() {
			return Bitmap.getAllocationByteCount();
		}

		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
		private int sizeHoneycombMR1() {
			return Bitmap.getByteCount();
		}

		int size() {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				return sizeKitkat();
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
				return sizeHoneycombMR1();
			} else {
				return Bitmap.getRowBytes() * Bitmap.getHeight();
			}
		}
	}

	private static final Container NULL = new Container(null) {
		@Override
		int size() {
			return 1;
		}
	};

	private final LruCache<Long,Container> myLruCache;
	private final Set<Long> myNulls = Collections.synchronizedSet(new HashSet<Long>());
	private final SparseArray<Drawable> myResourceDrawableCache = new SparseArray<Drawable>();

	BitmapCache(float factor) {
		myLruCache = new LruCache<Long,Container>((int)(factor * Runtime.getRuntime().maxMemory())) {
			@Override
			protected int sizeOf(Long key, Container container) {
				return container.size();
			}
		};
	}

	Container get(Long key) {
		if (myNulls.contains(key)) {
			return NULL;
		}
		return myLruCache.get(key);
	}

	void put(Long key, Bitmap bitmap) {
		if (bitmap != null) {
			myLruCache.put(key, new Container(bitmap));
		} else {
			myNulls.add(key);
		}
	}

	void removeNulls() {
		myNulls.clear();
	}

	Drawable getDrawable(int id) {
		synchronized(myResourceDrawableCache) {
			return myResourceDrawableCache.get(id);
		}
	}

	void putDrawable(int id, Drawable drawable) {
		synchronized(myResourceDrawableCache) {
			myResourceDrawableCache.put(id, drawable);
		}
	}
}
