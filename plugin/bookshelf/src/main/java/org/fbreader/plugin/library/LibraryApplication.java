package org.fbreader.plugin.library;

import android.app.Application;

import org.geometerplus.android.fbreader.config.ConfigShadow;

public class LibraryApplication extends Application {
	private ConfigShadow myConfig;

	@Override
	public void onCreate() {
		super.onCreate();
		// this is a workaround for strange issue on some devices:
		//    NoClassDefFoundError for android.os.AsyncTask
		try {
			Class.forName("android.os.AsyncTask");
		} catch (Throwable t) {
		}

		myConfig = new ConfigShadow(this);
	}
}
