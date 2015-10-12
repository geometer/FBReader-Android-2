package org.fbreader.plugin.library;

import java.io.*;
import java.util.*;

import org.amse.ys.zip.LocalFileHeader;
import org.amse.ys.zip.ZipFile;

abstract class ZipUtil {
	static List<String> entries(File zipArchive) {
		final Collection<LocalFileHeader> headers = new ZipFile(zipArchive).headers();
		final List<String> result = new ArrayList<String>(headers.size());
		for (LocalFileHeader h : headers) {
			if (!h.FileName.endsWith("/")) {
				result.add(h.FileName);
			}
		}
		return result;
	}
}
