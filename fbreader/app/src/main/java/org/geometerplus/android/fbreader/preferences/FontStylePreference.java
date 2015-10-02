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

import android.content.Context;

import org.geometerplus.zlibrary.core.options.ZLBooleanOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;

class FontStylePreference extends SingleChoicePreference {
	private static final String[] ourKeys = { "regular", "bold", "italic", "boldItalic" };

	private final ZLBooleanOption myBoldOption;
	private final ZLBooleanOption myItalicOption;

	FontStylePreference(Context context, ZLResource resource, ZLBooleanOption boldOption, ZLBooleanOption italicOption) {
		super(context, resource);
		myBoldOption = boldOption;
		myItalicOption = italicOption;
		setList(ourKeys);
	}

	@Override
	protected String currentValue() {
		final int intValue =
			(myBoldOption.getValue() ? 1 : 0) |
			(myItalicOption.getValue() ? 2 : 0);
		return ourKeys[intValue];
	}

	@Override
	protected void onValueSelected(int index, String value) {
		myBoldOption.setValue((index & 0x1) == 0x1);
		myItalicOption.setValue((index & 0x2) == 0x2);
	}
}
