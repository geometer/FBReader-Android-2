package org.geometerplus.fbreader.plugin.base.document;

import java.io.*;
import java.util.*;

import android.graphics.*;

import org.fbreader.util.IOUtil;
import org.fbreader.util.NaturalOrderComparator;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import org.geometerplus.zlibrary.core.util.SystemInfo;

public class RarArchive extends OneLevelImageArchive {
	private final SystemInfo mySystemInfo;
	private Archive myFile;
	private final HashMap<String, FileHeader> myFileHeaders = new HashMap<String, FileHeader>();

	public RarArchive(File f, SystemInfo info) throws IOException, RarException {
		mySystemInfo = info;
		myFile = new Archive(f);
		List<FileHeader> list =  myFile.getFileHeaders();
		for (FileHeader fh : list) {
			if (!fh.isDirectory() && isImage(fh.getFileNameString())) {
				myContents.add(fh.getFileNameString());
				myFileHeaders.put(fh.getFileNameString(), fh);
			}
		}
		Collections.sort(myContents, new NaturalOrderComparator());
	}

	// use in synchronized(myFile) block only
	private final InputStream getInputStream(int pageNo) throws IOException {
		if (pageNo < 0 || pageNo >= myContents.size()) {
			return null;
		}
		final FileHeader fh = myFileHeaders.get(myContents.get(pageNo));
		return myFile.getInputStream(fh, CBFileCacheUtil.pageFile(mySystemInfo, pageNo));
	}

	@Override
	protected BitmapFactory.Options getPageOptions(int pageNo) throws IOException {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		synchronized (myFile) {
			final InputStream is = getInputStream(pageNo);
			if (is == null) {
				return null;
			}
			try {
				BitmapFactory.decodeStream(is, null, options);
			} finally {
				IOUtil.closeQuietly(is);
			}
		}
		return options;
	}

	@Override
	protected Bitmap getPageInternal(int pageNo, int scaleFactor) throws IOException {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = scaleFactor;
		synchronized (myFile) {
			final InputStream is = getInputStream(pageNo);
			if (is == null) {
				return null;
			}
			try {
				return BitmapFactory.decodeStream(is, null, options);
			} finally {
				IOUtil.closeQuietly(is);
			}
		}
	}

	@Override
	public void close() {
		IOUtil.closeQuietly(myFile);
	}

	@Override
	public Bitmap getCover(int maxWidth, int maxHeight) throws IOException {
		synchronized (myFile) {
			if (myContents.isEmpty()) {
				return null;
			}

			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			final FileHeader fh = myFileHeaders.get(myContents.get(0));
			final byte[] data = myFile.getByteArray(fh);
			InputStream is = null;
			try {
				is = new ByteArrayInputStream(data);
				BitmapFactory.decodeStream(is, null, options);
			} finally {
				IOUtil.closeQuietly(is);
				is = null;
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
				is = new ByteArrayInputStream(data);
				return BitmapFactory.decodeStream(is, null, options);
			} finally {
				IOUtil.closeQuietly(is);
			}
		}
	}
}
