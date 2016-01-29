package org.fbreader.plugin.library.prefs;

import android.app.Fragment;
import android.os.Bundle;
import android.widget.LinearLayout;

import org.fbreader.plugin.library.FullActivity;
import org.fbreader.plugin.library.R;

import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public class SettingsActivity extends FullActivity {
	final BookCollectionShadow Collection = new BookCollectionShadow();
	private volatile SettingsFragment myFragment;

	@Override
	protected int layoutId() {
		return R.layout.bks_settings;
	}

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		findViewById(R.id.bks_statusbar_strut).setLayoutParams(new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, getStatusBarHeight()
		));

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

	void enableShelvesPreferences() {
		final SettingsFragment fragment = myFragment;
		if (fragment != null) {
			fragment.enableShelvesPreferences();
		}
	}
}
