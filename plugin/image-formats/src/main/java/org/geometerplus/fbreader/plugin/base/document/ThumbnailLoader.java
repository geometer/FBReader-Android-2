package org.geometerplus.fbreader.plugin.base.document;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.geometerplus.zlibrary.core.util.BitmapUtil;

public final class ThumbnailLoader {
	private static final ExecutorService ourThreadPool = Executors.newSingleThreadExecutor();

	public interface Listener {
		void onLoaded();
	}

	private final DocumentHolder myDocument;
	private final int myPageNo;
	private final int myHeight;
	private final Listener myListener;

	private volatile Bitmap myBitmap = null;

	public ThumbnailLoader(DocumentHolder document, int pageNo, int height, Listener listener) {
		myDocument = document;
		myPageNo = pageNo;
		myHeight = height;
		myListener = listener;

		ourThreadPool.execute(new Runnable() {
			public void run() {
				final Bitmap bitmap = createBitmap(true);
				try {
					myDocument.renderPage(bitmap, myPageNo, null, null);
					synchronized (ThumbnailLoader.this) {
						myBitmap = bitmap;
					}
					myListener.onLoaded();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private Bitmap createBitmap(boolean real) {
		int w = getWidth(real);
		int h = myHeight;
		if (w <= 0 || h <= 0) {
			w = 1;
			h = 1;
		}
		final Bitmap bitmap = myDocument.createCleanBitmap(w, h, myDocument.isInverted());
		return bitmap;
	}

	public Bitmap getBitmapOrPlaceholder() {
		Bitmap bitmap = myBitmap;
		if (bitmap == null) {
			bitmap = createBitmap(false);
			synchronized (this) {
				if (myBitmap == null) {
					myBitmap = bitmap;
				} else {
					bitmap = myBitmap;
				}
			}
		}
		return bitmap;
	}

	private int getWidth(boolean real) {
		final DocumentHolder.Size size = real
			? myDocument.getPageSize(myPageNo)
			: myDocument.getPageSizeOrNull(myPageNo);
		if (size != null) {
			return (int)(myHeight * size.Width / size.Height);
		} else {
			return myHeight * 5 / 7;
		}
	}
}
