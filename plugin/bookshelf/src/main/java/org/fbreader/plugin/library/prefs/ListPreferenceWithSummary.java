package org.fbreader.plugin.library.prefs;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ListPreferenceWithSummary extends ListPreference {
	public ListPreferenceWithSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public CharSequence getSummary() {
		return getEntry();
	}
}
