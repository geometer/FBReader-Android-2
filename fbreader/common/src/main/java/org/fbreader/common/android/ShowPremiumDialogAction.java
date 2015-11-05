/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.fbreader.common.android;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.android.util.PackageUtil;

import org.fbreader.common.AbstractReader;

class ShowPremiumDialogAction extends MainActivity.Action<MainActivity,AbstractReader> {
	private final boolean myInstalled;

	ShowPremiumDialogAction(MainActivity baseActivity, boolean installed) {
		super(baseActivity);
		myInstalled = installed;
	}

	@Override
	public boolean isVisible() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return false;
		}
		return PackageUtil.canBeStarted(BaseActivity, FBReaderUtil.premiumIntent(), false) == myInstalled;
	}

	@Override
	protected void run(Object ... params) {
		if (myInstalled) {
			try {
				BaseActivity.startActivity(FBReaderUtil.premiumIntent());
				BaseActivity.finish();
			} catch (ActivityNotFoundException e) {
				// TODO: show toast
			}
		} else {
			try {
				BaseActivity.startActivity(new Intent(
					Intent.ACTION_VIEW, Uri.parse("market://details?id=com.fbreader")
				));
			} catch (ActivityNotFoundException e) {
				// TODO: show toast
			}
		}
	}
}
