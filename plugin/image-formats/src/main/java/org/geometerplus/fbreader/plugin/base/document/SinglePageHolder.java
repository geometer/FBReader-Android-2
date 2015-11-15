package org.geometerplus.fbreader.plugin.base.document;

import java.util.List;

import android.graphics.*;

import org.geometerplus.fbreader.plugin.base.reader.PluginView;

public final class SinglePageHolder extends PageHolder {
	public SinglePageHolder(PluginView view, DocumentHolder doc, int w, int h, int n) {
		super(view, doc, w, h, n);
	}

	@Override
	protected int getAdjustedWidth() {
		return Width;
	}

	@Override
	public int getContainedPageNum() {
		return 1;
	}

	@Override
	public float getRealHeight() {
		return myDoc.getAdjustedPageHeight(myPageNo);
	}

	@Override
	public float getRealWidth() {
		return myDoc.getAdjustedPageWidth(myPageNo);
	}

	@Override
	protected void draw(Bitmap canvas, Rect dst, float ratio, float zoom) {
		myDoc.renderPage(canvas, myPageNo, myDoc.srcRect(myPageNo), dst);
	}

	@Override
	protected List<RectF> createAllRects() {
		return myDoc.createAllRectsInternal(myPageNo);
	}

	@Override
	public int getPageCharNum() {
		return myDoc.getPageCharNumInternal(myPageNo);
	}

	@Override
	public String getSelectionText() {
		final DocumentHolder.SelectionInfo selection = myDoc.Selection;
		return myDoc.getTextInternal(myPageNo, selection.startIndex(), selection.endIndex());
	}

	@Override
	protected List<List<RectF>> createSearchRects(String pattern) {
		return myDoc.createSearchRectsInternal(myPageNo, pattern);
	}

	@Override
	public boolean matches(String pattern) {
		return myDoc.findInPageInternal(myPageNo, pattern);
	}

	@Override
	public int checkInternalPageLink(float x, float y) {
		return myDoc.checkInternalPageLinkInternal(
			myPageNo,
			myDoc.xBmpToDocument(x, myRatio, myPageNo),
			myDoc.yBmpToDocument(y, myRatio, myPageNo)
		);
	}

	@Override
	public String checkHyperLink(float x, float y) {
		return myDoc.checkHyperLinkInternal(
			myPageNo,
			myDoc.xBmpToDocument(x, myRatio, myPageNo),
			myDoc.yBmpToDocument(y, myRatio, myPageNo)
		);
	}
}
