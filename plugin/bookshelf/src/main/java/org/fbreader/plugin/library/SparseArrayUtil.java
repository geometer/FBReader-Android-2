package org.fbreader.plugin.library;

import android.util.SparseArray;

public abstract class SparseArrayUtil {
	static <T> SparseArray<T> empty() {
		return new SparseArray<T>();
	}

	static <T> SparseArray<T> singleton(int index, T value) {
		final SparseArray<T> array = new SparseArray<T>();
		array.append(index, value);
		return array;
	}
}
