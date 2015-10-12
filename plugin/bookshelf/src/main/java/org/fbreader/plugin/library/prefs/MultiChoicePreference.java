package org.fbreader.plugin.library.prefs;

import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
abstract class MultiChoicePreference extends MultiSelectListPreference {
	public MultiChoicePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MultiChoicePreference(Context context) {
		super(context);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			update();
		}
	}

	protected abstract int noneSummaryId();
	protected abstract int allSummaryId();

	protected void update() {
		final Set<String> values = getValues();

		if (values.isEmpty()) {
			setSummary(noneSummaryId());
		} else {
			final CharSequence[] allValues = getEntryValues();
			if (values.size() == allValues.length) {
				setSummary(allSummaryId());
			} else {
				final CharSequence[] readableNames = getEntries();
				final StringBuilder list = new StringBuilder();
				for (int i = 0; i < allValues.length; ++i) {
					if (values.contains(allValues[i].toString())) {
						if (list.length() > 0) {
							list.append(", ");
						}
						list.append(readableNames[i]);
					}
				}
				setSummary(list.toString());
			}
		}
	}
}
