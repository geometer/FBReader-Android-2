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

import java.util.*;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.*;
import android.widget.*;

import org.fbreader.util.Boolean3;

import com.github.johnpersano.supertoasts.SuperActivityToast;

import org.geometerplus.zlibrary.core.image.ZLImageProxy;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.options.*;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;

import org.geometerplus.android.fbreader.api.MenuNode;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.util.AndroidImageSynchronizer;
import org.geometerplus.android.util.ViewUtil;

import org.fbreader.md.MDActivity;
import org.fbreader.md.MDAlertDialogBuilder;
import org.fbreader.common.R;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.CoverUtil;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.formats.IFormatPluginCollection;

public abstract class FBReaderMainActivity extends MDActivity {
	public static final int REQUEST_PREFERENCES = 1;
	public static final int REQUEST_CANCEL_MENU = 2;
	public static final int REQUEST_DICTIONARY = 3;

	private final AndroidImageSynchronizer myImageSynchronizer = new AndroidImageSynchronizer(this);

	private volatile SuperActivityToast myToast;

	private volatile DrawerLayout myDrawerLayout;
	private volatile ActionBarDrawerToggle myDrawerToggle;
	private volatile Toolbar myDrawerToolbar;
	private volatile Bitmap myCoverBitmap;

	private volatile Book myCurrentBook;

	@Override
	protected void onCreate(Bundle saved) {
		super.onCreate(saved);
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));
	}

	private String[] ACTION_IDS = {
		ActionCode.SHOW_BOOK_INFO,
		ActionCode.SHOW_TOC,
		ActionCode.SHOW_BOOKMARKS,
		ActionCode.SHARE_BOOK,
		ActionCode.GOTO_PAGE_NUMBER,
		null,
		ActionCode.SHOW_LIBRARY,
		ActionCode.SHOW_NETWORK_LIBRARY
	};

	private ZLResource myMenuResource = ZLResource.resource("menu");

	private final class HamburgerMenuAdapter extends BaseAdapter implements ListView.OnItemClickListener {
		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getCount() {
			return ACTION_IDS.length;
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position) == null ? 0 : 1;
		}

		@Override
		public String getItem(int position) {
			return ACTION_IDS[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, final ViewGroup parent) {
			final String code = getItem(position);
			final int id = code != null ? R.layout.menu_item_with_icon : R.layout.menu_separator;
			final View view = convertView != null
				? convertView
				: LayoutInflater.from(parent.getContext()).inflate(id, parent, false);
			if (code != null) {
				ViewUtil.findTextView(view, R.id.menu_item_text).setText(
					myMenuResource.getResource(code).getValue()
				);
				final Drawable icon = DrawableCompat.wrap(
					getResources().getDrawable(MenuData.iconId(code))
				);
				DrawableCompat.setTint(icon, getResources().getColor(R.color.text_primary));
				ViewUtil.findImageView(view, R.id.menu_item_icon).setImageDrawable(icon);
			}
			return view;
		}

		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			runMenuAction(getItem(position));
			myDrawerLayout.closeDrawer(GravityCompat.START);
		}
	};

	/* ++++ MENU ++++ */
	private final List<Pair<MenuItem,String>> myMenuItems =
		Collections.synchronizedList(new LinkedList<Pair<MenuItem,String>>());

	private void fillMenu(Menu menu, List<MenuNode> nodes) {
		for (MenuNode item : nodes) {
			if (item instanceof MenuNode.Item) {
				addMenuItem(menu, item.Code, itemName(item.Code), ((MenuNode.Item)item).IconId);
			} else /* if (item instanceof MenuNode.Submenu) */ {
				final Menu subMenu = menu.addSubMenu(itemName(item.Code));
				fillMenu(subMenu, ((MenuNode.Submenu)item).Children);
			}
		}
	}

	private String itemName(String code) {
		return myMenuResource.getResource(code).getValue();
	}

	protected final void addMenuItem(Menu menu, final String actionId, String name, Integer iconId) {
		final MenuItem menuItem = menu.add(name);
		if (iconId != null) {
			final Drawable icon = DrawableCompat.wrap(getResources().getDrawable(iconId));
			DrawableCompat.setTint(icon, getResources().getColor(R.color.text_primary));
			menuItem.setIcon(icon);
		}
		menuItem.setShowAsAction(
			iconId != null && isActionBarVisible()
				? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER
		);
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				runMenuAction(actionId);
				return true;
			}
		});

		myMenuItems.add(new Pair<MenuItem,String>(menuItem, actionId));
	}

	protected final void refreshMenu() {
		for (Pair<MenuItem,String> pair : myMenuItems) {
			final MenuItem menuItem = pair.first;
			final String actionId = pair.second;
			menuItem.setVisible(isMenuActionVisible(actionId) && isMenuActionEnabled(actionId));
			switch (isMenuActionChecked(actionId)) {
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
		fillMenu(menu, MenuData.topLevelNodes());
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		refreshMenu();
		return super.onPrepareOptionsMenu(menu);
	}

	protected abstract void runMenuAction(String code);
	protected abstract boolean isMenuActionVisible(String code);
	protected abstract boolean isMenuActionEnabled(String code);
	protected abstract Boolean3 isMenuActionChecked(String code);
	protected abstract boolean isActionBarVisible();
	/* ---- MENU ---- */

	protected final void setupDrawer(int menuId, int layoutId, int toolbarId) {
		final ListView drawerMenu = (ListView)findViewById(menuId);
		final HamburgerMenuAdapter adapter = new HamburgerMenuAdapter();
		drawerMenu.setAdapter(adapter);
		drawerMenu.setOnItemClickListener(adapter);

		myDrawerLayout = (DrawerLayout)findViewById(layoutId);
		myDrawerToolbar = (Toolbar)findViewById(toolbarId);
		myDrawerToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				myDrawerLayout.closeDrawer(GravityCompat.START);
			}
		});
		myDrawerToggle = new ActionBarDrawerToggle(
			this, myDrawerLayout, getToolbar(), R.string.empty_string, R.string.empty_string
		);
		myDrawerLayout.setDrawerListener(myDrawerToggle);
		myDrawerLayout.setDrawerShadow(R.drawable.shadow_right_6dp, GravityCompat.START);
	}

	@Override
	protected void onPostCreate(Bundle savedState) {
		super.onPostCreate(savedState);
		myDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		myDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
		myDrawerLayout.closeDrawer(GravityCompat.START);
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
				DictionaryUtil.onActivityResult(this, resultCode, data);
				break;
		}
	}

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
		final int percent = new ZLIntegerRangeOption("Options", "ToastFontSizePercent", 25, 100, 90).getValue();
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

	/* ++++++ TOOLBAR ++++++ */
	protected final void setupToolbar(View mainView, boolean visibleAlways) {
		if (visibleAlways) {
			final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
			);
			params.addRule(RelativeLayout.BELOW, getToolbar().getId());
			mainView.setLayoutParams(params);
		}
	}
	/* ------ TOOLBAR ------ */

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

				FBReaderUtil.setBookTitle(FBReaderMainActivity.this, book);
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
				if (bmp != null) {
					coverView.setVisibility(View.VISIBLE);
					coverView.setImageBitmap(bmp);
				} else {
					coverView.setVisibility(View.GONE);
				}
				if (myDrawerToolbar != null) {
					myDrawerToolbar.setTitleTextAppearance(FBReaderMainActivity.this, R.style.FBReaderMD_TextAppearance_Title);
					myDrawerToolbar.setSubtitleTextAppearance(FBReaderMainActivity.this, R.style.FBReaderMD_TextAppearance_Subtitle);
					myDrawerToolbar.setTitle(book.getTitle());
					myDrawerToolbar.setSubtitle(book.authorsString(", "));
				}
			}
		});
	}
}
