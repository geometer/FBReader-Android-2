/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.fbreader.reader.android;

import java.util.*;

import android.content.Intent;

import org.fbreader.reader.AbstractReader;
import org.fbreader.reader.options.CancelMenuHelper;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

class CancelAction extends MainActivity.Action<MainActivity,AbstractReader> {
	CancelAction(MainActivity activity) {
		super(activity);
	}

	@Override
	protected void run(Object ... params) {
		if (BaseActivity.hideSearchItem()) {
			return;
		}

		if (BaseActivity.barsAreShown()) {
			BaseActivity.hideBars();
			//return;
		}

		if (Reader.jumpBack()) {
			return;
		}

		if (Reader.hasCancelActions()) {
			final Intent intent =
				FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.CANCEL_MENU);
			final BookCollectionShadow collection = BaseActivity.getCollection();
			collection.bindToService(BaseActivity, new Runnable() {
				public void run() {
					final List<CancelMenuHelper.ActionDescription> actions =
						new CancelMenuHelper().getActionsList(collection);
					final ArrayList<HashMap<String,String>> maps =
						new ArrayList<HashMap<String,String>>(actions.size());
					for (CancelMenuHelper.ActionDescription a : actions) {
						maps.add(a.toMap());
					}
					intent.putExtra(FBReaderIntents.Key.CANCEL_ACTIONS, maps);
					BaseActivity.startActivityForResult(intent, MainActivity.REQUEST_CANCEL_MENU);
				}
			});
		} else {
			Reader.closeWindow();
		}
	}
}
