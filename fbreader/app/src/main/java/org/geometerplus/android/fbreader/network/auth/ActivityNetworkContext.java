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

import java.net.URI;
import java.util.*;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.geometerplus.zlibrary.core.network.*;
import org.geometerplus.android.fbreader.network.NetworkLibraryActivity;
import org.geometerplus.android.util.OrientationUtil;

public final class ActivityNetworkContext extends AndroidNetworkContext {
	private static class AuthorizationInProgressException extends ZLNetworkAuthenticationException {
	}

	private class DelayedAction {
		public final ZLNetworkRequest Request;
		public final Runnable OnSuccess;
		public final OnError OnError;

		public DelayedAction(ZLNetworkRequest request, Runnable onSuccess, OnError onError) {
			Request = request;
			OnSuccess = onSuccess;
			OnError = onError;
		}
	}

	private final Activity myActivity;
	private volatile boolean myAuthorizationConfirmed;
	private volatile DelayedAction myDelayed;

	private volatile String myAccountName;

	public ActivityNetworkContext(Activity activity) {
		myActivity = activity;
	}

	public Context getContext() {
		return myActivity;
	}

	public synchronized void onResume() {
		final DelayedAction action = myDelayed;
		if (action == null) {
			return;
		}

		cookieStore().reset();
		myDelayed = null;
		new AsyncTask<Void,Void,Void>() {
			@Override
			protected Void doInBackground(Void ... params) {
				try {
					perform(action.Request);
					if (action.OnSuccess != null) {
						action.OnSuccess.run();
					}
				} catch (ZLNetworkException e) {
					if (action.OnError != null) {
						action.OnError.run(e);
					}
				}
				return null;
			}
		}.execute();
	}

	public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		return false;
	}

	@Override
	protected Map<String,String> authenticateWeb(String realm, Uri uri) throws ZLNetworkAuthenticationException {
		if (myDelayed == null) {
			throw new ZLNetworkAuthenticationException();
		}

		final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		myActivity.startActivity(intent);
		throw new AuthorizationInProgressException();
	}

	@Override
	public final void perform(ZLNetworkRequest request, Runnable onSuccess, OnError onError) {
		myDelayed = new DelayedAction(request, onSuccess, onError);
		try {
			perform(request);
			myDelayed = null;
			if (onSuccess != null) {
				onSuccess.run();
			}
		} catch (AuthorizationInProgressException e) {
		} catch (ZLNetworkException e) {
			final DelayedAction action = myDelayed;
			myDelayed = null;
			if (action != null && action.OnError != null) {
				action.OnError.run(e);
			}
		}
	}
}
