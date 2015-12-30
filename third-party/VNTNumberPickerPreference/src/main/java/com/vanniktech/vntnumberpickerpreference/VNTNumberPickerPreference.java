/*
 * Copyright (C) 2014-2015 Vanniktech - Niklas Baudy <http://vanniktech.de/Imprint>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vanniktech.vntnumberpickerpreference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import net.simonvt.numberpicker.NumberPicker;

import org.fbreader.md.MDDialogPreference;

public class VNTNumberPickerPreference extends MDDialogPreference {
	private int mySelectedValue;
	private int myMinValue = 0;
	private int myMaxValue = 100;
	private NumberPicker myPicker;

	public VNTNumberPickerPreference(Context context) {
		super(context);
	}

	public VNTNumberPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		final TypedArray array =
			context.obtainStyledAttributes(attrs, R.styleable.VNTNumberPickerPreference);
		setRange(
			array.getInt(R.styleable.VNTNumberPickerPreference_minValue, myMinValue),
			array.getInt(R.styleable.VNTNumberPickerPreference_maxValue, myMaxValue)
		);
		array.recycle();
	}

	public void setRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("Invalid range parameters: " + min + " >= " + max);
		}
		myMinValue = min;
		myMaxValue = max;
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		mySelectedValue = restoreValue ? getPersistedInt(0) : (Integer)defaultValue;
		updateSummary();
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, 0);
	}

	@Override
	protected String positiveButtonText() {
		return getContext().getResources().getString(android.R.string.ok);
	}

	@Override
	protected int dialogLayoutId() {
		return R.layout.vnt_picker_preference;
	}

	@Override
	protected void onBindDialogView(View view) {
		myPicker = (NumberPicker)view.findViewById(R.id.vnt_picker_preference_picker);
		myPicker.setMinValue(myMinValue);
		myPicker.setMaxValue(myMaxValue);
		myPicker.setValue(mySelectedValue);
		myPicker.setWrapSelectorWheel(false);
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
	protected void onPositiveDialogResult() {
		if (shouldPersist()) {
			mySelectedValue = myPicker.getValue();
			persistInt(mySelectedValue);
			updateSummary();
		}
	}

	private void updateSummary() {
		setSummary(String.valueOf(mySelectedValue));
	}
}
