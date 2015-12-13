package org.geometerplus.fbreader.plugin.base.document;

import java.util.Collections;
import java.util.List;

import android.graphics.*;

import org.fbreader.reader.TOCTree;

import org.geometerplus.fbreader.book.AbstractBook;

public class DummyDocument extends DocumentHolder {
	//Just a workaround for NPE in PluginView

	@Override
	protected boolean openDocumentInternal(String path) {
		return false;
	}

	@Override
	protected int getPageCountInternal() {
		return 0;
	}

	@Override
	public Size getPageSizeInternal(int pageNo) {
		return null;
	}

	@Override
	public void renderPageInternal(Bitmap canvas, int pageNo, Rect src, Rect dst, boolean inverted) {
	}

	@Override
	public void closeInternal() {
	}

	@Override
	public void initTOC(TOCTree root) {
	}

	@Override
	public void readMetainfo(AbstractBook book) {
	}

	@Override
	public String readAnnotation() {
		return null;
	}

	@Override
	synchronized List<RectF> createAllRectsInternal(int pageNo) {
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
	protected PageCache createPage(int no) {
		return null;
	}

	@Override
	public boolean acceptsPath(String path) {
		return false;
	}

	@Override
	public Bitmap getCover(int maxw, int maxh) {
		return null;
	}
}
