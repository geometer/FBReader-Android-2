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

import java.util.ArrayList;

import android.content.Context;

import org.fbreader.reader.android.view.AndroidFontUtil;

import org.geometerplus.zlibrary.core.options.ZLStringOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;

class FontPreference extends SingleChoicePreference implements ReloadablePreference {
	private final ZLStringOption myOption;
	private final boolean myIncludeDummyValue;

	private static String UNCHANGED = "inherit";

	FontPreference(Context context, ZLResource resource, ZLStringOption option, boolean includeDummyValue) {
		super(context, resource);

		myOption = option;
		myIncludeDummyValue = includeDummyValue;

		reload();
	}

	public void reload() {
		final ArrayList<String> fonts = new ArrayList<String>();
		AndroidFontUtil.fillFamiliesList(fonts);
		if (myIncludeDummyValue) {
			fonts.add(0, UNCHANGED);
		}
		setList((String[])fonts.toArray(new String[fonts.size()]));
	}

	@Override
	protected String currentValue() {
		final String[] fonts = values();

		final String optionValue = myOption.getValue();
		final String fntValue = optionValue.length() > 0 ?
			AndroidFontUtil.realFontFamilyName(optionValue) : UNCHANGED;

		for (String fontName : fonts) {
			if (fntValue.equals(fontName)) {
				return fontName;
			}
		}
		for (String fontName : fonts) {
			if (fntValue.equals(AndroidFontUtil.realFontFamilyName(fontName))) {
				return fontName;
			}
		}
		return fntValue;
	}

	@Override
	protected void onValueSelected(int index, String value) {
		myOption.setValue(UNCHANGED.equals(value) ? "" : value);
	}
}
