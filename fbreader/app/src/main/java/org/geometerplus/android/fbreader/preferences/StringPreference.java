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

import org.fbreader.md.MDEditTextPreference;

import org.geometerplus.zlibrary.core.options.ZLStringOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;

public final class StringPreference extends MDEditTextPreference {
	public static final Constraint CONSTRAINT_LENGTH = new Constraint(
		"-{0,1}([0-9]*\\.){0,1}[0-9]+(%|em|ex|px|pt)|",
		ZLResource.resource("hint").getResource("length").getValue()
	);
	public static final Constraint CONSTRAINT_POSITIVE_LENGTH = new Constraint(
		"([0-9]*\\.){0,1}[0-9]+(%|em|ex|px|pt)|",
		ZLResource.resource("hint").getResource("positiveLength").getValue()
	);
	public static final Constraint CONSTRAINT_PERCENT = new Constraint(
		"([1-9][0-9]{1,2}%)|",
		ZLResource.resource("hint").getResource("percent").getValue()
	);

	private final ZLStringOption myOption;

	protected StringPreference(Context context, ZLStringOption option, Constraint constraint, ZLResource rootResource, String resourceKey) {
		super(context);

		myOption = option;
		setConstraint(constraint);

		setTitle(rootResource.getResource(resourceKey).getValue());
	}

	@Override
	protected String positiveButtonText() {
		return ZLResource.resource("dialog").getResource("button").getResource("ok").getValue();
	}

	@Override
	protected String getValue() {
		return myOption.getValue();
	}

	@Override
	protected void setValue(String value) {
		myOption.setValue(value);
	}
}
