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
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

public class VNTNumberPickerPreference extends DialogPreference {
	private int mySelectedValue;
	private int myMinValue = 0;
	private int myMaxValue = 100;
	private View myCentralView;

	public VNTNumberPickerPreference(Context context) {
		super(context, null);
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupNumberPicker() {
		final NumberPicker picker = (NumberPicker)myCentralView;
		picker.setMinValue(myMinValue);
		picker.setMaxValue(myMaxValue);
		picker.setValue(mySelectedValue);
		picker.setWrapSelectorWheel(false);
	}

	private void setupSimpleEditor() {
		final EditText text = (EditText)myCentralView;
		text.setText(String.valueOf(mySelectedValue));
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);

		final View layout = ((Activity)getContext()).getLayoutInflater().inflate(
			R.layout.picker_preference, null
		);
		myCentralView = layout.findViewById(R.id.picker_preference_central);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setupNumberPicker();
		} else {
			setupSimpleEditor();
		}
		builder.setTitle(getTitle());
		builder.setView(layout);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private int getValueHoneycomb() {
		return ((NumberPicker)myCentralView).getValue();
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult && shouldPersist()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				mySelectedValue = getValueHoneycomb();
			} else {
				try {
					final String text = ((EditText)myCentralView).getText().toString();
					mySelectedValue =
						Math.min(myMaxValue, Math.max(myMinValue, Integer.valueOf(text)));
				} catch (Throwable t) {
					// ignore
				}
			}
			persistInt(mySelectedValue);
			updateSummary();
		}
	}

	private void updateSummary() {
		setSummary(String.valueOf(mySelectedValue));
	}
}
