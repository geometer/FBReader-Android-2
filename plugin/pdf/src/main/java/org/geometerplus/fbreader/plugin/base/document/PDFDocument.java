package org.geometerplus.fbreader.plugin.base.document;

import java.util.*;

import android.content.ContextWrapper;
import android.graphics.*;

import com.radaee.pdf.*;

import org.geometerplus.fbreader.plugin.base.tree.TOCTree;

public class PDFDocument extends DocumentHolder {
	private final Object myDocumentLock = new Object();
	private Document myDocument;

	public static void init(ContextWrapper context) {
		Global.Init(context);
	}

	@Override
	protected boolean openDocumentInternal(String path) {
		synchronized (myDocumentLock) {
			final Document tmp = new Document();
			if (tmp.Open(path, null) == 0) {
				myDocument = tmp;
				return true;
			}
			return false;
		}
	}

	@Override
	protected int getPageCountInternal() {
		return myDocument != null ? myDocument.GetPageCount() : 0;
	}

	@Override
	public Size getPageSizeInternal(int pageNo) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return null;
			}
			return new Size(myDocument.GetPageWidth(pageNo), myDocument.GetPageHeight(pageNo));
		}
	}

	@Override
	protected void renderPageInternal(Bitmap bitmap, int pageNo, Rect src, Rect dst, boolean inverted) {
		Bitmap realBitmap;
		if (inverted ||
			dst.left != 0 ||
			dst.top != 0 ||
			dst.right != bitmap.getWidth() ||
			dst.bottom != bitmap.getHeight()
		) {
			realBitmap = createCleanBitmap(dst.right - dst.left, dst.bottom - dst.top, false);
		} else {
			realBitmap = bitmap;
		}

		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return;
			}

			final float scaleX = 1f * (dst.right - dst.left) / (src.right - src.left);
			final float scaleY = 1f * (dst.bottom - dst.top) / (src.bottom - src.top);
			final float h = myDocument.GetPageHeight(pageNo);

			final com.radaee.pdf.Matrix mat = new com.radaee.pdf.Matrix(
				scaleX,
				- scaleY,
				- scaleX * src.left,
				scaleY * (h - src.top)
			);

			//final int bitmapId = Global.lockBitmap(realBitmap);
			final Page page = myDocument.GetPage(pageNo);
			try {
				page.RenderToBmp(realBitmap, mat);
				//Global.unlockBitmap(realBitmap, bitmapId);
			} finally {
				page.Close();
			}

			mat.Destroy();
		}
		if (realBitmap != bitmap) {
			if (inverted) {
				final Bitmap blackBitmap =
					createCleanBitmap(dst.right - dst.left, dst.bottom - dst.top, true);
				new Canvas(blackBitmap).drawBitmap(realBitmap, 0, 0, createPaint(true));
				realBitmap.recycle();
				realBitmap = blackBitmap;
			}
			new Canvas(bitmap).drawBitmap(realBitmap, dst.left, dst.top, createPaint(false));
		}
	}

	@Override
	public void closeInternal() {
		synchronized (myDocumentLock) {
			if (myDocument != null) {
				myDocument.Close();
				myDocument = null;
			}
		}
	}

	@Override
	public void initTOC(TOCTree root) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return;
			}
			final Document.Outline outline = myDocument.GetOutlines();
			if (outline != null) {
				createTOCTree(outline, root, true);
			}
		}
	}

	private void createTOCTree(final Document.Outline n, TOCTree parent, boolean firstChild) {
		final TOCTree t = new TOCTree(parent);
		t.setText(n.GetTitle());
		t.setReference(n.GetDest());

		Document.Outline nextnum = n.GetNext();
		while (firstChild && nextnum != null) {
			createTOCTree(nextnum, parent, false);
			nextnum = nextnum.GetNext();
		}

		final Document.Outline childnum = n.GetChild();
		if (childnum != null) {
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
		synchronized (myDocumentLock) {
			return myDocument != null ? myDocument.GetMeta(tag) : null;
		}
	}

	@Override
	public String getPageStartText(int pageNo) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return null;
			}

			final Page page = myDocument.GetPage(pageNo);
			try {
				page.ObjsStart();
				final int len = Math.min(page.ObjsGetCharCount(), 128);
				return len > 0 ? page.ObjsGetString(0, len) : super.getPageStartText(pageNo);
			} finally {
				page.Close();
			}
		}
	}

	@Override
	List<RectF> createAllRectsInternal(int pageNo) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return Collections.emptyList();
			}

			final Page page = myDocument.GetPage(pageNo);
			try {
				page.ObjsStart();
				final int count = page.ObjsGetCharCount();
				final ArrayList<RectF> allRectangles = new ArrayList<RectF>(count);
				final float[] rect = new float[4];
				for (int i = 0; i < count; ++i) {
					page.ObjsGetCharRect(i, rect);
					allRectangles.add(new RectF(rect[0], rect[3], rect[2], rect[1]));
				}
				return allRectangles;
			} finally {
				page.Close();
			}
		}
	}

	@Override
	String getTextInternal(int pageNo, int startElement, int endElement) {
		synchronized (myDocumentLock) {
			if (myDocument == null || startElement < 0 || endElement < 0) {
				return null;
			}

			final Page page = myDocument.GetPage(pageNo);
			try {
				page.ObjsStart();
				return page.ObjsGetString(
					Math.min(endElement, startElement),
					Math.max(endElement, startElement) + 1
				);
			} finally {
				page.Close();
			}
		}
	}

	@Override
	List<List<RectF>> createSearchRectsInternal(int pageNo, String pattern) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return Collections.emptyList();
			}

			final Page page = myDocument.GetPage(pageNo);
			try {
				page.ObjsStart();
				final Page.Finder handler = page.FindOpen(pattern, false, false);
				final List<List<RectF>> allRectangles = new ArrayList<List<RectF>>();
				if (handler != null) {
					final int count = handler.GetCount();
					final float[] data = new float[4];
					for (int i = 0; i < count; ++i) {
						final List<RectF> wordRects = new ArrayList<RectF>(pattern.length());
						final int start = handler.GetFirstChar(i);
						RectF rect = null;
						for (int index = start; index < start + pattern.length(); ++index) {
							page.ObjsGetCharRect(index, data);
							if (rect != null && rect.bottom == data[1] && rect.top == data[3]) {
								rect.left = Math.min(rect.left, data[0]);
								rect.right = Math.max(rect.right, data[2]);
							} else {
								rect = new RectF(data[0], data[3], data[2], data[1]);
								wordRects.add(rect);
							}
						}
						allRectangles.add(wordRects);
					}
					handler.Close();
				}
				return allRectangles;
			} finally {
				page.Close();
			}
		}
	}

	@Override
	boolean findInPageInternal(int no, String pattern) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return false;
			}

			final Page page = myDocument.GetPage(no);
			try {
				page.ObjsStart();
				final Page.Finder finder = page.FindOpen(pattern, false, false);
				final boolean result = finder != null && finder.GetCount() > 0;
				if (finder != null) {
					finder.Close();
				}
				return result;
			} finally {
				page.Close();
			}
		}
	}

	@Override
	public int getPageCharNumInternal(int pageNo) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return 0;
			}

			final Page page = myDocument.GetPage(pageNo);
			try {
				page.ObjsStart();
				return page.ObjsGetCharCount();
			} finally {
				page.Close();
			}
		}
	}

	@Override
	int checkInternalPageLinkInternal(int pageNo, float x, float y) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return -1;
			}

			final Page page = myDocument.GetPage(pageNo);
			try {
				page.ObjsStart();
				final Page.Annotation anno = page.GetAnnotFromPoint(x, y);
				return anno != null ? anno.GetDest() : -1;
			} finally {
				page.Close();
			}
		}
	}

	@Override
	public String checkHyperLinkInternal(int pageNo, float x, float y) {
		synchronized (myDocumentLock) {
			if (myDocument == null) {
				return null;
			}

			final Page page = myDocument.GetPage(pageNo);
			try {
				page.ObjsStart();
				final Page.Annotation anno = page.GetAnnotFromPoint(x, y);
				return anno != null ? anno.GetURI() : null;
			} finally {
				page.Close();
			}
		}
	}

	@Override
	public boolean acceptsPath(String path) {
		return path.endsWith(".pdf");
	}

	@Override
	protected PageCache createPage(int no) {
		return null;
	}

	@Override
	public Bitmap getCover(int maxWidth, int maxHeight) {
		final Size size = getPageSize(0);
		if (size == null) {
			return null;
		}
		final float ratio = Math.min(maxWidth / size.Width, maxHeight / size.Height);
		final Bitmap cover = createCleanBitmap(
			Math.round(size.Width * ratio),
			Math.round(size.Height * ratio),
			false
		);
		renderPage(cover, 0, null, null);
		return cover;
	}
}
