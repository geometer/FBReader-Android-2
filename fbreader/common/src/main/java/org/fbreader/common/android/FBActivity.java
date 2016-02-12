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

package org.fbreader.common.android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.fbreader.md.MDActivity;

public abstract class FBActivity extends MDActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
	public static final int REQUEST_STORAGE_PERMISSION = 201;

	@Override
	protected void onPreCreate() {
		FBActivityUtil.applyParameters(this, getIntent());
		super.onPreCreate();
	}

	@Override
	protected void onStart() {
		FBActivityUtil.applyParameters(this, getIntent());
		super.onStart();
	}

	@Override
	protected void onResume() {
		FBActivityUtil.updateLocale(this);
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		FBActivityUtil.applyParameters(this, intent);
		super.onNewIntent(intent);
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(FBActivityUtil.updatedIntent(intent, this));
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		super.startActivityForResult(FBActivityUtil.updatedIntent(intent, this), requestCode);
	}

	@Override
	protected Thread.UncaughtExceptionHandler exceptionHandler() {
		return new UncaughtExceptionHandler(this);
	}

	protected final int getStatusBarHeight() {
		final Resources res = getResources();
		int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
		return resourceId > 0 ? res.getDimensionPixelSize(resourceId) : 0;
	}

	protected final int getStrutHeight() {
		return Build.VERSION.SDK_INT >= 19 ? getStatusBarHeight() : 0;
	}

	private final String[] STORAGE_PERMISSIONS = new String[] {
		Manifest.permission.READ_EXTERNAL_STORAGE,
		Manifest.permission.WRITE_EXTERNAL_STORAGE
	};
	private volatile int myPermissionsRequestCounter = 0;

	public final boolean checkStoragePermission() {
		boolean hasPermissions = true;
		for (String sp : STORAGE_PERMISSIONS) {
			hasPermissions &= ContextCompat.checkSelfPermission(this, sp)
				== PackageManager.PERMISSION_GRANTED;
		}
		if (!hasPermissions) {
			if (myPermissionsRequestCounter++ >= 10) {
				return false;
			}

			ActivityCompat.requestPermissions(
				this, STORAGE_PERMISSIONS, REQUEST_STORAGE_PERMISSION
			);
		}
		return hasPermissions;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
		if (requestCode != REQUEST_STORAGE_PERMISSION) {
			return;
		}
		if (permissions == null || results == null || permissions.length != results.length) {
			return;
		}

		int count = 0;
		for (int i = 0; i < permissions.length; ++i) {
			if (results[i] == PackageManager.PERMISSION_GRANTED) {
				for (String sp : STORAGE_PERMISSIONS) {
					if (sp.equals(permissions[i])) {
						++count;
						break;
					}
				}
			}
		}
		onStoragePermissionGranted(count == STORAGE_PERMISSIONS.length);
	}

	protected void onStoragePermissionGranted(boolean granted) {
	}
}
