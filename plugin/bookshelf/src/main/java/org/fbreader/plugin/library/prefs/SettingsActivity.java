package org.fbreader.plugin.library.prefs;

import java.util.Set;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;

import org.fbreader.plugin.library.*;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public class SettingsActivity extends FullActivity {
	final BookCollectionShadow Collection = new BookCollectionShadow();
	private volatile SettingsFragment myFragment;

	@Override
	protected ActivityUtil.ActivityType type() {
		return ActivityUtil.ActivityType.Full;
	}

	@Override
	protected int layoutId() {
		return R.layout.bks_settings;
	}

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		getFragmentManager()
			.beginTransaction()
			.replace(R.id.bks_settings_content, new SettingsFragment())
			.commit();
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		myFragment = (SettingsFragment)fragment;
	}

	@Override
	public void onDestroy() {
		Collection.unbind();
		super.onDestroy();
	}

	public static class SettingsFragment extends PreferenceFragment {
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

	void enableShelvesPreferences() {
		final SettingsFragment fragment = myFragment;
		if (fragment != null) {
			fragment.enableShelvesPreferences();
		}
	}
}
