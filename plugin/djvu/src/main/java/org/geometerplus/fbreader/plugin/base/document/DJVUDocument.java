package org.geometerplus.fbreader.plugin.base.document;

import java.util.*;

import android.content.ContextWrapper;
import android.graphics.*;
import android.util.Log;

import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.fbreader.plugin.base.tree.TOCTree;

public class DJVUDocument extends DocumentHolder {
	private static native void initNative();
	private static native void destroyNative();
	private native boolean openDocumentNative(String path);
	private native int getPageCountNative();
	private native long getPageSizeNative(int pageNo);
	private native void closeNative();
	private native void renderNative(Bitmap canvas, int left, int top, int right, int bottom, long ptr);
	private native long getOutlineRoot();
	private native long getOutlineNext(long cur);
	private native long getOutlineChild(long cur);
	private native String getOutlineText(long cur);
	private native int getOutlinePage(long cur);
	private native int createText(int pageNo);
	private native int getWordCoord(int no, int type);
	private native String getWordText(int no);
	private native long createPageNative(int pageNo);
	private native void freePageNative(long p);

	public static void init(ContextWrapper c) {
		System.loadLibrary("DjVuLibre");
		initNative();
	}

	public static void destroy() {
		destroyNative();
	}

	@Override
	protected boolean openDocumentInternal(String path) {
		myPageCache.clear();
		return openDocumentNative(path);
	}

	@Override
	protected int getPageCountInternal() {
		return getPageCountNative();
	}

	@Override
	public Size getPageSizeInternal(int pageNo) {
		final long size = getPageSizeNative(pageNo);
		if (size == -1L) {
			return null;
		}
		return new Size((int)(size >> 32), (int)size);
	}

	@Override
	protected void renderPageInternal(Bitmap canvas, final int pageNo, final Rect src, Rect dst, boolean inverted) {
		final Bitmap realCanvas;
		if (dst.left != 0 ||
			dst.top != 0 ||
			dst.right != canvas.getWidth() ||
			dst.bottom != canvas.getHeight()
		) {
			realCanvas = BitmapUtil.createBitmap(
				dst.right - dst.left,
				dst.bottom - dst.top,
				Bitmap.Config.ARGB_8888
			);
		} else {
			realCanvas = canvas;
		}

		doSomethingWithCache(new Runnable() {
			@Override
			public void run() {
				final PageCache pc = getOrCreatePage(pageNo);
				if (pc instanceof DJVUCache) {
					final DJVUCache djvc = (DJVUCache)pc;
					renderNative(realCanvas, src.left, src.top, src.right, src.bottom, djvc.myObject);
				}
				freeIfNotCached(pageNo, pc);
			}
		});

		if (realCanvas != canvas) {
			new Canvas(canvas).drawBitmap(realCanvas, dst.left, dst.top, new Paint());
		}
	}

	@Override
	public void closeInternal() {
		closeNative();
	}

	@Override
	public void initTOC(TOCTree root) {
		long nroot = getOutlineRoot();
		if (nroot != 0) {
			createTOCTree(nroot, root, true);
		}
	}

	private void createTOCTree(long n, TOCTree parent, boolean fistChild) {
		TOCTree t = new TOCTree(parent);
		t.setText(getOutlineText(n));
		t.setReference(getOutlinePage(n));
		long nextnum = getOutlineNext(n);
		while (fistChild && nextnum != 0) {
			createTOCTree(nextnum, parent, false);
			nextnum = getOutlineNext(nextnum);
		}
		long childnum = getOutlineChild(n);
		if (childnum != 0) {
			createTOCTree(childnum, t, true);
		}
	}

	@Override
	public String getTitle() {
		return getMeta("Title");
	}

	@Override
	public String getAuthor() {
		return getMeta("Author");
	}

	private String getMeta(String tag) {
		return null;
	}

	@Override
	public String getPageStartText(int pageNo) {
		checkPage(pageNo);
		final int count = Math.min(myPageCache.get(pageNo).Words.size(), 10);
		return count > 0
			? getTextInternal(pageNo, 0, count)
			: super.getPageStartText(pageNo);
	}

	@Override
	List<RectF> createAllRectsInternal(int pageNo) {
		checkPage(pageNo);
		List<RectF> temp = new ArrayList<RectF>(myPageCache.get(pageNo).Rects.size());
		for (int i = 0; i < myPageCache.get(pageNo).Rects.size(); ++i) {
			temp.add(myPageCache.get(pageNo).Rects.get(i));
		}
		return temp;
	}

	@Override
	String getTextInternal(int pageNo, int is, int ie) {
		checkPage(pageNo);
		if (ie != -1) {
			int start = Math.min(ie, is);
			int end = Math.max(ie, is) + 1;
			String res = "";
			for (int i = start; i < end && i < myPageCache.get(pageNo).Words.size(); ++i) {
				if (i > start && myPageCache.get(pageNo).Rects.get(i - 1).bottom >= myPageCache.get(pageNo).Rects.get(i).top) {
					res += "\n";
				}
				res += myPageCache.get(pageNo).Words.get(i) + " ";
			}
			return res;
		}
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

	private class PageInfo {
		final List<RectF> Rects = new ArrayList<RectF>();
		final List<String> Words = new ArrayList<String>();
	}

	private HashMap<Integer, PageInfo> myPageCache = new HashMap<Integer, PageInfo>();

	private void cachePage(int pageNo) {
		Log.e("THREAD", "cachePage: start");
		if (myPageCache.containsKey(pageNo)) {
			return;
		}
		PageInfo p = new PageInfo();
		int num = createText(pageNo);
		Log.e("THREAD", "cachePage: text created");
		for (int i = 0; i < num; ++i) {
			p.Rects.add(new RectF(
				getWordCoord(i, 0),
				getWordCoord(i, 3),
				getWordCoord(i, 2),
				getWordCoord(i, 1)
			));
		}
		Log.e("THREAD", "cachePage: text processed");
		for (int i = 0; i < num; ++i) {
			p.Words.add(getWordText(i));
		}
		myPageCache.put(pageNo, p);
		Log.e("THREAD", "cachePage: end");
	}

	private void checkPage(int pageNo) {
		if (!myPageCache.containsKey(pageNo)) {
			cachePage(pageNo);
		}
	}

	@Override
	public int getPageCharNumInternal(int pageNo) {
		checkPage(pageNo);
		return myPageCache.get(pageNo).Words.size();
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
		return path.endsWith(".djvu");
	}

	class DJVUCache extends PageCache {
		private long myObject;

		DJVUCache(long p) {
			myObject = p;
		}

		@Override
		protected synchronized void recycle() {
			if (myObject != 0) {
				freePageNative(myObject);
			}
			myObject = 0;
		}

		@Override
		protected void finalize() {
			recycle();
		}
	}

	@Override
	protected PageCache createPage(int no) {
		return new DJVUCache(createPageNative(no));
	}

	public Bitmap getCover(int maxWidth, int maxHeight) {
		final Size size = getPageSize(0);
		if (size == null || size.Width == 0 || size.Height == 0) {
			return null;
		}

		final float ratio = Math.min(maxWidth / size.Width, maxHeight / size.Height);
		final Bitmap cover = BitmapUtil.createBitmap(
			Math.round(size.Width * ratio),
			Math.round(size.Height * ratio),
			Bitmap.Config.ARGB_8888
		);
		renderPage(cover, 0, null, null);
		return cover;
	}
}
