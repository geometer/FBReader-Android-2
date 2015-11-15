/*
 * Copyright (C) 2007-2014 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.fbreader.plugin.base;

import android.app.ActivityManager;
import android.os.Process;

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.android.fbreader.api.FBReaderIntents;

import org.geometerplus.fbreader.plugin.base.document.DocumentHolder;

public abstract class PluginApplication extends ZLAndroidApplication {
	@Override
	public void onCreate() {
		super.onCreate();

		final int pid = Process.myPid();
		String processName = null;
		final ActivityManager aManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		for (ActivityManager.RunningAppProcessInfo info : aManager.getRunningAppProcesses()) {
			if (pid == info.pid) {
				processName = info.processName;
				break;
			}
		}
		if (processName.endsWith("premium")) {
			//FBReaderIntents.initPremium();
		}
	}

	public abstract DocumentHolder createDocument();
}
