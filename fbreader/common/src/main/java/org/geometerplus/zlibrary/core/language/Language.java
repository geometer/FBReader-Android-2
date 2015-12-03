/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.zlibrary.core.language;

import java.text.Normalizer;
import java.util.Locale;
import java.util.MissingResourceException;

import android.annotation.TargetApi;
import android.os.Build;

import org.geometerplus.zlibrary.core.options.ZLStringOption;

public class Language implements Comparable<Language> {
	public static final String ANY_CODE = "any";
	public static final String OTHER_CODE = "other";
	public static final String MULTI_CODE = "multi";
	public static final String SYSTEM_CODE = "system";

	private static enum Order {
		Before,
		Normal,
		After
	}

	private static final ZLStringOption ourUiLanguageOption =
		new ZLStringOption("LookNFeel", "Language", SYSTEM_CODE);
	public static ZLStringOption uiLanguageOption() {
		return ourUiLanguageOption;
	}
	public static Locale uiLocale() {
		final String code = uiLanguageOption().getValue();

		final String split[] = code.split("_");
		final Locale locale;
		switch (split.length) {
			case 1:
				locale = new Locale(split[0]);
				break;
			case 2:
				locale = new Locale(split[0], split[1]);
				break;
			default:
				return Locale.getDefault();
		}
		try {
			locale.getISO3Language();
			return locale;
		} catch (MissingResourceException e) {
			return Locale.getDefault();
		}
	}

	public final String Code;
	public final String Name;
	private final String mySortKey;
	private final Order myOrder;

	Language(String code, String name) {
		Code = code;
		Name = name;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			mySortKey = normalize(Name);
		} else {
			mySortKey = Name.toLowerCase();
		}
		if (SYSTEM_CODE.equals(code) || ANY_CODE.equals(code)) {
			myOrder = Order.Before;
		} else if (MULTI_CODE.equals(code) || OTHER_CODE.equals(code)) {
			myOrder = Order.After;
		} else {
			myOrder = Order.Normal;
		}
	}


	public int compareTo(Language other) {
		final int diff = myOrder.compareTo(other.myOrder);
		return diff != 0 ? diff : mySortKey.compareTo(other.mySortKey);
	}

	@Override
	public boolean equals(Object lang) {
		if (this == lang) {
			return true;
		}
		if (!(lang instanceof Language)) {
			return false;
		}
		return Code.equals(((Language)lang).Code);
	}

	@Override
	public int hashCode() {
		return Code.hashCode();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private static String normalize(String s) {
		return Normalizer.normalize(s, Normalizer.Form.NFKD);
	}
}
