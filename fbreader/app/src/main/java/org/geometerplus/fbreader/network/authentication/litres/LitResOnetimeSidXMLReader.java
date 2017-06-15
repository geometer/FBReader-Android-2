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

package org.geometerplus.fbreader.network.authentication.litres;

import org.geometerplus.zlibrary.core.xml.ZLStringMap;
import org.geometerplus.zlibrary.core.network.ZLNetworkAuthenticationException;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;

import org.geometerplus.fbreader.network.NetworkException;

class LitResOnetimeSidXMLReader extends LitResAuthenticationXMLReader {
	private static final String TAG_AUTHORIZATION_FAILED = "catalit-authorization-failed";
	private static final String TAG_GOT_SID = "catalit_get_onetime_sid";

	public String Sid;

	@Override
	public boolean startElementHandler(String tag, ZLStringMap attributes) {
		if (TAG_GOT_SID.equalsIgnoreCase(tag)) {
			Sid = attributes.getValue("otsid");
			if (Sid == null) {
				setException(ZLNetworkException.forCode(NetworkException.ERROR_SOMETHING_WRONG, LitResUtil.HOST_NAME));
			}
		} else if (TAG_AUTHORIZATION_FAILED.equalsIgnoreCase(tag)) {
			setException(new ZLNetworkAuthenticationException());
		} else {
			setException(ZLNetworkException.forCode(NetworkException.ERROR_SOMETHING_WRONG, LitResUtil.HOST_NAME));
		}
		return true;
	}
}
