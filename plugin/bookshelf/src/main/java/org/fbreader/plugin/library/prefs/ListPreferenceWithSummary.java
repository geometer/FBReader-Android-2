package org.fbreader.plugin.library.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import org.fbreader.md.MDListPreference;
import org.fbreader.plugin.library.R;

public class ListPreferenceWithSummary extends MDListPreference {
	private final String[] myValues;
	private final String[] myNames;
	private String myCurrentValue;

	public ListPreferenceWithSummary(Context context, AttributeSet attrs) {
		super(context, attrs);

		final TypedArray a = context.obtainStyledAttributes(
			attrs, R.styleable.BksListPreference, 0, 0
		);
		final CharSequence[] names = a.getTextArray(R.styleable.BksListPreference_bks_names);
		final CharSequence[] values = a.getTextArray(R.styleable.BksListPreference_bks_values);
		System.err.println("ARR LEN = " + a.length());
		a.recycle();
		myNames = new String[names.length];
		for (int i = 0; i < names.length; ++i) {
			myNames[i] = (String)names[i];
		}
		myValues = new String[values.length];
		for (int i = 0; i < values.length; ++i) {
			myValues[i] = (String)values[i];
		}
	}

	public void setValue(String value) {
		myCurrentValue = value;
		persistString(value);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setValue(restoreValue ? getPersistedString(myCurrentValue) : (String)defaultValue);
	}

	@Override
	protected final String currentValue() {
		return myCurrentValue;
	}

	@Override
	protected final String[] values() {
		return myValues;
	}

	@Override
	protected final String[] names() {
		return myNames;
	}

	@Override
	protected void onValueSelected(int index, String value) {
		setValue(value);
	}
}
