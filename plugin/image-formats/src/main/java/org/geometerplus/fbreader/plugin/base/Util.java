package org.geometerplus.fbreader.plugin.base;

import org.geometerplus.zlibrary.core.resources.ZLResource;

public abstract class Util {
	public static String getResourceString(String ... keys) {
		ZLResource resource = ZLResource.resource(keys[0]);
		for (int i = 1; i < keys.length; ++i) {
			resource = resource.getResource(keys[i]);
		}
		return resource.getValue();
	}
}
