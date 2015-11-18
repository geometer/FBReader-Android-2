package org.geometerplus.fbreader.plugin.djvu;

import org.geometerplus.fbreader.plugin.base.PluginApplication;
import org.geometerplus.fbreader.plugin.base.document.DJVUDocument;

public class DJVUApplication extends PluginApplication {
	@Override
	public DJVUDocument createDocument() {
		return new DJVUDocument();
	}
}
