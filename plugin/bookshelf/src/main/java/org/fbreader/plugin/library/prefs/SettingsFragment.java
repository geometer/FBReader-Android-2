package org.fbreader.plugin.library.prefs;

import java.util.Set;

import android.os.Bundle;
import android.preference.*;

import org.fbreader.plugin.library.R;

public class SettingsFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		final SettingsActivity activity = (SettingsActivity)getActivity();
		if (activity == null) {
			return;
		}
		activity.Collection.bindToService(activity, new Runnable() {
			@Override
			public void run() {
				addPreferencesFromResource(R.xml.settings);

				enableShelvesPreferences();
				final PreferenceGroup library = (PreferenceGroup)findPreference("library_group");
				library.addPreference(new FileFormatsPreference(activity, activity.Collection));
			}
		});
	}

	void enableShelvesPreferences() {
		final PreferenceGroup shelves = (PreferenceGroup)findPreference("shelves_group");
		final VisibleShelvesPreference visibleShelves =
			(VisibleShelvesPreference)findPreference("visible_standard_shelves");
		if (shelves == null || visibleShelves == null) {
			return;
		}
		final Set<String> disabledValues = visibleShelves.disabledValues();

		for (int i = 0; i < shelves.getPreferenceCount(); ++i) {
			final Preference pref = shelves.getPreference(i);
			final String[] split = pref.getKey().split(":");
			if (split.length == 2) {
				pref.setEnabled(!disabledValues.contains(split[0]));
			}
		}
	}
}
