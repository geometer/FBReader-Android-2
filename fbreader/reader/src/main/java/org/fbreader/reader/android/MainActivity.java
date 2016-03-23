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

package org.fbreader.reader.android;

import java.util.*;

import android.app.SearchManager;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.*;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.widget.*;

import org.fbreader.common.android.FBActivity;
import org.fbreader.common.android.FBReaderUtil;
import org.fbreader.md.MDAlertDialogBuilder;
import org.fbreader.reader.*;
import org.fbreader.reader.BuildConfig;
import org.fbreader.reader.android.view.AndroidFontUtil;
import org.fbreader.reader.options.CancelMenuHelper;
import org.fbreader.util.Boolean3;
import org.fbreader.util.Pair;
import org.fbreader.util.android.DrawableUtil;
import org.fbreader.util.android.ViewUtil;

import com.github.johnpersano.supertoasts.SuperActivityToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;

import org.geometerplus.zlibrary.core.image.ZLImageProxy;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.options.*;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;

import org.geometerplus.android.fbreader.MenuData;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.api.MenuNode;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.fbreader.util.AndroidImageSynchronizer;
import org.geometerplus.android.util.DeviceType;

import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.formats.IFormatPluginCollection;

public abstract class MainActivity extends FBActivity {
	public static final int REQUEST_PREFERENCES = 1;
	public static final int REQUEST_CANCEL_MENU = 2;
	public static final int REQUEST_DICTIONARY = 3;
	public static final int REQUEST_DICTIONARY_EXTRA = 4;
	public static final int REQUEST_TOC = 5;

	public interface TOCKey {
		String TREE_FILE = "fbreader:toc:file";
		String REF = "fbreader:toc:ref";
		String PAGEMAP = "fbreader:toc:pageMap";
	}

	private final AndroidImageSynchronizer myImageSynchronizer = new AndroidImageSynchronizer(this);

	private volatile MainView myMainView;
	private volatile SuperActivityToast myToast;

	private volatile View myStrut;
	private volatile View myShadowView;

	private final HamburgerMenuAdapter myHamburgerMenuAdapter = new HamburgerMenuAdapter();
	private volatile DrawerLayout myDrawerLayout;
	private volatile ActionBarDrawerToggle myDrawerToggle;
	private volatile Toolbar myDrawerToolbar;
	private volatile Bitmap myCoverBitmap;

	private ZLResource myMenuResource = ZLResource.resource("menu");
	private volatile MenuItem mySearchItem;

	private volatile Book myCurrentBook;

	private volatile PowerManager.WakeLock myWakeLock;
	private volatile boolean myWakeLockToCreate;

	private ZLStringOption myTextSearchPatternOption =
		new ZLStringOption("TextSearch", "Pattern", "");

	private final BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			final int level = intent.getIntExtra("level", 100);
			myMainView.setBatteryLevel(level);
			switchWakeLock(
				getZLibrary().BatteryLevelToTurnScreenOffOption.getValue() < level
			);
		}
	};

	@Override
	protected int layoutId() {
		return R.layout.main;
	}

	protected final void selectMainView(int id) {
		myMainView = (MainView)findViewById(id);
		myMainView.setVisibility(View.VISIBLE);
	}

	protected final MainView getMainView() {
		return myMainView;
	}

	@Override
	protected void onCreate(Bundle saved) {
		super.onCreate(saved);

		final ListView drawerMenu = (ListView)findViewById(R.id.main_drawer_menu);
		drawerMenu.setAdapter(myHamburgerMenuAdapter);
		drawerMenu.setOnItemClickListener(myHamburgerMenuAdapter);

		myStrut = findViewById(R.id.main_statusbar_strut);
		myShadowView = findViewById(R.id.main_shadow);

		myDrawerLayout = (DrawerLayout)findViewById(R.id.main_drawer_layout);
		myDrawerToolbar = (Toolbar)findViewById(R.id.main_drawer_toolbar);
		myDrawerToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				closeDrawer();
			}
		});
		myDrawerToolbar.setNavigationContentDescription(R.string.desc_hide_book_menu);
		myDrawerToggle = new ActionBarDrawerToggle(
			this,
			myDrawerLayout,
			getToolbar(),
			R.string.desc_show_book_menu,
			R.string.desc_hide_book_menu
		);
		getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (myDrawerLayout.isDrawerOpen(GravityCompat.START)) {
					myDrawerLayout.closeDrawer(GravityCompat.START);
				} else {
					myDrawerLayout.openDrawer(GravityCompat.START);
				}
			}
		});

		myDrawerLayout.setDrawerShadow(R.drawable.shadow_right_6dp, GravityCompat.START);
	}

	protected final void closeDrawer() {
		myDrawerLayout.closeDrawer(GravityCompat.START);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			doSearch(intent.getStringExtra(SearchManager.QUERY));
		} else {
			super.onNewIntent(intent);
		}
	}

	/* ++++ MENU ++++ */
	private final class HamburgerMenuAdapter extends BaseAdapter implements ListView.OnItemClickListener {
		private final List<MenuNode> myNodes = new ArrayList<MenuNode>();

		private void rebuild(AbstractReader reader) {
			final List<MenuNode> upperSection =
				MenuData.topLevelNodes(MenuData.Location.bookMenuUpperSection);
			final List<MenuNode> lowerSection =
				MenuData.topLevelNodes(MenuData.Location.bookMenuLowerSection);
			final List<MenuNode> nodes =
				new ArrayList<MenuNode>(upperSection.size() + lowerSection.size() + 1);
			for (MenuNode node : upperSection) {
				if (reader.isActionVisible(node.Code) && reader.isActionEnabled(node.Code)) {
					nodes.add(node);
				}
			}
			if (!upperSection.isEmpty() && !lowerSection.isEmpty()) {
				nodes.add(null);
			}
			for (MenuNode node : lowerSection) {
				if (reader.isActionVisible(node.Code) && reader.isActionEnabled(node.Code)) {
					nodes.add(node);
				}
			}
			if (!nodes.equals(myNodes)) {
				myNodes.clear();
				myNodes.addAll(nodes);
				notifyDataSetChanged();
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getCount() {
			return myNodes.size();
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position) == null ? 0 : 1;
		}

		@Override
		public MenuNode getItem(int position) {
			return myNodes.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, final ViewGroup parent) {
			final MenuNode node = getItem(position);
			final int id = node != null ? R.layout.menu_item_with_icon : R.layout.menu_separator;
			final View view = convertView != null
				? convertView
				: LayoutInflater.from(parent.getContext()).inflate(id, parent, false);
			if (node != null) {
				ViewUtil.findTextView(view, R.id.menu_item_text).setText(
					myMenuResource.getResource(node.Code).getValue()
				);
				ViewUtil.findImageView(view, R.id.menu_item_icon).setImageDrawable(
					DrawableUtil.tintedDrawable(
						MainActivity.this, node.IconId, R.color.text_primary
					)
				);
			}
			return view;
		}

		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final MenuNode node = getItem(position);
			if (node != null) {
				getReader().runAction(node.Code);
				closeDrawer();
			}
		}
	};

	private final List<Pair<MenuItem,String>> myMenuItems =
		Collections.synchronizedList(new LinkedList<Pair<MenuItem,String>>());

	private void addMenuNodes(Menu menu, List<MenuNode> nodes, boolean placeOnToolbarIfPossible) {
		for (MenuNode item : nodes) {
			if (item instanceof MenuNode.Item) {
				addMenuItem(
					menu,
					item.Code,
					itemName(item.Code),
					((MenuNode.Item)item).IconId,
					placeOnToolbarIfPossible
				);
			} else /* if (item instanceof MenuNode.Submenu) */ {
				final Menu subMenu = menu.addSubMenu(itemName(item.Code));
				addMenuNodes(subMenu, ((MenuNode.Submenu)item).Children, false);
			}
		}
	}

	private String itemName(String code) {
		return myMenuResource.getResource(code).getValue();
	}

	protected final void addMenuItem(Menu menu, final String actionId, String name) {
		addMenuItem(menu, actionId, name, null, false);
	}

	private final void addMenuItem(Menu menu, final String actionId, String name, Integer iconId, boolean placeOnToolbarIfPossible) {
		final MenuItem menuItem = menu.add(Menu.NONE, actionId.hashCode(), Menu.NONE, name);
		if (iconId != null) {
			menuItem.setIcon(DrawableUtil.tintedDrawable(
				MainActivity.this, iconId, R.color.text_primary
			));
		}
		menuItem.setShowAsAction(
			iconId != null && isActionBarVisible() && placeOnToolbarIfPossible
				? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER
		);
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				getReader().runAction(actionId);
				return true;
			}
		});

		myMenuItems.add(new Pair<MenuItem,String>(menuItem, actionId));
	}

	protected final void refreshMenu() {
		final AbstractReader reader = getReader();
		if (reader == null) {
			return;
		}

		myHamburgerMenuAdapter.rebuild(reader);

		for (Pair<MenuItem,String> pair : myMenuItems) {
			final MenuItem menuItem = pair.First;
			final String actionId = pair.Second;
			menuItem.setVisible(reader.isActionVisible(actionId) && reader.isActionEnabled(actionId));
			switch (reader.isActionChecked(actionId)) {
				case TRUE:
					menuItem.setCheckable(true);
					menuItem.setChecked(true);
					break;
				case FALSE:
					menuItem.setCheckable(true);
					menuItem.setChecked(false);
					break;
				case UNDEFINED:
					menuItem.setCheckable(false);
					break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search_only, menu);
		mySearchItem = menu.findItem(R.id.menu_search_item);
		if (myOpenSearchView) {
			openSearchViewInternal();
		} else {
			mySearchItem.setVisible(false);
		}
		addMenuNodes(menu, MenuData.topLevelNodes(MenuData.Location.toolbarOrMainMenu), true);
		addMenuNodes(menu, MenuData.topLevelNodes(MenuData.Location.mainMenu), false);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		refreshMenu();
		return super.onPrepareOptionsMenu(menu);
	}

	protected abstract boolean isActionBarVisible();
	/* ---- MENU ---- */

	/* ++++ SEARCH ++++ */
	public boolean hasSearchView() {
		return mySearchItem != null;
	}

	private volatile boolean myOpenSearchView = false;

	public void openSearchView() {
		if (mySearchItem == null) {
			return;
		}
		if (!barsAreShown()) {
			showBars();
		}
		myOpenSearchView = true;
		openSearchViewInternal();
	}

	private void openSearchViewInternal() {
		mySearchItem.setVisible(true);
		final SearchView searchView = (SearchView)mySearchItem.getActionView();
		if (searchView == null) {
			return;
		}
		searchView.setIconified(false);
		searchView.setQuery(myTextSearchPatternOption.getValue(), false);
		searchView.setOnCloseListener(new SearchView.OnCloseListener() {
			@Override
			public boolean onClose() {
				hideSearchItem();
				return false;
			}
		});
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String query) {
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				query = query.trim();
				if (!"".equals(query)) {
					myTextSearchPatternOption.setValue(query);
					doSearch(query);
				}
				return false;
			}
		});
		getToolbar().getMenu().removeItem(ActionCode.SEARCH.hashCode());
	}

	public boolean hideSearchItem() {
		myOpenSearchView = false;

		final MenuItem searchItem = mySearchItem;
		if (searchItem == null || !searchItem.isVisible()) {
			return false;
		}

		searchItem.getActionView().clearFocus();
		searchItem.setVisible(false);

		invalidateOptionsMenu();

		return true;
	}

	protected abstract void doSearch(String query);

	@Override
	public boolean onSearchRequested() {
		openSearchView();
		return true;
	}
	/* ---- SEARCH ---- */

	@Override
	protected void onPostCreate(Bundle savedState) {
		super.onPostCreate(savedState);
		myDrawerToggle.syncState();

		final AbstractReader reader = getReader();
		reader.addAction(ActionCode.SHOW_NETWORK_LIBRARY, new ShowNetworkLibraryAction(this));
		reader.addAction(ActionCode.SHARE_BOOK, new ShareBookAction(this));
		reader.addAction(ActionCode.OPEN_WEB_HELP, new OpenWebHelpAction(this));
		reader.addAction(ActionCode.SHOW_WHATSNEW_DIALOG, new ShowWhatsNewDialogAction(this));
		reader.addAction(ActionCode.SHOW_CANCEL_MENU, new CancelAction(this));
		reader.addAction(ActionCode.INSTALL_PREMIUM, new ShowPremiumDialogAction(this, false));
		reader.addAction(ActionCode.OPEN_PREMIUM, new ShowPremiumDialogAction(this, true));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		myDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Config.Instance().runOnConnect(new Runnable() {
			public void run() {
				myDrawerLayout.setDrawerLockMode(
					getReader().MiscOptions.EnableBookMenuSwipeGesture.getValue()
						? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED
				);
			}
		});
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (hasFocus) {
			switchWakeLock(
				getZLibrary().BatteryLevelToTurnScreenOffOption.getValue() <
				getMainView().getBatteryLevel()
			);
			registerReceiver(
				myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
			);
		} else {
			try {
				unregisterReceiver(myBatteryInfoReceiver);
			} catch (IllegalArgumentException e) {
				// myBatteryInfoReceiver was not registered
			}
			switchWakeLock(false);
		}
	}

	@Override
	protected void onPause() {
		closeDrawer();

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		myImageSynchronizer.clear();
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			default:
				super.onActivityResult(requestCode, resultCode, data);
				break;
			case REQUEST_DICTIONARY:
			case REQUEST_DICTIONARY_EXTRA:
				DictionaryUtil.onActivityResult(this, requestCode, resultCode, data);
				break;
			case REQUEST_CANCEL_MENU:
				if (resultCode == RESULT_OK && data != null) {
					runCancelAction(data);
				}
				break;
		}
	}

	protected final void runCancelAction(Intent data) {
		final CancelMenuHelper.ActionType type;
		try {
			type = CancelMenuHelper.ActionType.valueOf(
				data.getStringExtra(FBReaderIntents.Key.TYPE)
			);
		} catch (Exception e) {
			// invalid (or null) type value
			return;
		}

		switch (type) {
			case library:
				getReader().runAction(ActionCode.SHOW_LIBRARY);
				break;
			case networkLibrary:
				getReader().runAction(ActionCode.SHOW_NETWORK_LIBRARY);
				break;
			case previousBook:
				openPreviousBook();
				break;
			case returnTo:
			{
				final Bookmark bookmark = FBReaderIntents.getBookmarkExtra(data);
				if (bookmark != null) {
					getCollection().deleteBookmark(bookmark);
					getReader().gotoBookmark(bookmark);
				}
				break;
			}
			case close:
				getReader().closeWindow();
				break;
		}
	}

	protected abstract void openPreviousBook();

	public ZLAndroidLibrary getZLibrary() {
		return FBReaderUtil.getZLibrary(this);
	}

	/* ++++++ SCREEN BRIGHTNESS ++++++ */
	protected void setScreenBrightnessAuto() {
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.screenBrightness = -1.0f;
		getWindow().setAttributes(attrs);
	}

	public void setScreenBrightnessSystem(float level) {
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.screenBrightness = level;
		getWindow().setAttributes(attrs);
	}

	public float getScreenBrightnessSystem() {
		final float level = getWindow().getAttributes().screenBrightness;
		return level >= 0 ? level : .5f;
	}
	/* ------ SCREEN BRIGHTNESS ------ */

	/* ++++++ SUPER TOAST ++++++ */
	public boolean isToastShown() {
		final SuperActivityToast toast = myToast;
		return toast != null && toast.isShowing();
	}

	public void hideToast() {
		final SuperActivityToast toast = myToast;
		if (toast != null && toast.isShowing()) {
			myToast = null;
			runOnUiThread(new Runnable() {
				public void run() {
					toast.dismiss();
				}
			});
		}
	}

	public void showToast(final SuperActivityToast toast) {
		hideToast();
		myToast = toast;
		// TODO: avoid this hack (accessing text style via option)
		final int dpi = getZLibrary().getDisplayDPI();
		final int defaultFontSize = dpi * 18 / 160;
		final int fontSize = new ZLIntegerOption("Style", "Base:fontSize", defaultFontSize).getValue();
		final int percent = getReader().MiscOptions.ToastFontSizePercent.getValue();
		final int dpFontSize = fontSize * 160 * percent / dpi / 100;
		toast.setTextSize(dpFontSize);
		toast.setButtonTextSize(dpFontSize * 7 / 8);

		final String fontFamily =
			new ZLStringOption("Style", "Base:fontFamily", "sans-serif").getValue();
		toast.setTypeface(AndroidFontUtil.systemTypeface(fontFamily, false, false));

		runOnUiThread(new Runnable() {
			public void run() {
				toast.show();
			}
		});
	}
	/* ------ SUPER TOAST ------ */

	/* ++++++ STATUS BAR & ACTION BAR ++++++ */
	protected final void setupTopBars(boolean alwaysShowStatusBar, boolean alwaysShowActionBar) {
		final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
		);
		if (alwaysShowActionBar) {
			params.addRule(RelativeLayout.BELOW, getToolbar().getId());
		} else if (alwaysShowStatusBar) {
			params.addRule(RelativeLayout.BELOW, R.id.main_statusbar_strut);
		}
		getMainView().setLayoutParams(params);
	}
	/* ------ STATUS BAR & ACTION BAR ------ */

	public abstract void hideDictionarySelection();

	public final void updateBookInfo(final Book book) {
		if (book == null) {
			return;
		}
		myCurrentBook = book;

		runOnUiThread(new Runnable() {
			public void run() {
				if (myCurrentBook != book) {
					return;
				}

				FBReaderUtil.setBookTitle(MainActivity.this, book);
			}
		});

		final ZLImage coverImage = CoverUtil.getCover(book, (IFormatPluginCollection)getApplication());
		if (coverImage instanceof ZLImageProxy) {
			final ZLImageProxy proxy = (ZLImageProxy)coverImage;
			myImageSynchronizer.synchronize(proxy, new Runnable() {
				public void run() {
					setData(book, proxy.getRealImage());
				}
			});
		} else {
			setData(book, coverImage);
		}
	}

	private void setData(final Book book, ZLImage image) {
		if (myCurrentBook != book) {
			return;
		}

		if (image != null) {
			final ZLAndroidImageManager manager =
				(ZLAndroidImageManager)ZLAndroidImageManager.Instance();
			final ZLAndroidImageData data = manager.getImageData(image);
			myCoverBitmap = data != null ? data.getBitmap(600, 800) : null;
		} else {
			myCoverBitmap = null;
		}

		runOnUiThread(new Runnable() {
			public void run() {
				final ImageView coverView = (ImageView)findViewById(R.id.main_drawer_cover);
				final Bitmap bmp = myCoverBitmap;
				if (bmp != null && getReader().MiscOptions.CoverAsMenuBackground.getValue()) {
					coverView.setVisibility(View.VISIBLE);
					coverView.setImageBitmap(bmp);
				} else {
					coverView.setVisibility(View.GONE);
				}
				if (myDrawerToolbar != null) {
					final String subtitle = book.authorsString(", ");
					if (subtitle == null || "".equals(subtitle)) {
						myDrawerToolbar.setTitleTextAppearance(MainActivity.this, R.style.FBReaderMD_TextAppearance_TitleOnly);
					} else {
						myDrawerToolbar.setTitleTextAppearance(MainActivity.this, R.style.FBReaderMD_TextAppearance_Title);
						myDrawerToolbar.setSubtitleTextAppearance(MainActivity.this, R.style.FBReaderMD_TextAppearance_Subtitle);
						myDrawerToolbar.setSubtitle(subtitle);
					}
					myDrawerToolbar.setTitle(book.getTitle());
				}
			}
		});
	}

	public void createWakeLock() {
		if (myWakeLockToCreate) {
			synchronized (this) {
				if (myWakeLockToCreate) {
					myWakeLockToCreate = false;
					myWakeLock = ((PowerManager)getSystemService(POWER_SERVICE))
						.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader");
					myWakeLock.acquire();
				}
			}
		}
	}

	private final void switchWakeLock(boolean on) {
		if (on) {
			if (myWakeLock == null) {
				myWakeLockToCreate = true;
			}
		} else {
			if (myWakeLockToCreate) {
				synchronized (this) {
					myWakeLockToCreate = false;
				}
			}
			if (myWakeLock != null) {
				synchronized (this) {
					if (myWakeLock != null) {
						myWakeLock.release();
						myWakeLock = null;
					}
				}
			}
		}
	}

	public abstract static class Action<A extends MainActivity,R extends AbstractReader> extends AbstractReader.Action<R> {
		protected final A BaseActivity;

		protected Action(A baseActivity) {
			super((R)baseActivity.getReader());
			BaseActivity = baseActivity;
		}
	}

	@Override
	protected void setTitleVisible(boolean visible) {
		super.setTitleVisible(visible);
		myShadowView.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	protected abstract AbstractReader getReader();
	public abstract BookCollectionShadow getCollection();

	public final void showBookmarkToast(final Bookmark bookmark) {
		final SuperActivityToast toast = new SuperActivityToast(this, SuperToast.Type.BUTTON);
		toast.setText(bookmark.getText());
		toast.setDuration(SuperToast.Duration.EXTRA_LONG);
		toast.setButtonIcon(
			android.R.drawable.ic_menu_edit, 0,
			ZLResource.resource("dialog").getResource("button").getResource("edit").getValue()
		);
		toast.setOnClickWrapper(new OnClickWrapper("bkmk", new SuperToast.OnClickListener() {
			@Override
			public void onClick(View view, Parcelable token) {
				final Intent intent =
					FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.EDIT_BOOKMARK);
				FBReaderIntents.putBookmarkExtra(intent, bookmark);
				startActivity(intent);
			}
		}));
		showToast(toast);
	}

	public abstract boolean barsAreShown();
	public abstract void showBars();
	public abstract void hideBars();

	public final void setOrientation(String optionValue) {
		final int orientation;
		switch (optionValue) {
			default:
				orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
				break;
			case ZLAndroidLibrary.SCREEN_ORIENTATION_SENSOR:
				orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
				break;
			case ZLAndroidLibrary.SCREEN_ORIENTATION_PORTRAIT:
				orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				break;
			case ZLAndroidLibrary.SCREEN_ORIENTATION_LANDSCAPE:
				orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				break;
			case ZLAndroidLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
				orientation = 9; // ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
				break;
			case ZLAndroidLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
				orientation = 8; // ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
				break;
		}
		setRequestedOrientation(orientation);
	}

	protected Thread.UncaughtExceptionHandler exceptionHandler() {
		return new ReaderExceptionHandler(this);
	}

	final protected ZLStringOption crashBookPathOption() {
		return new ZLStringOption("Crash", "Path", "");
	}

	void saveCrashBookPath() {
		final AbstractReader reader = getReader();
		if (reader == null) {
			return;
		}

		if (reader.getActionCount() == 0) {
			final Book book = reader.getCurrentBook();
			crashBookPathOption().setValue(book != null ? book.getPath() : "");
		}
	}

	protected final void setStatusBarVisible(boolean visible) {
		final ZLAndroidLibrary zLibrary = getZLibrary();
		final boolean shownAlways = zLibrary.showStatusBar();
		if (DeviceType.Instance() != DeviceType.KINDLE_FIRE_1ST_GENERATION && !shownAlways) {
			final int statusBarHeight;
			if (Build.VERSION.SDK_INT >= 19) {
				statusBarHeight = zLibrary.ShowActionBarOption.getValue() ? getStatusBarHeight() : 0;
			} else {
				statusBarHeight = getStatusBarHeight();
			}
			getMainView().setPreserveSize(visible, statusBarHeight);
			if (visible) {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			} else {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			}
		}

		final boolean reallyVisible = shownAlways || visible;
		myStrut.setLayoutParams(new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.MATCH_PARENT,
			reallyVisible ? getStrutHeight() : 0
		));
		myStrut.setVisibility(reallyVisible ? View.VISIBLE : View.INVISIBLE);
	}
}
