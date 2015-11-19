package org.geometerplus.android.fbreader.formatPlugin;

import org.geometerplus.fbreader.plugin.base.document.PDFDocument;

public class PDFCoverService extends CoverService {
	@Override
	public void onCreate() {
		super.onCreate();
		PDFDocument.init(this);
	}
}
