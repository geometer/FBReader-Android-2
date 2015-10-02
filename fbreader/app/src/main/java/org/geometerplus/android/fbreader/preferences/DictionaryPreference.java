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

import java.util.List;

import android.content.Context;

import org.geometerplus.zlibrary.core.options.ZLStringOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.android.fbreader.dict.DictionaryUtil;

class DictionaryPreference extends SingleChoicePreference {
	private final ZLStringOption myOption;

	DictionaryPreference(Context context, ZLResource resource, ZLStringOption dictionaryOption, List<DictionaryUtil.PackageInfo> infos) {
		super(context, resource);

		myOption = dictionaryOption;

		final String[] values = new String[infos.size()];
		final String[] texts = new String[infos.size()];
		int index = 0;
		final ZLResource titlesResource = ZLResource.resource("dictionary");
		for (DictionaryUtil.PackageInfo i : infos) {
			values[index] = i.getId();
			final String title = i.getTitle();
			final ZLResource tr = titlesResource.getResource(title);
			if (tr.hasValue()) {
				texts[index] = tr.getValue();
			} else {
				texts[index] = title;
			}
			++index;
		}
		setLists(values, texts);
	}

	@Override
	protected String currentValue() {
		return myOption.getValue();
	}

	@Override
	protected void onValueSelected(int index, String value) {
		myOption.setValue(value);
	}
}
