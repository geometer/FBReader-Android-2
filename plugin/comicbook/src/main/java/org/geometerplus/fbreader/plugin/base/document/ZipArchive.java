package org.geometerplus.fbreader.plugin.base.document;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.fbreader.util.IOUtil;
import org.fbreader.util.NaturalOrderComparator;

import android.graphics.*;

public class ZipArchive extends OneLevelImageArchive {
	private final ZipFile myFile;

	public ZipArchive(File f) throws ZipException, IOException {
		myFile = new ZipFile(f);
		Enumeration<? extends ZipEntry> entries = myFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry e = entries.nextElement();
			if ((!e.isDirectory()) && isImage(e.getName())) {
				myContents.add(e.getName());
			}
		}
		Collections.sort(myContents, new NaturalOrderComparator());
	}

	private InputStream getInputStream(int pageNo) throws IOException {
		if (pageNo < 0 || pageNo >= myContents.size()) {
			return null;
		}
		return getInputStream(myContents.get(pageNo));
	}

	private InputStream getInputStream(String name) throws IOException {
		return myFile.getInputStream(myFile.getEntry(name));
	}

	@Override
	protected BitmapFactory.Options getPageOptions(int pageNo) throws IOException {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		InputStream is = null;
		try {
			is = getInputStream(pageNo);
			if (is == null) {
				return null;
			}
			BitmapFactory.decodeStream(is, null, options);
		} finally {
			IOUtil.closeQuietly(is);
		}
		return options;
	}

	@Override
	protected Bitmap getPageInternal(int pageNo, int scaleFactor) throws IOException {
		InputStream is = null;
		try {
			is = getInputStream(pageNo);
			if (is == null) {
				return null;
			}
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = scaleFactor;
			return BitmapFactory.decodeStream(is, null, options);
		} finally {
			IOUtil.closeQuietly(is);
		}
	}

	@Override
	public Bitmap getCover(int maxWidth, int maxHeight) throws IOException {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		InputStream is = null;
		try {
			is = getInputStream(0);
			if (is == null) {
				return null;
			}
			BitmapFactory.decodeStream(is, null, options);
		} finally {
			IOUtil.closeQuietly(is);
		}
		int width = options.outWidth;
		int height = options.outHeight;
		options.inJustDecodeBounds = false;
		options.inSampleSize = 1;
		while (height > maxHeight || width > maxWidth) {
			height >>= 1;
			width >>= 1;
			options.inSampleSize <<= 1;
		}
		try {
			is = getInputStream(0);
			if (is == null) {
				return null;
			}
			return BitmapFactory.decodeStream(is, null, options);
		} finally {
			IOUtil.closeQuietly(is);
		}
	}

	@Override
	public void close() {
		try {
			myFile.close();
		} catch (Throwable t) {
			// ignore
		}
	}
}
