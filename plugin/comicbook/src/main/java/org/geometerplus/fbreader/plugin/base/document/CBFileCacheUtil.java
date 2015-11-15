package org.geometerplus.fbreader.plugin.base.document;

import java.io.File;

import org.geometerplus.zlibrary.core.util.SystemInfo;

public abstract class CBFileCacheUtil {
	static void clearCache(SystemInfo systemInfo) {
		final File cacheDir = cacheDirectory(systemInfo);
		if (!cacheDir.exists()) {
			return;
		}
		if (cacheDir.isDirectory()) {
			final File[] children = cacheDir.listFiles();
			if (children != null) {
				for (File c : children) {
					c.delete();
				}
			}
		} else {
			cacheDir.delete();
		}
	}

	static File pageFile(SystemInfo systemInfo, int pageNo) {
		final File cacheDir = cacheDirectory(systemInfo);
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		return new File(cacheDir, "page" + pageNo);
	}

	private static File cacheDirectory(SystemInfo systemInfo) {
		return new File(systemInfo.tempDirectory() + "/comics");
	}
}
