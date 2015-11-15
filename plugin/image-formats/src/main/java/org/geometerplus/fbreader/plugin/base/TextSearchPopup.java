/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.plugin.base;

import android.view.View;
import android.widget.RelativeLayout;

import org.fbreader.common.ActionCode;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.android.fbreader.SimplePopupWindow;
import org.fbreader.plugin.format.base.R;

final class TextSearchPopup extends FBReaderPluginActivity.PopupPanel {
	final static String ID = "TextSearchPopup";

	@Override
	public void createControlPanel(FBReaderPluginActivity activity, RelativeLayout root) {
		if (myWindow != null && activity == myWindow.getContext()) {
			update();
			return;
		}

		activity.getLayoutInflater().inflate(R.layout.search_panel, root, true);
		myWindow = (SimplePopupWindow)root.findViewById(R.id.search_panel);

		update();

		final ZLResource resource = ZLResource.resource("textSearchPopup");
		setupButton(R.id.search_panel_previous, resource.getResource("findPrevious").getValue());
		setupButton(R.id.search_panel_next, resource.getResource("findNext").getValue());
		setupButton(R.id.search_panel_close, resource.getResource("close").getValue());
	}

	private void setupButton(int buttonId, String description) {
		final View button = myWindow.findViewById(buttonId);
		button.setOnClickListener(this);
		button.setContentDescription(description);
	}

	protected void update() {
		if (myWindow == null) {
			return;
		}
		final FBReaderPluginActivity activity = (FBReaderPluginActivity)myWindow.getContext();

		myWindow.findViewById(R.id.search_panel_previous).setEnabled(
			activity.getReader().isActionEnabled(ActionCode.FIND_PREVIOUS)
		);
		myWindow.findViewById(R.id.search_panel_next).setEnabled(
			activity.getReader().isActionEnabled(ActionCode.FIND_NEXT)
		);
	}

	public void onClick(View view) {
		final FBReaderPluginActivity activity = (FBReaderPluginActivity)myWindow.getContext();

		final int viewId = view.getId();
		if (viewId == R.id.search_panel_previous) {
			activity.getReader().runAction(ActionCode.FIND_PREVIOUS);
		} else if (viewId == R.id.search_panel_next) {
			activity.getReader().runAction(ActionCode.FIND_NEXT);
		} else if (viewId == R.id.search_panel_close) {
			activity.getReader().runAction(ActionCode.CLEAR_FIND_RESULTS);
			activity.hideActivePopup();
		}
	}
}
