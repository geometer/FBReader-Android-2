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

import org.fbreader.md.MDSettingsActivity;

public abstract class FBSettingsActivity extends MDSettingsActivity {
	@Override
	protected void onPreCreate() {
		applyParameters(getIntent());
		super.onPreCreate();
	}

	@Override
	protected void onStart() {
		applyParameters(getIntent());
		super.onStart();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		applyParameters(intent);
		super.onNewIntent(intent);
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(
			intent
				.putExtra(FBActivity.ORIENTATION_KEY, getRequestedOrientation())
		);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		super.startActivityForResult(
			intent
				.putExtra(FBActivity.ORIENTATION_KEY, getRequestedOrientation()),
			requestCode
		);
	}

	protected final void applyParameters(Intent intent) {
		if (intent == null) {
			return;
		}
		final int orientation = intent.getIntExtra(FBActivity.ORIENTATION_KEY, Integer.MIN_VALUE);
		if (orientation != Integer.MIN_VALUE) {
			setRequestedOrientation(orientation);
		}
	}
}
