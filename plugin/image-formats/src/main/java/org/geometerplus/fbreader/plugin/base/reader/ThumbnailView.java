package org.geometerplus.fbreader.plugin.base.reader;

import java.util.Collections;
import java.util.HashSet;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.*;

import org.geometerplus.fbreader.plugin.base.FBReaderPluginActivity;
import org.geometerplus.fbreader.plugin.base.document.*;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

public class ThumbnailView extends View implements ThumbnailLoader.Listener {
	private DocumentHolder.ThumbnailFactory myThumbnailFactory;

	private class PageBounds {
		public final int PageNo;
		public final int Left;
		public final int Right;

		public PageBounds(int pageNo, int left, int right) {
			Left = left;
			Right = right;
			PageNo = pageNo;
		}
	}

	private HashSet<PageBounds> myBounds = new HashSet<PageBounds>();

	public ThumbnailView(Context context) {
		super(context);
		setup();
	}

	public ThumbnailView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	public ThumbnailView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup();
	}

	public interface PageChangeListener {
		void onPageChanged(int no);
		void onPageSelected(int no);
	}

	private PageChangeListener myListener;

	public void setListener(PageChangeListener c) {
		myListener = c;
	}

	private void setup() {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		myScreenHeight = Math.max(displaymetrics.heightPixels, displaymetrics.widthPixels);
		myBorderPaint.setColor(Color.GREEN);
		myBorderPaint.setStyle(Paint.Style.STROKE);
		myBorderPaint.setStrokeWidth(2);
		myTrianglePaint.setColor(Color.LTGRAY);
		myTrianglePaint.setStyle(Paint.Style.FILL_AND_STROKE);
	}

	private int myScreenHeight;

	private int myPageNo = 0;
	private int myShift = 0;

	private int myYMarginPercent = 10;

	private int myXSpacing = 10;//pix

	public void setPage(int no) {
		myPageNo = no;
		myShift = 0;
		HashSet<Integer> set = new HashSet<Integer>();
		for (int i = no - 10; i <= no + 10; ++i) {
			set.add(i);
		}
		postInvalidate();
	}

	private Paint myBorderPaint = new Paint();
	private Paint myTrianglePaint = new Paint();

	private void drawCurPos(PluginView view, Canvas canvas, int x, int w) {
		final DocumentHolder document = view.getDocument();
		final int pageNo = view.getCurPageNo();
		final PageHolder page = document.getPage(pageNo);

		final int margin = getHeight() * myYMarginPercent / 100;
		final PluginView.PDFPosition pos = view.getPosition();
		final DocumentHolder.CropInfo cropInfo = document.getCropInfo();
		final int cropx = w * cropInfo.LeftPercent / 100;
		w = (int)Math.round(cropInfo.adjustedWidth(w));
		final int h = (int)Math.round(cropInfo.adjustedHeight(getHeight() - margin * 2));
		final int cropy = (getHeight() - margin * 2) * cropInfo.TopPercent / 100;
		final float ratio = 1.f * w / page.getBmpWidth();
		final int left = (int)(x + cropx + ((pos.FixedX * (pos.Zoom - 1) - page.getShiftX()) * ratio / pos.Zoom));
		final int right = (int)(left + (w + 2 * page.getShiftX() * ratio) / pos.Zoom);
		final int top = (int)(margin + cropy + ((pos.FixedY * (pos.Zoom - 1) - page.getShiftY()) * ratio / pos.Zoom));
		final int bottom = (int)(top + (h + 2 * page.getShiftY() * ratio) / pos.Zoom);
		canvas.drawRect(left, top, right + 1, bottom + 1, myBorderPaint);
	}

	private int myCpX = -1;
	private int myCpW = -1;

	private void drawThumb(PluginView view, Canvas canvas, int x, int n) {
		final Bitmap bmp = myThumbnailFactory.get(n);
		final int margin = getHeight() * myYMarginPercent / 100;
		final int h = getHeight() - margin * 2;
		final Matrix mat = new Matrix();
		mat.setScale(1, 1, 0, 0);
		mat.postTranslate(x, margin);
		canvas.drawBitmap(bmp, mat, null);
		myBounds.add(new PageBounds(n, x, x + bmp.getWidth()));
		if (n == view.getCurPageNo()) {
			myCpX = x;
			myCpW = bmp.getWidth();
		}
		final DocumentHolder.CropInfo cropInfo = view.getDocument().getCropInfo();
		final int crtop = cropInfo.TopPercent * h / 100 + margin;
		final int crbottom = h * (100 - cropInfo.BottomPercent) / 100 + margin;
		final int crleft = cropInfo.LeftPercent * bmp.getWidth() / 100 + x;
		final int crright = bmp.getWidth() * (100 - cropInfo.RightPercent) / 100 + x;
		final Rect top = new Rect(x, margin, x + bmp.getWidth(), crtop);
		final Rect left = new Rect(x, crtop, crleft, crbottom);
		final Rect right = new Rect(crright, crtop, x + bmp.getWidth(), crbottom);
		final Rect bottom = new Rect(x, crbottom, x + bmp.getWidth(), margin + h);
		final Paint p = new Paint();
		p.setStyle(Paint.Style.FILL);
		p.setARGB(127, 127, 127, 127);
		canvas.drawRect(top, p);
		canvas.drawRect(left, p);
		canvas.drawRect(right, p);
		canvas.drawRect(bottom, p);
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		final PluginView view = getView();

		myCpW = -1;
		int w = getWidth();
		int h = getHeight();
		int margin = getHeight() * myYMarginPercent / 100;
		Path p = new Path();
		p.moveTo(w / 2, margin - 2);
		p.lineTo(w / 2 - margin / 4, 1);
		p.lineTo(w / 2 + margin / 4, 1);
		p.lineTo(w / 2, margin - 2);
		p.moveTo(w / 2, h - margin + 2);
		p.lineTo(w / 2 - margin / 4, h - 1);
		p.lineTo(w / 2 + margin / 4, h - 1);
		p.lineTo(w / 2, h - margin + 2);
		canvas.drawPath(p, myTrianglePaint);
		int n = myPageNo;
		Bitmap bmp = myThumbnailFactory.get(n);
		int x = w / 2 - bmp.getWidth() / 2 + myShift;
		myBounds.clear();
		drawThumb(view, canvas, x, n);
		boolean stop = false;
		n--;
		stop = n < 0;
		if (n >= 0) {
			x -= myThumbnailFactory.get(n).getWidth() + myXSpacing;
		}
		while (!stop) {
			drawThumb(view, canvas, x, n);
			n--;
			if (n >= 0) {
				x -= myThumbnailFactory.get(n).getWidth() + myXSpacing;
			}
			stop = n < 0 || x + myThumbnailFactory.get(n).getWidth() < 0;
		}
		n = myPageNo + 1;
		x = w / 2 + bmp.getWidth() / 2 + myXSpacing + myShift;
		stop = n >= view.getDocument().getPageCount();
		while (!stop) {
			drawThumb(view, canvas, x, n);
			x += myThumbnailFactory.get(n).getWidth() + myXSpacing;
			n++;
			stop = n >= view.getDocument().getPageCount() || x >= w;
		}
		if (myCpW != -1) {
			drawCurPos(view, canvas, myCpX, myCpW);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		myThumbnailFactory = getView().getDocument().createThumbsFactory(
			(int)(getHeight() * (100f - myYMarginPercent * 2) / 100), this
		);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		if (parentHeight > myScreenHeight / 5) {
			parentHeight = myScreenHeight / 5;
		}
		setMeasuredDimension(parentWidth, parentHeight);
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
	private boolean myLongClickPerformed;
	private boolean myPendingDoubleTap;
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
		final PluginView view = getView();

		if (event.getPointerCount() != 1) {
			return false;
		}
		final int x = (int)event.getX();
		final int y = (int)event.getY();

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
				myScrollInProgress = false;
				myShift = 0;
				myPendingDoubleTap = false;
				myPendingPress = false;
				myScreenIsTouched = false;
				postInvalidate();
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
						onFingerMove(view, x, y);
					}
				}
				break;
			}
		}

		return true;
	}

	private boolean isDoubleTapSupported() {
		return false;
	}

	private int myStartX;
	private boolean myScrollInProgress = false;

	private void startScrolling(int x) {
		myScrollInProgress = true;
		myStartX = x;
	}

	private void scrollTo(PluginView view, int x) {
		myShift = x - myStartX;
		checkShift(view);
		postInvalidate();
	}

	private void checkShift(PluginView view) {
		if (myPageNo >= view.getDocument().getPageCount() - 1) {
			if (myShift < 0) {
				myShift = 0;
				return;
			}
		}
		if (myPageNo <= 0) {
			if (myShift > 0) {
				myShift = 0;
				return;
			}
		}
		if (myShift > myThumbnailFactory.get(myPageNo).getWidth() / 2 + myXSpacing / 2) {
			myPageNo -= 1;
			myShift -= myThumbnailFactory.get(myPageNo).getWidth() / 2 + myXSpacing + myThumbnailFactory.get(myPageNo + 1).getWidth() / 2;
			myStartX += myThumbnailFactory.get(myPageNo).getWidth() / 2 + myXSpacing + myThumbnailFactory.get(myPageNo + 1).getWidth() / 2;
			if (myListener != null) {
				myListener.onPageChanged(myPageNo);
			}
			return;
		}
		if (myShift < - myThumbnailFactory.get(myPageNo).getWidth() / 2 - myXSpacing / 2) {
			myPageNo += 1;
			myShift += myThumbnailFactory.get(myPageNo).getWidth() / 2 + myXSpacing + myThumbnailFactory.get(myPageNo - 1).getWidth() / 2;
			myStartX -= myThumbnailFactory.get(myPageNo).getWidth() / 2 + myXSpacing + myThumbnailFactory.get(myPageNo - 1).getWidth() / 2;
			if (myListener != null) {
				myListener.onPageChanged(myPageNo);
			}
			return;
		}
	}

	private final Object myFingerMoveLock = new Object();
	private boolean onFingerMove(PluginView view, int x, int y) {
		synchronized (myFingerMoveLock) {
			if (myScrollInProgress) {
				scrollTo(view, x);
			} else {
				startScrolling(x);
			}
		}
		return true;
	}

	private boolean onFingerPress(int x, int y) {
		startScrolling(x);
		return true;
	}

	private boolean onFingerRelease(int x, int y) {
		return true;
	}

	public void onFingerSingleTap(int x, int y) {
		int n = -1;
		for (PageBounds pb : myBounds) {
			if (x >= pb.Left && x <= pb.Right) {
				n = pb.PageNo;
			}
		}
		if (n == -1) {
			return;
		}
		setPage(n);
		if (myListener != null) {
			myListener.onPageChanged(myPageNo);
			myListener.onPageSelected(myPageNo);
		}
	}

	private void onFingerDoubleTap(int x, int y) {
	}

	private void onFingerReleaseAfterLongPress(int x, int y) {
	}

	private void onFingerMoveAfterLongPress(int x, int y) {
	}

	// method from ThumbnailLoader.Listener
	public synchronized void onLoaded() {
		postInvalidate();
	}

	private PluginView getView() {
		return ((FBReaderPluginActivity)getContext()).getPluginView();
	}
}
