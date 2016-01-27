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

package org.geometerplus.zlibrary.text.model;

import java.lang.ref.WeakReference;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import android.os.StatFs;

import org.fbreader.reader.SafeFileHandler;
import org.fbreader.util.IOUtil;

public final class CachedCharStorage {
	protected final ArrayList<WeakReference<char[]>> myArray =
		new ArrayList<WeakReference<char[]>>();

	private final SafeFileHandler myFileHandler;
	private final String myFileExtension;

	public CachedCharStorage(SafeFileHandler handler, String fileExtension, int blocksNumber) {
		myFileHandler = handler;
		myFileExtension = '.' + fileExtension;
		myArray.addAll(Collections.nCopies(blocksNumber, new WeakReference<char[]>(null)));
	}

	private String fileName(int index) {
		return index + myFileExtension;
	}

	public int size() {
		return myArray.size();
	}

	private String exceptionMessage(int index, String extra) {
		final StringBuilder buffer = new StringBuilder("Cannot read " + myFileHandler.Dir + "/" + fileName(index));
		if (extra != null) {
			buffer.append("; ").append(extra);
		}
		buffer.append("\n");
		try {
			final File dir = new File(myFileHandler.Dir);
			buffer.append("ts = ").append(System.currentTimeMillis()).append("\n");
			buffer.append("dir exists = ").append(dir.exists()).append("\n");
			if (dir.exists()) {
				final StatFs stat = new StatFs(myFileHandler.Dir);
				buffer.append("blocks available = ").append(stat.getAvailableBlocks()).append("\n");
				buffer.append("block size = ").append(stat.getBlockSize()).append("\n");
				for (File f : dir.listFiles()) {
					buffer.append(f.getName()).append(" :: ");
					buffer.append(f.length()).append(" :: ");
					buffer.append(f.lastModified()).append("\n");
				}
			}
		} catch (Throwable t) {
			buffer.append(t.getClass().getName());
			buffer.append("\n");
			buffer.append(t.getMessage());
		}
		return buffer.toString();
	}

	public char[] block(int index) {
		if (index < 0 || index >= myArray.size()) {
			return null;
		}
		char[] block = myArray.get(index).get();
		if (block == null) {
			Reader reader = null;
			try {
				final String name = fileName(index);
				final int size = myFileHandler.fileSize(name);
				if (size < 0) {
					throw new CachedCharStorageException(exceptionMessage(index, "size = " + size));
				}
				block = new char[size / 2];
				reader = myFileHandler.blockReader(name);
				final int rd = reader.read(block);
				if (rd != block.length) {
					throw new CachedCharStorageException(exceptionMessage(index, rd + " != " + block.length));
				}
				reader.close();
			} catch (IOException e) {
				throw new CachedCharStorageException(exceptionMessage(index, null), e);
			} finally {
				IOUtil.closeQuietly(reader);
			}
			myArray.set(index, new WeakReference<char[]>(block));
		}
		return block;
	}
}
