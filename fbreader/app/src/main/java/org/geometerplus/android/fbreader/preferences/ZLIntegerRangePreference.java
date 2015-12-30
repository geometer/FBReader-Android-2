/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import net.simonvt.numberpicker.NumberPicker;

import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.fbreader.md.MDDialogPreference;
import org.geometerplus.zlibrary.ui.android.R;

class ZLIntegerRangePreference extends MDDialogPreference {
	private final ZLIntegerRangeOption myOption;
	private NumberPicker myPicker;

	public ZLIntegerRangePreference(Context context, ZLResource resource, ZLIntegerRangeOption option) {
		super(context);
		myOption = option;
		setTitle(resource.getValue());
		updateSummary();
	}

	@Override
	protected int dialogLayoutId() {
		return R.layout.picker_preference;
	}

	@Override
	protected void onBindDialogView(View view) {
		myPicker = (NumberPicker)view.findViewById(R.id.picker_preference_central);
		myPicker.setMinValue(myOption.MinValue);
		myPicker.setMaxValue(myOption.MaxValue);
		myPicker.setValue(myOption.getValue());
		myPicker.setWrapSelectorWheel(false);

		super.onBindDialogView(view);
	}

	@Override
	public void onDialogShow(AlertDialog dialog) {
		myPicker.setFocusable(true);
		myPicker.setFocusableInTouchMode(true);
		myPicker.requestFocus();
		final InputMethodManager imm =
			(InputMethodManager)myPicker.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(myPicker.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	@Override
	protected String positiveButtonText() {
		return ZLResource.resource("dialog").getResource("button").getResource("ok").getValue();
	}

	@Override
	protected void onPositiveDialogResult() {
		myOption.setValue(myPicker.getValue());
		updateSummary();
	}

	private void updateSummary() {
		setSummary(String.valueOf(myOption.getValue()));
	}
}
