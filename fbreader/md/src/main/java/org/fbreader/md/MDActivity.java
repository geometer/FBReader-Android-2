/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.fbreader.md;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;

public abstract class MDActivity extends ActionBarActivity {
	private Toolbar myToolbar;
	private View myProgressIndicator;

	protected abstract int layoutId();

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(layoutId());

		myToolbar = (Toolbar)findViewById(R.id.md_toolbar);
		setSupportActionBar(myToolbar);
		myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onBackPressed();
			}
		});

		myProgressIndicator = findViewById(R.id.md_progress_indicator);
	}

	protected final void setTitleVisibility(boolean visible) {
		if (myToolbar != null) {
			myToolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	protected final Toolbar getToolbar() {
		return myToolbar;
	}

	public final void setTitleAndSubtitle(String title, String subtitle) {
		setTitle(title);

		if (myToolbar != null) {
			final TypedValue typedValue = new TypedValue();
			getTheme().resolveAttribute(R.attr.titleTextAppearance, typedValue, true);
			myToolbar.setTitleTextAppearance(this, typedValue.resourceId);
			getTheme().resolveAttribute(R.attr.subtitleTextAppearance, typedValue, true);
			myToolbar.setSubtitleTextAppearance(this, typedValue.resourceId);

			myToolbar.setSubtitle(subtitle);
		}
	}

	protected final void showProgressIndicator(boolean show) {
		if (myProgressIndicator != null) {
			myProgressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
		} 
	}
}
