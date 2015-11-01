/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

import android.app.Activity;

import org.fbreader.util.Boolean3;
import org.fbreader.common.AbstractReader;
import org.fbreader.common.android.MainActivity;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

class SetScreenOrientationAction extends MainActivity.Action<MainActivity,FBReaderApp> {
	private final String myOptionValue;

	SetScreenOrientationAction(MainActivity baseActivity, String optionValue) {
		super(baseActivity);
		myOptionValue = optionValue;
	}

	@Override
	public Boolean3 isChecked() {
		return myOptionValue.equals(BaseActivity.getZLibrary().getOrientationOption().getValue())
			? Boolean3.TRUE : Boolean3.FALSE;
	}

	@Override
	protected void run(Object ... params) {
		BaseActivity.setOrientation(myOptionValue);
		BaseActivity.getZLibrary().getOrientationOption().setValue(myOptionValue);
		Reader.onRepaintFinished();
	}
}
