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

package org.geometerplus.android.fbreader.bookmark;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.fbreader.common.android.FBActivity;

import org.geometerplus.zlibrary.core.options.ZLIntegerOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.book.Bookmark;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public class EditBookmarkActivity extends FBActivity {
	private TabLayout myTabLayout;
	private ViewPager myViewPager;

	static final ZLResource Resource = ZLResource.resource("editBookmark");

	final BookCollectionShadow Collection = new BookCollectionShadow();
	volatile Bookmark Bookmark;

	@Override
	protected int layoutId() {
		return R.layout.edit_bookmark;
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		setTitle("");

		Bookmark = FBReaderIntents.getBookmarkExtra(getIntent());
		if (Bookmark == null) {
			finish();
			return;
		}

        myTabLayout = (TabLayout)findViewById(R.id.edit_bookmark_tab_layout);
        myViewPager = (ViewPager)findViewById(R.id.edit_bookmark_view_pager);

		final PagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
			@Override
			public int getCount() {
				return 3;
			}

			@Override
			public Fragment getItem(int position) {
				switch (position) {
					default:
					case 0:
						return new EditTextFragment();
					case 1:
						return new SelectStyleFragment();
					case 2:
						return new DeleteBookmarkFragment();
				}
			}

			@Override
			public CharSequence getPageTitle(int position) {
				final String key;
				switch (position) {
					default:
					case 0:
						key = "text";
						break;
					case 1:
						key = "style";
						break;
					case 2:
						key = "delete";
						break;
				}
				return Resource.getResource(key).getValue();
			}
		};
		myViewPager.setAdapter(adapter);
		myTabLayout.setupWithViewPager(myViewPager);

		setWindowSize();

		final ZLIntegerOption currentTabOption =
			new ZLIntegerOption("LookNFeel", "EditBookmarkTab", 0);
		myViewPager.setCurrentItem(currentTabOption.getValue());
		myTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				myViewPager.setCurrentItem(tab.getPosition(), false);
				if (tab.getPosition() != 2 /* delete tab */) {
					currentTabOption.setValue(tab.getPosition());
				}
			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
			}
		});
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setWindowSize();
	}

	private void setWindowSize() {
		final DisplayMetrics dm = getResources().getDisplayMetrics();
		final int width = Math.min(
			(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, dm),
			dm.widthPixels * 9 / 10
		);
		final int height = Math.min(
			(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350, dm),
			dm.heightPixels * 9 / 10
		);

        final LinearLayout root = (LinearLayout)findViewById(R.id.edit_bookmark);
		root.setLayoutParams(new FrameLayout.LayoutParams(
			new ViewGroup.LayoutParams(width, height)
		));
	}

	@Override
	protected void onDestroy() {
		Collection.unbind();
		super.onDestroy();
	}
}
