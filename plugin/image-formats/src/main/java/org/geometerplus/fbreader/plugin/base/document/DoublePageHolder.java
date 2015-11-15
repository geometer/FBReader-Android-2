package org.geometerplus.fbreader.plugin.base.document;

import java.util.ArrayList;
import java.util.List;

import android.graphics.*;

import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

public final class DoublePageHolder extends PageHolder {
	public DoublePageHolder(PluginView view, DocumentHolder doc, int w, int h, int n) {
		super(view, doc, w, h, n);
	}

	@Override
	public int getContainedPageNum() {
		return 2;
	}

	private int spaceBetweenColumns() {
		return myView != null ? myView.getSettings().SpaceBetweenColumns.getValue() : 0;
	}

	@Override
	protected int getAdjustedWidth() {
		return Width - spaceBetweenColumns();
	}

	@Override
	protected void draw(Bitmap canvas, Rect dst, float ratio, float zoom) {
		final int space = (int)Math.round(spaceBetweenColumns() * zoom);

		if (myDoc.getPageCount() - 1 == myPageNo) {
			myDoc.renderPage(canvas, myPageNo, myDoc.srcRect(myPageNo), dst);
		} else {
			final int width1 = (int)Math.round(myDoc.getAdjustedPageWidth(myPageNo) * ratio * zoom);
			final int width2 = (int)Math.round(myDoc.getAdjustedPageWidth(myPageNo + 1) * ratio * zoom);
			final int height1 = (int)Math.round(myDoc.getAdjustedPageHeight(myPageNo) * ratio * zoom);
			final int height2 = (int)Math.round(myDoc.getAdjustedPageHeight(myPageNo + 1) * ratio * zoom);
			final int hDiff = (height2 - height1) / 2;

			final int top1 = dst.top + (hDiff > 0 ? hDiff : 0);
			myDoc.renderPage(
				canvas, myPageNo,
				myDoc.srcRect(myPageNo),
				new Rect(dst.left, top1, dst.left + width1, top1 + height1)
			);
			final int top2 = dst.top - (hDiff < 0 ? hDiff : 0);
			myDoc.renderPage(
				canvas, myPageNo + 1,
				myDoc.srcRect(myPageNo + 1),
				new Rect(dst.right - width2, top2, dst.right, top2 + height2)
			);
		}
	}

	@Override
	public float getRealHeight() {
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return myDoc.getAdjustedPageHeight(myPageNo);
		} else {
			return Math.max(myDoc.getAdjustedPageHeight(myPageNo), myDoc.getAdjustedPageHeight(myPageNo + 1));
		}
	}

	@Override
	public float getRealWidth() {
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return myDoc.getAdjustedPageWidth(myPageNo);
		}
		return myDoc.getAdjustedPageWidth(myPageNo) + myDoc.getAdjustedPageWidth(myPageNo + 1);
	}

	@Override
	protected List<RectF> createAllRects() {
		final List<RectF> rects1 = myDoc.createAllRectsInternal(myPageNo);
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return rects1;
		}

		final float dh = (myDoc.getAdjustedPageHeight(myPageNo + 1) - myDoc.getAdjustedPageHeight(myPageNo)) / 2;
		final float dh1 = Math.max(0, dh);
		final float dh2 = Math.max(0, -dh);
		final float dw2 = (Width - 2 * getShiftX()) / myRatio - myDoc.getAdjustedPageWidth(myPageNo + 1);

		final List<RectF> rects2 = myDoc.createAllRectsInternal(myPageNo + 1);
		final List<RectF> allRects = new ArrayList<RectF>(rects1.size() + rects2.size());

		for (RectF r : rects1) {
			r.top += dh1;
			r.bottom += dh1;
		}
		allRects.addAll(rects1);

		for (RectF r : rects2) {
			r.left += dw2;
			r.right += dw2;
			r.top += dh2;
			r.bottom += dh2;
		}
		allRects.addAll(rects2);

		return allRects;
	}

	@Override
	public int getPageCharNum() {
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return myDoc.getPageCharNumInternal(myPageNo);
		}
		return myDoc.getPageCharNumInternal(myPageNo) + myDoc.getPageCharNumInternal(myPageNo + 1);
	}

	@Override
	public String getSelectionText() {
		final DocumentHolder.SelectionInfo selection = myDoc.Selection;
		final int start = selection.startIndex();
		final int end = selection.endIndex();
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return myDoc.getTextInternal(myPageNo, start, end);
		}
		final int i1 = myDoc.getPageCharNumInternal(myPageNo);
		if (end < i1) {
			return myDoc.getTextInternal(myPageNo, start, end);
		}
		if (start >= i1) {
			return myDoc.getTextInternal(myPageNo + 1, start - i1, end - i1);
		}
		return
			myDoc.getTextInternal(myPageNo, start, i1) +
			myDoc.getTextInternal(myPageNo + 1, 0, end - i1);
	}

	@Override
	protected List<List<RectF>> createSearchRects(String pattern) {
		final List<List<RectF>> rects1 = myDoc.createSearchRectsInternal(myPageNo, pattern);
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return rects1;
		}

		final float dh = (myDoc.getAdjustedPageHeight(myPageNo + 1) - myDoc.getAdjustedPageHeight(myPageNo)) / 2;
		float dh1 = Math.max(0, dh);
		float dh2 = Math.max(0, -dh);
		final float dw2 = (Width - 2 * getShiftX()) / myRatio - myDoc.getAdjustedPageWidth(myPageNo + 1);

		final List<List<RectF>> rects2 = myDoc.createSearchRectsInternal(myPageNo + 1, pattern);
		final List<List<RectF>> allRects = new ArrayList<List<RectF>>(rects1.size() + rects2.size());

		for (List<RectF> ar : rects1) {
			for (RectF r : ar) {
				r.top += dh1;
				r.bottom += dh1;
			}
		}
		allRects.addAll(rects1);

		for (List<RectF> ar : rects2) {
			for (RectF r : ar) {
				r.left += dw2;
				r.right += dw2;
				r.top += dh2;
				r.bottom += dh2;
			}
		}
		allRects.addAll(rects2);

		return allRects;
	}

	@Override
	public boolean matches(String pattern) {
		final boolean foundOnPage1 = myDoc.findInPageInternal(myPageNo, pattern);
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return foundOnPage1;
		}
		return foundOnPage1 || myDoc.findInPageInternal(myPageNo + 1, pattern);
	}

	@Override
	public int checkInternalPageLink(float x, float y) {
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return myDoc.checkInternalPageLinkInternal(
				myPageNo,
				myDoc.xBmpToDocument(x, myRatio, myPageNo),
				myDoc.yBmpToDocument(y, myRatio, myPageNo)
			);
		}
		float w1 = myRatio * myDoc.getAdjustedPageWidth(myPageNo);
		if (x <= w1) {
			return myDoc.checkInternalPageLinkInternal(
				myPageNo,
				myDoc.xBmpToDocument(x, myRatio, myPageNo),
				myDoc.yBmpToDocument(y, myRatio, myPageNo)
			);
		} else {
			return myDoc.checkInternalPageLinkInternal(
				myPageNo + 1,
				myDoc.xBmpToDocument(x - w1, myRatio, myPageNo),
				myDoc.yBmpToDocument(y, myRatio, myPageNo)
			);
		}
	}

	@Override
	public String checkHyperLink(float x, float y) {
		if (myDoc.getPageCount() - 1 == myPageNo) {
			return myDoc.checkHyperLinkInternal(
				myPageNo,
				myDoc.xBmpToDocument(x, myRatio, myPageNo),
				myDoc.yBmpToDocument(y, myRatio, myPageNo)
			);
		}
		float w1 = myRatio * myDoc.getAdjustedPageWidth(myPageNo);
		if (x <= w1) {
			return myDoc.checkHyperLinkInternal(
				myPageNo,
				myDoc.xBmpToDocument(x, myRatio, myPageNo),
				myDoc.yBmpToDocument(y, myRatio, myPageNo)
			);
		} else {
			return myDoc.checkHyperLinkInternal(
				myPageNo + 1,
				myDoc.xBmpToDocument(x - w1, myRatio, myPageNo),
				myDoc.yBmpToDocument(y, myRatio, myPageNo)
			);
		}
	}
}
