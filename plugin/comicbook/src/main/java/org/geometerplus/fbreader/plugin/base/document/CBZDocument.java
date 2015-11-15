package org.geometerplus.fbreader.plugin.base.document;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import android.graphics.*;

import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.fbreader.plugin.base.tree.TOCTree;

public class CBZDocument extends DocumentHolder {
	private final SystemInfo mySystemInfo;
	private OneLevelImageArchive myArchive = null;

	public CBZDocument(SystemInfo info) {
		mySystemInfo = info;
		CBFileCacheUtil.clearCache(info);
	}

	@Override
	protected boolean openDocumentInternal(String path) {
		try {
			if (path.toLowerCase().endsWith(".cbz")) {
				myArchive = new ZipArchive(new File(path));
			} else if (path.toLowerCase().endsWith(".cbr")) {
				myArchive = new RarArchive(new File(path), mySystemInfo);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int getPageCountInternal() {
		return myArchive == null ? 0 : myArchive.size();
	}

	@Override
	public Size getPageSizeInternal(int pageNo) {
		if (myArchive == null) {
			return null;
		}
		try {
			final BitmapFactory.Options options = myArchive.getPageOptions(pageNo);
			if (options == null) {
				return null;
			}
			return new Size(options.outWidth, options.outHeight);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void renderPageInternal(final Bitmap canvas, final int pageNo, final Rect src, final Rect dst, final boolean inverted) {
		final Size size = getPageSize(pageNo);
		if (size == null) {
			return;
		}
		doSomethingWithCache(new Runnable() {
			@Override
			public void run() {
				final PageCache pc = getOrCreatePage(pageNo);
				if (pc instanceof CBZCache) {
					final CBZCache cbzc = (CBZCache)pc;
					final int w = cbzc.myBitmap.getWidth();
					if (w != (int)size.Width) {
						final float ratio = w / size.Width;
						src.top = (int)(src.top * ratio + .5f);
						src.bottom = (int)(src.bottom * ratio + .5f);
						src.left = (int)(src.left * ratio + .5f);
						src.right = (int)(src.right * ratio + .5f);
					}
					new Canvas(canvas).drawBitmap(cbzc.myBitmap, src, dst, createPaint(inverted));
				}
				freeIfNotCached(pageNo, pc);
			}
		});
	}

	@Override
	public void closeInternal() {
		if (myArchive != null) {
			myArchive.close();
		}
		CBFileCacheUtil.clearCache(mySystemInfo);
	}

	@Override
	public void initTOC(TOCTree root) {
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public String getAuthor() {
		return null;
	}

	@Override
	List<RectF> createAllRectsInternal(int pageNo) {
		return Collections.emptyList();
	}

	@Override
	String getTextInternal(int pageNo, int startIndex, int endIndex) {
		return null;
	}

	@Override
	List<List<RectF>> createSearchRectsInternal(int pageNo, String pattern) {
		return Collections.emptyList();
	}

	@Override
	boolean findInPageInternal(int no, String pattern) {
		return false;
	}

	@Override
	public int getPageCharNumInternal(int pageNo) {
		return 0;
	}

	@Override
	int checkInternalPageLinkInternal(int pageNo, float x, float y) {
		return -1;
	}

	@Override
	public String checkHyperLinkInternal(int pageNo, float x, float y) {
		return null;
	}

	@Override
	public boolean acceptsPath(String path) {
		return path.endsWith(".cbz") || path.endsWith(".cbr");
	}

	class CBZCache extends PageCache {
		private Bitmap myBitmap;

		CBZCache(Bitmap b) {
			myBitmap = b;
		}

		@Override
		protected void recycle() {
			myBitmap = null;
		}
	}

	@Override
	protected PageCache createPage(int no) {
		try {
			if (myArchive == null) {
				return null;
			}
			final Bitmap bmp = myArchive.getPage(no);
			return bmp != null ? new CBZCache(bmp) : null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Bitmap getCover(int maxWidth, int maxHeight) {
		try {
			return myArchive != null ? myArchive.getCover(maxWidth, maxHeight) : null;
		} catch (IOException e) {
			return null;
		}
	}
}
