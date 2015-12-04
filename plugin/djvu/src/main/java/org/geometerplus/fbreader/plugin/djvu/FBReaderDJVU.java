package org.geometerplus.fbreader.plugin.djvu;

import android.os.Bundle;

import org.geometerplus.fbreader.plugin.base.FBReaderPluginActivity;
import org.geometerplus.fbreader.plugin.base.document.DJVUDocument;

public class FBReaderDJVU extends FBReaderPluginActivity {
	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		DJVUDocument.init(this);
	}
}
