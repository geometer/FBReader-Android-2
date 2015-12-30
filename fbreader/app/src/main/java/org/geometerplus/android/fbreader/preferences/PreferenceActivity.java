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

package org.geometerplus.android.fbreader.preferences;

import android.content.Intent;
import android.os.Bundle;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.ui.android.network.SQLiteCookieDatabase;

import org.geometerplus.android.fbreader.network.auth.ActivityNetworkContext;
import org.geometerplus.android.fbreader.preferences.background.BackgroundPreference;
import org.geometerplus.android.fbreader.preferences.fileChooser.FileChooserCollection;

import org.fbreader.common.android.FBSettingsActivity;

public class PreferenceActivity extends FBSettingsActivity {
	public static final String SCREEN_KEY = "screen";
	static final int BACKGROUND_REQUEST_CODE = 3000;

	static final ZLResource Resource = ZLResource.resource("Preferences");

	final ActivityNetworkContext NetworkContext = new ActivityNetworkContext(this);
	final FileChooserCollection ChooserCollection = new FileChooserCollection(this, 2000);
	volatile BackgroundPreference BackgroundPreference;

	@Override
	protected void onResume() {
		super.onResume();
		NetworkContext.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (NetworkContext.onActivityResult(requestCode, resultCode, data)) {
			return;
		}

		if (resultCode != RESULT_OK) {
			return;
		}

		switch (requestCode) {
			default:
				ChooserCollection.update(requestCode, data);
				break;
			case BACKGROUND_REQUEST_CODE:
				if (BackgroundPreference != null) {
					BackgroundPreference.update(data);
				}
				break;
		}
	}

	@Override
	protected android.preference.PreferenceFragment preferenceFragment() {
		return new PreferenceFragment();
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setTitle(Resource.getValue());
		SQLiteCookieDatabase.init(this);
	}
}
