/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import java.io.*;
import java.util.*;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.text.Html;
import android.os.*;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.widget.RelativeLayout;

import org.fbreader.common.android.FBReaderUtil;
import org.fbreader.reader.ActionCode;
import org.fbreader.reader.android.MainActivity;
import org.fbreader.reader.android.UIMessageUtil;
import org.fbreader.util.Boolean3;

import com.yotadevices.fbreader.FBReaderYotaService;

import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.language.Language;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.*;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;

import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.text.view.ZLTextView;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.error.ErrorKeys;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.*;
import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper;
import org.fbreader.common.options.ColorProfile;
import org.geometerplus.fbreader.formats.ExternalFormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.tips.TipsManager;

import org.geometerplus.android.fbreader.api.*;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.formatPlugin.PluginUtil;
import org.geometerplus.android.fbreader.httpd.DataService;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.fbreader.sync.SyncOperations;
import org.geometerplus.android.fbreader.tips.TipsActivity;

import org.geometerplus.android.util.*;

import org.fbreader.md.MDAlertDialogBuilder;

public final class FBReader extends MainActivity implements ZLApplicationWindow, IBookCollection.Listener<Book> {
	public static final int RESULT_DO_NOTHING = RESULT_FIRST_USER;
	public static final int RESULT_REPAINT = RESULT_FIRST_USER + 1;

	public static Intent defaultIntent(Context context) {
		return new Intent(context, FBReader.class)
			.setAction(FBReaderIntents.Action.VIEW)
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	}

	public static void openBookActivity(Context context, Book book, Bookmark bookmark) {
		final Intent intent = defaultIntent(context);
		FBReaderIntents.putBookExtra(intent, book);
		FBReaderIntents.putBookmarkExtra(intent, bookmark);
		context.startActivity(intent);
	}

	private FBReaderApp myFBReaderApp;
	private volatile Book myBook;

	private RelativeLayout myRootView;

	private volatile boolean myShowStatusBarFlag;
	private volatile boolean myShowActionBarFlag;
	private volatile boolean myActionBarIsVisible;

	final DataService.Connection DataConnection = new DataService.Connection();

	volatile boolean IsPaused = false;
	private volatile long myResumeTimestamp;
	volatile Runnable OnResumeAction = null;

	private Intent myCancelIntent = null;
	private Intent myOpenBookIntent = null;

	private final FBReaderApp.Notifier myNotifier = new AppNotifier(this);

	private static final String PLUGIN_ACTION_PREFIX = "___";
	private final List<PluginApi.ActionInfo> myPluginActions =
		new LinkedList<PluginApi.ActionInfo>();
	private final BroadcastReceiver myPluginInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final ArrayList<PluginApi.ActionInfo> actions = getResultExtras(true).<PluginApi.ActionInfo>getParcelableArrayList(PluginApi.PluginInfo.KEY);
			if (actions != null) {
				synchronized (myPluginActions) {
					int index = 0;
					while (index < myPluginActions.size()) {
						myFBReaderApp.removeAction(PLUGIN_ACTION_PREFIX + index++);
					}
					myPluginActions.addAll(actions);
					index = 0;
					for (PluginApi.ActionInfo info : myPluginActions) {
						myFBReaderApp.addAction(
							PLUGIN_ACTION_PREFIX + index++,
							new RunPluginAction(FBReader.this, info.getId())
						);
					}
					if (!myPluginActions.isEmpty()) {
						invalidateOptionsMenu();
					}
				}
			}
		}
	};

	private synchronized void openBook(Intent intent, final Runnable action, boolean force) {
		if (!force && myBook != null) {
			return;
		}

		myBook = FBReaderIntents.getBookExtra(intent, myFBReaderApp.Collection);
		final Bookmark bookmark = FBReaderIntents.getBookmarkExtra(intent);
		if (myBook == null) {
			final Uri data = intent.getData();
			if (data != null) {
				myBook = createBookForFile(ZLFile.createFileByPath(data.getPath()));
			}
		}
		if (myBook != null) {
			ZLFile file = BookUtil.fileByBook(myBook);
			if (!file.exists()) {
				if (file.getPhysicalFile() != null) {
					file = file.getPhysicalFile();
				}
				UIMessageUtil.showErrorMessage(this, "fileNotFound", file.getPath());
				myBook = null;
			} else {
				NotificationUtil.drop(this, myBook);
			}
		}
		Config.Instance().runOnConnect(new Runnable() {
			public void run() {
				myFBReaderApp.openBook(myBook, bookmark, new Runnable() {
					public void run() {
						if (action != null) {
							action.run();
						}
						hideBars();
						if (DeviceType.Instance() == DeviceType.YOTA_PHONE) {
							refreshYotaScreen();
						}
					}
				}, myNotifier);
				AndroidFontUtil.clearFontCache();
			}
		});
	}

	private Book createBookForFile(ZLFile file) {
		if (file == null) {
			return null;
		}
		Book book = myFBReaderApp.Collection.getBookByFile(file.getPath());
		if (book != null) {
			return book;
		}
		if (file.isArchive()) {
			for (ZLFile child : file.children()) {
				book = myFBReaderApp.Collection.getBookByFile(child.getPath());
				if (book != null) {
					return book;
				}
			}
		}
		return null;
	}

	private Runnable getPostponedInitAction() {
		return new Runnable() {
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						new TipRunner().start();
						DictionaryUtil.init(FBReader.this, null);
						final Intent intent = getIntent();
						if (intent != null && FBReaderIntents.Action.PLUGIN.equals(intent.getAction())) {
							new RunPluginAction(FBReader.this, intent.getData()).run();
						}
					}
				});
			}
		};
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		bindService(
			new Intent(this, DataService.class),
			DataConnection,
			DataService.BIND_AUTO_CREATE
		);

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
			}
		});

		final ZLAndroidLibrary zlibrary = getZLibrary();
		myShowStatusBarFlag = zlibrary.ShowStatusBarOption.getValue();
		myShowActionBarFlag = zlibrary.ShowActionBarOption.getValue();
		myActionBarIsVisible = myShowActionBarFlag;

		getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN,
			myShowStatusBarFlag ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN
		);

		myRootView = (RelativeLayout)findViewById(R.id.root_view);
		selectMainView(R.id.main_view);

		setupToolbar(findViewById(R.id.main_drawer_layout), myShowActionBarFlag);

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		myFBReaderApp = (FBReaderApp)FBReaderApp.Instance();
		if (myFBReaderApp == null) {
			final BookCollectionShadow collection = new BookCollectionShadow();
			collection.addListener(this);
			myFBReaderApp = new FBReaderApp(Paths.systemInfo(this), collection);
		}
		getCollection().bindToService(this, null);
		myBook = null;

		myFBReaderApp.setWindow(this);
		myFBReaderApp.initWindow();

		myFBReaderApp.setExternalFileOpener(new ExternalFileOpener(this));

		updateWindowTitle();

		if (myFBReaderApp.getPopupById(TextSearchPopup.ID) == null) {
			new TextSearchPopup(myFBReaderApp);
		}
		if (myFBReaderApp.getPopupById(SelectionPopup.ID) == null) {
			new SelectionPopup(myFBReaderApp);
		}

		myFBReaderApp.addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(this));
		myFBReaderApp.addAction(ActionCode.SHOW_PREFERENCES, new ShowPreferencesAction(this));
		myFBReaderApp.addAction(ActionCode.SHOW_BOOK_INFO, new ShowBookInfoAction(this));
		myFBReaderApp.addAction(ActionCode.SHOW_TOC, new ShowTOCAction(this));
		myFBReaderApp.addAction(ActionCode.SHOW_BOOKMARKS, new ShowBookmarksAction(this));

		myFBReaderApp.addAction(ActionCode.TOGGLE_BARS, new ToggleBarsAction(this));
		myFBReaderApp.addAction(ActionCode.SEARCH, new SearchAction(this));

		myFBReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(this));
		myFBReaderApp.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(this));
		myFBReaderApp.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(this));
		myFBReaderApp.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(this));
		myFBReaderApp.addAction(ActionCode.SELECTION_TRANSLATE, new SelectionTranslateAction(this));
		myFBReaderApp.addAction(ActionCode.SELECTION_BOOKMARK, new SelectionBookmarkAction(this));

		myFBReaderApp.addAction(ActionCode.DISPLAY_BOOK_POPUP, new DisplayBookPopupAction(this));
		myFBReaderApp.addAction(ActionCode.PROCESS_HYPERLINK, new ProcessHyperlinkAction(this));
		myFBReaderApp.addAction(ActionCode.OPEN_VIDEO, new OpenVideoAction(this));
		myFBReaderApp.addAction(ActionCode.HIDE_TOAST, new HideToastAction(this));

		myFBReaderApp.addAction(ActionCode.SHOW_CANCEL_MENU, new ShowCancelMenuAction(this));
		myFBReaderApp.addAction(ActionCode.OPEN_START_SCREEN, new StartScreenAction(this));

		myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM, new SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_SYSTEM));
		myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_SENSOR, new SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_SENSOR));
		myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, new SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_PORTRAIT));
		myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, new SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_LANDSCAPE));
		if (getZLibrary().supportsAllOrientations()) {
			myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT, new SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT));
			myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE, new SetScreenOrientationAction(this, ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
		}
		myFBReaderApp.addAction(ActionCode.INSTALL_PLUGINS, new InstallPluginsAction(this));

		myFBReaderApp.addAction(ActionCode.SWITCH_TO_DAY_PROFILE, new SwitchProfileAction(this, ColorProfile.DAY));
		myFBReaderApp.addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, new SwitchProfileAction(this, ColorProfile.NIGHT));

		myFBReaderApp.addAction(ActionCode.GOTO_PAGE_NUMBER, new GotoPageNumberAction(this));

		myFBReaderApp.addAction(ActionCode.YOTA_SWITCH_TO_BACK_SCREEN, new YotaSwitchScreenAction(this, true));
		myFBReaderApp.addAction(ActionCode.YOTA_SWITCH_TO_FRONT_SCREEN, new YotaSwitchScreenAction(this, false));

		Config.Instance().runOnConnect(new Runnable() {
			public void run() {
				showPremiumDialog();
			}
		});

		final Intent intent = getIntent();
		final String action = intent.getAction();

		myOpenBookIntent = intent;
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
			if (FBReaderIntents.Action.CLOSE.equals(action)) {
				myCancelIntent = intent;
				myOpenBookIntent = null;
			} else if (FBReaderIntents.Action.PLUGIN_CRASH.equals(action)) {
				setExternalBook(null);
				myOpenBookIntent = null;
				getCollection().bindToService(this, new Runnable() {
					public void run() {
						myFBReaderApp.openBook(null, null, null, myNotifier);
					}
				});
			}
		}
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		final String action = intent.getAction();
		final Uri data = intent.getData();

		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
			super.onNewIntent(intent);
		} else if (Intent.ACTION_VIEW.equals(action)
				   && data != null && "fbreader-action".equals(data.getScheme())) {
			myFBReaderApp.runAction(data.getEncodedSchemeSpecificPart(), data.getFragment());
		} else if (Intent.ACTION_VIEW.equals(action) || FBReaderIntents.Action.VIEW.equals(action)) {
			myOpenBookIntent = intent;
			if (myFBReaderApp.Model == null) {
				final Book external = getExternalBook();
				if (external != null) {
					final BookCollectionShadow collection = getCollection();
					final Book b = FBReaderIntents.getBookExtra(intent, collection);
					if (!collection.sameBook(b, external)) {
						try {
							final ExternalFormatPlugin plugin =
								(ExternalFormatPlugin)BookUtil.getPlugin(
									PluginCollection.Instance(Paths.systemInfo(this)),
									external
								);
							startActivity(PluginUtil.createIntent(plugin, FBReaderIntents.Action.PLUGIN_KILL));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} else if (FBReaderIntents.Action.PLUGIN.equals(action)) {
			new RunPluginAction(this, data).run();
		} else if (FBReaderIntents.Action.CLOSE.equals(intent.getAction())) {
			myCancelIntent = intent;
			myOpenBookIntent = null;
		} else if (FBReaderIntents.Action.PLUGIN_CRASH.equals(intent.getAction())) {
			final Book book = FBReaderIntents.getBookExtra(intent, myFBReaderApp.Collection);
			afterExternalCrash(book);
		} else {
			super.onNewIntent(intent);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		getCollection().bindToService(this, new Runnable() {
			public void run() {
				new Thread() {
					public void run() {
						getPostponedInitAction().run();
					}
				}.start();

				myFBReaderApp.getViewWidget().repaint();
			}
		});

		initPluginActions();

		final ZLAndroidLibrary zlibrary = getZLibrary();

		Config.Instance().runOnConnect(new Runnable() {
			public void run() {
				final boolean showStatusBar = zlibrary.ShowStatusBarOption.getValue();
				final boolean showActionBar = zlibrary.ShowActionBarOption.getValue();
				if (showStatusBar != myShowStatusBarFlag || showActionBar != myShowActionBarFlag) {
					finish();
					startActivity(new Intent(FBReader.this, FBReader.class));
				}
				zlibrary.ShowStatusBarOption.saveSpecialValue();
				zlibrary.ShowActionBarOption.saveSpecialValue();
				myFBReaderApp.ViewOptions.ColorProfileName.saveSpecialValue();
				setOrientation(zlibrary.getOrientationOption().getValue());
			}
		});

		((PopupPanel)myFBReaderApp.getPopupById(TextSearchPopup.ID)).setPanelInfo(this, myRootView);
		((PopupPanel)myFBReaderApp.getPopupById(SelectionPopup.ID)).setPanelInfo(this, myRootView);
	}

	private void initPluginActions() {
		synchronized (myPluginActions) {
			if (!myPluginActions.isEmpty()) {
				int index = 0;
				while (index < myPluginActions.size()) {
					myFBReaderApp.removeAction(PLUGIN_ACTION_PREFIX + index++);
				}
				myPluginActions.clear();
				invalidateOptionsMenu();
			}
		}

		sendOrderedBroadcast(
			new Intent(PluginApi.ACTION_REGISTER).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
			null,
			myPluginInfoReceiver,
			null,
			RESULT_OK,
			null,
			null
		);
	}

	private class TipRunner extends Thread {
		TipRunner() {
			setPriority(MIN_PRIORITY);
		}

		public void run() {
			final TipsManager manager = new TipsManager(Paths.systemInfo(FBReader.this));
			switch (manager.requiredAction()) {
				case Initialize:
					startActivity(new Intent(
						TipsActivity.INITIALIZE_ACTION, null, FBReader.this, TipsActivity.class
					));
					break;
				case Show:
					startActivity(new Intent(
						TipsActivity.SHOW_TIP_ACTION, null, FBReader.this, TipsActivity.class
					));
					break;
				case Download:
					manager.startDownloading();
					break;
				case None:
					break;
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		myStartTimer = true;
		Config.Instance().runOnConnect(new Runnable() {
			public void run() {
				SyncOperations.enableSync(FBReader.this, myFBReaderApp.SyncOptions);

				final int brightnessLevel =
					getZLibrary().ScreenBrightnessLevelOption.getValue();
				if (brightnessLevel != 0) {
					getViewWidget().setScreenBrightness(brightnessLevel, false);
				} else {
					setScreenBrightnessAuto();
				}
				if (getZLibrary().DisableButtonLightsOption.getValue()) {
					setButtonLight(false);
				}

				getCollection().bindToService(FBReader.this, new Runnable() {
					public void run() {
						final BookModel model = myFBReaderApp.Model;
						if (model == null || model.Book == null) {
							return;
						}
						onPreferencesUpdate(myFBReaderApp.Collection.getBookById(model.Book.getId()));
					}
				});
			}
		});

		IsPaused = false;
		myResumeTimestamp = System.currentTimeMillis();
		if (OnResumeAction != null) {
			final Runnable action = OnResumeAction;
			OnResumeAction = null;
			action.run();
		}

		registerReceiver(mySyncUpdateReceiver, new IntentFilter(FBReaderIntents.Event.SYNC_UPDATED));

		setOrientation(getZLibrary().getOrientationOption().getValue());
		final Book external = getExternalBook();
		if (myCancelIntent != null) {
			final Intent intent = myCancelIntent;
			myCancelIntent = null;
			getCollection().bindToService(this, new Runnable() {
				public void run() {
					runCancelAction(intent);
				}
			});
			return;
		} else if (myOpenBookIntent != null) {
			final Intent intent = myOpenBookIntent;
			myOpenBookIntent = null;
			getCollection().bindToService(this, new Runnable() {
				public void run() {
					openBook(intent, null, true);
				}
			});
		} else if (myFBReaderApp.getCurrentServerBook(null) != null) {
			getCollection().bindToService(this, new Runnable() {
				public void run() {
					myFBReaderApp.useSyncInfo(true, myNotifier);
				}
			});
		} else if (myFBReaderApp.Model == null && external != null) {
			reopenExternalBook(external);
		} else {
			getCollection().bindToService(this, new Runnable() {
				public void run() {
					myFBReaderApp.useSyncInfo(true, myNotifier);
				}
			});
		}

		PopupPanel.restoreVisibilities(myFBReaderApp);

		hideBars();

		ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_OPENED);
	}

	private void reopenExternalBook(final Book external) {
		final ZLStringOption option = crashBookPathOption();
		final String crashPath = option.getValue();
		final ZLResource resource = ZLResource.resource("crash").getResource("recoveringDialog");
		final ZLResource btnResource = ZLResource.resource("dialog").getResource("button");
		option.setValue("");
		if (external.getPath().equals(crashPath)) {
			afterExternalCrash(external);
		} else {
			getCollection().bindToService(this, new Runnable() {
				public void run() {
					myFBReaderApp.openBook(external, null, null, myNotifier);
				}
			});
		}
	}

	private void afterExternalCrash(final Book external) {
		setExternalBook(null);
		myOpenBookIntent = null;
		final BookCollectionShadow collection = getCollection();
		collection.bindToService(this, new Runnable() {
			public void run() {
				Book recent = collection.getRecentBook(0);
				if (collection.sameBook(recent, external)) {
					recent = collection.getRecentBook(1);
				}
				if (recent != null) {
					myFBReaderApp.openBook(recent, null, null, myNotifier);
				} else {
					myFBReaderApp.openHelpBook();
				}
			}
		});
	}

	@Override
	protected void onPause() {
		SyncOperations.quickSync(this, myFBReaderApp.SyncOptions);

		IsPaused = true;
		try {
			unregisterReceiver(mySyncUpdateReceiver);
		} catch (IllegalArgumentException e) {
		}

		myFBReaderApp.stopTimer();
		if (getZLibrary().DisableButtonLightsOption.getValue()) {
			setButtonLight(true);
		}
		myFBReaderApp.onWindowClosing();

		super.onPause();
	}

	@Override
	protected void onStop() {
		ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_CLOSED);
		PopupPanel.removeAllWindows(myFBReaderApp, this);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		getCollection().unbind();
		unbindService(DataConnection);
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		myFBReaderApp.onWindowClosing();
		super.onLowMemory();
	}

	public void showSelectionPanel() {
		final ZLTextView view = myFBReaderApp.getTextView();
		((SelectionPopup)myFBReaderApp.getPopupById(SelectionPopup.ID))
			.move(view.getSelectionStartY(), view.getSelectionEndY());
		myFBReaderApp.showPopup(SelectionPopup.ID);
		hideBars();
	}

	public void hideSelectionPanel() {
		final FBReaderApp.PopupPanel popup = myFBReaderApp.getActivePopup();
		if (popup != null && popup.getId() == SelectionPopup.ID) {
			myFBReaderApp.hideActivePopup();
		}
	}

	private void onPreferencesUpdate(Book book) {
		AndroidFontUtil.clearFontCache();
		myFBReaderApp.onBookUpdated(book);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			default:
				super.onActivityResult(requestCode, resultCode, data);
				break;
			case REQUEST_PREFERENCES:
				if (resultCode != RESULT_DO_NOTHING) {
					invalidateOptionsMenu();
					final Book book = data != null ? FBReaderIntents.getBookExtra(data, myFBReaderApp.Collection) : null;
					if (book != null) {
						getCollection().bindToService(this, new Runnable() {
							public void run() {
								onPreferencesUpdate(book);
							}
						});
					}
				}
				break;
			case REQUEST_TOC:
				if (resultCode == RESULT_OK && data != null) {
					final int ref = data.getIntExtra(TOCKey.REF, -1);
					if (ref != -1) {
						final FBReaderApp fbreader = myFBReaderApp;
						fbreader.addInvisibleBookmark();
						fbreader.BookTextView.gotoPosition(ref, 0, 0);
						fbreader.showBookTextView();
						fbreader.storePosition();
					}
				}
			case REQUEST_CANCEL_MENU:
				runCancelAction(data);
				break;
		}
	}

	private void runCancelAction(Intent intent) {
		final CancelMenuHelper.ActionType type;
		try {
			type = CancelMenuHelper.ActionType.valueOf(
				intent.getStringExtra(FBReaderIntents.Key.TYPE)
			);
		} catch (Exception e) {
			// invalid (or null) type value
			return;
		}
		Bookmark bookmark = null;
		if (type == CancelMenuHelper.ActionType.returnTo) {
			bookmark = FBReaderIntents.getBookmarkExtra(intent);
			if (bookmark == null) {
				return;
			}
		}
		myFBReaderApp.runCancelAction(type, bookmark);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		synchronized (myPluginActions) {
			int index = 0;
			for (PluginApi.ActionInfo info : myPluginActions) {
				if (info instanceof PluginApi.MenuActionInfo) {
					addMenuItem(
						menu,
						PLUGIN_ACTION_PREFIX + index++,
						((PluginApi.MenuActionInfo)info).MenuItemName
					);
				}
			}
		}

		return true;
	}

	protected void onPluginNotFound(final Book book) {
		final BookCollectionShadow collection = getCollection();
		collection.bindToService(this, new Runnable() {
			public void run() {
				final Book recent = collection.getRecentBook(0);
				if (recent != null && !collection.sameBook(recent, book)) {
					myFBReaderApp.openBook(recent, null, null, null);
				} else {
					myFBReaderApp.openHelpBook();
				}
			}
		});
	}

	private void setStatusBarVisible(boolean visible) {
		final ZLAndroidLibrary zlibrary = getZLibrary();
		if (DeviceType.Instance() != DeviceType.KINDLE_FIRE_1ST_GENERATION && !myShowStatusBarFlag) {
			getMainView().setPreserveSize(visible);
			if (visible) {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			} else {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			}
		}
	}

	private NavigationPopup myNavigationPopup;

	public boolean barsAreShown() {
		return myNavigationPopup != null;
	}

	@Override
	protected final void hideBars() {
		closeDrawer();

		if (myNavigationPopup != null) {
			myNavigationPopup.stopNavigation();
			myNavigationPopup = null;
		}

		if (!myShowActionBarFlag) {
			setTitleVisible(false);
			myActionBarIsVisible = false;
			invalidateOptionsMenu();
		}

		FBReaderUtil.ensureFullscreen(this, myRootView);

		setStatusBarVisible(false);
	}

	void showBars() {
		setStatusBarVisible(true);

		setTitleVisible(true);
		myActionBarIsVisible = true;
		invalidateOptionsMenu();

		myRootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

		if (myNavigationPopup == null) {
			myFBReaderApp.hideActivePopup();
			myNavigationPopup = new NavigationPopup(myFBReaderApp);
			myNavigationPopup.runNavigation(this, myRootView);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		final View view = getMainView();
		return (view != null && view.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		final View view = getMainView();
		return (view != null && view.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
	}

	private void setButtonLight(boolean enabled) {
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.buttonBrightness = enabled ? -1.0f : 0.0f;
		getWindow().setAttributes(attrs);
	}

	private boolean myStartTimer;

	@Override
	public void createWakeLock() {
		super.createWakeLock();

		if (myStartTimer) {
			myFBReaderApp.startTimer();
			myStartTimer = false;
		}
	}

	private BookCollectionShadow getCollection() {
		return (BookCollectionShadow)myFBReaderApp.Collection;
	}

	// methods from ZLApplicationWindow interface
	@Override
	public void showErrorMessage(String key) {
		UIMessageUtil.showErrorMessage(this, key);
	}

	@Override
	public void showErrorMessage(String key, String parameter) {
		UIMessageUtil.showErrorMessage(this, key, parameter);
	}

	@Override
	public FBReaderApp.SynchronousExecutor createExecutor(String key) {
		return UIUtil.createExecutor(this, key);
	}

	@Override
	public void close() {
		finish();
	}

	@Override
	public ZLViewWidget getViewWidget() {
		return (ZLViewWidget)getMainView();
	}

	@Override
	public void refresh() {
		runOnUiThread(new Runnable() {
			public void run() {
				refreshMenu();

				if (myNavigationPopup != null) {
					myNavigationPopup.update();
				}
			}
		});
	}

	@Override
	public void processException(Exception exception) {
		exception.printStackTrace();

		final Intent intent = new Intent(
			FBReaderIntents.Action.ERROR,
			new Uri.Builder().scheme(exception.getClass().getSimpleName()).build()
		);
		intent.setPackage(FBReaderIntents.DEFAULT_PACKAGE);
		intent.putExtra(ErrorKeys.MESSAGE, exception.getMessage());
		final StringWriter stackTrace = new StringWriter();
		exception.printStackTrace(new PrintWriter(stackTrace));
		intent.putExtra(ErrorKeys.STACKTRACE, stackTrace.toString());
		/*
		if (exception instanceof BookReadingException) {
			final ZLFile file = ((BookReadingException)exception).File;
			if (file != null) {
				intent.putExtra("file", file.getPath());
			}
		}
		*/
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			// ignore
			e.printStackTrace();
		}
	}

	@Override
	public void updateWindowTitle() {
		if (myFBReaderApp == null) {
			return;
		}
		final BookModel model = myFBReaderApp.Model;
		if (model != null) {
			updateBookInfo(model.Book);
		}
	}

	public void refreshYotaScreen() {
		final Intent intent = new Intent(this, FBReaderYotaService.class);
		intent.putExtra(
			FBReaderYotaService.KEY_BACK_SCREEN_IS_ACTIVE,
			myFBReaderApp.ViewOptions.YotaDrawOnBackScreen.getValue()
		);
		if (myFBReaderApp.Model != null) {
			FBReaderIntents.putBookExtra(intent, myFBReaderApp.Model.Book);
		}
		try {
			startService(intent);
		} catch (Throwable t) {
			// ignore
		}
	}

	private BroadcastReceiver mySyncUpdateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			myFBReaderApp.useSyncInfo(myResumeTimestamp + 10 * 1000 > System.currentTimeMillis(), myNotifier);
		}
	};

	public void outlineRegion(ZLTextRegion.Soul soul) {
		myFBReaderApp.getTextView().outlineRegion(soul);
		myFBReaderApp.getViewWidget().repaint();
	}

	public void hideOutline() {
		myFBReaderApp.getTextView().hideOutline();
		myFBReaderApp.getViewWidget().repaint();
	}

	public void hideDictionarySelection() {
		myFBReaderApp.getTextView().hideOutline();
		myFBReaderApp.getTextView().removeHighlightings(DictionaryHighlighting.class);
		myFBReaderApp.getViewWidget().reset();
		myFBReaderApp.getViewWidget().repaint();
	}

	private boolean resolveVersionConflict() {
		final Intent intent = getIntent();
		if (intent == null) {
			return false;
		}

		final Intent premiumIntent = FBReaderUtil.premiumIntent();
		if (!PackageUtil.canBeStarted(this, premiumIntent, false)) {
			return false;
		}

		if (!intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
			return true;
		}

		final ZLResource resource = ZLResource.resource("premium");
		final AlertDialog.Builder builder = new MDAlertDialogBuilder(this)
			.setMessage(resource.getResource("conflict").getValue())
			.setIcon(0)
			.setPositiveButton(
				resource.getResource("shortTitle").getValue(),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						startActivity(premiumIntent);
						finish();
					}
				}
			)
			.setNegativeButton(getResources().getString(R.string.app_name), null);
		ensureFullscreenOnDismiss(builder);
		builder.create().show();
		return true;
	}

	private void showPremiumDialog() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return;
		}

		if (resolveVersionConflict()) {
			return;
		}

		final int currentTime = (int)(System.currentTimeMillis() / 1000 / 60 / 60);
		final ZLIntegerOption lastCallOption = new ZLIntegerOption("Premium", "LastCall", 0);
		final int lastCall = lastCallOption.getValue();
		if (lastCall == 0) {
			lastCallOption.setValue(currentTime - 10 * 24);
			return;
		}
		final ZLIntegerOption countOption = new ZLIntegerOption("Premium", "Count", 0);
		final int count = countOption.getValue();
		if (count < 5) {
			countOption.setValue(count + 1);
			return;
		}
		if (lastCall + 15 * 24 > currentTime) {
			return;
		}

		if (isFinishing()) {
			return;
		}

		countOption.setValue(0);
		lastCallOption.setValue(currentTime);

		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		final AlertDialog.Builder builder = new MDAlertDialogBuilder(this)
			.setTitle(ZLResource.resource("premium").getValue())
			.setMessage(Html.fromHtml(FBReaderUtil.fromResourceFile(this, "data/premium")))
			.setIcon(0)
			.setPositiveButton(
				buttonResource.getResource("buy").getValue(),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(
							Intent.ACTION_VIEW,
							Uri.parse("market://details?id=com.fbreader")
						));
					}
				}
			)
			.setNegativeButton(buttonResource.getResource("noThanks").getValue(), null);
		ensureFullscreenOnDismiss(builder);
		builder.create().show();
	}

	void ensureFullscreenOnDismiss(AlertDialog.Builder builder) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					FBReaderUtil.ensureFullscreen(FBReader.this, myRootView);
				}
			});
		}
	}

	@Override
	protected FBReaderApp getReader() {
		return myFBReaderApp;
	}

	@Override
	protected boolean isActionBarVisible() {
		return myActionBarIsVisible;
	}

	@Override
	protected void doSearch(final String pattern) {
		final Runnable runnable = new Runnable() {
			public void run() {
				final TextSearchPopup popup = (TextSearchPopup)myFBReaderApp.getPopupById(TextSearchPopup.ID);
				popup.initPosition();
				if (myFBReaderApp.getTextView().search(pattern, true, false, false, false) != 0) {
					runOnUiThread(new Runnable() {
						public void run() {
							myFBReaderApp.showPopup(popup.getId());
							hideBars();
						}
					});
				} else {
					runOnUiThread(new Runnable() {
						public void run() {
							UIMessageUtil.showErrorMessage(FBReader.this, "textNotFound");
							popup.StartPosition = null;
						}
					});
				}
			}
		};
		UIUtil.wait("search", runnable, this);
	}

	// method from IBookCollection.Listener
	public void onBuildEvent(IBookCollection.Status status) {
	}

	// method from IBookCollection.Listener
	public void onBookEvent(BookEvent event, Book book) {
		if (event == BookEvent.Updated) {
			final Book external = getExternalBook();
			if (external != null && getCollection().sameBook(external, book)) {
				external.updateFrom(book);
			}
		}
	}

	private final Book getExternalBook() {
		return ((FBReaderApplication)getApplication()).ExternalBook;
	}

	void setExternalBook(Book book) {
		((FBReaderApplication)getApplication()).ExternalBook = book;
	}
}
