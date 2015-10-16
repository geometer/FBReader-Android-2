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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.LayoutInflater;

public class MDAlertDialogBuilder extends AlertDialog.Builder {
	private Toolbar myToolbar;
	private View.OnClickListener myNavigationListener;

	public MDAlertDialogBuilder(Context context) {
		super(context, R.style.FBReaderMD_Dialog);
	}

	@Override
	public MDAlertDialogBuilder setTitle(CharSequence title) {
		if (title != null) {
			createToolbar();
			myToolbar.setTitle(title);
		}
		return this;
	}

	public MDAlertDialogBuilder setNavigationOnClickListener(View.OnClickListener listener) {
		if (listener != null) {
			createToolbar();
			myNavigationListener = listener;
		}
		return this;
	}

	@Override
	public AlertDialog create() {
		final AlertDialog dialog = super.create();
		if (myToolbar != null) {
			if (myNavigationListener == null) {
				myNavigationListener = new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						dialog.dismiss();
					}
				};
			}
			myToolbar.setNavigationOnClickListener(myNavigationListener);
		}
		final View view = dialog.getWindow().getDecorView();
		if (view != null && view.getMinimumWidth() == 0) {
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
