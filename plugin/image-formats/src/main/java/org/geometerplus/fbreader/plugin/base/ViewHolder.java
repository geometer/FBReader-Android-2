package org.geometerplus.fbreader.plugin.base;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.*;

import org.fbreader.common.options.ColorProfile;
import org.fbreader.reader.AbstractReader;

import org.geometerplus.zlibrary.core.application.ZLKeyBindings;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.util.RationalNumber;
import org.geometerplus.zlibrary.text.view.ZLTextPositionWithTimestamp;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;
import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.fbreader.TapZoneMap;
import org.geometerplus.fbreader.network.sync.SyncData;
import org.geometerplus.fbreader.plugin.base.document.DocumentHolder;
import org.geometerplus.fbreader.plugin.base.reader.*;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public final class ViewHolder extends AbstractReader implements PluginView.ChangeListener {
	public static final String BOOKMARK_ID_KEY = "fbreader.bookmark.id";

	private static ViewHolder ourInstance;

	public static ViewHolder getInstance() {
		return ourInstance;
	}

	final BookCollectionShadow Collection = new BookCollectionShadow();
	private final SyncData mySyncData = new SyncData();
	private final ZLKeyBindings myBindings = new ZLKeyBindings();

	private final FBReaderPluginActivity myActivity;

	public PluginView getView() {
		return myActivity.getPluginView();
	}

	public FBReaderPluginActivity getActivity() {
		return myActivity;
	}

	private volatile BookSettingsDB myDB;

	public void storeAll() {
		myDB.storeAll(this);
	}

	void startActivity(Intent intent) {
		final FBReaderPluginActivity activity = getActivity();
		activity.startActivity(intent);
		activity.overridePendingTransition(0, 0);
	}

	private Book myBook;

	@Override
	public Book getCurrentBook() {
		return myBook;
	}

	public static class BookInfo {
		public final String Path;
		public final String DcId;

		private BookInfo(String path, String dcid) {
			Path = path;
			if (dcid != null) {
				DcId = dcid;
			} else {
				DcId = createDCID(path);
			}
		}

		private String createDCID(String path) {
			InputStream stream = null;
			try {
				final MessageDigest hash = MessageDigest.getInstance("SHA-256");
				stream = new FileInputStream(path);

				final byte[] buffer = new byte[2048];
				while (true) {
					final int nread = stream.read(buffer);
					if (nread == -1) {
						break;
					}
					hash.update(buffer, 0, nread);
				}

				final Formatter f = new Formatter();
				for (byte b : hash.digest()) {
					f.format("%02X", b & 0xFF);
				}
				try {
					return f.toString();
				} finally {
					f.close();
				}
			} catch (IOException e) {
				return null;
			} catch (NoSuchAlgorithmException e) {
				return null;
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	private BookInfo myBookInfo;

	BookInfo getBookInfo() {
		return myBookInfo;
	}

	public ViewHolder(final FBReaderPluginActivity activity) {
		myActivity = activity;
		Log.e("VIEWHOLDER", "CREATE" + activity.toString());
		ourInstance = this;
		myDB = new BookSettingsDB(activity);
		myNeedToOpen = true;
		Collection.bindToService(activity, null);
		final Config config = Config.Instance();
		config.runOnConnect(new Runnable() {
			public void run() {
				config.requestAllValuesForGroup("Options");
				config.requestAllValuesForGroup("Style");
				config.requestAllValuesForGroup("LookNFeel");
				config.requestAllValuesForGroup("Fonts");
				config.requestAllValuesForGroup("Colors");
				config.requestAllValuesForGroup("Files");
				config.requestAllValuesForGroup("ReadingModeMenu");

				if (!activity.restartIfNewOptionsFound()) {
					Collection.bindToService(activity, new Runnable() {
						public void run() {
							tryToOpenFile();
							Collection.addListener(ViewHolder.this);
						}
					});
				}
			}
		});
		addAction(ActionCode.SEARCH, new Actions.StartSearchAction(this));
		addAction(ActionCode.CLEAR_FIND_RESULTS, new Actions.StopSearchAction(this));
		addAction(ActionCode.FIND_NEXT, new Actions.FindNextAction(this));
		addAction(ActionCode.FIND_PREVIOUS, new Actions.FindPrevAction(this));
		addAction(ActionCode.SHOW_NAVIGATION, new Actions.NavigateAction(this));
		addAction(ActionCode.TOGGLE_BARS, new Actions.NavigateAction(this));
		addAction(ActionCode.SELECTION_CLEAR, new Actions.ClearSelectionAction(this));
		addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new Actions.CopySelectionAction(this));
		addAction(ActionCode.SELECTION_SHARE, new Actions.ShareSelectionAction(this));
		addAction(ActionCode.SELECTION_TRANSLATE, new Actions.TranslateSelectionAction(this));
		addAction(ActionCode.SELECTION_BOOKMARK, new Actions.BookmarkSelectionAction(this));
		addAction(ActionCode.SHOW_LIBRARY, new Actions.OpenLibraryAction(this));
		addAction(ActionCode.SHOW_PREFERENCES, new Actions.OpenSettingsAction(this));
		addAction(ActionCode.SHOW_BOOKMARKS, new Actions.OpenBookmarksAction(this));
		addAction(ActionCode.SHOW_BOOK_INFO, new Actions.OpenBookInfoAction(this));
		addAction(ActionCode.SHOW_TOC, new Actions.ShowTOCAction(this));
		addAction(ActionCode.SWITCH_TO_DAY_PROFILE, new Actions.SwitchProfileAction(this, activity.getSettings(), ColorProfile.DAY));
		addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, new Actions.SwitchProfileAction(this, activity.getSettings(), ColorProfile.NIGHT));
		addAction(ActionCode.INCREASE_FONT, new Actions.ZoomInAction(this));
		addAction(ActionCode.DECREASE_FONT, new Actions.ZoomOutAction(this));
		addAction(ActionCode.TURN_PAGE_BACK, new Actions.PrevPageAction(this));
		addAction(ActionCode.TURN_PAGE_FORWARD, new Actions.NextPageAction(this));
		addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, new Actions.PrevPageAction(this));
		addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, new Actions.NextPageAction(this));
		addAction(ActionCode.SHOW_MENU, new Actions.ShowMenuAction(this));
		addAction(ActionCode.SHOW_CANCEL_MENU, new Actions.CancelMenuAction(this));
		addAction(ActionCode.EXIT, new Actions.ExitAction(this));

		addAction(ActionCode.CROP, new Actions.CropAction(this));
		addAction(ActionCode.ZOOM_MODE, new Actions.ZoomModeAction(this));
		addAction(ActionCode.INTERSECTION, new Actions.IntersectionAction(this));
		addAction(ActionCode.PAGE_WAY, new Actions.PageWayAction(this));
		addAction(ActionCode.USE_BACKGROUND, new Actions.UseBackgroundAction(this));
		addAction(ActionCode.GOTO_PAGE_NUMBER, new Actions.GotoPageNumberAction(this));

		addAction(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM, new Actions.SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_SYSTEM));
		addAction(ActionCode.SET_SCREEN_ORIENTATION_SENSOR, new Actions.SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_SENSOR));
		addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, new Actions.SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_PORTRAIT));
		addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, new Actions.SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_LANDSCAPE));
		addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT, new Actions.SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT));
		addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE, new Actions.SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
	}

	private boolean myNeedToOpen;

	public synchronized void tryToOpenFile() {
		if (myNeedToOpen && Config.Instance().isInitialized() && myActivity.checkStoragePermission()) {
			final Intent intent = myActivity.getIntent();
			if (intent != null) {
				myNeedToOpen = false;
				openFile(intent);
			}
		}
	}

	private String mySelectedText;
	private int mySelectionStart;
	private int mySelectionEnd;
	private int mySelectedPageLength;

	private void applySettings() {
		final PluginView view = myActivity.getPluginView();
		view.resetNightMode();
		view.resetFooterParams();
		AndroidFontUtil.clearFontCache();
		view.postInvalidate();
	}

	synchronized void openFile(final Intent i) {
		myRenderedOnce = false;
		resetActionCount();
		String message = Util.getResourceString("dialog", "waitMessage", "loadingBook");
		final ProgressDialog progress = ProgressDialog.show(myActivity, null, message, true, false);
		new Thread() {
			public void run() {
				applySettings();
				final boolean success;
				synchronized (mySyncLock) {
					success = openFileInternal(i);
				}
				if (!success) {
					onSync(false);
					progress.dismiss();
					return;
				}
				myDB.loadAll(ViewHolder.this);
				myActivity.runOnUiThread(new Runnable() {
					public void run() {
						myActivity.applyUISettings();
						myActivity.updateBookInfo(myBook);
						myActivity.invalidateOptionsMenu();
						try {
							progress.dismiss();
						} catch (Throwable t) {
							// ignore
						}
					}
				});
			}
		}.start();
	}

	private boolean openFileInternal(Intent i) {
		if (i == null) {
			return false;
		}
		String oldPath = myBookInfo == null ? null : myBookInfo.Path;
		int pageIndex = -1;
		final Bookmark bookmark = FBReaderIntents.getBookmarkExtra(i);
		if (bookmark != null) {
			pageIndex = bookmark.ParagraphIndex;
		}
		myBook = FBReaderIntents.getBookExtra(i, Collection);
		if (myBook != null) {
			final String path = myBook.getPath();
			final List<UID> uids = myBook.uids();
			final String dcid = uids.isEmpty() ? null : uids.get(0).Id;
			myBookInfo = new BookInfo(path, dcid);
		}

		if ((myBookInfo != null && myBookInfo.Path == null) ||
			!FBReaderIntents.Action.PLUGIN_VIEW.equals(i.getAction())) {
			return false;
		}
		final PluginView view = myActivity.getPluginView();
		if (myBookInfo.Path.equals(oldPath)) {
			if (pageIndex != -1) {
				view.gotoPage(pageIndex, false);
				return true;
			}
		}
		if (!view.open(myBook)) {
			onFatalError(true);
			return false;
		}
		view.setListener(this);
		if (pageIndex == -1) {
			pageIndex = 0;
		}
		view.gotoPage(pageIndex, false);
		loadBookmarks();
		return true;
	}

	public void saveBookmark(final Bookmark bookmark) {
		final PluginView view = myActivity.getPluginView();
		Collection.bindToService(myActivity, new Runnable() {
			@Override
			public void run() {
				Collection.saveBookmark(bookmark);
				view.clearSelection();
				view.postInvalidate();
				myActivity.showBookmarkToast(bookmark);
			}
		});
	}

	public void loadBookmarks() {
		final PluginView view = myActivity.getPluginView();
		Collection.bindToService(myActivity, new Runnable() {
			public void run() {
				view.setStyles(Collection.highlightingStyles());
				final List<Bookmark> bookmarks = new ArrayList<Bookmark>();
				for (BookmarkQuery query = new BookmarkQuery(myBook, 50);; query = query.next()) {
					final List<Bookmark> response = Collection.bookmarks(query);
					if (response.isEmpty()) {
						break;
					}
					bookmarks.addAll(response);
				}
				view.getDocument().setBookmarks(bookmarks);
				view.postInvalidate();
			}
		});
	}

	synchronized void onSettingsChange() {
		applySettings();
		myActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				myActivity.applyUISettings();
			}
		});
	}

	public void onTouch() {
		myActivity.onTouch();
	}

	@Override
	public void onSelectionStart() {
		myActivity.getPluginView().stopSearch();
		myActivity.hideAllPopups();
	}

	@Override
	public void onSelectionEnd(String text, DocumentHolder.SelectionInfo selection, int length) {
		synchronized (selection) {
			if (selection.isEmpty()) {
				return;
			}
			mySelectedText = text;
			mySelectionStart = selection.startIndex();
			mySelectionEnd = selection.endIndex();
			mySelectedPageLength = length;
			myActivity.onSelectionEnd();
		}
	}

	String getSelectedText() {
		return mySelectedText;
	}

	int getSelectionStart() {
		return mySelectionStart;
	}

	int getSelectionEnd() {
		return mySelectionEnd;
	}

	int getSelectedPageLength() {
		return mySelectedPageLength;
	}

	private void savePosition() {
		if (myBook == null) {
			return;
		}
		try {
			final PluginView view = myActivity.getPluginView();
			final int pageIndex = view.getCurPageNo();
			final ZLTextPositionWithTimestamp local =
				Collection.getStoredPosition(myBook.getId());
			if (local == null || pageIndex != local.Position.ParagraphIndex) {
				Collection.storePosition(
					myBook.getId(),
					new ZLTextPositionWithTimestamp(pageIndex, 0, 0, System.currentTimeMillis())
				);
				myBook.setProgress(RationalNumber.create(pageIndex, view.getPagesNum()));
				Collection.saveBook(myBook);
			}
		} catch (Throwable t) {
			// ignore
		}
	}

	public void storePosition() {
		Collection.bindToService(myActivity, new Runnable() {
			public void run() {
				savePosition();
			}
		});
	}

	public void unbindCollection() {
		Collection.bindToService(myActivity, new Runnable() {
			public void run() {
				savePosition();
				Collection.unbind();
			}
		});
	}

	public synchronized void finish() {
		Log.e("VIEWHOLDER", "FINISH" + myActivity.toString());
		if (ourInstance == null) {
			return;
		}
		unbindCollection();
		final PluginView view = myActivity.getPluginView();
		if (view != null) {
			view.close();
		}
		myDB.storeAll(this);
		myDB.close();
		ourInstance = null;
	}

	private int myKeyUnderTracking = -1;
	private long myTrackingStartTime;

	public boolean onKeyDownInternal(int keyCode, KeyEvent event) {
		if (myBindings.hasBinding(keyCode, true) || myBindings.hasBinding(keyCode, false)) {
			if (myKeyUnderTracking != -1) {
				if (myKeyUnderTracking == keyCode) {
					return true;
				} else {
					myKeyUnderTracking = -1;
				}
			}
			if (myBindings.hasBinding(keyCode, true)) {
				myKeyUnderTracking = keyCode;
				myTrackingStartTime = System.currentTimeMillis();
				return true;
			} else {
				return runAction(myBindings.getBinding(keyCode, false));
			}
		} else {
			return false;
		}
	}

	public boolean onKeyUpInternal(int keyCode, KeyEvent event) {
		if (myKeyUnderTracking != -1) {
			if (myKeyUnderTracking == keyCode) {
				final boolean longPress =
					System.currentTimeMillis() >
					myTrackingStartTime + ViewConfiguration.getLongPressTimeout();
				runAction(myBindings.getBinding(keyCode, longPress));
			}
			myKeyUnderTracking = -1;
			return true;
		} else {
			return
				myBindings.hasBinding(keyCode, false) ||
				myBindings.hasBinding(keyCode, true);
		}
	}

	@Override
	public void onTapZoneClick(int x, int y, int width, int height) {
		String id = PageTurningOptions.TapZoneMap.getValue();
		if ("".equals(id)) {
			id = PageTurningOptions.Horizontal.getValue() ? "right_to_left" : "up";
		}
		final String action = TapZoneMap.zoneMap(id).getActionByCoordinates(
			x, y, width, height, TapZoneMap.Tap.singleTap
		);

		if ("previousPage".equals(action) || "nextPage".equals(action)) {
			switch (PageTurningOptions.FingerScrolling.getValue()) {
				default:
					break;
				case byTap:
				case byTapAndFlick:
					runAction(action);
					break;
			}
		} else {
			runAction(action);
		}
	}

	boolean myRenderedOnce = false;

	@Override
	public void onCorrectRendering() {
		if (!myRenderedOnce) {
			Collection.bindToService(myActivity, new Runnable() {
				public void run() {
					Collection.addToRecentlyOpened(myBook);
				}
			});
			myRenderedOnce = true;
		}
	}

	@Override
	public void onFatalError(boolean force) {
		myActivity.onFatalError(force);
	}

	private void onBookUpdated(Book book) {
		if (myBook == null || !Collection.sameBook(myBook, book)) {
			return;
		}
		myBook.updateFrom(book);
		myActivity.updateBookInfo(myBook);
	}

	// method from IBookCollection.Listener
	public void onBookEvent(BookEvent event, Book book) {
		switch (event) {
			default:
				onBookUpdated(book);
				break;
			case BookmarkStyleChanged:
			{
				final PluginView view = myActivity.getPluginView();
				view.setStyles(Collection.highlightingStyles());
				view.postInvalidate();
				break;
			}
			case BookmarksUpdated:
				if (myBook.getId() == book.getId()) {
					loadBookmarks();
				}
				break;
			case Updated:
				onBookUpdated(book);
				break;
		}
	}

	private final Object mySyncLock = new Object();
	void onSync(boolean openOtherBook) {
		synchronized (mySyncLock) {
			onSyncInternal(openOtherBook);
		}
	}

	private void onSyncInternal(boolean openOtherBook) {
		if (myBook == null) {
			return;
		}

		if (openOtherBook) {
			final SyncData.ServerBookInfo info = mySyncData.getServerBookInfo();
			if (info != null) {
				Book book = null;
				for (String hash : info.Hashes) {
					book = Collection.getBookByHash(hash);
					if (book != null) {
						break;
					}
				}
				if (book != null && !Collection.sameBook(book, myBook)) {
					final Intent intent = FBReaderIntents.internalIntent(FBReaderIntents.Action.VIEW)
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					FBReaderIntents.putBookExtra(intent, book);
					myActivity.startActivity(intent);
					myActivity.finish();
					myActivity.overridePendingTransition(0, 0);
					return;
				}
			}
		}

		final ZLTextPositionWithTimestamp fromServer =
			mySyncData.getAndCleanPosition(Collection.getHash(myBook, true));
		if (fromServer == null) {
			return;
		}
		final ZLTextPositionWithTimestamp local =
			Collection.getStoredPosition(myBook.getId());

		if (local == null || local.Timestamp < fromServer.Timestamp) {
			myActivity.getPluginView().gotoPage(fromServer.Position.ParagraphIndex, false);
		}
	}
}
