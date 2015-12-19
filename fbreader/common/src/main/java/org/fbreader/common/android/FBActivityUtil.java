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

import java.util.Locale;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.Intent;

import org.fbreader.md.MDActivity;

import org.geometerplus.zlibrary.core.language.Language;

abstract class FBActivityUtil {
	static final String ORIENTATION_KEY = "fbreader.orientation";
	static final String LANGUAGE_KEY = "fbreader.language";

	static Intent updatedIntent(Intent intent, MDActivity activity) {
		return intent
			.putExtra(LANGUAGE_KEY, Language.uiLanguageOption().getValue())
			.putExtra(ORIENTATION_KEY, activity.getRequestedOrientation());
	}

	static void updateLocale(MDActivity activity) {
		final Locale locale = Language.uiLocale(null);
		if (locale != null) {
			final Resources res = activity.getBaseContext().getResources();
			final Configuration config = new Configuration();
			config.locale = locale;
			res.updateConfiguration(config, res.getDisplayMetrics());
		}
	}

	static void applyParameters(MDActivity activity, Intent intent) {
		if (intent == null) {
			return;
		}
		final String language = intent.getStringExtra(LANGUAGE_KEY);
		if (language != null) {
			Language.uiLanguageOption().setValue(language);
			intent.removeExtra(LANGUAGE_KEY);
		}
		final int orientation = intent.getIntExtra(ORIENTATION_KEY, Integer.MIN_VALUE);
		if (orientation != Integer.MIN_VALUE) {
			activity.setRequestedOrientation(orientation);
			intent.removeExtra(ORIENTATION_KEY);
		}
	}
}
