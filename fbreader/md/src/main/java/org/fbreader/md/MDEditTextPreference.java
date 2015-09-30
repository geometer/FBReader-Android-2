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

import java.util.regex.Pattern;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

public abstract class MDEditTextPreference extends MDDialogPreference {
	public static class Constraint {
		private final Pattern myPattern;
		public final String Hint;

		public Constraint(String pattern, String hint) {
			myPattern = Pattern.compile(pattern);
			Hint = hint;
		}

		public boolean matches(String text) {
			return myPattern.matcher(text).matches();
		}
	}

	private EditText myEditor;
	private Constraint myConstraint;
	private final TextWatcher myWatcher = new TextWatcher() {
		@Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

        @Override
        public void beforeTextChanged(CharSequence s, int start, int before, int count) {
		}

        @Override
        public void afterTextChanged(Editable s) {
			final AlertDialog dialog = (AlertDialog)getDialog();
			final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
			okButton.setEnabled(myConstraint.matches(myEditor.getText().toString()));
        }
	};

	protected MDEditTextPreference(Context context) {
		super(context);
	}

	protected void setConstraint(Constraint constraint) {
		myConstraint = constraint;
	}

	@Override
	protected final int dialogLayoutId() {
		return R.layout.md_edit_text_preference;
	}

	@Override
	public String getSummary() {
		return getValue();
	}

	@Override
	protected final void onBindDialogView(View view) {
		myEditor = (EditText)view.findViewById(R.id.md_edit_text_preference_editor);
		myEditor.setText(getValue());

		if (myConstraint != null) {
			final TextView hintView =
				(TextView)view.findViewById(R.id.md_edit_text_preference_hint);
			hintView.setVisibility(View.VISIBLE);
			hintView.setText(myConstraint.Hint);
		}
	}

	@Override
    protected void onDialogShow(AlertDialog dialog) {
		if (myConstraint != null) {
			myEditor.removeTextChangedListener(myWatcher);
			myEditor.addTextChangedListener(myWatcher);
			myWatcher.afterTextChanged(null);
		}
    }

	@Override
	protected void onPositiveDialogResult() {
		setValue(myEditor.getText().toString());
		notifyChanged();
	}

	protected abstract String getValue();
	protected abstract void setValue(String value);
}
