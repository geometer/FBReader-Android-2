package org.fbreader.util;

import java.io.*;
import java.util.Date;

public class IOUtil {
	public static void log2File(String filename, String text) {
		if (filename == null) {
			return;
		}

		Writer writer = null;
		try {
			writer = new FileWriter(filename, true);
			writer.write("[" + new Date() + "] " + text + "\n");
		} catch (IOException e) {
			// ignore
		} finally {
			closeQuietly(writer);
		}
	}

	public static void log2File(String filename, Throwable exc) {
		if (filename == null) {
			return;
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileOutputStream(filename, true));
			writer.write("[" + new Date() + "] ");
			exc.printStackTrace(writer);
		} catch (IOException e) {
			// ignore
		} finally {
			closeQuietly(writer);
		}
	}

	public static File copyToDir(File file, File dstDir, String name) {
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			final File copy = new File(dstDir, name != null ? name : file.getName());
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
