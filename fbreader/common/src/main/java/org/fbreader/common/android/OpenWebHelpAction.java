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

package org.fbreader.common.android;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;

import org.fbreader.common.AbstractReader;

import org.geometerplus.zlibrary.core.resources.ZLResource;

class OpenWebHelpAction extends MainActivity.Action<MainActivity,AbstractReader> {
	OpenWebHelpAction(MainActivity baseActivity, AbstractReader fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	protected void run(Object ... params) {
		final String url = ZLResource.resource("links").getResource("faqPage").getValue();
		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		new Thread(new Runnable() {
			public void run() {
				BaseActivity.runOnUiThread(new Runnable() {
					public void run() {
						try {
							BaseActivity.startActivity(intent);
						} catch (ActivityNotFoundException e) {
							e.printStackTrace();
						}
					}
				});
			}
		}).start();
	}
}
