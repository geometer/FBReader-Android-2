package org.geometerplus.fbreader.plugin.comicbook;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.plugin.base.PluginApplication;
import org.geometerplus.fbreader.plugin.base.document.CBZDocument;

public class CBZApplication extends PluginApplication {
	@Override
	public CBZDocument createDocument() {
		return new CBZDocument(Paths.systemInfo(this));
	}
}
