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

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import com.github.johnpersano.supertoasts.SuperActivityToast;

import org.geometerplus.zlibrary.core.image.ZLFileImageProxy;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.options.*;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;

import org.geometerplus.android.fbreader.dict.DictionaryUtil;

import org.fbreader.md.MDActivity;
import org.fbreader.common.R;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.CoverUtil;
import org.geometerplus.fbreader.formats.IFormatPluginCollection;

public abstract class FBReaderMainActivity extends MDActivity {
	public static final int REQUEST_PREFERENCES = 1;
	public static final int REQUEST_CANCEL_MENU = 2;
	public static final int REQUEST_DICTIONARY = 3;

	private volatile SuperActivityToast myToast;

	private volatile DrawerLayout myDrawerLayout;
	private volatile ActionBarDrawerToggle myDrawerToggle;
	private volatile Toolbar myDrawerToolbar;
	private volatile Bitmap myCoverBitmap;

	@Override
	protected void onCreate(Bundle saved) {
		super.onCreate(saved);
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));
	}

	protected final void setupDrawer(int menuId, int layoutId, int toolbarId) {
		final ListView drawerMenu = (ListView)findViewById(menuId);
		drawerMenu.setAdapter(new android.widget.ArrayAdapter(this, R.layout.menu_item, new String[] {
			"Book info",
			"TOC",
			"Bookmarks",
			"Share book"
		}));
		myDrawerLayout = ((DrawerLayout)findViewById(layoutId));
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

	protected final void updateBookInfo(final Book book) {
		if (book == null) {
			return;
		}

		runOnUiThread(new Runnable() {
			public void run() {
				FBReaderUtil.setBookTitle(FBReaderMainActivity.this, book);
			}
		});

		ZLImage coverImage = CoverUtil.getCover(book, (IFormatPluginCollection)getApplication());
		if (coverImage instanceof ZLFileImageProxy) {
			((ZLFileImageProxy)coverImage).synchronize();
			coverImage = ((ZLFileImageProxy)coverImage).getRealImage();
		}
		if (coverImage != null) {
			final ZLAndroidImageManager manager =
				(ZLAndroidImageManager)ZLAndroidImageManager.Instance();
			final ZLAndroidImageData data = manager.getImageData(coverImage);
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
