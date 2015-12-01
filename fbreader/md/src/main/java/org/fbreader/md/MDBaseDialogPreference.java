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
import android.content.DialogInterface;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;

public abstract class MDBaseDialogPreference extends Preference {
	private volatile AlertDialog myDialog;

	protected MDBaseDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected MDBaseDialogPreference(Context context) {
		super(context);
	}

	@Override
	protected void onClick() {
		final MDAlertDialogBuilder builder = new MDAlertDialogBuilder(getContext());
		builder.setTitle(getTitle());
		configureDialog(builder);

		myDialog = builder.create();
		myDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			public void onShow(DialogInterface dialog) {
				onDialogShow(myDialog);
			}
		});
		myDialog.show();
	}

	protected AlertDialog getDialog() {
		return myDialog;
	}

	protected void onDialogShow(AlertDialog dialog) {
	}

	abstract void configureDialog(MDAlertDialogBuilder builder);
}
