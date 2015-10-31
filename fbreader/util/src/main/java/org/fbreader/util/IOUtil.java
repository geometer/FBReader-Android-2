package org.fbreader.util;

import java.io.*;

public class IOUtil {
	public static File copyToDir(File file, File dstDir) {
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			final File copy = new File(dstDir, file.getName());
			return copyToFile(is, copy) ? copy : null;
		} catch (IOException e) {
			return null;
		} finally {
			closeQuietly(is);
		}
	}

	public static boolean copyToFile(InputStream is, String to) {
		return copyToFile(is, new File(to));
	}

	public static boolean copyToFile(InputStream is, File to) {
		OutputStream os = null;
		try {
			os = new FileOutputStream(to);
			final byte[] buffer = new byte[8192];
			while (true) {
				final int len = is.read(buffer);
				if (len <= 0) {
					break;
				}
				os.write(buffer, 0, len);
			}
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			closeQuietly(os);
		}
	}

	public static void closeQuietly(Closeable c) {
		try {
			c.close();
		} catch (Throwable t) {
			// ignore
		}
	}
}
