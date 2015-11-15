package org.geometerplus.fbreader.plugin.base.document;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public abstract class OneLevelImageArchive {
	protected static boolean isImage(String name) {//FIXME
		return
			name.toLowerCase().endsWith(".jpg") ||
			name.toLowerCase().endsWith(".jpeg") ||
			name.toLowerCase().endsWith(".png") ||
			name.toLowerCase().endsWith(".gif") ||
			name.toLowerCase().endsWith(".bmp") ||
			name.toLowerCase().endsWith(".tif");
	}

	protected final ArrayList<String> myContents = new ArrayList<String>();

	protected abstract BitmapFactory.Options getPageOptions(int pageNo) throws IOException;
	public Bitmap getPage(int pageNo) throws IOException {
		for (int scale : new int[] { 1, 1, 2, 4 }) {
			try {
				return getPageInternal(pageNo, scale);
			} catch (OutOfMemoryError e) {
				System.gc();
				System.gc();
			}
		}
		throw new IOException("Cannot read page");
	}
	protected abstract Bitmap getPageInternal(int pageNo, int scaleFactor) throws IOException;
	public abstract Bitmap getCover(int maxWidth, int maxHeight) throws IOException;
	public abstract void close();

	public int size() {
		return myContents.size();
	}
}
