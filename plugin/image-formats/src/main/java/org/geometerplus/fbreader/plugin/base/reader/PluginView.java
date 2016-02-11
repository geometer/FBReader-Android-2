package org.geometerplus.fbreader.plugin.base.reader;

import java.util.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import android.app.Activity;
import android.content.*;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.*;

import org.fbreader.common.android.FBReaderUtil;
import org.fbreader.common.options.PageTurningOptions;
import org.fbreader.reader.*;
import org.fbreader.reader.android.MainView;

import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.view.*;
import org.geometerplus.zlibrary.ui.android.view.*;
import org.geometerplus.zlibrary.ui.android.view.animation.*;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.plugin.base.*;
import org.geometerplus.fbreader.plugin.base.document.*;
import org.geometerplus.fbreader.plugin.base.reader.Footers.*;
import org.geometerplus.fbreader.plugin.base.tree.TOCActivity;
import org.geometerplus.android.fbreader.api.FBReaderIntents;

public class PluginView extends MainView implements View.OnLongClickListener, BitmapManager {
	public static final ExecutorService AuxService = Executors.newSingleThreadExecutor();

	private final Object myDocumentLock = new Object();
	private DocumentHolder myDocument = new DummyDocument(null);

	public DocumentHolder getDocument() {
		return myDocument;
	}

	public class PDFPosition {
		public final int PageNo;
		public final float FixedX;
		public final float FixedY;
		public final float Zoom;
		public final float PageZoom;

		private PDFPosition() {
			PageNo = myCurrPageNo;
			FixedX = myZoomInfo.FixedX;
			FixedY = myZoomInfo.FixedY;
			Zoom = myZoomInfo.Factor;
			final PageHolder page = getCurrentPage();
			PageZoom = (getWidth() - page.getShiftX() * 2) * myZoomInfo.Factor / page.getRealWidth();
		}
	};

	public PDFPosition getPosition() {
		return new PDFPosition();
	}

	public void gotoPosition(PDFPosition p) {
		int prevPage = myCurrPageNo;
		myCurrPageNo = p.PageNo;
		myZoomInfo.FixedX = p.FixedX;
		myZoomInfo.FixedY = p.FixedY;
		myZoomInfo.Factor = p.Zoom;
		onPageChanged(prevPage, false);
	}

	private int myScrollbarType = -1;

	private Footers.Footer myFooter = null;

	private boolean myUseWp = true;

	public boolean useWallPaper() {
		return myUseWp;
	}

	public void useWallPaper(boolean use) {
		myUseWp = use;
		post(new Runnable() {
			@Override
			public void run() {
				myDocument.clearAll();
				myAverageBgColor = DocumentUtil.getAverageBgColor();
				postInvalidate();
			}
		});
	}

	int myCurrPageNo;

	private int getMainAreaHeight() {
		final int height;
		switch (myScrollbarType) {
			case SettingsHolder.SCROLLBAR_SHOW_AS_FOOTER:
			case SettingsHolder.SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE:
				height = getHeight() - getFooterHeight();
				break;
			default:
				height = getHeight();
		}
		return height - myHDiff;
	}

	private AnimationProvider myAnimationProvider;

	public TOCTree getCurrentTOCElement(int pageNo) {
		return TOCTreeUtil.findTreeByReference(getTOCTree(), pageNo);
	}

	public TOCTree getTOCTree() {
		return myDocument.getTOCTree();
	}

	public interface ChangeListener {
		void onTouch();
		void onSelectionStart();
		void onSelectionEnd(String text, DocumentHolder.SelectionInfo selection, int length);
		void onCorrectRendering();
		void onTapZoneClick(int x, int y, int width, int height);
		void onFatalError(boolean force);
	}

	private ChangeListener myListener;
	private boolean myPendingDoubleTap;
	private boolean myLongClickPerformed;

	private ZLViewEnums.Animation myAnimationType = ZLViewEnums.Animation.none;

	private final int myDpi;
	private int myAverageBgColor;

	public PluginView(Context context) {
		super(context);
		setOnLongClickListener(this);
		startTimer();
		myDpi = getDpi((Activity)context);
	}

	public PluginView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnLongClickListener(this);
		startTimer();
		myDpi = getDpi((Activity)context);
	}

	private FBReaderPluginActivity getActivity() {
		return (FBReaderPluginActivity)getContext();
	}

	public SettingsHolder getSettings() {
		return getActivity().getSettings();
	}

	public AbstractReader getReader() {
		return getActivity().getReader();
	}

	private int getDpi(Activity activity) {
		final DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return (int)(160 * metrics.density);
	}

	public void setListener(ChangeListener l) {
		myListener = l;
		myDocument.setListener(l);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	public boolean open(Book book) {
		synchronized (myDocumentLock) {
			myDocument.close();
			myDocument = new DummyDocument(Paths.systemInfo(getContext()));
			System.gc();
			System.gc();
			myDocument = getActivity().createDocument(book);
		}

		myDocument.init(this, getWidth(), getMainAreaHeight());
		if (myDocument.open(book.getPath(), true)) {
			myCurrPageNo = 0;
			if (myFooter != null) {
				myFooter.resetTOCMarks();
			}
			myDocument.clearAll();
			return true;
		}
		return false;
	}

	public void close() {
		myDocument.clearAll();
		myDocument.close();
	}

	public void resetFooterParams() {
		SettingsHolder s = getSettings();
		if (myScrollbarType != s.ScrollbarType.getValue()) {
			myScrollbarType = s.ScrollbarType.getValue();
		}
		myDocument.init(this, getWidth(), getMainAreaHeight());
		validateZoom();
		validateShift();
		postInvalidate();
	}

	private volatile boolean myAmendSize = false;
	private volatile int myHDiff = 0;
	private volatile int myHShift = 0;
	private volatile int myStatusBarHeight = 0;

	@Override
	public void setPreserveSize(boolean preserve, int statusBarHeight) {
		myAmendSize = preserve;
		myStatusBarHeight = statusBarHeight;
		if (!preserve) {
			myHDiff = 0;
			myHShift = 0;
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (myAmendSize && oldw == w) {
			myHDiff += h - oldh;
			myHShift -= myStatusBarHeight;
		} else {
			myHDiff = 0;
			myHShift = 0;
		}

		if (myAnimationProvider != null) {
			getAnimationProvider().terminate();
		}
		myDocument.init(this, getWidth(), getMainAreaHeight());
		if (myDocument.fullyInitialized()) {
			myAverageBgColor = DocumentUtil.getAverageBgColor();
		}
		if (myDocument.opened()) {
			validateZoom();
			validateShift();
			myDocument.createSelectionWordRects(myCurrPageNo);
		}
	}

	private boolean myDrawIntersections = false;

	public void setDrawIntersections(boolean set) {
		myDrawIntersections = set;
		postInvalidate();
	}

	private void drawIntersections(Canvas canvas) {
		Paint p = new Paint();
		p.setStyle(Paint.Style.FILL);
		p.setColor(Color.CYAN);
		p.setAlpha(72);

		final int w = getWidth();
		final int h = getMainAreaHeight();
		final int w1 = w * (100 - myIntersections.XPercent) / 100;
		final int h1 = h * (100 - myIntersections.YPercent) / 100;
		canvas.drawRect(0, h1, w, h, p);
		canvas.drawRect(w1, 0, w, h, p);

		p = new Paint();
		canvas.drawLine(0, h1, w, h1, p);
		canvas.drawLine(w1, 0, w1, h, p);
	}

	private boolean myDrawBorders = false;

	public void setDrawBorders(boolean set) {
		myDrawBorders = set;
		postInvalidate();
	}

	private void drawBorders(Canvas canvas) {
		final PageHolder page = getCurrentPage();
		float x1 = 1f + page.getShiftX() + myZoomInfo.FixedX * (1 - myZoomInfo.Factor);
		float x2 = x1 + page.getBmpWidth() * myZoomInfo.Factor;
		float y1 = 1f + page.getShiftY() + myZoomInfo.FixedY * (1 - myZoomInfo.Factor);
		float y2 = y1 + page.getBmpHeight() * myZoomInfo.Factor;

		final Paint p = new Paint();
		p.setColor(Color.RED);
		canvas.drawLine(x1, y1, x1, y2, p);
		canvas.drawLine(x1, y2, x2, y2, p);
		canvas.drawLine(x2, y2, x2, y1, p);
		canvas.drawLine(x2, y1, x1, y1, p);
	}

	private Paint myPaint = new Paint();

	@Override
	protected void updateColorLevel() {
		ViewUtil.setColorLevel(myPaint, myColorLevel);
	}

	private Bitmap myFooterBitmap;

	private int getFooterHeight() {
		FooterArea f = getFooterArea();
		if (f == null) {
			return 0;
		} else {
			return f.getHeight();
		}
	}

	public Footer getFooterArea() {
		switch (getSettings().ScrollbarType.getValue()) {
			case SettingsHolder.SCROLLBAR_SHOW_AS_FOOTER:
				if (!(myFooter instanceof FooterNewStyle)) {
					if (myFooter != null) {
						removeTimerTask(myFooter.UpdateTask);
					}
					myFooter = new FooterNewStyle(this);
					addTimerTask(myFooter.UpdateTask, 15000);
				}
				break;
			case SettingsHolder.SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE:
				if (!(myFooter instanceof FooterOldStyle)) {
					if (myFooter != null) {
						removeTimerTask(myFooter.UpdateTask);
					}
					myFooter = new FooterOldStyle(this);
					addTimerTask(myFooter.UpdateTask, 15000);
				}
				break;
			default:
				if (myFooter != null) {
					removeTimerTask(myFooter.UpdateTask);
					myFooter = null;
				}
				break;
		}
		return myFooter;
	}

	private void drawFooter(Canvas canvas, AnimationProvider animator) {
		final FooterArea footer = getFooterArea();

		if (footer == null) {
			myFooterBitmap = null;
			return;
		}

		final int w = getWidth();
		final int h = footer.getHeight();
		if (myFooterBitmap != null &&
			(myFooterBitmap.getWidth() != w || myFooterBitmap.getHeight() != h)) {
			myFooterBitmap = null;
		}
		if (myFooterBitmap == null) {
			myFooterBitmap = BitmapUtil.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		}
		final Canvas bitmapCanvas = new Canvas(myFooterBitmap);
		if (!getSettings().getColorProfile().WallpaperOption.getValue().equals("")) {
			DocumentUtil.drawWallpaper(this, bitmapCanvas, 0, getMainAreaHeight(), 1, false);
		} else {
			bitmapCanvas.drawColor(getSettings().getColorProfile().BackgroundOption.getValue().intValue());
		}
		final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
			Paths.systemInfo(getContext()),
			bitmapCanvas,
			new ZLAndroidPaintContext.Geometry(
				getWidth(),
				getHeight(),
				getWidth(),
				getFooterHeight(),
				0,
				getMainAreaHeight()
			),
			isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
		footer.paint(context);
		final int voffset = getHeight() - footer.getHeight() - myHDiff;
		if (animator != null) {
			animator.drawFooterBitmap(canvas, myFooterBitmap, voffset);
		} else {
			canvas.drawBitmap(myFooterBitmap, 0, voffset, null);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		getActivity().createWakeLock();
		if (getActivity().getReader() == null) {
			return;
		}
		if (!myDocument.fullyInitialized()) {
			if (!getSettings().getColorProfile().WallpaperOption.getValue().equals("")) {
				DocumentUtil.drawWallpaper(this, canvas, 0, 0, 1, false);
			} else {
				canvas.drawColor(getSettings().getColorProfile().BackgroundOption.getValue().intValue());
			}
			return;
		}

		if (myHShift != 0) {
			canvas.translate(0, myHShift);
		}

		final AnimationProvider animator = getAnimationProvider();
		drawFooter(canvas, animator.inProgress() ? animator : null);
		final int count = canvas.save();
		canvas.clipRect(0, 0, getWidth(), getMainAreaHeight());
		if (animator.inProgress()) {
			onDrawInScrolling(canvas, myPaint);
		} else if (myZoomInfo.Factor == 1) {
			drawBitmap(canvas, 0, 0, ZLViewEnums.PageIndex.current, myPaint);
			if (myDocument.fullyInitialized()) {
				post(new Runnable() {
					public void run() {
						if (canScroll(ZLViewEnums.PageIndex.next)) {
							myDocument.getPage(myDocument.getNextPageNo(myCurrPageNo)).initFullsizeBitmapLoading();
						}
						if (canScroll(ZLViewEnums.PageIndex.previous)) {
							myDocument.getPage(myDocument.getPrevPageNo(myCurrPageNo)).initFullsizeBitmapLoading();
						}
					}
				});
			}
		} else if (myScrollingThroughPage) {
			onDrawInPageScrolling(canvas, myPaint);
		} else {
			onDrawZoomed(canvas, myPaint);
		}
		canvas.restoreToCount(count);
		if (myDrawIntersections) {
			drawIntersections(canvas);
		}
		if (myDrawBorders) {
			drawBorders(canvas);
		}
	}

	private boolean myZoomInProgress = false;

	public static class ZoomMode {

		public static final int FREE_ZOOM = 0;
		public static final int FIT_PAGE = 1;
		public static final int FIT_WIDTH = 2;
		public static final int FIT_HEIGHT = 3;
		public static final int SCREEN_ZOOM = 4;
		public static final int PAGE_ZOOM = 5;

		public final int Mode;
		public int Percent;

		public ZoomMode(int m, int p) {
			Mode = m;
			Percent = p;
		}
	}

	private ZoomMode myZoomMode = new ZoomMode(ZoomMode.FREE_ZOOM, 100);

	public void setZoomMode(ZoomMode z) {
		myZoomMode = z;
		myZoomInfo.Factor = 1f * z.Percent / 100;
		myZoomInfo.FixedX = getMinFixedX();
		myZoomInfo.FixedY = getMinFixedY();
		validateZoom();
		myDocument.refreshZoom(myCurrPageNo, myZoomInfo);
		postInvalidate();
	}

	public ZoomMode getZoomMode() {
		if (myZoomMode.Mode == ZoomMode.FREE_ZOOM) {
			myZoomMode.Percent = (int)(myZoomInfo.Factor * 100);
		}
		return myZoomMode;
	}

	public static class IntersectionsHolder {
		public final int XPercent;
		public final int YPercent;

		public IntersectionsHolder(int x, int y) {
			XPercent = x;
			YPercent = y;
		}
	}

	private IntersectionsHolder myIntersections = new IntersectionsHolder(10, 10);

	public IntersectionsHolder getIntersections() {
		return myIntersections;
	}

	public void setIntersections(IntersectionsHolder t) {
		myIntersections = t;
		postInvalidate();
	}

	public static final class ZoomInfo {
		public volatile float Factor = 1;
		public volatile float FixedX = 0;
		public volatile float FixedY = 0;

		public float xScreenToBmp(float coord, PageHolder page) {
			return (coord + FixedX * (Factor - 1) - page.getShiftX()) / Factor;
		}

		public float yScreenToBmp(float coord, PageHolder page) {
			return (coord + FixedY * (Factor - 1) - page.getShiftY()) / Factor;
		}

		public float xBmpToScreen(float coord, PageHolder page) {
			return coord * Factor - FixedX * (Factor - 1) + page.getShiftX();
		}

		public float yBmpToScreen(float coord, PageHolder page) {
			return coord * Factor - FixedY * (Factor - 1) + page.getShiftY();
		}
	}

	private final ZoomInfo myZoomInfo = new ZoomInfo();

	private float myStartFixedX = 0;
	private float myStartFixedY = 0;
	private int myPinchX = 0;
	private int myPinchY = 0;
	private int myStartPressedX;
	private int myStartPressedY;
	private float myStartPinchDistance2 = -1;
	private float myStartZoomFactor;
	private static final float MAX_ZOOM_FACTOR = 10;
	private static final float MAX_REAL_ZOOM_FACTOR = 15;
	private volatile boolean myCanScrollZoomedPage = true;

	private void startZoom(int pinchx, int pinchy, float dist) {
		final PageHolder page = getCurrentPage();

		myPinchX = pinchx;
		myPinchY = pinchy;
		myStartPinchDistance2 = dist;
		float x1 = (myZoomInfo.FixedX * (myZoomInfo.Factor - 1) / myZoomInfo.Factor);
		float realPinchX = (myPinchX - page.getShiftX()) / myZoomInfo.Factor + x1;
		float y1 = (myZoomInfo.FixedY * (myZoomInfo.Factor - 1) / myZoomInfo.Factor);
		float realPinchY = (myPinchY - page.getShiftY()) / myZoomInfo.Factor + y1;
		if ((realPinchX < 0 || realPinchX > page.getBmpWidth()) ||
			(realPinchY < 0 || realPinchY > page.getBmpHeight())) {
			return;
		}
		myStartZoomFactor = myZoomInfo.Factor;
		myStartFixedX = myZoomInfo.FixedX;
		myStartFixedY = myZoomInfo.FixedY;
		myZoomInProgress = true;
		myCanScrollZoomedPage = false;
	}

	private void zoom(float factor) {
		if (Float.isNaN(myZoomInfo.FixedX)) {
			throw new RuntimeException();
		}
		myZoomInfo.Factor = myStartZoomFactor * factor;
		validateZoom();
		if (myZoomInfo.Factor == 1) {
			myZoomInfo.FixedX = getMinFixedX();
			myZoomInfo.FixedY = getMinFixedY();
		} else {
			final PageHolder page = getCurrentPage();
			float x1 = (myStartFixedX * (myStartZoomFactor - 1) / myStartZoomFactor);
			float realPinchX = (myPinchX - page.getShiftX()) / myStartZoomFactor + x1;
			float x2 = ((realPinchX * (myZoomInfo.Factor - myStartZoomFactor) + myStartZoomFactor * x1) / myZoomInfo.Factor);
			myZoomInfo.FixedX = (x2 / (myZoomInfo.Factor - 1) * myZoomInfo.Factor);

			float y1 = (myStartFixedY * (myStartZoomFactor - 1) / myStartZoomFactor);
			float realPinchY = (myPinchY - page.getShiftY()) / myStartZoomFactor + y1;
			float y2 = ((realPinchY * (myZoomInfo.Factor - myStartZoomFactor) + myStartZoomFactor * y1) / myZoomInfo.Factor);
			myZoomInfo.FixedY = (y2 / (myZoomInfo.Factor - 1) * myZoomInfo.Factor);
		}

		myZoomMode = new ZoomMode(ZoomMode.FREE_ZOOM, (int)(myZoomInfo.Factor * 100));
		validateShift();
		postInvalidate();
	}

	private void validateZoom() {
		final PageHolder page = getCurrentPage();

		switch (myZoomMode.Mode) {
			case ZoomMode.FREE_ZOOM:
				myZoomInfo.Factor = Math.min(myZoomInfo.Factor, MAX_REAL_ZOOM_FACTOR / page.getRatio());
				myZoomInfo.Factor = Math.max(myZoomInfo.Factor, 1);
				myZoomInfo.Factor = Math.min(myZoomInfo.Factor, MAX_ZOOM_FACTOR);
				break;
			case ZoomMode.FIT_PAGE:
				myZoomInfo.Factor = 1;
				break;
			case ZoomMode.FIT_WIDTH:
			{
				final int w = getWidth();
				myZoomInfo.Factor = 1f * w / (w - page.getShiftX() * 2 + 1);
				break;
			}
			case ZoomMode.FIT_HEIGHT:
			{
				final int h = getMainAreaHeight();
				myZoomInfo.Factor = 1f * h / (h - page.getShiftY() * 2 + 1);
				break;
			}
			case ZoomMode.SCREEN_ZOOM:
				myZoomInfo.Factor = Math.max(1, 1f * myZoomMode.Percent / 100);
				break;
			case ZoomMode.PAGE_ZOOM:
				myZoomInfo.Factor = page.getRealWidth() / (getWidth() - page.getShiftX() * 2) * myZoomMode.Percent / 100;
				if (myZoomInfo.Factor < 1) {
					myZoomInfo.Factor = 1;
				}
				break;
			default:
				break;
		}
	}

	private void validateShift() {
		if (Float.isNaN(myZoomInfo.FixedX)) {
			myZoomInfo.FixedX = 0;
			myZoomInfo.FixedY = 0;
		}
		if (myZoomInfo.FixedX < getMinFixedX()) {
			myZoomInfo.FixedX = getMinFixedX();
		}
		if (myZoomInfo.FixedX > getMaxFixedX()) {
			myZoomInfo.FixedX = getMaxFixedX();
		}
		if (myZoomInfo.FixedY < getMinFixedY()) {
			myZoomInfo.FixedY = getMinFixedY();
		}
		if (myZoomInfo.FixedY > getMaxFixedY()) {
			myZoomInfo.FixedY = getMaxFixedY();
		}
		if (Float.isNaN(myZoomInfo.FixedX)) {
			throw new RuntimeException();
		}
	}

	private void stopZoom() {
		myStartPinchDistance2 = -1;
		myZoomInProgress = false;
		if (myStartZoomFactor != myZoomInfo.Factor) {
			myDocument.refreshZoom(myCurrPageNo, myZoomInfo);
		}
		postInvalidate();
		showInfo(((int)(myZoomInfo.Factor * 100)) + "%");
	}

	private boolean onDoubleTouchEvent(MotionEvent event) {
		if (getAnimationProvider().inProgress()) {
			return false;
		}
		if (myZoomMode.Mode != ZoomMode.FREE_ZOOM) {
			return false;
		}
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_POINTER_UP:
				stopZoom();
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
			{
				final int pinchx = (int)((event.getX(0) + event.getX(1)) / 2);
				final int pinchy = (int)((event.getY(0) + event.getY(1)) / 2);
				final float diffX = event.getX(0) - event.getX(1);
				final float diffY = event.getY(0) - event.getY(1);
				final float dist = Math.max(diffX * diffX + diffY * diffY, 10f);
				startZoom(pinchx, pinchy, dist);
				break;
			}
			case MotionEvent.ACTION_MOVE:
			{
				final float diffX = event.getX(0) - event.getX(1);
				final float diffY = event.getY(0) - event.getY(1);
				final float dist = Math.max(diffX * diffX + diffY * diffY, 10f);
				if (myStartPinchDistance2 < 0) {
					final int pinchx = (int)((event.getX(0) + event.getX(1)) / 2);
					final int pinchy = (int)((event.getY(0) + event.getY(1)) / 2);
					startZoom(pinchx, pinchy, dist);
				} else {
					zoom(dist / myStartPinchDistance2);
				}
				break;
			}
		}
		return true;
	}

	private void onDrawZoomed(Canvas canvas, Paint bmpPaint) {
		final PageHolder page = getCurrentPage();

		Bitmap pageBitmap = myDocument.getWholeBitmap(myCurrPageNo);
		if (pageBitmap != null) {
			final Rect src = new Rect(
				(int)Math.round(myZoomInfo.xScreenToBmp(0, page) + page.getShiftX()),
				(int)Math.round(myZoomInfo.yScreenToBmp(0, page) + page.getShiftY()),
				(int)Math.round(myZoomInfo.xScreenToBmp(page.Width, page) + page.getShiftX()),
				(int)Math.round(myZoomInfo.yScreenToBmp(page.Height, page) + page.getShiftY())
			);
			final Rect dst = new Rect(0, 0, page.Width, page.Height);
			canvas.drawBitmap(pageBitmap, src, dst, bmpPaint);
			pageBitmap = null;
		}

		final PageHolder.ZoomedBitmapLoader loader = page.getZoomedBitmapLoader(myZoomInfo);
		if (loader != null && loader.ZoomFactor == myZoomInfo.Factor) {
			final Bitmap zoomedBitmap = loader.getBitmapIfReady();
			if (zoomedBitmap != null) {
				canvas.drawBitmap(
					zoomedBitmap,
					(1 - loader.ZoomFactor) * (myZoomInfo.FixedX - loader.FixedX),
					(1 - loader.ZoomFactor) * (myZoomInfo.FixedY - loader.FixedY),
					bmpPaint
				);
			}
		}

		final ZLPaintContext context = mainContext(canvas);
		drawSearchResults(context, page, 0, 0);
		drawSelection(context, 0, 0);
		drawBookmarks(context, myCurrPageNo, 0, 0);
	}

	private int myScrollMarginX = 0;
	private int myScrollMarginY = 0;

	public Bitmap getBitmap(ZLViewEnums.PageIndex index) {
		return getBitmap(getPageNo(index));
	}

	private Bitmap getBitmap(int pageNo) {
		if (pageNo < 0 || pageNo >= myDocument.getPageCount()) {
			return null;
		}
		return myDocument.getWholeBitmap(pageNo);
	}

	private int getPageNo(ZLViewEnums.PageIndex index) {
		switch (index) {
			case previous:
				return myDocument.getPrevPageNo(myCurrPageNo);
			case next:
				return myDocument.getNextPageNo(myCurrPageNo);
			default:
				return myCurrPageNo;
		}
	}

	final ZLPaintContext mainContext(Canvas canvas) {
		return new ZLAndroidPaintContext(
			Paths.systemInfo(getContext()),
			canvas,
			new ZLAndroidPaintContext.Geometry(
				getWidth(),
				getHeight(),
				getWidth(),
				getMainAreaHeight(),
				0,
				0
			),
			isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
	}

	public void drawBitmap(Canvas canvas, int x, int y, ZLViewEnums.PageIndex index, Paint paint) {
		final int pageNo = getPageNo(index);

		final Bitmap bmp = getBitmap(pageNo);
		if (bmp == null) {
			return;
		}
		canvas.drawBitmap(bmp, x, y, paint);

		final PageHolder page = myDocument.getPage(pageNo);
		final ZLPaintContext context = mainContext(canvas);
		drawSearchResults(context, page, x, y);
		if (index == ZLViewEnums.PageIndex.current) {
			drawSelection(context, x, y);
		}
		drawBookmarks(context, pageNo, x, y);
	}

	private float getMinFixedX() {
		if (!myDocument.fullyInitialized()) {
			return 0;
		}
		final PageHolder page = getCurrentPage();
		final float w = page.getBmpWidth();
		if (w * myZoomInfo.Factor <= getWidth()) {
			return w / 2;
		} else {
			return page.getShiftX() / (myZoomInfo.Factor - 1);
		}
	}

	private float getMaxFixedX() {
		if (!myDocument.fullyInitialized()) {
			return 0;
		}
		final PageHolder page = getCurrentPage();
		final float w = page.getBmpWidth();
		if (w * myZoomInfo.Factor <= getWidth()) {
			return w / 2;
		} else {
			return w - page.getShiftX() / (myZoomInfo.Factor - 1);
		}
	}

	private float getMinFixedY() {
		if (!myDocument.fullyInitialized()) {
			return 0;
		}
		final PageHolder page = getCurrentPage();
		final float h = page.getBmpHeight();
		if (h * myZoomInfo.Factor <= getMainAreaHeight()) {
			return h / 2;
		} else {
			return page.getShiftY() / (myZoomInfo.Factor - 1);
		}
	}

	private float getMaxFixedY() {
		if (!myDocument.fullyInitialized()) {
			return 0;
		}
		final PageHolder page = getCurrentPage();
		final float h = page.getBmpHeight();
		if (h * myZoomInfo.Factor <= getMainAreaHeight()) {
			return h / 2;
		} else {
			return h - page.getShiftY() / (myZoomInfo.Factor - 1);
		}
	}

	private boolean isEndOfX() {
		return myZoomInfo.FixedX >= getMaxFixedX();
	}

	private boolean isEndOfY() {
		return myZoomInfo.FixedY >= getMaxFixedY();
	}

	private boolean isStartOfX() {
		return myZoomInfo.FixedX <= getMinFixedX();
	}

	private boolean isStartOfY() {
		return myZoomInfo.FixedY <= getMinFixedY();
	}

	private float getNextFixedX() {
		float maxx = getMaxFixedX();
		if (myZoomInfo.FixedX == maxx) {
			return getMinFixedX();
		}
		float delta = getWidth() * (1 - 1f * myIntersections.XPercent / 100);
		float newX = (myZoomInfo.FixedX + delta / (myZoomInfo.Factor - 1));
		if (newX > maxx) {
			newX = maxx;
		}
		return newX;
	}

	private float getNextFixedY() {
		float maxy = getMaxFixedY();
		if (myZoomInfo.FixedY == maxy) {
			return getMinFixedY();
		}
		float delta = getMainAreaHeight() * (1 - 1f * myIntersections.YPercent / 100);
		float newY = (myZoomInfo.FixedY + delta / (myZoomInfo.Factor - 1));
		if (newY > maxy) {
			newY = maxy;
		}
		return newY;
	}

	private float getPrevFixedX() {
		float minx = getMinFixedX();
		if (myZoomInfo.FixedX == minx) {
			return getMaxFixedX();
		}
		float delta = getWidth() * (1 - 1f * myIntersections.XPercent / 100);
		float newX = (myZoomInfo.FixedX - delta / (myZoomInfo.Factor - 1));
		if (newX < minx) {
			newX = minx;
		}
		return newX;
	}

	private float getPrevFixedY() {
		float miny = getMinFixedY();
		if (myZoomInfo.FixedY == miny) {
			return getMaxFixedY();
		}
		float delta = getMainAreaHeight() * (1 - 1f * myIntersections.YPercent / 100);
		float newY = (myZoomInfo.FixedY - delta / (myZoomInfo.Factor - 1));
		if (newY < miny) {
			newY = miny;
		}
		return newY;
	}

	private boolean myScrollingThroughPage = false;
	private boolean myForwardThroughPage = true;
	private float myNextFixedX;
	private float myNextFixedY;
	private int myStepCounter = 0;

	private boolean myHorizontalFirst = true;

	public boolean isHorizontalFirst() {
		return myHorizontalFirst;
	}

	public void setHorizontalFirst(boolean h) {
		myHorizontalFirst = h;
	}

	private void setUpInPageScrolling(boolean forward) {
		myStartFixedX = myZoomInfo.FixedX;
		myStartFixedY = myZoomInfo.FixedY;
		myForwardThroughPage = forward;
		if (myForwardThroughPage) {
			if (myHorizontalFirst) {
				if (!isEndOfX()) {
					myNextFixedX = getNextFixedX();
					myNextFixedY = myZoomInfo.FixedY;
				} else {
					if (!isEndOfY()) {
						myNextFixedX = getNextFixedX();
						myNextFixedY = getNextFixedY();
					} else {
						myNextFixedX = myZoomInfo.FixedX;
						myNextFixedY = myZoomInfo.FixedY;
					}
				}
			} else {
				if (!isEndOfY()) {
					myNextFixedY = getNextFixedY();
					myNextFixedX = myZoomInfo.FixedX;
				} else {
					if (!isEndOfX()) {
						myNextFixedY = getNextFixedY();
						myNextFixedX = getNextFixedX();
					} else {
						myNextFixedY = myZoomInfo.FixedY;
						myNextFixedX = myZoomInfo.FixedX;
					}
				}
			}
		} else {
			if (myHorizontalFirst) {
				if (!isStartOfX()) {
					myNextFixedX = getPrevFixedX();
					myNextFixedY = myZoomInfo.FixedY;
				} else {
					if (!isStartOfY()) {
						myNextFixedX = getPrevFixedX();
						myNextFixedY = getPrevFixedY();
					} else {
						myNextFixedX = myZoomInfo.FixedX;
						myNextFixedY = myZoomInfo.FixedY;
					}
				}
			} else {
				if (!isStartOfY()) {
					myNextFixedY = getPrevFixedY();
					myNextFixedX = myZoomInfo.FixedX;
				} else {
					if (!isStartOfX()) {
						myNextFixedY = getPrevFixedY();
						myNextFixedX = getPrevFixedX();
					} else {
						myNextFixedY = myZoomInfo.FixedY;
						myNextFixedX = myZoomInfo.FixedX;
					}
				}
			}
		}
		myScrollingThroughPage = true;
		myStepCounter = 0;
	}

	private void doInPageScrollingStep() {
		if (myAnimationType == ZLViewEnums.Animation.none) {
			stopInPageScrolling();
			return;
		}
		final int animationSpeed = getReader().PageTurningOptions.AnimationSpeed.getValue();
		myStepCounter++;
		myZoomInfo.FixedX = myStartFixedX + ((1f * myNextFixedX - myStartFixedX) * myStepCounter * animationSpeed / 15);
		myZoomInfo.FixedY = myStartFixedY + ((1f * myNextFixedY - myStartFixedY) * myStepCounter * animationSpeed / 15);
		if (myStepCounter * animationSpeed > 15) {
			stopInPageScrolling();
		}
	}

	private void stopInPageScrolling() {
		myZoomInfo.FixedX = myNextFixedX;
		myZoomInfo.FixedY = myNextFixedY;
		myScrollingThroughPage = false;
		myDocument.refreshZoom(myCurrPageNo, myZoomInfo);
	}

	private void onDrawInPageScrolling(Canvas canvas, Paint bmpPaint) {
		onDrawZoomed(canvas, bmpPaint);
		doInPageScrollingStep();
		postInvalidate();
	}

	private void onDrawInScrolling(Canvas canvas, Paint bmpPaint) {
		final AnimationProvider animator = getAnimationProvider();
		final AnimationProvider.Mode mode = animator.getMode();
		animator.doStep();
		if (animator.inProgress()) {
			final int count = canvas.save();
			canvas.translate(myScrollMarginX, myScrollMarginY);
			canvas.clipRect(0, 0, getWidth() - 2 * myScrollMarginX, getMainAreaHeight() - 2 * myScrollMarginY);
			animator.draw(canvas);
			canvas.restoreToCount(count);
			if (animator.getMode().Auto) {
				postInvalidate();
			}
		} else {
			switch (mode) {
				case AnimatedScrollingForward:
				{
					final ZLViewEnums.PageIndex index = animator.getPageToScrollTo();
					onScrollingFinished(index);
					break;
				}
				case AnimatedScrollingBackward:
					onScrollingFinished(ZLViewEnums.PageIndex.current);
					break;
				case ManualScrolling:
					break;
				case NoScrolling:
					break;
				default:
					break;
			}
			drawBitmap(canvas, 0, 0, ZLViewEnums.PageIndex.current, bmpPaint);
		}
	}

	private void onScrollingFinished(ZLViewEnums.PageIndex index) {
		switch (index) {
			case current:
				break;
			case previous:
				gotoPage(myDocument.getPrevPageNo(myCurrPageNo), true);
				break;
			case next:
				gotoPage(myDocument.getNextPageNo(myCurrPageNo), false);
				break;
		}
	}

	private final Paint myRectPaint = new Paint();

	private void drawSearchResults(ZLPaintContext context, PageHolder page, int dx, int dy) {
		final List<List<RectF>> searchRectangles = page.getSearchRects(myPattern);
		if (searchRectangles.isEmpty()) {
			return;
		}

		context.setFillColor(getSettings().getColorProfile().HighlightingBackgroundOption.getValue(), 128);
		for (int i = 0; i < searchRectangles.size(); ++i) {
			drawHighlighting(context, searchRectangles.get(i), page, dx, dy);
		}
	}

	private void drawSelection(ZLPaintContext context, int dx, int dy) {
		final PageHolder page = getCurrentPage();

		final List<RectF> selectionRectangles = myDocument.Selection.rectangles();
		if (selectionRectangles.isEmpty()) {
			return;
		}

		final ZLColor color = getSettings().getColorProfile().SelectionBackgroundOption.getValue();
		context.setFillColor(color, 128);
		drawHighlighting(context, selectionRectangles, page, dx, dy);

		SelectionCursor.draw(
			context,
			SelectionCursor.Which.Left,
			(int)(.5f + myZoomInfo.xBmpToScreen(myDocument.getFirstSelectionCursorX(), page) + dx),
			(int)(.5f + myZoomInfo.yBmpToScreen(myDocument.getFirstSelectionCursorY(), page) + dy),
			color
		);
		SelectionCursor.draw(
			context,
			SelectionCursor.Which.Right,
			(int)(.5f + myZoomInfo.xBmpToScreen(myDocument.getLastSelectionCursorX(), page) + dx),
			(int)(.5f + myZoomInfo.yBmpToScreen(myDocument.getLastSelectionCursorY(), page) + dy),
			color
		);
	}

	private void drawBookmarks(ZLPaintContext context, int pageNo, int dx, int dy) {
		final List<DocumentHolder.BookmarkHighlighting> hilites =
			myDocument.getBookmarkHilites(pageNo);
		if (hilites == null) {
			return;
		}

		final PageHolder page = myDocument.getPage(pageNo);
		for (DocumentHolder.BookmarkHighlighting h : hilites) {
			final Bookmark b = h.Bookmark;

			final HighlightingStyle style = getStyle(b.getStyleId());
			if (style == null) {
				continue;
			}
			final ZLColor color = style.getBackgroundColor();
			if (color == null) {
				continue;
			}
			context.setFillColor(color, 128);
			drawHighlighting(context, h.Rects, page, dx, dy);
		}
	}

	private void drawHighlighting(ZLPaintContext context, List<RectF> highlighting, PageHolder page, int dx, int dy) {
		for (RectF r : highlighting) {
			final Rect scaled = new Rect(
				(int)(.5f + myZoomInfo.xBmpToScreen(r.left, page) + dx),
				(int)(.5f + myZoomInfo.yBmpToScreen(r.top, page) + dy),
				(int)(.5f + myZoomInfo.xBmpToScreen(r.right, page) + dx),
				(int)(.5f + myZoomInfo.yBmpToScreen(r.bottom, page) + dy)
			);
			context.fillRectangle(scaled.left, scaled.top, scaled.right, scaled.bottom);
		}
	}

	private class LongClickRunnable implements Runnable {
		public void run() {
			if (performLongClick()) {
				myLongClickPerformed = true;
			}
		}
	}
	private volatile LongClickRunnable myPendingLongClickRunnable;
	private boolean myPendingPress;
	public int myPressedX;
	public int myPressedY;

	private void postLongClickRunnable() {
		myLongClickPerformed = false;
		myPendingPress = false;
		if (myPendingLongClickRunnable == null) {
			myPendingLongClickRunnable = new LongClickRunnable();
		}
		postDelayed(myPendingLongClickRunnable, 2 * ViewConfiguration.getLongPressTimeout());
	}

	private class ShortClickRunnable implements Runnable {
		public void run() {
			onFingerSingleTap(myPressedX, myPressedY);
			myPendingPress = false;
			myPendingShortClickRunnable = null;
		}
	}
	private volatile ShortClickRunnable myPendingShortClickRunnable;
	private boolean myScreenIsTouched;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!myDocument.fullyInitialized()) {
			return false;
		}
		if (myScrollingThroughPage || (getAnimationProvider().inProgress() && getAnimationProvider().getMode().Auto)) {
			return false;
		}

		if (event.getAction() == MotionEvent.ACTION_CANCEL) {
			myPendingDoubleTap = false;
			myPendingPress = false;
			myScreenIsTouched = false;
			myLongClickPerformed = false;
			if (myPendingShortClickRunnable != null) {
				removeCallbacks(myPendingShortClickRunnable);
				myPendingShortClickRunnable = null;
			}
			if (myPendingLongClickRunnable != null) {
				removeCallbacks(myPendingLongClickRunnable);
				myPendingLongClickRunnable = null;
			}
			onFingerEventCancelled();
			return true;
		}

		switch (event.getPointerCount()) {
			case 1:
				return onSingleTouchEvent(event);
			case 2:
				return onDoubleTouchEvent(event);
			default:
				return false;
		}
	}

	public boolean onSingleTouchEvent(MotionEvent event) {
		if (myListener != null) {
			myListener.onTouch();
		}
		int x = (int)event.getX();
		int y = (int)event.getY();

		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				if (myPendingDoubleTap) {
					onFingerDoubleTap(x, y);
				} else if (myLongClickPerformed) {
					onFingerReleaseAfterLongPress(x, y);
				} else {
					if (myPendingLongClickRunnable != null) {
						removeCallbacks(myPendingLongClickRunnable);
						myPendingLongClickRunnable = null;
					}
					if (myPendingPress) {
						if (isDoubleTapSupported()) {
							if (myPendingShortClickRunnable == null) {
								myPendingShortClickRunnable = new ShortClickRunnable();
							}
							postDelayed(myPendingShortClickRunnable, ViewConfiguration.getDoubleTapTimeout());
						} else {
							onFingerSingleTap(x, y);
						}
					} else {
						onFingerRelease(x, y);
					}
				}
				myCanScrollZoomedPage = true;
				myDocument.refreshZoom(myCurrPageNo, myZoomInfo);
				myPendingDoubleTap = false;
				myPendingPress = false;
				myScreenIsTouched = false;
				break;
			case MotionEvent.ACTION_DOWN:
				if (myPendingShortClickRunnable != null) {
					removeCallbacks(myPendingShortClickRunnable);
					myPendingShortClickRunnable = null;
					myPendingDoubleTap = true;
				} else {
					postLongClickRunnable();
					myPendingPress = true;
				}
				myScreenIsTouched = true;
				myPressedX = x;
				myPressedY = y;
				break;
			case MotionEvent.ACTION_MOVE:
			{
				final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
				final boolean isAMove =
					Math.abs(myPressedX - x) > slop || Math.abs(myPressedY - y) > slop;
				if (isAMove) {
					myPendingDoubleTap = false;
				}
				if (myLongClickPerformed) {
					onFingerMoveAfterLongPress(x, y);
				} else {
					if (myPendingPress) {
						if (isAMove) {
							if (myPendingShortClickRunnable != null) {
								removeCallbacks(myPendingShortClickRunnable);
								myPendingShortClickRunnable = null;
							}
							if (myPendingLongClickRunnable != null) {
								removeCallbacks(myPendingLongClickRunnable);
							}
							onFingerPress(myPressedX, myPressedY);
							myPendingPress = false;
						}
					}
					if (!myPendingPress) {
						onFingerMove(x, y);
					}
				}
				break;
			}
		}

		return true;
	}

	private synchronized boolean onFingerMove(int x, int y) {
		if (mySelectionInProgress) {
			extendSelection(x, y);
			return true;
		}

		switch (mySlideMode) {
			case none:
			{
				final float maxDist = myDpi / 12;
				final float xDiff = Math.abs(x - myStartPressedX);
				final float yDiff = Math.abs(y - myStartPressedY);
				if (yDiff >= maxDist && xDiff <= maxDist / 1.5f && x < getWidth() / 10 &&
					getReader().MiscOptions.AllowScreenBrightnessAdjustment.getValue()) {
					myStartBrightness = getScreenBrightness();
					mySlideMode = SlideMode.brightnessAdjustment;
				} else if (xDiff >= maxDist || yDiff >= maxDist) {
					final PageHolder page = getCurrentPage();

					if (myZoomInfo.Factor == 1 &&
						x > 0 && x < getWidth() &&
						y > 0 && y < getMainAreaHeight()
					) {
						x -= page.getShiftX();
						y -= page.getShiftY();
						startManualScrolling(x, y);
						mySlideMode = SlideMode.pageTurning;
					} else if (myCanScrollZoomedPage) {
						myStartFixedX = myZoomInfo.FixedX;
						myStartFixedY = myZoomInfo.FixedY;
						mySlideMode = SlideMode.zoomSliding;
					}
				}
				break;
			}
			case brightnessAdjustment:
			{
				final int delta = (myStartBrightness + 30) * (myStartPressedY - y) / getMainAreaHeight();
				setScreenBrightness(myStartBrightness + delta, true);
				break;
			}
			case pageTurning:
			{
				final PageHolder page = getCurrentPage();
				if (myZoomInfo.Factor == 1 && isFlickScrollingEnabled()) {
					x -= page.getShiftX();
					y -= page.getShiftY();
					scrollManuallyTo(x, y);
				}
				break;
			}
			case zoomSliding:
			{
				final PageHolder page = getCurrentPage();
				if (myCanScrollZoomedPage) {
					myZoomInfo.FixedX = ((myStartPressedX - x) / (myZoomInfo.Factor - 1)) + myStartFixedX;
					myZoomInfo.FixedY = ((myStartPressedY - y) / (myZoomInfo.Factor - 1)) + myStartFixedY;
					validateShift();
					postInvalidate();
				}
				break;
			}
			case stopped:
				break;
		}
		return true;
	}

	private boolean isFlickScrollingEnabled() {
		switch (getReader().PageTurningOptions.FingerScrolling.getValue()) {
			case byFlick:
			case byTapAndFlick:
				return true;
			default:
				return false;
		}
	}

	private enum SlideMode {
		none,
		brightnessAdjustment,
		pageTurning,
		zoomSliding,
		stopped
	}

	private SlideMode mySlideMode = SlideMode.none;

	private int myStartBrightness;

	private boolean onFingerPress(int x, int y) {
		if (myDocument.getSelectionCursor(myCurrPageNo, x, y, myZoomInfo)) {
			mySelectionInProgress = true;
			if (myListener != null) {
				myListener.onSelectionStart();
			}
			extendSelection(x, y);
		}

		mySlideMode = SlideMode.none;
		myStartPressedX = x;
		myStartPressedY = y;

		return true;
	}

	private void startManualScrolling(int x, int y) {
		final ZLViewEnums.Direction direction = getReader().PageTurningOptions.Horizontal.getValue()
			? ZLViewEnums.Direction.rightToLeft
			: ZLViewEnums.Direction.up;
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight(), myColorLevel);
		animator.startManualScrolling(x, y);
	}

	private boolean isDoubleTapSupported() {
		return false;
	}

	private void startAnimatedScrolling(int x, int y, int speed) {
		final AnimationProvider animator = getAnimationProvider();
		if (!canScroll(animator.getPageToScrollTo(x, y))) {
			animator.terminate();
			return;
		}
		animator.startAnimatedScrolling(x, y, speed);
		postInvalidate();
	}

	private void startAnimatedScrolling(ZLViewEnums.PageIndex pageIndex) {
		final PageTurningOptions options = getReader().PageTurningOptions;
		if (pageIndex == ZLViewEnums.PageIndex.current || !canScroll(pageIndex)) {
			return;
		}
		final AnimationProvider animator = getAnimationProvider();
		final ZLViewEnums.Direction direction = options.Horizontal.getValue()
			? ZLViewEnums.Direction.rightToLeft
			: ZLViewEnums.Direction.up;

		final PageHolder page = getCurrentPage();
		animator.setup(direction, (int)(getWidth() - page.getShiftX() * 2), (int)(getMainAreaHeight() - page.getShiftY() * 2), myColorLevel);
		animator.startAnimatedScrolling(pageIndex, null, null, options.AnimationSpeed.getValue());
		if (animator.getMode().Auto) {
			postInvalidate();
		}
	}

	public void gotoPrevPage() {
		if (getAnimationProvider().inProgress() || myScrollingThroughPage) {
			return;
		}
		if (myZoomInfo.Factor == 1) {
			startAnimatedScrolling(ZLViewEnums.PageIndex.previous);
		} else {
			if (isStartOfX() && isStartOfY()) {
				myCanScrollZoomedPage = true;
				startAnimatedScrolling(ZLViewEnums.PageIndex.previous);
			} else {
				setUpInPageScrolling(false);
				postInvalidate();
			}
		}
	}

	public void gotoNextPage() {
		if (getAnimationProvider().inProgress() || myScrollingThroughPage) {
			return;
		}
		if (myZoomInfo.Factor == 1) {
			startAnimatedScrolling(ZLViewEnums.PageIndex.next);
		} else {
			if (isEndOfX() && isEndOfY()) {
				myCanScrollZoomedPage = true;
				startAnimatedScrolling(ZLViewEnums.PageIndex.next);
			} else {
				setUpInPageScrolling(true);
				postInvalidate();
			}
		}
	}

	private void onFingerEventCancelled() {
		if (mySelectionInProgress) {
			mySelectionInProgress = false;
			if (myListener != null) {
				final String txt = getCurrentPage().getSelectionText();
				if (txt != null) {
					myListener.onSelectionEnd(
						txt,
						myDocument.Selection,
						myDocument.getPageCharNumInternal(myCurrPageNo)
					);
				}
			}
			postInvalidate();
		}

		mySlideMode = SlideMode.stopped;
	}

	private void onFingerRelease(int x, int y) {
		if (mySelectionInProgress) {
			mySelectionInProgress = false;
			if (myListener != null) {
				final String txt = getCurrentPage().getSelectionText();
				if (txt != null) {
					myListener.onSelectionEnd(
						txt,
						myDocument.Selection,
						myDocument.getPageCharNumInternal(myCurrPageNo)
					);
				}
			}
			postInvalidate();
			return;
		}

		switch (mySlideMode) {
			case pageTurning:
				if (myZoomInfo.Factor == 1 && isFlickScrollingEnabled()) {
					final PageHolder page = getCurrentPage();
					x -= page.getShiftX();
					x = Math.min(Math.max(0, x), (int)(getWidth() - page.getShiftX() * 2 - 1));
					y -= page.getShiftY();
					y = Math.min(Math.max(0, y), (int)(getMainAreaHeight() - page.getShiftY() * 2 - 1));
					startAnimatedScrolling(x, y, getReader().PageTurningOptions.AnimationSpeed.getValue());
				}
		}
		mySlideMode = SlideMode.stopped;
	}

	public void onFingerSingleTap(int x, int y) {
		if (!myCanScrollZoomedPage) {
			return;
		}
		final Bookmark bookmark = myDocument.checkBookmark(myCurrPageNo, x, y, myZoomInfo);
		if (bookmark != null) {
			getActivity().showBookmarkToast(bookmark);
			return;
		}

		if (getActivity().isToastShown()) {
			getActivity().hideToast();
			return;
		}

		final PageHolder page = getCurrentPage();
		final int pageNo = page.checkInternalPageLink(
			myZoomInfo.xScreenToBmp(x, page),
			myZoomInfo.yScreenToBmp(y, page)
		);
		if (pageNo != -1) {
			gotoPage(pageNo, false);
			if (mySelectionInProgress) {
				getActivity().hideActivePopup();
				clearSelection();
			}
			return;
		}
		String url = page.checkHyperLink(
			myZoomInfo.xScreenToBmp(x, page),
			myZoomInfo.yScreenToBmp(y, page)
		);
		if (url != null) {
			if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("mailto:")) {
				url = "http://" + url;
			}
			final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			try {
				getContext().startActivity(intent);
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
			}
			if (mySelectionInProgress) {
				getActivity().hideActivePopup();
				clearSelection();
			}
			return;
		}
		if (mySelectionInProgress) {
			getActivity().hideActivePopup();
			clearSelection();
		}
		if (myListener != null) {
			myListener.onTapZoneClick(x, y, getWidth(), getMainAreaHeight());
		}
	}

	private void onFingerDoubleTap(int x, int y) {
	}

	protected final BitmapManager getBitmapManager() {
		return this;
	}

	private int myStoredLayerType = -1;
	private AnimationProvider getAnimationProvider() {
		final ZLViewEnums.Animation type = getReader().PageTurningOptions.Animation.getValue();
		if (myAnimationProvider == null || myAnimationType != type) {
			myAnimationType = type;
			if (myStoredLayerType != -1) {
				setLayerType(myStoredLayerType, null);
			}
			switch (type) {
				case none:
					myAnimationProvider = new NoneAnimationProvider(getBitmapManager());
					break;
				case curl:
					myStoredLayerType = getLayerType();
					myAnimationProvider = new CurlAnimationProvider(getBitmapManager());
					setLayerType(LAYER_TYPE_SOFTWARE, null);
					break;
				case slide:
					myAnimationProvider = new SlideAnimationProvider(getBitmapManager());
					break;
				case slideOldStyle:
					myAnimationProvider = new SlideOldStyleAnimationProvider(getBitmapManager());
					break;
				case shift:
					myAnimationProvider = new ShiftAnimationProvider(getBitmapManager());
					break;
			}
		}
		return myAnimationProvider;
	}

	public boolean canScroll(ZLViewEnums.PageIndex index) {
		if (!myDocument.opened()) {
			return false;
		}
		switch (index) {
			case next:
				return myDocument.getNextPageNo(myCurrPageNo) < myDocument.getPageCount();
			case previous:
				return myDocument.getPrevPageNo(myCurrPageNo) >= 0;
			default:
				return true;
		}
	}

	public void scrollManuallyTo(int x, int y) {
		final AnimationProvider animator = getAnimationProvider();
		if (canScroll(animator.getPageToScrollTo(x, y))) {
			animator.scrollTo(x, y);
			postInvalidate();
		}
	}

	private void onPageChanged(int prevPage, boolean toEnd) {
		myDocument.gotoPage(myCurrPageNo);
		setSearchWordRects(toEnd);
		getActivity().onPageChanged();
		myAverageBgColor = DocumentUtil.getAverageBgColor();
		validateZoom();
		validateShift();
		postInvalidate();
		final AbstractReader reader = getActivity().getReader();
		if (reader != null) {
			reader.storePosition();
		}
	}

	private void setSearchWordRects(boolean toEnd) {
		final Map<Integer,Boolean> pagesFound = myPagesFound;
		if (pagesFound != null) {
			Boolean found = pagesFound.get(myCurrPageNo);
			if (found == null) {
				found = getCurrentPage().matches(myPattern);
				pagesFound.put(myCurrPageNo, found);
			}
			if (found) {
				final List<List<RectF>> searchRectangles = getCurrentPage().getSearchRects(myPattern);
				if (!searchRectangles.isEmpty()) {
					myHLWordNum = toEnd ? searchRectangles.size() - 1 : 0;
					showSearchResult(searchRectangles, myHLWordNum);
				}
			}
		}
	}

	public synchronized void gotoPage(int pageIndex, boolean toEnd) {
		if (!myDocument.opened()) {
			return;
		}
		if (pageIndex < 0 || pageIndex >= myDocument.getPageCount()) {
			return;
		}
		final int prevPage = myCurrPageNo;
		myCurrPageNo = pageIndex;
		if (myDocument.fullyInitialized()) {
			if (toEnd) {
				myZoomInfo.FixedX = getMaxFixedX();
				myZoomInfo.FixedY = getMaxFixedY();
			} else {
				myZoomInfo.FixedX = getMinFixedX();
				myZoomInfo.FixedY = getMinFixedY();
			}
		}
		onPageChanged(prevPage, toEnd);
	}

	public int getCurPageNo() {
		return myCurrPageNo;
	}

	public int getPagesNum() {
		return myDocument.getPageCount();
	}

	private String myPattern = null;
	private volatile Map<Integer,Boolean> myPagesFound = null;
	private int myHLWordNum = 0;

	public boolean startSearch(String pattern) {
		myPattern = pattern;

		final Map<Integer,Boolean> pagesFound = new HashMap<Integer,Boolean>();
		myPagesFound = pagesFound;

		final int firstPageNo = findFirstPage(pagesFound, myCurrPageNo);
		if (firstPageNo != -1) {
			gotoPage(firstPageNo, false);
			return true;
		} else if (findPreviousPage(pagesFound, myCurrPageNo) != -1) {
			gotoPage(myCurrPageNo, false);
			return true;
		}
		stopSearch();
		return false;
	}

	private int findFirstPage(Map<Integer,Boolean> pagesFound, int startNo) {
		for (int pageNo = startNo; pageNo < myDocument.getPageCount(); ++pageNo) {
			final boolean found = myDocument.getPage(pageNo).matches(myPattern);
			pagesFound.put(pageNo, found);
			if (found) {
				return pageNo;
			}
		}
		return -1;
	}

	private int findNextPage(Map<Integer,Boolean> pagesFound, int startNo) {
		for (int pageNo = myDocument.getNextPageNo(startNo); pageNo < myDocument.getPageCount(); pageNo = myDocument.getNextPageNo(pageNo)) {
			Boolean found = pagesFound.get(pageNo);
			if (found == null) {
				found = myDocument.getPage(pageNo).matches(myPattern);
				pagesFound.put(pageNo, found);
			}
			if (found) {
				return pageNo;
			}
		}
		return -1;
	}

	private int findPreviousPage(Map<Integer,Boolean> pagesFound, int startNo) {
		for (int pageNo = myDocument.getPrevPageNo(startNo); pageNo >= 0; pageNo = myDocument.getPrevPageNo(pageNo)) {
			Boolean found = pagesFound.get(pageNo);
			if (found == null) {
				found = myDocument.getPage(pageNo).matches(myPattern);
				pagesFound.put(pageNo, found);
			}
			if (found) {
				return pageNo;
			}
		}
		return -1;
	}

	public void stopSearch() {
		myPagesFound = null;
		myPattern = null;
		postInvalidate();
	}

	public boolean canFindNext() {
		final Map<Integer,Boolean> pagesFound = myPagesFound;
		if (pagesFound == null) {
			return false;
		}
		if (myHLWordNum < getCurrentPage().getSearchRects(myPattern).size() - 1) {
			return true;
		}
		return findNextPage(pagesFound, myCurrPageNo) != -1;
	}

	public boolean canFindPrev() {
		final Map<Integer,Boolean> pagesFound = myPagesFound;
		if (pagesFound == null) {
			return false;
		}
		if (!getCurrentPage().getSearchRects(myPattern).isEmpty() && myHLWordNum > 0) {
			return true;
		}
		return findPreviousPage(pagesFound, myCurrPageNo) != -1;
	}

	private void showSearchResult(List<List<RectF>> searchRectangles, int index) {
		final RectF start = searchRectangles.get(index).get(0);
		final float x = start.left;
		final float y = start.top;
		if (myZoomInfo.Factor > 1) {
			final PageHolder page = getCurrentPage();
			myZoomInfo.FixedX = ((-1.f * getWidth() / 2 + page.getShiftX() + myZoomInfo.Factor * x) / (myZoomInfo.Factor - 1));
			myZoomInfo.FixedY = ((-1.f * getMainAreaHeight() / 2 + page.getShiftY() + myZoomInfo.Factor * y) / (myZoomInfo.Factor - 1));
		}
		validateShift();
		myDocument.refreshZoom(myCurrPageNo, myZoomInfo);
		postInvalidate();
	}

	public void findNext() {
		final Map<Integer,Boolean> pagesFound = myPagesFound;
		if (pagesFound == null) {
			return;
		}

		final List<List<RectF>> searchRectangles = getCurrentPage().getSearchRects(myPattern);
		if (myHLWordNum < searchRectangles.size() - 1) {
			++myHLWordNum;
			showSearchResult(searchRectangles, myHLWordNum);
		} else {
			gotoPage(findNextPage(pagesFound, myCurrPageNo), false);
		}
	}

	public void findPrev() {
		final Map<Integer,Boolean> pagesFound = myPagesFound;
		if (pagesFound == null) {
			return;
		}
		final List<List<RectF>> searchRectangles = getCurrentPage().getSearchRects(myPattern);
		if (0 < myHLWordNum && myHLWordNum <= searchRectangles.size()) {
			--myHLWordNum;
			showSearchResult(searchRectangles, myHLWordNum);
		} else {
			gotoPage(findPreviousPage(pagesFound, myCurrPageNo), true);
		}
	}

	@Override
	public boolean onLongClick(View v) {
		if (myZoomInProgress) {
			return false;
		}
		return onFingerLongPress(myPressedX, myPressedY);
	}

	private boolean mySelectionInProgress = false;

	public void clearSelection() {
		myDocument.Selection.clear();
		mySelectionInProgress = false;
		postInvalidate();
	}

	private boolean onFingerLongPress(int x, int y) {
		mySelectionInProgress = true;
		myDocument.Selection.init(getCharIndex(x, y));
		myDocument.createSelectionWordRects(myCurrPageNo);
		if (myListener != null) {
			myListener.onSelectionStart();
		}
		postInvalidate();
		return true;
	}

	private void onFingerReleaseAfterLongPress(int x, int y) {
		mySelectionInProgress = false;
		if (myListener != null) {
			final String txt = getCurrentPage().getSelectionText();
			if (txt != null) {
				myListener.onSelectionEnd(
					txt,
					myDocument.Selection,
					myDocument.getPageCharNumInternal(myCurrPageNo)
				);
			}
		}
		postInvalidate();
	}

	private void onFingerMoveAfterLongPress(int x, int y) {
		extendSelection(x, y);
	}

	public void resetNightMode() {
		myDocument.clearAll();
		myDocument.clearThumbnails();
		myAverageBgColor = DocumentUtil.getAverageBgColor();
		postInvalidate();
	}

	public String getPageStartText() {
		return myDocument.getPageStartText(myCurrPageNo);
	}

	public void zoomIn() {
		startZoom(getWidth() / 2, getMainAreaHeight() / 2, 10);
		zoom(2);
		stopZoom();
		myCanScrollZoomedPage = true;
	}

	public void zoomOut() {
		startZoom(getWidth() / 2, getMainAreaHeight() / 2, 10);
		zoom(0.5f);
		stopZoom();
		myCanScrollZoomedPage = true;
	}

	private volatile Timer myTimer;
	private final HashMap<Runnable,Long> myTimerTaskPeriods = new HashMap<Runnable,Long>();
	private final HashMap<Runnable,TimerTask> myTimerTasks = new HashMap<Runnable,TimerTask>();

	private static class MyTimerTask extends TimerTask {
		private final Runnable myRunnable;

		MyTimerTask(Runnable runnable) {
			myRunnable = runnable;
		}

		public void run() {
			myRunnable.run();
		}
	}

	private void addTimerTaskInternal(Runnable runnable, long periodMilliseconds) {
		final TimerTask task = new MyTimerTask(runnable);
		myTimer.schedule(task, periodMilliseconds / 2, periodMilliseconds);
		myTimerTasks.put(runnable, task);
	}

	private final Object myTimerLock = new Object();

	public final void startTimer() {
		synchronized (myTimerLock) {
			if (myTimer == null) {
				myTimer = new Timer();
				for (Map.Entry<Runnable,Long> entry : myTimerTaskPeriods.entrySet()) {
					addTimerTaskInternal(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	public final void stopTimer() {
		synchronized (myTimerLock) {
			if (myTimer != null) {
				myTimer.cancel();
				myTimer = null;
				myTimerTasks.clear();
			}
		}
	}

	public final void addTimerTask(Runnable runnable, long periodMilliseconds) {
		synchronized (myTimerLock) {
			removeTimerTask(runnable);
			myTimerTaskPeriods.put(runnable, periodMilliseconds);
			if (myTimer != null) {
				addTimerTaskInternal(runnable, periodMilliseconds);
			}
		}
	}

	public final void removeTimerTask(Runnable runnable) {
		synchronized (myTimerLock) {
			TimerTask task = myTimerTasks.get(runnable);
			if (task != null) {
				task.cancel();
				myTimerTasks.remove(runnable);
			}
			myTimerTaskPeriods.remove(runnable);
		}
	}

	public int getSelectionStartY() {
		return (int)myZoomInfo.yBmpToScreen(
			myDocument.getFirstSelectionCursorY(),
			getCurrentPage()
		);
	}

	public int getSelectionEndY() {
		return (int)myZoomInfo.yBmpToScreen(
			myDocument.getLastSelectionCursorY(),
			getCurrentPage()
		);
	}

	protected ArrayList<HighlightingStyle> myStyles;

	public void setStyles(List<HighlightingStyle> styles) {
		myStyles = new ArrayList<HighlightingStyle>(styles);
	}

	public ArrayList<HighlightingStyle> getStyles() {
		return myStyles;
	}

	public HighlightingStyle getStyle(int id) {
		if (myStyles == null) {
			return null;
		}
		for (HighlightingStyle s : myStyles) {
			if (s.Id == id) {
				return s;
			}
		}
		return null;
	}

	@Override
	protected int computeVerticalScrollExtent() {
		if (!isScrollbarShown()) {
			return 0;
		}
		final AnimationProvider animator = getAnimationProvider();
		if (animator.inProgress()) {
			final int from = getScrollbarThumbLength(ZLViewEnums.PageIndex.current);
			final int to = getScrollbarThumbLength(animator.getPageToScrollTo());
			final int percent = animator.getScrolledPercent();
			return (from * (100 - percent) + to * percent) / 100;
		} else {
			return getScrollbarThumbLength(ZLViewEnums.PageIndex.current);
		}
	}

	@Override
	protected int computeVerticalScrollOffset() {
		if (!isScrollbarShown()) {
			return 0;
		}
		final AnimationProvider animator = getAnimationProvider();
		if (animator.inProgress()) {
			final int from = getScrollbarThumbPosition(ZLViewEnums.PageIndex.current);
			final int to = getScrollbarThumbPosition(animator.getPageToScrollTo());
			final int percent = animator.getScrolledPercent();
			return (from * (100 - percent) + to * percent) / 100;
		} else {
			return getScrollbarThumbPosition(ZLViewEnums.PageIndex.current);
		}
	}

	@Override
	protected int computeVerticalScrollRange() {
		if (!isScrollbarShown()) {
			return 0;
		}
		return getScrollbarFullSize();
	}

	private int scrollbarType() {
		return getSettings().ScrollbarType.getValue();
	}

	public final boolean isScrollbarShown() {
		switch (scrollbarType()) {
			case SettingsHolder.SCROLLBAR_SHOW:
			case SettingsHolder.SCROLLBAR_SHOW_AS_PROGRESS:
				return true;
			default:
				return false;
		}
	}

	public final synchronized int getScrollbarThumbPosition(ZLViewEnums.PageIndex pageIndex) {
		return scrollbarType() == SettingsHolder.SCROLLBAR_SHOW_AS_PROGRESS
			? 0 : getPageNumber(pageIndex);
	}

	public final int getScrollbarThumbLength(ZLViewEnums.PageIndex pageIndex) {
		final int start = scrollbarType() == SettingsHolder.SCROLLBAR_SHOW_AS_PROGRESS
			? 0 : getPageNumber(pageIndex);
		final int end = getPageNumber(pageIndex);
		return Math.max(1, end - start);
	}

	private int getPageNumber(ZLViewEnums.PageIndex pageIndex) {
		switch (pageIndex) {
			case current:
				return myCurrPageNo;
			case next:
				return myCurrPageNo < myDocument.getPageCount() - 1
					? myCurrPageNo + 1 : myDocument.getPageCount() - 1;
			case previous:
				return myCurrPageNo > 0 ? myCurrPageNo - 1 : 0;
		}
		return 0;
	}

	private final int getScrollbarFullSize() {
		return myDocument.getPageCount();
	}

	private int getCharIndex(int x, int y) {
		final PageHolder page = getCurrentPage();
		return page.getCharIndex(
			myZoomInfo.xScreenToBmp(x, page),
			myZoomInfo.yScreenToBmp(y, page)
		);
	}

	private void extendSelection(int x, int y) {
		if (myDocument.extendSelection(getCharIndex(x, y - myDpi * 5 / 24))) {
			myDocument.createSelectionWordRects(myCurrPageNo);
			postInvalidate();
		}
	}

	private PageHolder getCurrentPage() {
		return myDocument.getPage(myCurrPageNo);
	}
}
