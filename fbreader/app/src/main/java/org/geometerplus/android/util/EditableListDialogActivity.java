/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.util;

import android.graphics.drawable.Drawable;

import org.fbreader.util.android.DrawableUtil;
import org.fbreader.md.MDListActivity;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.R;

public class EditableListDialogActivity extends MDListActivity {
	private Drawable myDeleteIcon;
	protected final ZLResource myButtonResource = ZLResource.resource("dialog").getResource("button");

	@Override
	protected int layoutId() {
		return R.layout.editable_list_dialog;
	}

	protected Drawable deleteIcon() {
		if (myDeleteIcon == null) {
			myDeleteIcon = DrawableUtil.tintedDrawable(
				this, R.drawable.ic_button_delete, R.color.text_primary
			);
		}
		return myDeleteIcon;
	}
}
