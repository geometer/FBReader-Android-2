/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
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

import org.fbreader.reader.ActionCode;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.android.fbreader.SimplePopupWindow;
import org.fbreader.plugin.format.base.R;

class SelectionPopup extends FBReaderPluginActivity.PopupPanel {
	final static String ID = "SelectionPopup";

	@Override
	public void createControlPanel(FBReaderPluginActivity activity, RelativeLayout root) {
		if (myWindow != null && activity == myWindow.getContext()) {
			return;
		}

		activity.getLayoutInflater().inflate(R.layout.selection_panel, root, true);
		myWindow = (SimplePopupWindow)root.findViewById(R.id.selection_panel);

		final ZLResource resource = ZLResource.resource("selectionPopup");
		setupButton(R.id.selection_panel_copy, resource.getResource("copyToClipboard").getValue());
		setupButton(R.id.selection_panel_share, resource.getResource("share").getValue());
		myWindow.findViewById(R.id.selection_panel_translate).setVisibility(View.GONE);
		//setupButton(R.id.selection_panel_translate, resource.getResource("translate").getValue());
		setupButton(R.id.selection_panel_bookmark, resource.getResource("bookmark").getValue());
		setupButton(R.id.selection_panel_close, resource.getResource("close").getValue());
	}

	private void setupButton(int buttonId, String description) {
		final View button = myWindow.findViewById(buttonId);
		button.setOnClickListener(this);
		button.setContentDescription(description);
	}

	public void move(int selectionStartY, int selectionEndY) {
		if (myWindow == null) {
			return;
		}

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.WRAP_CONTENT,
			RelativeLayout.LayoutParams.WRAP_CONTENT
		);
		layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

		final int verticalPosition;
		final int screenHeight = ((View)myWindow.getParent()).getHeight();
		final int diffTop = screenHeight - selectionEndY;
		final int diffBottom = selectionStartY;
		if (diffTop > diffBottom) {
			verticalPosition = diffTop > myWindow.getHeight() + 20
				? RelativeLayout.ALIGN_PARENT_BOTTOM : RelativeLayout.CENTER_VERTICAL;
		} else {
			verticalPosition = diffBottom > myWindow.getHeight() + 20
				? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.CENTER_VERTICAL;
		}

		layoutParams.addRule(verticalPosition);
		myWindow.setLayoutParams(layoutParams);
	}

	@Override
	protected void update() {
	}

	public void onClick(View view) {
		final FBReaderPluginActivity activity = (FBReaderPluginActivity)myWindow.getContext();

		final int viewId = view.getId();
		if (viewId == R.id.selection_panel_copy) {
			activity.getReader().runAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD);
		} else if (viewId == R.id.selection_panel_share) {
			activity.getReader().runAction(ActionCode.SELECTION_SHARE);
		//} else if (viewId == R.id.selection_panel_translate) {
		//	activity.getReader().runAction(ActionCode.SELECTION_TRANSLATE);
		} else if (viewId == R.id.selection_panel_bookmark) {
			activity.getReader().runAction(ActionCode.SELECTION_BOOKMARK);
		} else if (viewId == R.id.selection_panel_close) {
			activity.getReader().runAction(ActionCode.SELECTION_CLEAR);
		}
		activity.hideActivePopup();
	}
}
