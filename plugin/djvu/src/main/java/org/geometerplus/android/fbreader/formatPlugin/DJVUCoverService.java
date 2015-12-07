package org.geometerplus.android.fbreader.formatPlugin;

import org.geometerplus.fbreader.plugin.base.document.DJVUDocument;

public class DJVUCoverService extends CoverService {
	@Override
	public void onCreate() {
		super.onCreate();
		DJVUDocument.init(this);
	}
}
