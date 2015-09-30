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

public abstract class MDListPreference extends MDBaseDialogPreference {
	protected MDListPreference(Context context) {
		super(context);
	}

	@Override
	void configureDialog(MDAlertDialogBuilder builder) {
		final String[] values = this.values();
		final String[] names = this.names();
		final String currentValue = this.currentValue();

		if (values == null || names == null || values.length != names.length) {
			throw new IllegalStateException("Values = " + values + "; names = " + names);
		}

		int index = 0;
		if (currentValue != null) {
			for (int i = 0; i < values.length; ++i) {
				if (currentValue.equals(values[i])) {
					index = i;
					break;
				}
			}
		}
		builder.setSingleChoiceItems(names, index, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				onValueSelected(which, values[which]);
				dialog.dismiss();
				notifyChanged();
			}
		});
	}

	@Override
	public CharSequence getSummary() {
		final String currentValue = this.currentValue();
		if (currentValue != null) {
			final String[] values = this.values();
			for (int i = 0; i < values.length; ++i) {
				if (currentValue.equals(values[i])) {
					return names()[i];
				}
			}
		}
		return names()[0];
	}

	protected abstract String currentValue();
	protected abstract String[] values();
	protected abstract String[] names();
	protected abstract void onValueSelected(int index, String value);
}
