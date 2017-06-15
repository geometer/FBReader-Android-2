/*
 * Copyright (C) 2010-2017 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.network.auth;

import java.security.MessageDigest;
import java.util.Formatter;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.JsonRequest;
import org.geometerplus.zlibrary.ui.android.network.SQLiteCookieDatabase;

import org.geometerplus.zlibrary.ui.android.BuildConfig;
import org.geometerplus.android.fbreader.network.auth.ActivityNetworkContext;

public class TokenAuthActivity extends Activity {
	private final ActivityNetworkContext NetworkContext = new ActivityNetworkContext(this);

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		SQLiteCookieDatabase.init(this);

		final Intent intent = getIntent();
		final Uri data = intent != null ? intent.getData() : null;
		final String host = data != null ? data.getHost() : null;
		String token = data != null ? data.getPath() : null;
		if (host == null || token == null || "".equals(token)) {
			finish();
			return;
		}
		token = token.substring(1);

		final JsonRequest request;
		try {
			final SharedPreferences prefs = getSharedPreferences("fbreader.auth", 0);
			request = new JsonRequest(prefs.getString("claim-token-url", null)) {
				@Override
				public void processResponse(Object response) {
					if (response instanceof Map) {
						AndroidNetworkContext.setAccountName(host, (Map)response);
					}
				}
			};
			if (!host.equals(request.host())) {
				System.err.println("AUTH ISSUE: " + host + " != " + request.host());
				finish();
				return;
			}
			final MessageDigest hash = MessageDigest.getInstance("SHA-1");
			final String salt = prefs.getString("salt", null);
			hash.update(salt.getBytes("utf-8"));
			hash.update(BuildConfig.FBNETWORK_SECRET.getBytes("utf-8"));
			hash.update(token.getBytes("utf-8"));
			final Formatter f = new Formatter();
			for (byte b : hash.digest()) {
				f.format("%02x", b & 0xFF);
			}
			request.addPostParameter("token", f.toString());
		} catch (Throwable t) {
			t.printStackTrace();
			finish();
			return;
		}

		new AsyncTask<Void,Void,Void>() {
			@Override
			protected Void doInBackground(Void ... params) {
				try {
					NetworkContext.perform(request);
				} catch (ZLNetworkException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				finish();
			}
		}.execute();
	}
}
