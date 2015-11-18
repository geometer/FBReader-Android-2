package org.geometerplus.fbreader.plugin.pdf;

import org.geometerplus.fbreader.plugin.base.PluginApplication;
import org.geometerplus.fbreader.plugin.base.document.PDFDocument;

public class PDFApplication extends PluginApplication {
	@Override
	public PDFDocument createDocument() {
		return new PDFDocument();
	}
}
