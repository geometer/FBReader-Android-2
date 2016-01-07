package org.geometerplus.android.fbreader.formatPlugin;

import org.geometerplus.fbreader.plugin.base.PluginApplication;
import org.geometerplus.fbreader.plugin.base.document.DocumentHolder;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;

public abstract class CoverService extends Service {
	private final CoverReader.Stub binder = new CoverReader.Stub() {
		@Override
		public Bitmap readBitmap(String path, int maxWidth, int maxHeight) {
			return CoverService.this.readBitmap(path, maxWidth, maxHeight);
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		Log.e("METAINFO", "onBind");
		return binder;
	}

	@Override
	public void onDestroy() {
		Log.e("METAINFO", "onDestroy");
		super.onDestroy();
	}

	private synchronized Bitmap readBitmap(String path, int maxWidth, int maxHeight) {
		Log.d("METAINFO", "readbitmapstart");
		try {
			final DocumentHolder doc = ((PluginApplication)getApplication()).createDocument();
			Log.d("METAINFO", "doccreated");
			if (!doc.open(path, false)) {
				return null;
			}
			final int width = Math.min(300, Math.max(50, maxWidth));
			final int height = Math.min(425, Math.max(75, maxWidth));
			Log.d("METAINFO", "docopened");
			final Bitmap m_bmp = doc.getCover(width, height);
			Log.d("METAINFO", "bmpcreated");
			doc.close();
			return m_bmp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
