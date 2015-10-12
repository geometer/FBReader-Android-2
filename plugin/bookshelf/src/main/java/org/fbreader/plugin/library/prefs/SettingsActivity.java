package org.fbreader.plugin.library.prefs;

import java.util.Set;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;

import org.fbreader.plugin.library.*;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.OrientationUtil;

public class SettingsActivity extends FullActivity {
	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private volatile SettingsFragment myFragment;

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		setContentView(R.layout.bks_settings);
		final Toolbar toolbar = (Toolbar)findViewById(R.id.md_toolbar);
		toolbar.setTitle(getTitle());
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onBackPressed();
			}
		});

		myFragment = new SettingsFragment();
		getFragmentManager()
			.beginTransaction()
			.replace(R.id.bks_settings_content, myFragment)
			.commit();
	}

	@Override
	protected void onStart() {
		super.onStart();
		OrientationUtil.setOrientation(this, getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		OrientationUtil.setOrientation(this, intent);
		super.onNewIntent(intent);
	}

	private class SettingsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedState) {
			super.onCreate(savedState);
			myCollection.bindToService(SettingsActivity.this, new Runnable() {
				@Override
				public void run() {
					addPreferencesFromResource(R.xml.settings);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						enableShelvesPreferences(disabledShelves());
						addFileFormatsPreference();
					}
				}
			});
		}

		@Override
		public void onDestroy() {
			myCollection.unbind();
			super.onDestroy();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private Set<String> disabledShelves() {
		final VisibleShelvesPreference pref =
			(VisibleShelvesPreference)myFragment.findPreference("visible_standard_shelves");
		return pref.disabledValues();
	}

	void enableShelvesPreferences(Set<String> disabledValues) {
		final PreferenceGroup shelves = (PreferenceGroup)myFragment.findPreference("shelves_group");
		if (shelves == null) {
			return;
		}

		for (int i = 0; i < shelves.getPreferenceCount(); ++i) {
			final Preference pref = shelves.getPreference(i);
			final String[] split = pref.getKey().split(":");
			if (split.length == 2) {
				pref.setEnabled(!disabledValues.contains(split[0]));
			}
		}
	}

	private void addFileFormatsPreference() {
		final PreferenceGroup library = (PreferenceGroup)myFragment.findPreference("library_group");
		library.addPreference(new FileFormatsPreference(this, myCollection));
	}
}
