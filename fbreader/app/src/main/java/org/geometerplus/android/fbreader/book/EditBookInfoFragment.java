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

package org.geometerplus.android.fbreader.book;

import android.os.Bundle;
import android.view.*;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.fbreader.common.android.FBReaderUtil;

public final class EditBookInfoFragment extends PreferenceFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
		final View view = super.onCreateView(inflater, group, bundle);

		final EditBookInfoActivity ebia = (EditBookInfoActivity)getActivity();
		final ZLResource resource = ZLResource.resource("BookInfo");

		final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(ebia);
		setPreferenceScreen(screen);
		ebia.Screen = screen;

		FBReaderUtil.setBookTitle(ebia, ebia.Book);
		screen.addPreference(new BookTitlePreference(ebia, resource, "title", ebia.Book));
		screen.addPreference(new EditAuthorsPreference(ebia, resource, "authors"));
		screen.addPreference(new EditTagsPreference(ebia, resource, "tags"));
		screen.addPreference(new BookLanguagePreference(ebia, resource.getResource("language"), ebia.Book));
		screen.addPreference(new EncodingPreference(ebia, resource.getResource("encoding"), ebia.Book));

		return view;
	}
}
