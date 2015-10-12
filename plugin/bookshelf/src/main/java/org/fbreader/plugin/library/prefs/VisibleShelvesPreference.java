package org.fbreader.plugin.library.prefs;

import java.util.HashSet;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import org.fbreader.plugin.library.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class VisibleShelvesPreference extends MultiChoicePreference {
	public VisibleShelvesPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected int noneSummaryId() {
		return R.string.settings_visible_shelves_summary_none;
	}

	@Override
	protected int allSummaryId() {
		return R.string.settings_visible_shelves_summary_all;
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		super.onSetInitialValue(restoreValue, defaultValue);
		update();
	}

	Set<String> disabledValues() {
		final Set<String> enabled = getValues();
		final Set<String> disabled = new HashSet<String>();
		for (CharSequence s : getEntryValues()) {
			final String value = s.toString();
			if (!enabled.contains(value)) {
				disabled.add(value);
			}
		}
		return disabled;
	}

	@Override
	protected void update() {
		super.update();
		((SettingsActivity)getContext()).enableShelvesPreferences(disabledValues());
	}
}
