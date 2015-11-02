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

import android.content.Context;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.LayoutInflater;

public class MDAlertDialogBuilder extends AlertDialog.Builder {
	private Toolbar myToolbar;

	public MDAlertDialogBuilder(Context context) {
		super(context);
	}

	@Override
	public MDAlertDialogBuilder setTitle(CharSequence title) {
		if (title != null) {
			createToolbar();
			myToolbar.setTitle(title);
		}
		return this;
	}

	@Override
	public MDAlertDialogBuilder setTitle(int titleId) {
		createToolbar();
		myToolbar.setTitle(titleId);
		return this;
	}

	private int getMinimumWidth(View view) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			return view.getMinimumWidth();
		} else {
			return 0;
		}
	}

	@Override
	public AlertDialog create() {
		final AlertDialog dialog = super.create();
		if (myToolbar != null) {
			myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					dialog.cancel();
				}
			});
			myToolbar.setNavigationContentDescription(android.R.string.cancel);
		}
		final View view = dialog.getWindow().getDecorView();
		if (view != null && getMinimumWidth(view) == 0) {
			final DisplayMetrics dm = dialog.getContext().getResources().getDisplayMetrics();
			final int minWidth = Math.min(
				(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, dm),
				dm.widthPixels * 9 / 10
			);
			view.setMinimumWidth(minWidth);
			if (myToolbar != null) {
				myToolbar.setMinimumWidth(minWidth);
			}
		}
		return dialog;
	}

	private void createToolbar() {
		if (myToolbar == null) {
			final LayoutInflater inflater = LayoutInflater.from(getContext());
			myToolbar = (Toolbar)inflater.inflate(R.layout.md_toolbar, null);
			setCustomTitle(myToolbar);
			myToolbar.setTitleTextAppearance(getContext(), R.style.FBReaderMD_TextAppearance_TitleOnly);
		}
	}
}
