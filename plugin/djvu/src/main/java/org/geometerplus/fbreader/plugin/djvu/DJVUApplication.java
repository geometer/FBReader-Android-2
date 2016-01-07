package org.geometerplus.fbreader.plugin.djvu;

import android.app.ActivityManager;
import android.os.Process;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.plugin.base.PluginApplication;
import org.geometerplus.fbreader.plugin.base.document.DJVUDocument;
import org.geometerplus.zlibrary.core.util.SystemInfo;

public class DJVUApplication extends PluginApplication {
	@Override
	protected void initIntents() {
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
			FBReaderIntents.initPremium();
		}
	}

	@Override
	public DJVUDocument createDocument() {
		return new DJVUDocument(Paths.systemInfo(this));
	}
}
