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

import org.fbreader.md.MDListPreference;
import org.geometerplus.zlibrary.core.resources.ZLResource;

public abstract class SingleChoicePreference extends MDListPreference {
	private volatile String[] myValues;
	private volatile String[] myNames;
	protected final ZLResource myValuesResource;

	protected SingleChoicePreference(Context context, ZLResource resource) {
		this(context, resource, resource);
	}

	protected SingleChoicePreference(Context context, ZLResource resource, ZLResource valuesResource) {
		super(context);
		setTitle(resource.getValue());
		myValuesResource = valuesResource;
	}

	protected final void setList(String[] values) {
		String[] texts = new String[values.length];
		for (int i = 0; i < values.length; ++i) {
			final ZLResource resource = myValuesResource.getResource(values[i]);
			texts[i] = resource.hasValue() ? resource.getValue() : values[i];
		}
		setLists(values, texts);
	}

	protected final void setLists(String[] values, String[] texts) {
		// It appears that setEntries() DOES NOT perform any formatting on the char sequences
		// http://developer.android.com/reference/android/preference/ListPreference.html#setEntries(java.lang.CharSequence[])
		final String[] entries = new String[texts.length];
		for (int i = 0; i < texts.length; ++i) {
			try {
				entries[i] = String.format(texts[i]);
			} catch (Exception e) {
				entries[i] = texts[i];
			}
		}

		myValues = values;
		myNames = entries;
	}

	@Override
	protected String[] values() {
		return myValues;
	}

	@Override
	protected String[] names() {
		return myNames;
	}
}
