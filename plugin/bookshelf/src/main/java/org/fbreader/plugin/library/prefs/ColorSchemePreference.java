package org.fbreader.plugin.library.prefs;

import android.content.Context;
import android.util.AttributeSet;

public class ColorSchemePreference extends ListPreferenceWithSummary {
	public ColorSchemePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onValueSelected(int index, String value) {
		final String old = currentValue();
		if (!value.equals(old)) {
			setValue(value);
			((SettingsActivity)getContext()).applyTheme();
		}
	}
}
