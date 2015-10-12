package org.fbreader.plugin.library.prefs;

import android.content.Context;
import android.util.AttributeSet;

public class ColorSchemePreference extends ListPreferenceWithSummary {
	public ColorSchemePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			((SettingsActivity)getContext()).applyTheme();
		}
	}
}
