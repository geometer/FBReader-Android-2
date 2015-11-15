/*
 * Copyright (C) 2009-2013 Geometer Plus <contact@geometerplus.com>
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

import org.fbreader.plugin.format.base.R;

import android.animation.*;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class NavigationWindow extends LinearLayout {
	public NavigationWindow(Context context) {
		super(context);
	}

	public NavigationWindow(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NavigationWindow(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	FBReaderPluginActivity getActivity() {
		return (FBReaderPluginActivity)getContext();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}

	private Animator myShowHideAnimator;

	public void show() {
		post(new Runnable() {
			public void run() {
				showInternal();
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showAnimatedInternal() {
		if (myShowHideAnimator != null) {
			myShowHideAnimator.end();
		}
		if (getVisibility() == View.VISIBLE) {
			return;
		}
		setVisibility(View.VISIBLE);
		setAlpha(0);
		final AnimatorSet animator = new AnimatorSet();
		animator.play(ObjectAnimator.ofFloat(this, "alpha", 1));
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animator) {
				myShowHideAnimator = null;
				requestLayout();
			}
		});
		myShowHideAnimator = animator;
		animator.start();
	}

	private void showInternal() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			showAnimatedInternal();
		} else {
			setVisibility(View.VISIBLE);
		}
	}

	public void hide() {
		post(new Runnable() {
			public void run() {
				hideInternal();
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void hideAnimatedInternal() {
		if (myShowHideAnimator != null) {
			myShowHideAnimator.end();
		}
		if (getVisibility() == View.GONE) {
			return;
		}
		setAlpha(1);
		final AnimatorSet animator = new AnimatorSet();
		animator.play(ObjectAnimator.ofFloat(this, "alpha", 0));
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animator) {
				myShowHideAnimator = null;
				setVisibility(View.GONE);
			}
		});
		myShowHideAnimator = animator;
		animator.start();
	}

	private void hideInternal() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			hideAnimatedInternal();
		} else {
			setVisibility(View.GONE);
		}
	}
}
