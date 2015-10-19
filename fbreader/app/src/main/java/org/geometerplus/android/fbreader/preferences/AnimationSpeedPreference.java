/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.preferences;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;
import android.widget.TextView;

import org.fbreader.md.MDDialogPreference;
import org.fbreader.md.widget.Slider;

import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.ui.android.R;

class AnimationSpeedPreference extends MDDialogPreference {
	private final ZLIntegerRangeOption myOption;
	private final ZLResource myResource;

	private Slider mySlider;

	AnimationSpeedPreference(Context context, ZLResource resource, String resourceKey, ZLIntegerRangeOption option) {
		super(context);
		myOption = option;
		myResource = resource.getResource(resourceKey);
		setTitle(myResource.getValue());
	}

	@Override
	protected String positiveButtonText() {
		return ZLResource.resource("dialog").getResource("button").getResource("ok").getValue();
	}

	@Override
	protected int dialogLayoutId() {
		return R.layout.animation_speed_dialog;
	}

	@Override
	protected void onBindDialogView(View view) {
		mySlider = (Slider)view.findViewById(R.id.animation_speed_slider);
		mySlider.setMax(myOption.MaxValue - myOption.MinValue);
		mySlider.setValue(myOption.getValue() - myOption.MinValue);
		((TextView)view.findViewById(R.id.slow)).setText(myResource.getResource("slow").getValue());
		((TextView)view.findViewById(R.id.fast)).setText(myResource.getResource("fast").getValue());
		super.onBindDialogView(view);
	}

	@Override
	protected void onPositiveDialogResult() {
		myOption.setValue(myOption.MinValue + mySlider.getValue());
	}
}
