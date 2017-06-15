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

package org.geometerplus.android.fbreader.network;

import java.util.*;

import android.app.Activity;
import android.content.*;
import android.net.Uri;

import org.geometerplus.zlibrary.core.money.Money;
import org.geometerplus.zlibrary.core.network.ZLNetworkContext;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;

import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.NetworkLibrary;
import org.geometerplus.fbreader.network.urlInfo.UrlInfo;
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager;

import org.geometerplus.android.util.PackageUtil;

import org.geometerplus.android.fbreader.api.PluginApi;
import org.geometerplus.android.fbreader.network.auth.ActivityNetworkContext;

public class TopupMenuActivity extends MenuActivity {
	private static final String AMOUNT_KEY = "topup:amount";
	private static final String CURRENCY_KEY = "topup:currency";

	public static boolean isTopupSupported(INetworkLink link) {
		// TODO: more correct check
		return link.getUrlInfo(UrlInfo.Type.TopUp) != null;
	}

	public static void runMenu(Activity context, INetworkLink link, Money amount) {
		final Intent intent =
			Util.intentByLink(new Intent(context, TopupMenuActivity.class), link);
		intent.putExtra(AMOUNT_KEY, amount);
		context.startActivityForResult(intent, NetworkLibraryActivity.REQUEST_TOPUP);
	}

	final ActivityNetworkContext myNetworkContext = new ActivityNetworkContext(this);

	private INetworkLink myLink;
	private Money myAmount;

	@Override
	protected void onResume() {
		super.onResume();
		myNetworkContext.onResume();
	}

	@Override
	protected void init() {
		setTitle(NetworkLibrary.resource().getResource("topupTitle").getValue());
		final String url = getIntent().getData().toString();
		myLink = Util.networkLibrary(this).getLinkByUrl(url);
		myAmount = (Money)getIntent().getSerializableExtra(AMOUNT_KEY);

		if (myLink.getUrlInfo(UrlInfo.Type.TopUp) != null) {
			myInfos.add(new PluginApi.MenuActionInfo(
				Uri.parse(url + "/browser"),
				NetworkLibrary.resource().getResource("topupViaBrowser").getValue(),
				100
			));
		}
	}

	@Override
	protected String getAction() {
		return Util.TOPUP_ACTION;
	}

	private void runBrowserPopup() {
		try {
			Util.openInBrowser(
				this,
				myLink.authenticationManager().topupLink(myNetworkContext, myAmount)
			);
		} catch (ZLNetworkException e) {
			setResult(
				RESULT_OK,
				new Intent().putExtra(NetworkLibraryActivity.ERROR_KEY, e.getMessage())
			);
		}
		finish();
	}

	@Override
	protected boolean runItem(final PluginApi.MenuActionInfo info) {
		setResult(RESULT_OK, null);

		try {
			doTopup(new Runnable() {
				public void run() {
					try {
						final NetworkAuthenticationManager mgr = myLink.authenticationManager();
						if (info.getId().toString().endsWith("/browser")) {
							if (mgr != null) {
								new Thread() {
									public void run() {
										runBrowserPopup();
									}
								}.start();
							}
						} else {
							final Intent intent = new Intent(getAction(), info.getId());
							if (mgr != null) {
								for (Map.Entry<String,String> entry : mgr.getTopupData().entrySet()) {
									intent.putExtra(entry.getKey(), entry.getValue());
								}
							}
							if (myAmount != null) {
								intent.putExtra(AMOUNT_KEY, myAmount.Amount);
							}
							if (PackageUtil.canBeStarted(TopupMenuActivity.this, intent, true)) {
								startActivity(intent);
							}
							finish();
						}
					} catch (ActivityNotFoundException e) {
						finish();
					}
				}
			});
		} catch (Exception e) {
			return true;
		}
		return false;
	}

	private void doTopup(final Runnable action) {
		final NetworkAuthenticationManager mgr = myLink.authenticationManager();
		if (mgr.mayBeAuthorised(false)) {
			action.run();
		} else {
			Util.runAuthenticationDialog(this, myLink, action);
		}
	}
}
