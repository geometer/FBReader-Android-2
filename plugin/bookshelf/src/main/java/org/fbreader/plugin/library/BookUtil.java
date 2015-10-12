package org.fbreader.plugin.library;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.android.fbreader.api.FBReaderIntents;

public abstract class BookUtil {
	static final BitmapCache CoversCache = new BitmapCache(0.65f);
	static final ExecutorService ThreadPool = Executors.newFixedThreadPool(4);
	private static final Timer ourTimer = new Timer();

	private static BookPopupWindow ourPopup = null;

	static abstract class OnClickListener implements View.OnClickListener {
		public final void onClick(View view) {
			if (ourPopup != null) {
				return;
			}
			onClick2();
		}

		protected abstract void onClick2();
	}

	static boolean isPopupShown() {
		return ourPopup != null;
	}

	static void dismissPopup() {
		try {
			ourPopup.dismiss();
		} catch (Throwable t) {
			// ignore
		}
		ourPopup = null;
	}

	static void resetPopup() {
		ourPopup = null;
	}

	static void openBook(LibraryActivity activity, Book book) {
		final Intent intent = FBReaderIntents.internalIntent(FBReaderIntents.Action.VIEW)
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		FBReaderIntents.putBookExtra(intent, book);
		try {
			activity.startActivity(intent);
			activity.overridePendingTransition(0, 0);
			activity.finish();
			activity.overridePendingTransition(0, 0);
		} catch (ActivityNotFoundException e) {
			activity.showMissingFBReaderDialog();
		}
	}

	static void showPopup(final LibraryActivity activity, final Book book, final SparseArray<BookActionMenu.Action> extraActions) {
		ourPopup = new BookPopupWindow(activity, book, extraActions);
		ourPopup.showAtCenter();
	}

	interface BitmapRunnable {
		void run(Bitmap bmp);
	}

	private static Bitmap decodeBitmapFromConnection(HttpURLConnection connection) throws Exception {
		final BitmapFactory.Options options = new BitmapFactory.Options();

		final String xWidth = connection.getHeaderField("X-Width");
		final String xHeight = connection.getHeaderField("X-Height");

		int width, height;
		if (xWidth != null && xHeight != null) {
			width = Integer.valueOf(xWidth);
			height = Integer.valueOf(xHeight);
		} else {
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(connection.getInputStream(), null, options);
			width = options.outWidth;
			height = options.outHeight;
			options.inJustDecodeBounds = false;
			connection = (HttpURLConnection)connection.getURL().openConnection();
		}

		options.inSampleSize = 1;
		while (height > 480 || width > 320) {
			height >>= 1;
			width >>= 1;
			options.inSampleSize <<= 1;
		}
		return BitmapFactory.decodeStream(connection.getInputStream(), null, options);
	}

	static Bitmap getCover(Book book) {
		final BitmapCache.Container container = CoversCache.get(book.getId());
		return container != null ? container.Bitmap : null;
	}

	static void retrieveCover(LibraryActivity activity, Book book, BitmapRunnable postAction) {
		retrieveCover(activity, book, postAction, 5);
	}

	private static void retrieveCover(final LibraryActivity activity, final Book book, final BitmapRunnable postAction, final int retryCount) {
		ThreadPool.execute(new Runnable() {
			public void run() {
				final BitmapCache.Container container = CoversCache.get(book.getId());
				if (container != null) {
					postAction.run(container.Bitmap);
				} else {
					final String url;
					try {
						url = activity.Collection.getCoverUrl(book);
					} catch (Throwable t) {
						return;
					}

					if (url == null) {
						CoversCache.put(book.getId(), null);
						return;
					}

					Bitmap bmp = null;
					boolean skipCache = false;
					try {
						final HttpURLConnection connection =
							(HttpURLConnection)new URL(url).openConnection();
						connection.connect();
						switch (connection.getResponseCode()) {
							case 200:
								skipCache = true;
								bmp = decodeBitmapFromConnection(connection);
								skipCache = false;
								break;
							case 204:
								skipCache = true;
								break;
						}
					} catch (IOException e) {
						skipCache = true;
					} catch (Throwable t) {
						t.printStackTrace();
					}
					if (skipCache) {
						if (retryCount > 0) {
							ourTimer.schedule(new TimerTask() {
								@Override
								public void run() {
									retrieveCover(activity, book, postAction, retryCount - 1);
								}
							}, 2000);
						}
					} else {
						CoversCache.put(book.getId(), bmp);
					}
					postAction.run(bmp);
				}
			}
		});
	}

	static Bitmap stack(Bitmap ... bmps) {
		try {
			return stackInternal(bmps);
		} catch (Throwable t) {
			return null;
		}
	}

	private static Bitmap stackInternal(Bitmap ... bmps) {
		final List<Bitmap> bitmaps = new ArrayList<Bitmap>(bmps.length);
		for (Bitmap b : bmps) {
			if (b != null) {
				bitmaps.add(b);
			}
		}
		final int count = bitmaps.size();
		if (count == 0) {
			return null;
		}
		if (count == 1) {
			return bitmaps.get(0);
		}

		final Bitmap base = bitmaps.get(0);
		Collections.reverse(bitmaps);
		final int w = base.getWidth() * (95 + 5 * count) / 100;
		final int h = base.getHeight() * (95 + 5 * count) / 100;
		final Bitmap stacked = Bitmap.createBitmap(w, h, base.getConfig());
		final Canvas c = new Canvas(stacked);
		final Rect dst = new Rect(0, 0, base.getWidth(), base.getHeight());
		dst.offset(w * (count - 1) / 20, 0);
		for (Bitmap b : bitmaps) {
			final Rect src = new Rect(0, 0, b.getWidth(), b.getHeight());
			c.drawBitmap(b, src, dst, null);
			dst.offset(- w / 20, h / 20);
		}
		return stacked;
	}

	private static final String CUSTOM_PREFIX = "custom_";

	static boolean isCustomCategoryLabel(String label) {
		return label != null && label.startsWith(CUSTOM_PREFIX);
	}

	static String customCategoryTitle(String label) {
		return label.substring(CUSTOM_PREFIX.length());
	}

	static String customCategoryLabel(String title) {
		return CUSTOM_PREFIX + title;
	}

	static void setImageResource(ImageView imageView, int resourceId) {
		try {
			Drawable drawable = CoversCache.getDrawable(resourceId);
			if (drawable == null) {
				drawable = imageView.getContext().getResources().getDrawable(resourceId);
				CoversCache.putDrawable(resourceId, drawable);
			}
			imageView.setImageDrawable(drawable);
		} catch (Throwable t) {
			// ignore
		}
	}

	private static String MIME_FALLBACK = "application/octet-stream";

	static String mime(String url) {
		final int dotIndex = url.lastIndexOf(".");
		if (dotIndex == -1) {
			return MIME_FALLBACK;
		}
		final String extension = url.substring(dotIndex + 1).toLowerCase();
		if ("epub".equals(extension)) {
			return "application/epub+zip";
		} else if ("zip".equals(extension)) {
			return "application/zip";
		} else if ("pdf".equals(extension)) {
			return "application/pdf";
		} else if ("djvu".equals(extension)) {
			return "image/vnd-djvu";
		} else if ("fb2".equals(extension)) {
			return "application/fb2+xml";
		} else if ("cbz".equals(extension)) {
			return "application/x-cbz";
		} else if ("cbr".equals(extension)) {
			return "application/x-cbr";
		} else {
			return MIME_FALLBACK;
		}
	}
}
