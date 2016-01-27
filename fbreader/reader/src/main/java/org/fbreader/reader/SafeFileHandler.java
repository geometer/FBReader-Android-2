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

package org.fbreader.reader;

import java.io.*;
import java.util.HashMap;

public class SafeFileHandler {
	public final String Dir;
	private final HashMap<String,byte[]> myContents = new HashMap<String,byte[]>();

	public SafeFileHandler(String dir) {
		Dir = dir;
	}

	public void setContent(String name, byte[] content) {
		myContents.put(name, content);
	}

	public int fileSize(String name) {
		final byte[] content = myContents.get(name);
		if (content != null) {
			return content.length;
		} else {
			return (int)new File(Dir + "/" + name).length();
		}
	}

	public Reader blockReader(String name) throws IOException {
		final byte[] content = myContents.get(name);
		final InputStream is;
		if (content != null) {
			is = new ByteArrayInputStream(content);
		} else {
			is = new FileInputStream(Dir + "/" + name);
		}
		return new InputStreamReader(is, "UTF-16LE");
	}
}
