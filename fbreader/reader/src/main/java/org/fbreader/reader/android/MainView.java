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

package org.fbreader.reader.android;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.fbreader.reader.AbstractReader;
import org.fbreader.reader.R;
import org.fbreader.reader.android.animation.*;

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;

public abstract class MainView extends View {
	protected Integer myColorLevel;
	private TextView myInfoView;
	private final Timer myInfoTimer = new Timer();
	private volatile TimerTask myInfoCancelTask;

	public MainView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MainView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MainView(Context context) {
		super(context);
	}

	/* ++++ INFO ++++ */
	protected final void showInfo(final String text) {
		if (myInfoView == null) {
			final MainActivity activity = getActivity();
			if (activity == null) {
				return;
			}
			myInfoView = (TextView)activity.findViewById(R.id.main_view_info);
		}
		if (myInfoView == null) {
			return;
		}

		synchronized (myInfoTimer) {
			if (myInfoCancelTask != null) {
				myInfoCancelTask.cancel();
			}
			myInfoCancelTask = new TimerTask() {
				public void run() {
					synchronized (myInfoTimer) {
						post(new Runnable() {
							public void run() {
								myInfoView.setVisibility(View.GONE);
							}
						});
					}
				}
			};
			myInfoTimer.schedule(myInfoCancelTask, 1000);
		}
		final int color = myColorLevel == null ? 0x80 : 0x80 * myColorLevel / 0xFF;
		post(new Runnable() {
			public void run() {
				myInfoView.setVisibility(View.VISIBLE);
				myInfoView.setTextColor(Color.argb(0xCC, color, color, color));
				myInfoView.setText(text);
			}
		});
	}
	/* ---- INFO ---- */

	/* ++++ BRIGHTNESS ++++ */
	public final void setScreenBrightness(int percent, boolean showPercent) {
		if (percent < 1) {
			percent = 1;
		} else if (percent > 100) {
			percent = 100;
		}

		final MainActivity activity = getActivity();
		if (activity == null) {
			return;
		}

		final float level;
		final Integer oldColorLevel = myColorLevel;
		if (percent >= 25) {
			// 100 => 1f; 25 => .01f
			level = .01f + (percent - 25) * .99f / 75;
			myColorLevel = null;
		} else {
			level = .01f;
			myColorLevel = 0x60 + (0xFF - 0x60) * Math.max(percent, 0) / 25;
		}

		activity.getZLibrary().ScreenBrightnessLevelOption.setValue(percent);
		if (showPercent) {
			showInfo(percent + "%");
		}
		activity.setScreenBrightnessSystem(level);
		if (oldColorLevel != myColorLevel) {
			updateColorLevel();
			postInvalidate();
		}
	}

	public final int getScreenBrightness() {
		if (myColorLevel != null) {
			return (myColorLevel - 0x60) * 25 / (0xFF - 0x60);
		}

		final MainActivity activity = getActivity();
		if (activity == null) {
			return 50;
		}
		final float level = activity.getScreenBrightnessSystem();
		// level = .01f + (percent - 25) * .99f / 75;
		return 25 + (int)((level - .01f) * 75 / .99f);
	}

	protected abstract void updateColorLevel();
	/* ---- BRIGHTNESS ---- */

	/* ++++ BATTERY ++++ */
	private int myBatteryLevel;

	public int getBatteryLevel() {
		return myBatteryLevel;
	}

	public void setBatteryLevel(int percent) {
		myBatteryLevel = percent;
	}
	/* ---- BATTERY ---- */

	private MainActivity getActivity() {
		Context context = getContext();
		while (context instanceof ContextWrapper) {
			if (context instanceof MainActivity) {
				return (MainActivity)context;
			}
			context = ((ContextWrapper)context).getBaseContext();
		}
		return null;
	}

	public AbstractReader getReader() {
		final MainActivity activity = getActivity();
		return activity != null ? activity.getReader() : null;
	}

	public abstract void setPreserveSize(boolean preserve, int statusBarHeight);

	private AnimationProvider myAnimationProvider;
	private ZLViewEnums.Animation myAnimationType;
	private int myStoredLayerType = -1;

	protected final ZLViewEnums.Animation getAnimationType() {
		final AbstractReader reader = getReader();
		return reader != null ? reader.getAnimationType() : ZLViewEnums.Animation.none;
	}

	protected final AnimationProvider getAnimationProvider() {
		final ZLViewEnums.Animation type = getAnimationType();

		if (myAnimationProvider == null || myAnimationType != type) {
			myAnimationType = type;
			if (myStoredLayerType != -1) {
				setLayerType(myStoredLayerType, null);
			}
			switch (type) {
				case none:
					myAnimationProvider = new NoneAnimationProvider(this);
					break;
				case curl:
					myStoredLayerType = getLayerType();
					myAnimationProvider = new CurlAnimationProvider(this);
					setLayerType(LAYER_TYPE_SOFTWARE, null);
					break;
				case slide:
					myAnimationProvider = new SlideAnimationProvider(this);
					break;
				case slideOldStyle:
					myAnimationProvider = new SlideOldStyleAnimationProvider(this);
					break;
				case shift:
					myAnimationProvider = new ShiftAnimationProvider(this);
					break;
			}
		}
		return myAnimationProvider;
	}

	public abstract BitmapManager getBitmapManager();
}
