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

import android.content.Intent;
import android.os.Bundle;

import org.fbreader.md.MDActivity;

public abstract class FBActivity extends MDActivity {
	@Override
	protected void onPreCreate() {
		FBActivityUtil.applyParameters(this, getIntent());
		super.onPreCreate();
	}

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		FBActivityUtil.setExceptionHandler(this);
	}

	@Override
	protected void onStart() {
		FBActivityUtil.applyParameters(this, getIntent());
		super.onStart();
	}

	@Override
	protected void onResume() {
		FBActivityUtil.setExceptionHandler(this);
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
}
