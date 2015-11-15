package org.geometerplus.fbreader.plugin.base.reader;

import android.text.InputFilter;
import android.text.Spanned;

final class InputFilterMinMax implements InputFilter {
	private final int myMin;
	private final int myMax;

	public InputFilterMinMax(int min, int max) {
		myMin = Math.min(min, max);
		myMax = Math.max(min, max);
	}

	@Override
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
		try {
			final int input = Integer.parseInt(dest.toString() + source.toString());
			if (myMin <= input && input <= myMax) {
				return null;
			}
		} catch (NumberFormatException nfe) {
		}
		return "";
	}
}
