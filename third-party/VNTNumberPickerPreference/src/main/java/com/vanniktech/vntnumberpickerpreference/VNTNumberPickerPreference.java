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
import android.view.View;
import net.simonvt.numberpicker.NumberPicker;

public class VNTNumberPickerPreference extends DialogPreference {
	private int mySelectedValue;
	private int myMinValue = 0;
	private int myMaxValue = 100;
	private NumberPicker myPicker;

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

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);

		final View layout = ((Activity)getContext()).getLayoutInflater().inflate(
			R.layout.picker_preference, null
		);

		myPicker = layout.findViewById(R.id.picker_preference_picker);
		myPicker.setMinValue(myMinValue);
		myPicker.setMaxValue(myMaxValue);
		myPicker.setValue(mySelectedValue);
		myPicker.setWrapSelectorWheel(false);

		builder.setTitle(getTitle());
		builder.setView(layout);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult && shouldPersist()) {
			mySelectedValue = myPicker.getValue();
			persistInt(mySelectedValue);
			updateSummary();
		}
	}

	private void updateSummary() {
		setSummary(String.valueOf(mySelectedValue));
	}
}
