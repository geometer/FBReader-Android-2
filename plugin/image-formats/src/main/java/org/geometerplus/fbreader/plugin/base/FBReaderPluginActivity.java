package org.geometerplus.fbreader.plugin.base;

import java.util.HashMap;

import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.fbreader.common.android.FBReaderUtil;
import org.fbreader.plugin.format.base.R;
import org.fbreader.reader.AbstractReader;
import org.fbreader.reader.android.MainActivity;

import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.plugin.base.document.DocumentHolder;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;
import org.geometerplus.fbreader.plugin.base.tree.TOCActivity;

import org.geometerplus.android.fbreader.SimplePopupWindow;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;

public abstract class FBReaderPluginActivity extends MainActivity {

	static abstract class PopupPanel implements View.OnClickListener {
		protected volatile SimplePopupWindow myWindow;

		protected synchronized void show_(FBReaderPluginActivity activity, RelativeLayout root) {
			createControlPanel(activity, root);
			if (myWindow != null) {
				myWindow.show();
			}
		}

		protected synchronized void hide_() {
			if (myWindow != null) {
				myWindow.hide();
			}
		}

		public abstract void createControlPanel(FBReaderPluginActivity activity, RelativeLayout root);
		protected abstract void update();
	}

	private volatile long myResumeTimestamp;

	private volatile SettingsHolder mySettings;
	public SettingsHolder getSettings() {
		if (mySettings == null) {
			mySettings = new SettingsHolder(getZLibrary());
		}
		return mySettings;
	}

	private void setButtonLight(boolean enabled) {
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.buttonBrightness = enabled ? -1.0f : 0.0f;
		getWindow().setAttributes(attrs);
	}

	private final HashMap<String,PopupPanel> myPopups = new HashMap<String,PopupPanel>();
	private PopupPanel myActivePopup;

	private boolean mySettingsCalled;

	public void settingsCalled() {
		mySettingsCalled = true;
	}

	private volatile ViewHolder myViewHolder;

	public final PluginView getPluginView() {
		return (PluginView)getMainView();
	}

	private volatile boolean myShowActionBarFlag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		myShowActionBarFlag = getZLibrary().ShowActionBarOption.getValue();

		selectMainView(R.id.fmt_main_view);
		if (myViewHolder == null) {
			myViewHolder = new ViewHolder(this);
		}
		if (FBReaderIntents.Action.PLUGIN_KILL.equals(getIntent().getAction())) {
			finish();
			overridePendingTransition(0, 0);
			return;
		}

		setupToolbar(getMainView(), myShowActionBarFlag);

		if (getPopupById(TextSearchPopup.ID) == null) {
			myPopups.put(TextSearchPopup.ID, new TextSearchPopup());
		}
		if (getPopupById(SelectionPopup.ID) == null) {
			myPopups.put(SelectionPopup.ID, new SelectionPopup());
		}
		mySettingsCalled = false;
	}

	private void search(String pattern) {
		if (getPluginView().startSearch(pattern)) {
			runOnUiThread(new Runnable() {
				public void run() {
					showPopup(TextSearchPopup.ID);
				}
			});
		} else {
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(
						FBReaderPluginActivity.this,
						Util.getResourceString("errorMessage", "textNotFound"),
						Toast.LENGTH_SHORT
					).show();
				}
			});
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (FBReaderIntents.Action.PLUGIN_KILL.equals(intent.getAction())) {
			finish();
			overridePendingTransition(0, 0);
		} else if (FBReaderIntents.Action.PLUGIN_VIEW.equals(intent.getAction())) {
			setIntent(intent);
			myViewHolder.openFile(intent);
		} else {
			super.onNewIntent(intent);
		}
	}

	void applyUISettings() {
		final ZLAndroidLibrary zlibrary = getZLibrary();
		setOrientation(zlibrary.getOrientationOption().getValue());
		if (zlibrary.ShowStatusBarOption.getValue()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		if (zlibrary.DisableButtonLightsOption.getValue()) {
			setButtonLight(false);
		}

		final int brightnessLevel = zlibrary.ScreenBrightnessLevelOption.getValue();
		if (brightnessLevel != 0) {
			getMainView().setScreenBrightness(brightnessLevel, false);
		} else {
			setScreenBrightnessAuto();
		}

		hideBars();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		final Menu sm = menu.addSubMenu(getResources().getString(R.string.bookOptions));
		addMenuItem(sm, ActionCode.CROP, getResources().getString(R.string.crop));
		addMenuItem(sm, ActionCode.ZOOM_MODE, getResources().getString(R.string.zoomMode));
		addMenuItem(sm, ActionCode.PAGE_WAY, getResources().getString(R.string.pageWay));
		addMenuItem(sm, ActionCode.INTERSECTION, getResources().getString(R.string.intersections));
		addMenuItem(sm, ActionCode.USE_BACKGROUND, getResources().getString(R.string.useWallpaper));
		return true;
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		if (!getZLibrary().ShowActionBarOption.getValue()) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}

	void toggleBars() {
		if (myNavigationPopup != null) {
			hideBars();
		} else {
			showBars();
		}
	}

	public final PopupPanel getPopupById(String id) {
		return myPopups.get(id);
	}

	public final void hideActivePopup() {
		if (myActivePopup != null) {
			myActivePopup.hide_();
			myActivePopup = null;
		}
	}

	public final void showPopup(String id) {
		hideActivePopup();
		myActivePopup = myPopups.get(id);
		hideBars();
		if (myActivePopup != null) {
			myActivePopup.show_(this, (RelativeLayout)findViewById(R.id.root_view));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		Config.Instance().runOnConnect(new Runnable() {
			public void run() {
				final String action = myViewHolder.SyncOptions.Enabled.getValue()
					? FBReaderIntents.Action.SYNC_START : FBReaderIntents.Action.SYNC_STOP;
				startService(FBReaderIntents.internalIntent(action));
			}
		});

		if (mySettingsCalled) {
			mySettingsCalled = false;
			myViewHolder.onSettingsChange();
		}

		if (getZLibrary().DisableButtonLightsOption.getValue()) {
			setButtonLight(false);
		}
		hideBars();

		registerReceiver(mySyncUpdateReceiver, new IntentFilter(FBReaderIntents.Event.SYNC_UPDATED));
		myResumeTimestamp = System.currentTimeMillis();
		myViewHolder.onSync(myViewHolder.SyncOptions.ChangeCurrentBook.getValue());
		DictionaryUtil.init(this, null);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (myActivePopup != null) {
			myActivePopup.show_(this, (RelativeLayout)findViewById(R.id.root_view));
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return myViewHolder.onKeyDownInternal(keyCode, event) || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return myViewHolder.onKeyUpInternal(keyCode, event) || super.onKeyUp(keyCode, event);
	}

	/*
	@Override
	protected void closeApplication() {
		final Intent i = FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.CLOSE);
		i.putExtra(FBReaderIntents.Key.TYPE, "close");
		startActivity(i);
		finish();
	}
	*/

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			default:
				super.onActivityResult(requestCode, resultCode, data);
				break;
			case REQUEST_CANCEL_MENU:
				if (resultCode != RESULT_CANCELED && resultCode != -1) {
					final Intent i = FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.CLOSE);
					i.putExtra(
						FBReaderIntents.Key.TYPE, data.getStringExtra(FBReaderIntents.Key.TYPE)
					);
					startActivity(i);
					finish();
				}
				break;
			case REQUEST_TOC:
				if (resultCode != RESULT_CANCELED) {
					final int pageNo = data.getIntExtra(TOCActivity.PAGENO_KEY, -1);
					if (pageNo != -1) {
						myViewHolder.getView().gotoPage(pageNo, false);
					}
				}
				break;
		}
	}

	@Override
	public void onPause() {
		if (myViewHolder.SyncOptions.Enabled.getValue()) {
			startService(FBReaderIntents.internalIntent(FBReaderIntents.Action.SYNC_QUICK_SYNC));
		}
		unregisterReceiver(mySyncUpdateReceiver);
		if (getZLibrary().DisableButtonLightsOption.getValue()) {
			setButtonLight(true);
		}
		myViewHolder.storeAll();
		super.onPause();
	}

	@Override
	public void onDestroy() {
		getPluginView().getDocument().close();
		if (myViewHolder != null) {
			myViewHolder.finish();
			myViewHolder = null;
		}
		super.onDestroy();
	}

	private final BroadcastReceiver mySyncUpdateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			myViewHolder.onSync(
				myResumeTimestamp + 10 * 1000 > System.currentTimeMillis() &&
				myViewHolder.SyncOptions.ChangeCurrentBook.getValue()
			);
		}
	};

	private NavigationPopup myNavigationPopup;
	private boolean myActionBarIsVisible = true;

	private void setStatusBarVisible(boolean visible) {
		getMainView().setPreserveSize(visible);
		if (!getZLibrary().ShowStatusBarOption.getValue()) {
			if (visible) {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			} else {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			}
		}
	}

	void hideBars() {
		if (myNavigationPopup != null) {
			myNavigationPopup.stopNavigation();
			myNavigationPopup = null;
		}
		hideBarsInternal();
		setStatusBarVisible(false);
		FBReaderUtil.ensureFullscreen(this, getMainView());
	}

	void hideBarsInternal() {
		if (Build.VERSION.SDK_INT >= 19/*Build.VERSION_CODES.KITKAT*/
				&& getZLibrary().EnableFullscreenModeOption.getValue()) {
			getMainView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE |
				2048 /*View.SYSTEM_UI_FLAG_IMMERSIVE*/ |
				4096 /*View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY*/ |
				4 /*View.SYSTEM_UI_FLAG_FULLSCREEN*/ |
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			);
		} else {
			getMainView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
		if (!getZLibrary().ShowActionBarOption.getValue()) {
			setTitleVisible(false);
			myActionBarIsVisible = false;
			invalidateOptionsMenu();
		}
	}

	void showBars() {
		setStatusBarVisible(true);
		showBarsInternal();

		if (myNavigationPopup == null) {
			hideActivePopup();
			myNavigationPopup = new NavigationPopup(getPluginView());
			final RelativeLayout root = (RelativeLayout)findViewById(R.id.root_view);
			myNavigationPopup.runNavigation(
				this, root, Util.getResourceString("dialog", "button", "resetPosition")
			);
		}
	}

	void showBarsInternal() {
		setTitleVisible(true);
		myActionBarIsVisible = true;
		invalidateOptionsMenu();

		getMainView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
	}

	public void refresh() {
		runOnUiThread(new Runnable() {
			public void run() {
				refreshMenu();
			}
		});
	}

	public void hideAllPopups() {
		hideActivePopup();
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			hideBars();
		}
	}

	private boolean myCrashWasHandled = false;

	public void onFatalError(boolean force) {
		if (force) {
			myCrashWasHandled = false;
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final ViewHolder viewHolder = myViewHolder;
				if (myCrashWasHandled) {
					return;
				}
				Toast.makeText(
					FBReaderPluginActivity.this,
					Util.getResourceString("errorMessage", "cannotOpenBook"),
					Toast.LENGTH_SHORT
				).show();
				Intent i = FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.PLUGIN_CRASH);
				if (viewHolder != null) {
					FBReaderIntents.putBookExtra(i, FBReaderIntents.getBookExtra(getIntent(), viewHolder.Collection));
				}
				startActivity(i);
				myCrashWasHandled = true;
				finish();
				overridePendingTransition(0, 0);
			}
		});
	}

	public void onPageChanged() {
		runOnUiThread(new Runnable() {
			public void run() {
				getPluginView().clearSelection();
				if (myActivePopup instanceof SelectionPopup) {
					hideActivePopup();
				}
				if (myActivePopup != null) {
					myActivePopup.update();
				}
				if (myNavigationPopup != null) {
					myNavigationPopup.update(FBReaderPluginActivity.this);
				}
			}
		});
	}

	public void onTouch() {
		if (myActivePopup != null) {
			myActivePopup.update();
		}
		((SelectionPopup)getPopupById(SelectionPopup.ID)).move(
			getPluginView().getSelectionStartY(),
			getPluginView().getSelectionEndY()
		);
		if (myNavigationPopup != null) {
			myNavigationPopup.update(this);
		}
	}

	public void onSelectionEnd() {
		hideBars();
		((SelectionPopup)getPopupById(SelectionPopup.ID)).move(
			getPluginView().getSelectionStartY(),
			getPluginView().getSelectionEndY()
		);
		showPopup(SelectionPopup.ID);
	}

	boolean restartIfNewOptionsFound() {
		final boolean showActionBar = getZLibrary().ShowActionBarOption.getValue();
		getZLibrary().ShowActionBarOption.saveSpecialValue();
		if (showActionBar != myShowActionBarFlag) {
			finish();
			startActivity(new Intent(this, getClass()));
			return true;
		}
		return false;
	}

	@Override
	public final AbstractReader getReader() {
		return myViewHolder;
	}

	public final DocumentHolder createDocument(Book book) {
		return ((PluginApplication)getApplication()).createDocument();
	}

	@Override
	public void hideDictionarySelection() {
		// TODO: implement
	}

	@Override
	protected boolean isActionBarVisible() {
		return myActionBarIsVisible;
	}

	@Override
	protected void doSearch(final String pattern) {
		hideActivePopup();
		getPluginView().clearSelection();
		String message = Util.getResourceString("dialog", "waitMessage", "search");
		final ProgressDialog progress = ProgressDialog.show(this, null, message, true, false);
		PluginView.AuxService.execute(new Runnable() {
			public void run() {
				search(pattern);
				runOnUiThread(new Runnable() {
					public void run() {
						progress.dismiss();
					}
				});
			}
		});
	}
}
