package org.geometerplus.fbreader.plugin.base.document;

public abstract class DocumentFactory {
	private static DocumentHolder myDocument = null;

	public static void initDocument(DocumentHolder d) {
		myDocument = d;
	}

	public static DocumentHolder createDocument(String path) {
		if (myDocument != null && myDocument.acceptsPath(path.toLowerCase())) {
			myDocument.close();
			return myDocument;
		}
		return new DummyDocument();
	}
}
