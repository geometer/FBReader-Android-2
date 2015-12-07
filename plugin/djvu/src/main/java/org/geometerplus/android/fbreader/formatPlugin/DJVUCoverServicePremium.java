package org.geometerplus.android.fbreader.formatPlugin;

import org.geometerplus.fbreader.plugin.base.document.DJVUDocument;

public class DJVUCoverServicePremium extends CoverService {
	@Override
	public void onCreate() {
		super.onCreate();
		DJVUDocument.init(this);
	}
}
