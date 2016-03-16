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

package org.fbreader.reader.android.animation;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateInterpolator;

import org.fbreader.reader.android.MainView;

import org.geometerplus.zlibrary.core.view.ZLViewEnums;

abstract class SimpleAnimationProvider extends AnimationProvider implements ValueAnimator.AnimatorUpdateListener {
	SimpleAnimationProvider(MainView view) {
		super(view);
	}

	@Override
	public ZLViewEnums.PageIndex getPageToScrollTo(int x, int y) {
		if (myDirection == null) {
			return ZLViewEnums.PageIndex.current;
		}

		switch (myDirection) {
			case rightToLeft:
				return myStartX < x ? ZLViewEnums.PageIndex.previous : ZLViewEnums.PageIndex.next;
			case leftToRight:
				return myStartX < x ? ZLViewEnums.PageIndex.next : ZLViewEnums.PageIndex.previous;
			case up:
				return myStartY < y ? ZLViewEnums.PageIndex.previous : ZLViewEnums.PageIndex.next;
			case down:
				return myStartY < y ? ZLViewEnums.PageIndex.next : ZLViewEnums.PageIndex.previous;
		}
		return ZLViewEnums.PageIndex.current;
	}

	@Override
	protected void setupAnimatedScrollingStart(Integer x, Integer y) {
		if (x == null || y == null) {
			if (myDirection.IsHorizontal) {
				x = myForward ? myWidth : 0;
				y = 0;
			} else {
				x = 0;
				y = myForward ? myHeight : 0;
			}
		}
		myEndX = myStartX = x;
		myEndY = myStartY = y;
	}

	@Override
	protected ValueAnimator createAnimator() {
		final int sign;
		if (getMode() == Mode.AnimatedScrollingForward) {
			sign = myForward ? -1 : 1;
		} else {
			sign = 0;
		}

		final int total;
		final int start;
		final int end;
		if (myDirection.IsHorizontal) {
			total = myWidth;
			start = myEndX;
			end = myStartX + sign * total;
		} else {
			total = myHeight;
			start = myEndY;
			end = myStartY + sign * total;
		}
		final ValueAnimator animator = ValueAnimator.ofInt(start, end);
		setAnimatorDuration(animator, 1f * Math.abs(start - end) / total);
		animator.setInterpolator(new AccelerateInterpolator(1.2f));
		animator.addUpdateListener(this);
		return animator;
	}

	@Override
	protected void terminateAnimator(Animator animator) {
		super.terminateAnimator(animator);
		if (animator instanceof ValueAnimator) {
			((ValueAnimator)animator).removeAllUpdateListeners();
		}
	}

	public void onAnimationUpdate(ValueAnimator animator) {
		final int intValue;
		try {
			intValue = (int)(Integer)animator.getAnimatedValue();
		} catch (Exception e) {
			return;
		}

		if (myDirection.IsHorizontal) {
			myEndX = intValue;
		} else {
			myEndY = intValue;
		}
		myView.postInvalidate();
	}
}
