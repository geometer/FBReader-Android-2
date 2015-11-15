package org.geometerplus.fbreader.plugin.base.document;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.fbreader.plugin.base.*;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

import android.graphics.*;
import android.util.Log;

public abstract class PageHolder {
	private static final ExecutorService ourPageLoaderService = Executors.newSingleThreadExecutor();

	protected final PluginView myView;
	protected final DocumentHolder myDoc;
	protected final int myPageNo;

	public final int Width;
	public final int Height;

	protected final float myRatio;

	private final Object myLoaderLock = new Object();
	private volatile PageBitmapLoader myMainLoader;
	private final ZoomedBitmapManager myZoomedBitmapManager;

	private final Object myAllRectsLock = new Object();
	private volatile List<RectF> myAllRects;
	private final Object mySearchRectsLock = new Object();
	private volatile List<List<RectF>> mySearchRects;
	private volatile String mySearchPattern;

	public abstract int getContainedPageNum();
	protected abstract void draw(Bitmap canvas, Rect dst, float ratio, float zoom);

	public abstract float getRealHeight();
	public abstract float getRealWidth();

	protected abstract List<RectF> createAllRects();
	protected abstract List<List<RectF>> createSearchRects(String pattern);

	public abstract int getPageCharNum();
	public abstract String getSelectionText();
	public abstract boolean matches(String pattern);
	public abstract int checkInternalPageLink(float x, float y);
	public abstract String checkHyperLink(float x, float y);

	protected abstract int getAdjustedWidth();

	public PageHolder(PluginView view, DocumentHolder doc, int w, int h, int n) {
		myView = view;
		myDoc = doc;
		Width = w;
		Height = h;
		myPageNo = n;
		myZoomedBitmapManager = new ZoomedBitmapManager();

		myRatio = Math.min(
			1.f * getAdjustedWidth() / getRealWidth(),
			1.f * Height / getRealHeight()
		);
	}

	public List<RectF> getAllRects() {
		if (myAllRects == null) {
			synchronized (myAllRectsLock) {
				if (myAllRects == null) {
					final List<RectF> fromDoc = createAllRects();
					final List<RectF> converted = new ArrayList<RectF>(fromDoc.size());
					for (RectF r : fromDoc) {
						converted.add(myDoc.rectDocumentToBmp(r, myRatio, myPageNo));
					}
					myAllRects = Collections.unmodifiableList(converted);
				}
			}
		}
		return myAllRects;
	}

	public List<List<RectF>> getSearchRects(String pattern) {
		if (pattern == null) {
			return Collections.emptyList();
		}

		synchronized (mySearchRectsLock) {
			if (!pattern.equals(mySearchPattern)) {
				mySearchPattern = pattern;
				final List<List<RectF>> rects = createSearchRects(pattern);
				final List<List<RectF>> scaled = new ArrayList<List<RectF>>(rects.size());
				for (List<RectF> list : rects) {
					final List<RectF> scaledList = new ArrayList<RectF>(list.size());
					for (RectF r : list) {
						scaledList.add(myDoc.rectDocumentToBmp(r, myRatio, myPageNo));
					}
					scaled.add(Collections.unmodifiableList(scaledList));
				}
				mySearchRects = Collections.unmodifiableList(scaled);
			}
			return mySearchRects;
		}
	}

	public void initFullsizeBitmapLoading() {
		synchronized (myLoaderLock) {
			if (myMainLoader == null) {
				myMainLoader = new PageBitmapLoader();
			}
		}
	}

	public Bitmap getFullsizeBitmap() {
		final PageBitmapLoader loader;
		synchronized (myLoaderLock) {
			initFullsizeBitmapLoading();
			loader = myMainLoader;
		}
		return loader.getBitmap();
	}

	public ZoomedBitmapManager getZoomedBitmapManager() {
		return myZoomedBitmapManager;
	}

	public void refreshZoomedBitmap(PluginView.ZoomInfo zoomInfo) {
		myZoomedBitmapManager.startLoading(zoomInfo);
	}

	public PageHolder.ZoomedBitmapLoader getZoomedBitmapLoader(PluginView.ZoomInfo zoomInfo) {
		return getZoomedBitmapManager().getReadyBitmapLoader(zoomInfo);
	}

	public void clearAll() {
		synchronized (myLoaderLock) {
			myMainLoader = null;
			myZoomedBitmapManager.clearBitmaps();
		}
	}

	public int getBmpHeight() {
		if (!myDoc.fullyInitialized()) {
			return Height;
		}
		float ratio = Math.max(getRealHeight() / Height, getRealWidth() / Width);
		return (int)(getRealHeight() / ratio);
	}

	public int getBmpWidth() {
		if (!myDoc.fullyInitialized()) {
			return Width;
		}
		float ratio = Math.max(getRealHeight() / Height, getRealWidth() / Width);
		return (int)(getRealWidth() / ratio);
	}

	public float getShiftX() {
		return .5f * (getAdjustedWidth() - getBmpWidth());
	}

	public float getShiftY() {
		return .5f * (Height - getBmpHeight());
	}

	protected PluginView.ChangeListener myListener;

	public void setListener(PluginView.ChangeListener c) {
		myListener = c;
	}

	public final class PageBitmapLoader {
		private volatile Bitmap myBitmap = null;
		private volatile boolean myLoaded = false;
		private final Object myMonitor = new Object();

		private PageBitmapLoader() {
			ourPageLoaderService.execute(new Runnable() {
				public void run() {
					loadBitmapInternal();
				}
			});
		}

		private void loadBitmapInternal() {
			final Bitmap bitmap = myDoc.createCleanBitmap(Width, Height, myDoc.isInverted());
			try {
				final int horizontalMargin = (int)Math.round(getShiftX());
				final int verticalMargin = (int)Math.round(getShiftY());
				draw(
					bitmap,
					new Rect(
						horizontalMargin,
						verticalMargin,
						bitmap.getWidth() - horizontalMargin,
						bitmap.getHeight() - verticalMargin
					),
					myRatio, 1
				);
				DocumentUtil.drawWallpaper(myView, new Canvas(bitmap), 0, 0, 1, true);

				myBitmap = bitmap;
				if (myListener != null) {
					myListener.onCorrectRendering();
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (myListener != null) {
					myListener.onFatalError(false);
				}
			} finally {
				synchronized (myMonitor) {
					myLoaded = true;
					myMonitor.notifyAll();
				}
			}
		}

		public Bitmap getBitmap() {
			synchronized (myMonitor) {
				if (!myLoaded) {
					try {
						myMonitor.wait();
					} catch (InterruptedException e) {
					}
				}
			}
			return myBitmap;
		}
	}

	public final class ZoomedBitmapLoader {
		private volatile Bitmap myBitmap = null;
		private final float Margin = 0.3f;

		private final ZoomedBitmapManager myManager;

		public final float ZoomFactor;
		public final float FixedX;
		public final float FixedY;

		public ZoomedBitmapLoader(ZoomedBitmapManager manager, PluginView.ZoomInfo zoomInfo) {
			myManager = manager;
			FixedX = zoomInfo.FixedX;
			FixedY = zoomInfo.FixedY;
			ZoomFactor = zoomInfo.Factor;

			ourPageLoaderService.execute(new Runnable() {
				public void run() {
					// TODO: cancel if this loader is not actual more
					loadBitmapInternal();
				}
			});
		}

		private void loadBitmapInternal() {
			final Bitmap bitmap = myDoc.createCleanBitmap(Width, Height, myDoc.isInverted());

			final float startX = (1 - ZoomFactor) * FixedX + getShiftX();
			final float startY = (1 - ZoomFactor) * FixedY + getShiftY();
			final Rect dstRect = new Rect(
				(int)Math.round(startX),
				(int)Math.round(startY),
				(int)Math.round((Width - 2 * getShiftX()) * ZoomFactor + startX),
				(int)Math.round((Height - 2 * getShiftY()) * ZoomFactor + startY)
			);
			try {
				draw(bitmap, dstRect, myRatio, ZoomFactor);
				final float left = getLeftBorder();
				final float top = getTopBorder();
				float x0 = - getShiftX();
				x0 += (FixedX * (1 - 1 / ZoomFactor) - left / getRealWidth() * getBmpWidth()) * ZoomFactor;
				float y0 = - getShiftY();
				y0 += (FixedY * (1 - 1 / ZoomFactor) - top / getRealHeight() * getBmpHeight()) * ZoomFactor;
				if (myView != null) {
					final ZLPaintContext.FillMode f = myView.getSettings().getColorProfile().FillModeOption.getValue();
					switch (f) {
						case fullscreen:
						case stretch:
							x0 = (- Math.round(getShiftX()) - left / getRealWidth() * getBmpWidth()) * ZoomFactor;
							y0 = (- Math.round(getShiftY()) - top / getRealHeight() * getBmpHeight()) * ZoomFactor;
							break;
						case tileVertically:
							x0 = (- Math.round(getShiftX()) - left / getRealWidth() * getBmpWidth()) * ZoomFactor;
							break;
						case tileHorizontally:
							y0 = (- Math.round(getShiftY()) - top / getRealHeight() * getBmpHeight()) * ZoomFactor;
							break;
						default:
							break;
					}
					DocumentUtil.drawWallpaper(myView, new Canvas(bitmap), x0, y0, ZoomFactor, true);
				}
				myBitmap = bitmap;
				System.gc();
				System.gc();
				if (myListener != null) {
					myListener.onCorrectRendering();
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
				if (myListener != null) {
					myListener.onFatalError(false);
				}
			}
			myManager.onLoaded(this);
		}

		public Bitmap getBitmapIfReady() {
			return myBitmap;
		}

		private float getLeftBorder() {
			return
				Math.max(FixedX * (ZoomFactor - 1) - getShiftX(), 0) *
				getRealWidth() / getBmpWidth() / ZoomFactor;
		}

		private float getTopBorder() {
			return
				Math.max(FixedY * (ZoomFactor - 1) - getShiftY(), 0) *
				getRealHeight() / getBmpHeight() / ZoomFactor;
		}
	}

	public class ZoomedBitmapManager {
		private volatile ZoomedBitmapLoader myCurBmp = null;
		private volatile ZoomedBitmapLoader myNewBmp = null;

		public void clearBitmaps() {
			synchronized (myLoaderLock) {
				myCurBmp = null;
				myNewBmp = null;
			}
		}

		public ZoomedBitmapLoader getReadyBitmapLoader(PluginView.ZoomInfo zoomInfo) {
			synchronized (myLoaderLock) {
				if (myCurBmp == null && myNewBmp == null) {
					startLoading(zoomInfo);
				}
				return myCurBmp;
			}
		}

		public void startLoading(PluginView.ZoomInfo zoomInfo) {
			if (!myDoc.fullyInitialized()) {
				return;
			}
			synchronized (myLoaderLock) {
				if (myNewBmp != null &&
					myNewBmp.FixedX == zoomInfo.FixedX &&
					myNewBmp.FixedY == zoomInfo.FixedY &&
					myNewBmp.ZoomFactor == zoomInfo.Factor
				) {
					return;
				}
				myNewBmp = new ZoomedBitmapLoader(this, zoomInfo);
			}
		}

		public void onLoaded(ZoomedBitmapLoader b) {
			synchronized (myLoaderLock) {
				if (b != myNewBmp) {
					return;
				}
				myCurBmp = myNewBmp;
				myNewBmp = null;
			}
			myDoc.postInvalidate();
		}
	}

	public float getRatio() {
		return myRatio;
	}

	public int getCharIndex(float x, float y) {
		final List<RectF> allRects = getAllRects();

		if (allRects.isEmpty()) {
			return -1;
		}

		int minIndex = 0;
		float minD2 = Integer.MAX_VALUE;
		for (int index = 0; index < allRects.size(); ++index) {
			final float d2 = dist2(x, y, allRects.get(index));
			if (d2 == 0) {
				return index;
			} else if (d2 < minD2) {
				minIndex = index;
				minD2 = d2;
			}
		}
		return minIndex;
	}

	private static float dist2(float x, float y, RectF rect) {
		final float dx;
		if (x < rect.left) {
			dx = rect.left - x;
		} else if (x > rect.right) {
			dx = x - rect.right;
		} else {
			dx = 0;
		}

		final float dy;
		if (y < rect.top) {
			dy = rect.top - y;
		} else if (y > rect.bottom) {
			dy = y - rect.bottom;
		} else {
			dy = 0;
		}

		return dx * dx + dy * dy;
	}
}
