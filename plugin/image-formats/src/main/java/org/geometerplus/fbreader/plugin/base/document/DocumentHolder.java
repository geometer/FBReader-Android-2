package org.geometerplus.fbreader.plugin.base.document;

import java.util.*;

import android.graphics.*;
import android.support.v4.util.LruCache;

import org.fbreader.common.options.ColorProfile;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.plugin.base.SettingsHolder;
import org.geometerplus.fbreader.plugin.base.document.PageHolder;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;
import org.geometerplus.fbreader.plugin.base.tree.TOCTree;

public abstract class DocumentHolder {
	public static final class CropInfo {
		public static final CropInfo NULL = new CropInfo(0, 0, 0, 0);

		public final int TopPercent;
		public final int BottomPercent;
		public final int LeftPercent;
		public final int RightPercent;

		public CropInfo(int topPercent, int bottomPercent, int leftPercent, int rightPercent) {
			TopPercent = Math.min(Math.max(topPercent, 0), 49);
			BottomPercent = Math.min(Math.max(bottomPercent, 0), 49);
			LeftPercent = Math.min(Math.max(leftPercent, 0), 49);
			RightPercent = Math.min(Math.max(rightPercent, 0), 49);
		}

		public float adjustedHeight(float height) {
			return height * (100 - TopPercent - BottomPercent) / 100; 
		}

		public float adjustedWidth(float width) {
			return width * (100 - LeftPercent - RightPercent) / 100; 
		}
	}

	protected int myW = 100;
	protected int myH = 140;
	private PluginView myView;
	private CropInfo myCropInfo = CropInfo.NULL;
	protected PluginView.ChangeListener myListener;
	private TOCTree myTOCTree;

	protected boolean myDocInitialized = false;
	protected boolean myPageWasSet = false;

	private final LruCache<Integer,PageHolder> myWholePageMap = new LruCache<Integer,PageHolder>(4) {
		@Override
		protected PageHolder create(Integer pageNo) {
			final PageHolder holder;
			if (displayAsDoublePaged(pageNo)) {
				holder = new DoublePageHolder(myView, DocumentHolder.this, myW, myH, pageNo);
			} else {
				holder = new SinglePageHolder(myView, DocumentHolder.this, myW, myH, pageNo);
			}
			holder.setListener(myListener);
			return holder;
		}
	};
	private final List<Bookmark> myBookmarks = Collections.synchronizedList(new ArrayList<Bookmark>());

	public abstract boolean acceptsPath(String path);

	private int myPageCount = -1;
	public final int getPageCount() {
		if (myPageCount == -1) {
			myPageCount = getPageCountInternal();
		}
		return myPageCount;
	}
	protected abstract int getPageCountInternal();

	public final void renderPage(Bitmap canvas, int pageNo, Rect src, Rect dst) {
		final int dstW = canvas.getWidth();
		final int dstH = canvas.getHeight();
		if (dst == null) {
			dst = new Rect(0, 0, dstW, dstH);
		} else if (dst.right <= dst.left || dst.right <= 0 || dst.left >= dstW ||
				   dst.bottom <= dst.top || dst.bottom <= 0 || dst.top >= dstH) {
			return;
		}

		final int srcW = (int)Math.round(getPageWidth(pageNo));
		final int srcH = (int)Math.round(getPageHeight(pageNo));
		if (src == null) {
			src = new Rect(0, 0, srcW, srcH);
		} else if (src.right <= src.left || src.right <= 0 || src.left >= srcW ||
				   src.bottom <= src.top || src.bottom <= 0 || src.top >= srcH) {
			return;
		}

		if (dst.left < 0) {
			src.left -= dst.left * (src.right - src.left) / (dst.right - dst.left);
			dst.left = 0;
			if (src.right <= src.left) {
				return;
			}
		}
		if (dst.right > dstW) {
			src.right -= (dst.right - dstW) * (src.right - src.left) / (dst.right - dst.left);
			dst.right = dstW;
			if (src.right <= src.left) {
				return;
			}
		}
		if (dst.top < 0) {
			src.top -= dst.top * (src.bottom - src.top) / (dst.bottom - dst.top);
			dst.top = 0;
			if (src.bottom <= src.top) {
				return;
			}
		}
		if (dst.bottom > dstH) {
			src.bottom -= (dst.bottom - dstH) * (src.bottom - src.top) / (dst.bottom - dst.top);
			dst.bottom = dstH;
			if (src.bottom <= src.top) {
				return;
			}
		}

		if (src.left < 0) {
			dst.left -= src.left * (dst.right - dst.left) / (src.right - src.left);
			src.left = 0;
			if (dst.right <= dst.left) {
				return;
			}
		}
		if (src.right > srcW) {
			dst.right -= (src.right - srcW) * (dst.right - dst.left) / (src.right - src.left);
			src.right = srcW;
			if (dst.right <= dst.left) {
				return;
			}
		}
		if (src.top < 0) {
			dst.top -= src.top * (dst.bottom - dst.top) / (src.bottom - src.top);
			src.top = 0;
			if (dst.bottom <= dst.top) {
				return;
			}
		}
		if (src.bottom > srcH) {
			dst.bottom -= (src.bottom - srcH) * (dst.bottom - dst.top) / (src.bottom - src.top);
			src.bottom = srcH;
			if (dst.bottom <= dst.top) {
				return;
			}
		}

		final boolean inverted;
		final PluginView view = myView;
		if (view != null) {
			final SettingsHolder settings = view.getSettings();
			inverted =
				settings != null &&
				ColorProfile.NIGHT.equals(settings.ColorProfileName.getValue());
		} else {
			inverted = false;
		}

		renderPageInternal(canvas, pageNo, src, dst, isInverted());
	}
	protected abstract void renderPageInternal(Bitmap canvas, int pageNo, Rect src, Rect dst, boolean inverted);

	final boolean isInverted() {
		final PluginView view = myView;
		if (view == null) {
			return false;
		}
		final SettingsHolder settings = view.getSettings();
		return settings != null && ColorProfile.NIGHT.equals(settings.ColorProfileName.getValue());
	}

	protected final Paint createPaint(boolean inverted) {
		final Paint paint = new Paint();
		if (inverted) {
			final float invertArray[] = {
				-1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, -1.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 0.0f, -1.0f, 1.0f, 0.0f,
				1.0f, 1.0f, 1.0f, 1.0f, 0.0f
			};
			paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(invertArray)));
		}
		return paint;
	}

	protected final Bitmap createCleanBitmap(int width, int height, boolean inverted) {
		final Bitmap bitmap = BitmapUtil.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(inverted ? Color.BLACK : Color.WHITE);
		return bitmap;
	}

	public abstract Bitmap getCover(int maxWidth, int maxHeight);

	protected abstract boolean openDocumentInternal(String path);
	protected abstract void closeInternal();
	public abstract void initTOC(TOCTree root);

	public abstract void readMetainfo(AbstractBook book);
	public abstract String readAnnotation();

	public String getPageStartText(int pageNo) {
		return "<Page " + (pageNo + 1) + ">";
	}

	public final static class Size {
		final float Width;
		final float Height;

		Size(float width, float height) {
			Width = width;
			Height = height;
		}
	}

	protected abstract Size getPageSizeInternal(int pageNo);
	public abstract int getPageCharNumInternal(int pageNo);
	abstract List<RectF> createAllRectsInternal(int pageNo);
	abstract String getTextInternal(int pageNo, int startIndex, int endIndex);
	abstract List<List<RectF>> createSearchRectsInternal(int pageNo, String pattern);
	abstract boolean findInPageInternal(int pageNo, String pattern);
	abstract int checkInternalPageLinkInternal(int pageNo, float x, float y);
	abstract String checkHyperLinkInternal(int pageNo, float x, float y);

	private final Map<Integer,Size> mySizes =
		Collections.synchronizedMap(new HashMap<Integer,Size>());

	public final Size getPageSizeOrNull(final int pageNo) {
		return mySizes.get(pageNo);
	}

	public final Size getPageSize(int pageNo) {
		Size size = mySizes.get(pageNo);
		if (size == null) {
			synchronized (mySizes) {
				size = mySizes.get(pageNo);
				if (size == null) {
					size = getPageSizeInternal(pageNo);
					mySizes.put(pageNo, size);
				}
			}
		}
		return size;
	}

	public final float getPageHeight(int pageNo) {
		final Size size = getPageSize(pageNo);
		return size != null ? size.Height : 0;
	}

	public final float getPageWidth(int pageNo) {
		final Size size = getPageSize(pageNo);
		return size != null ? size.Width : 0;
	}

	public float getAdjustedPageHeight(int pageNo) {
		return myCropInfo.adjustedHeight(getPageHeight(pageNo));
	}

	public float getAdjustedPageWidth(int pageNo) {
		return myCropInfo.adjustedWidth(getPageWidth(pageNo));
	}

	protected final Rect srcRect(int pageNo) {
		final float w = getPageWidth(pageNo);
		final float h = getPageHeight(pageNo);

		return new Rect(
			(int)Math.round(w * myCropInfo.LeftPercent / 100),
			(int)Math.round(h * myCropInfo.TopPercent / 100),
			(int)Math.round(w * (100 - myCropInfo.RightPercent) / 100),
			(int)Math.round(h * (100 - myCropInfo.BottomPercent) / 100)
		);
	}

	protected abstract class PageCache {
		protected abstract void recycle();
	}

	protected Map<Integer,PageCache> myCache =
		Collections.synchronizedMap(new HashMap<Integer,PageCache>());

	protected void clearCache() {
		uncacheAllExcept(Collections.<Integer>emptyList());
	}

	protected abstract PageCache createPage(int no);

	private void cachePage(int no) {
		synchronized (myCache) {
			if (!myCache.containsKey(no)) {
				myCache.put(no, createPage(no));
			}
		}
	}

	private void uncacheAllExcept(Collection<Integer> c) {
		synchronized (myCache) {
			final List<Integer> toDelete = new ArrayList<Integer>(myCache.keySet());
			toDelete.removeAll(c);
			for (Integer i : toDelete) {
				myCache.remove(i);
			}
		}
	}

	protected PageCache getOrCreatePage(int no) {
		final PageCache page = myCache.get(no);
		return page != null ? page : createPage(no);
	}

	protected void freeIfNotCached(int no, PageCache p) {
		if (p != null && myCache.get(no) != p) {
			p.recycle();
		}
	}

	protected void doSomethingWithCache(Runnable r) {
		synchronized (myCache) {
			r.run();
		}
	}

	public final void setPage(int pageNo) {
		if (displayAsDoublePaged(pageNo)) {
			cachePage(pageNo);
			if (pageNo + 1 < getPageCount()) {
				cachePage(pageNo + 1);
			}
			uncacheAllExcept(Collections.unmodifiableList(Arrays.<Integer>asList(pageNo, pageNo + 1)));
		} else {
			cachePage(pageNo);
			uncacheAllExcept(Collections.singleton(pageNo));
		}
	}

	public void close() {
		clearCache();
		if (myDocInitialized) {
			closeInternal();
		}
		myDocInitialized = false;
		myPageWasSet = false;
		myCropInfo = CropInfo.NULL;
	}

	public boolean open(String path) {
		clearCache();
		if (!openDocumentInternal(path)) {
			return false;
		}
		mySizes.clear();
		myPageCount = -1;
		myTOCTree = new TOCTree(null);
		initTOC(myTOCTree);
		myDocInitialized = true;
		return true;
	}

	public TOCTree getTOCTree() {
		return myTOCTree;
	}

	public boolean opened() {
		return myDocInitialized;
	}

	public boolean resized() {
		return myView != null;
	}

	public boolean fullyInitialized() {
		return
			myDocInitialized &&
			myView != null &&
			myPageWasSet &&
			myCropInfo != CropInfo.NULL;
	}

	private boolean canBeShownAsTwoPages(int pageNo) {
		return
			pageNo + 1 < getPageCount() &&
			getPageHeight(pageNo) > getPageWidth(pageNo) &&
			getPageHeight(pageNo + 1) > getPageWidth(pageNo + 1);
	}

	private boolean displayAsDoublePaged(int pageNo) {
		return
			myW > myH &&
			canBeShownAsTwoPages(pageNo) &&
			myView != null &&
			myView.getSettings().TwoColumnView.getValue();
	}

	public int getNextPageNo(int pageNo) {
		if (displayAsDoublePaged(pageNo)) {
			return pageNo + 2;
		} else {
			return pageNo + 1;
		}
	}

	public int getPrevPageNo(int pageNo) {
		if (pageNo >= 2 && displayAsDoublePaged(pageNo - 2)) {
			return pageNo - 2;
		} else {
			return pageNo - 1;
		}
	}

	public void init(PluginView v, int w, int h) {
		if (v == myView && w == myW && h == myH) {
			return;
		}
		if (w <= 0 || h <= 0) {
			return;
		}

		myView = v;
		myW = w;
		myH = h;

		clearAll();
		clearAllRects();
	}

	public void setListener(PluginView.ChangeListener c) {
		myListener = c;
	}

	public void setCropInfo(int topPercent, int bottomPercent, int leftPercent, int rightPercent) {
		myCropInfo = new CropInfo(topPercent, bottomPercent, leftPercent, rightPercent);
		clearAll();
		clearAllRects();
		postInvalidate();
	}

	private void clearAllRects() {
		final PluginView view = myView;
		if (view != null) {
			view.clearSelection();
		}
	}

	public CropInfo getCropInfo() {
		return myCropInfo;
	}

	public void gotoPage(final int no) {
		myPageWasSet = true;
		PluginView.AuxService.execute(new Runnable() {
			public void run() {
				setPage(no);
			}
		});
	}

	public void clearAll() {
		myWholePageMap.evictAll();
		myBookmarkHilitesCache.evictAll();
	}

	public void clearThumbnails() {
		final ThumbnailFactory tf = myThumbnailFactory;
		if (tf != null) {
			tf.clear();
		}
	}

	public void refreshZoom(int pageNo, PluginView.ZoomInfo zoomInfo) {
		if (zoomInfo.Factor != 1) {
			getPage(pageNo).refreshZoomedBitmap(zoomInfo);
		} else {
			getPage(pageNo).getZoomedBitmapManager().clearBitmaps();
		}
		postInvalidate();
	}

	public PageHolder getPage(int pageNo) {
		return myWholePageMap.get(pageNo);
	}

	public Bitmap getWholeBitmap(int pageNo) {
		if (fullyInitialized()) {
			return getPage(pageNo).getFullsizeBitmap();
		}
		return null;
	}

	public void postInvalidate() {
		if (myView != null) {
			myView.postInvalidate();
		}
	}

	public class ThumbnailFactory {
		private final int myHeight;
		private final ThumbnailLoader.Listener myListener;

		private final LruCache<Integer,ThumbnailLoader> myThumbnailCache =
			new LruCache<Integer,ThumbnailLoader>(20) {
				@Override
				protected ThumbnailLoader create(Integer pageNo) {
					return new ThumbnailLoader(DocumentHolder.this, pageNo, myHeight, myListener);
				}
			};

		private ThumbnailFactory(int height, ThumbnailLoader.Listener listener) {
			myHeight = height;
			myListener = listener;
		}

		public synchronized Bitmap get(int pageNo) {
			return myThumbnailCache.get(pageNo).getBitmapOrPlaceholder();
		}

		public synchronized void clear() {
			myThumbnailCache.evictAll();
		}
	}

	private volatile ThumbnailFactory myThumbnailFactory;

	public ThumbnailFactory createThumbsFactory(int h, ThumbnailLoader.Listener l) {
		myThumbnailFactory = new ThumbnailFactory(h, l);
		return myThumbnailFactory;
	}

	public final static class SelectionInfo {
		private int myFirst = -1;
		private int mySecond = -1;
		private final List<RectF> myRectangles = new ArrayList<RectF>();

		public synchronized boolean isEmpty() {
			return myFirst == -1 || mySecond == -1;
		}

		public synchronized boolean isForward() {
			return myFirst <= mySecond;
		}

		public synchronized void clear() {
			myFirst = -1;
			mySecond = -1;
			myRectangles.clear();
		}

		public synchronized void init(int index) {
			myFirst = index;
			mySecond = index;
		}

		public synchronized void setFirst(int index) {
			myFirst = index;
		}

		private synchronized boolean setSecond(int index) {
			if (index == mySecond) {
				return false;
			} else {
				mySecond = index;
				return true;
			}
		}

		public int firstIndex() {
			return myFirst;
		}

		public int secondIndex() {
			return mySecond;
		}

		public synchronized int startIndex() {
			return Math.min(myFirst, mySecond);
		}

		public synchronized int endIndex() {
			return Math.max(myFirst, mySecond);
		}

		public synchronized void setRectangles(List<RectF> rectangles) {
			myRectangles.clear();
			myRectangles.addAll(rectangles);
		}

		public synchronized List<RectF> rectangles() {
			return myRectangles.isEmpty()
				? Collections.<RectF>emptyList() : new ArrayList<RectF>(myRectangles);
		}
	}

	public final SelectionInfo Selection = new SelectionInfo();

	protected float myFirstSelectionCursorX;
	protected float myFirstSelectionCursorY;
	protected float myLastSelectionCursorX;
	protected float myLastSelectionCursorY;

	public float getFirstSelectionCursorX() {
		return myFirstSelectionCursorX;
	}

	public float getFirstSelectionCursorY() {
		return myFirstSelectionCursorY;
	}

	public float getLastSelectionCursorX() {
		return myLastSelectionCursorX;
	}

	public float getLastSelectionCursorY() {
		return myLastSelectionCursorY;
	}

	public boolean extendSelection(int charIndex) {
		return Selection.setSecond(charIndex);
	}

	public boolean getSelectionCursor(int pageNo, int x, int y, PluginView.ZoomInfo zoomInfo) {
		synchronized (Selection) {
			if (Selection.isEmpty()) {
				return false;
			}
			final PageHolder page = getPage(pageNo);
			final float dx1 = zoomInfo.xBmpToScreen(myFirstSelectionCursorX, page) - x;
			final float dy1 = zoomInfo.yBmpToScreen(myFirstSelectionCursorY, page) - y;
			final float dx2 = zoomInfo.xBmpToScreen(myLastSelectionCursorX, page) - x;
			final float dy2 = zoomInfo.yBmpToScreen(myLastSelectionCursorY, page) - y;

			final float dist1_2 = dx1 * dx1 + dy1 * dy1;
			final float dist2_2 = dx2 * dx2 + dy2 * dy2;
			final float maxDist = ZLibrary.Instance().getDisplayDPI() / 4;
			final float maxDist_2 = maxDist * maxDist;
			if (dist1_2 < dist2_2) {
				if (dist1_2 > maxDist_2) {
					return false;
				}
				if (Selection.isForward()) {
					Selection.setFirst(Selection.secondIndex());
				}
			} else {
				if (dist2_2 > maxDist_2) {
					return false;
				}
				if (!Selection.isForward()) {
					Selection.setFirst(Selection.secondIndex());
				}
			}
		}
		return true;
	}

	public void createSelectionWordRects(int pageNo) {
		synchronized (Selection) {
			if (Selection.isEmpty()) {
				return;
			}
			final List<RectF> allRects = myWholePageMap.get(pageNo).getAllRects();
			final int start = Selection.startIndex();
			final int end = Selection.endIndex();
			final List<RectF> rectangles = new ArrayList<RectF>(end - start + 1);
			for (int index = start; index <= end; ++index) {
				final RectF r = new RectF(allRects.get(index));
				if (index == start) {
					myFirstSelectionCursorX = r.left;
					myFirstSelectionCursorY = (r.top + r.bottom) / 2;
				}
				if (index == end) {
					myLastSelectionCursorX = r.right;
					myLastSelectionCursorY = (r.top + r.bottom) / 2;
				}
				if (index > start) {
					final RectF lastRect = rectangles.get(rectangles.size() - 1);
					if (lastRect.top == r.top && lastRect.bottom == r.bottom) {
						lastRect.right = r.right;
					} else {
						rectangles.add(r);
					}
				} else {
					rectangles.add(r);
				}
			}
			Selection.setRectangles(rectangles);
		}
	}

	private final LruCache<Integer,List<BookmarkHighlighting>> myBookmarkHilitesCache =
		new LruCache<Integer,List<BookmarkHighlighting>>(20) {
			@Override
			protected List<BookmarkHighlighting> create(Integer pageNo) {
				synchronized (myBookmarks) {
					return createHilites(pageNo, myBookmarks);
				}
			}
		};

	public List<BookmarkHighlighting> getBookmarkHilites(int pageNo) {
		return myBookmarkHilitesCache.get(pageNo);
	}

	public static class BookmarkHighlighting {
		public final Bookmark Bookmark;
		public final int Start;
		public final int End;
		public final List<RectF> Rects;

		private BookmarkHighlighting(Bookmark bookmark, int s, int e, List<RectF> rects) {
			Bookmark = bookmark;
			Start = s;
			End = e;
			Rects = rects;
		}
	}

	public void setBookmarks(List<Bookmark> bookmarks) {
		synchronized (myBookmarks) {
			myBookmarks.clear();
			myBookmarks.addAll(bookmarks);
			myBookmarkHilitesCache.evictAll();
		}
	}

	private final List<BookmarkHighlighting> createHilites(int pageNo, List<Bookmark> bookmarks) {
		if (bookmarks == null) {
			return Collections.emptyList();
		}

		final List<RectF> allRects = myWholePageMap.get(pageNo).getAllRects();
		if (allRects.size() == 0) {
			return Collections.emptyList();
		}

		final List<BookmarkHighlighting> hilites = new ArrayList<BookmarkHighlighting>();
		for (Bookmark b : bookmarks) {
			BookmarkHighlighting h = null;
			final int page1 = b.getParagraphIndex();
			final int page2 = b.getEnd().getParagraphIndex();
			final int start = b.getElementIndex();
			final int end = b.getEnd().getElementIndex();
			if (displayAsDoublePaged(pageNo)) {
				if (page1 == pageNo - 1 && page2 == pageNo) {
					h = createSingleHilite(allRects, b, 0, end);
				} else if (page1 == pageNo && page2 == pageNo) {
					h = createSingleHilite(allRects, b, start, end);
				} else if (page1 == pageNo && page2 == pageNo + 1) {
					int e = end + getPageCharNumInternal(pageNo);
					h = createSingleHilite(allRects, b, start, e);
				} else if (page1 == pageNo + 1 && page2 == pageNo + 1) {
					int e = end + getPageCharNumInternal(pageNo);
					int s = start + getPageCharNumInternal(pageNo);
					h = createSingleHilite(allRects, b, s, e);
				} else if (page1 == pageNo + 1 && page2 == pageNo + 2) {
					int e = getPageCharNumInternal(pageNo) + getPageCharNumInternal(pageNo + 1);
					int s = start + getPageCharNumInternal(pageNo);
					h = createSingleHilite(allRects, b, s, e);
				}
			} else {
				if (page1 == pageNo - 1 && page2 == pageNo) {
					h = createSingleHilite(allRects, b, 0, end);
				} else if (page1 == pageNo && page2 == pageNo) {
					h = createSingleHilite(allRects, b, start, end);
				} else if (page1 == pageNo && page2 == pageNo + 1) {
					int e = getPageCharNumInternal(pageNo);
					h = createSingleHilite(allRects, b, start, e);
				}
			}
			if (h != null) {
				hilites.add(h);
			}
		}
		return hilites;
	}

	private BookmarkHighlighting createSingleHilite(List<RectF> allRects, Bookmark bookmark, int start, int end) {
		end = Math.min(end, allRects.size() - 1);
		final List<RectF> rects = new ArrayList<RectF>(Math.max(0, end - start + 1));
		for (int index = start; index <= end; ++index) {
			final RectF r = new RectF(allRects.get(index));
			if (index > start) {
				int last = rects.size() - 1;
				RectF lastRect = rects.get(last);
				if (lastRect.top == r.top && lastRect.bottom == r.bottom) {
					lastRect.right = r.right;
				} else {
					rects.add(r);
				}
			} else {
				rects.add(r);
			}
		}
		return new BookmarkHighlighting(bookmark, start, end, rects);
	}

	public Bookmark checkBookmark(int pageNo, float x, float y, PluginView.ZoomInfo zoomInfo) {
		final PluginView view = myView;
		if (view == null) {
			return null;
		}

		final PageHolder page = getPage(pageNo);
		x = zoomInfo.xScreenToBmp(x, page);
		y = zoomInfo.yScreenToBmp(y, page);
		for (BookmarkHighlighting h : getBookmarkHilites(pageNo)) {
			final HighlightingStyle style = view.getStyle(h.Bookmark.getStyleId());
			if (style == null) {
				continue;
			}
			final ZLColor color = style.getBackgroundColor();
			if (color == null || color.intValue() == -1) {
				continue;
			}
			for (RectF r : h.Rects) {
				if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) {
					return h.Bookmark;
				}
			}
		}
		return null;
	}

	protected final float xBmpToDocument(float coord, float ratio, int pageNo) {
		return coord / ratio + getPageWidth(pageNo) * myCropInfo.LeftPercent / 100f;
	}

	protected final float yBmpToDocument(float coord, float ratio, int pageNo) {
		return getPageHeight(pageNo) * (100f - myCropInfo.TopPercent) / 100 - coord / ratio;
	}

	protected final float xDocumentToBmp(float coord, float ratio, int pageNo) {
		return (coord - getPageWidth(pageNo) * myCropInfo.LeftPercent / 100f) * ratio;
	}

	protected final float yDocumentToBmp(float coord, float ratio, int pageNo) {
		return (getPageHeight(pageNo) * (100f - myCropInfo.TopPercent) / 100 - coord) * ratio;
	}

	protected final RectF rectDocumentToBmp(RectF r, float ratio, int pageNo) {
		return new RectF(
			xDocumentToBmp(r.left, ratio, pageNo),
			yDocumentToBmp(r.top, ratio, pageNo),
			xDocumentToBmp(r.right, ratio, pageNo),
			yDocumentToBmp(r.bottom, ratio, pageNo)
		);
	}
}
