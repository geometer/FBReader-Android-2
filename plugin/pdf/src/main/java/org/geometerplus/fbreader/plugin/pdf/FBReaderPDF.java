package org.geometerplus.fbreader.plugin.pdf;

import android.os.Bundle;

import org.geometerplus.fbreader.plugin.base.FBReaderPluginActivity;
import org.geometerplus.fbreader.plugin.base.document.PDFDocument;

public class FBReaderPDF extends FBReaderPluginActivity {
	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		PDFDocument.init(this);
	}
}
