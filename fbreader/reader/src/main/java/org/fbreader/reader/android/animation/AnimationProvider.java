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

import java.util.LinkedList;
import java.util.List;

import android.animation.Animator;
import android.graphics.*;

import org.fbreader.reader.android.MainView;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;

public abstract class AnimationProvider implements Animator.AnimatorListener {
	public static enum Mode {
		NoScrolling(false),
		PreManualScrolling(false),
		ManualScrolling(false),
		AnimatedScrollingForward(true),
		AnimatedScrollingBackward(true),
		TerminatedScrollingForward(true),
		TerminatedScrollingBackward(true);

		public final boolean Auto;

		Mode(boolean auto) {
			Auto = auto;
		}
	}

	private Mode myMode = Mode.NoScrolling;

	protected final MainView myView;
	private volatile Animator myAnimator;

	protected int myStartX;
	protected int myStartY;
	protected int myEndX;
	protected int myEndY;
	protected ZLViewEnums.Direction myDirection;
	private float myManualDuration;
	protected boolean myForward;

	protected int myWidth;
	protected int myHeight;
	protected Integer myColorLevel;

	protected AnimationProvider(MainView view) {
		myView = view;
	}

	public Mode getMode() {
		return myMode;
	}

	public final void terminate2() {
		myMode = Mode.NoScrolling;
	}

	public final void terminate() {
		switch (myMode) {
			case AnimatedScrollingBackward:
			case TerminatedScrollingBackward:
				myMode = Mode.TerminatedScrollingBackward;
				break;
			case AnimatedScrollingForward:
			case TerminatedScrollingForward:
				myMode = Mode.TerminatedScrollingForward;
				break;
			default:
				myMode = Mode.NoScrolling;
		}
		terminateAnimator(myAnimator);
		myAnimator = null;
		myForward = false;
		myDrawInfos.clear();
	}

	public final void startManualScrolling(int x, int y) {
		if (!myMode.Auto) {
			myMode = Mode.PreManualScrolling;
			myEndX = myStartX = x;
			myEndY = myStartY = y;
		}
	}

	private final Mode detectManualMode() {
		final int dX = Math.abs(myStartX - myEndX);
		final int dY = Math.abs(myStartY - myEndY);
		if (myDirection.IsHorizontal) {
			if (dY > ZLibrary.Instance().getDisplayDPI() / 2 && dY > dX) {
				return Mode.NoScrolling;
			} else if (dX > ZLibrary.Instance().getDisplayDPI() / 10) {
				return Mode.ManualScrolling;
			}
		} else {
			if (dX > ZLibrary.Instance().getDisplayDPI() / 2 && dX > dY) {
				return Mode.NoScrolling;
			} else if (dY > ZLibrary.Instance().getDisplayDPI() / 10) {
				return Mode.ManualScrolling;
			}
		}
		return Mode.PreManualScrolling;
	}

	public final void scrollTo(int x, int y) {
		switch (myMode) {
			case ManualScrolling:
				myEndX = x;
				myEndY = y;
				break;
			case PreManualScrolling:
				myEndX = x;
				myEndY = y;
				myMode = detectManualMode();
				break;
			default:
				return;
		}
		myDrawInfos.add(new DrawInfo(myEndX, myEndY));
		if (myDrawInfos.size() > 3) {
			myDrawInfos.remove(0);
		}
	}

	public final void startAnimatedScrolling(int x, int y) {
		if (myMode != Mode.ManualScrolling) {
			return;
		}

		if (getPageToScrollTo(x, y) == ZLViewEnums.PageIndex.current) {
			return;
		}

		final int dpi = ZLibrary.Instance().getDisplayDPI();
		final int diff = myDirection.IsHorizontal ? x - myStartX : y - myStartY;
		final int minDiff = myDirection.IsHorizontal
			? (myWidth > myHeight ? myWidth / 4 : myWidth / 3)
			: (myHeight > myWidth ? myHeight / 4 : myHeight / 3);
		boolean forward = Math.abs(diff) > Math.min(minDiff, dpi / 2);

		myMode = forward ? Mode.AnimatedScrollingForward : Mode.AnimatedScrollingBackward;

		System.err.println("INFOS SIZE: " + myDrawInfos.size());
		if (myDrawInfos.size() > 1) {
			final DrawInfo first = myDrawInfos.get(0);
			final DrawInfo last = new DrawInfo(x, y);
			final float duration = last.Timestamp - first.Timestamp;
			final float distance = (float)Math.hypot(last.X - first.X, last.Y - first.Y);
			float w = ZLibrary.Instance().getWidthInPixels();
			float h = ZLibrary.Instance().getHeightInPixels();
			myManualDuration = duration * (float)Math.hypot(w, h) / Math.max(distance, 1f) / 2;
		} else {
			myManualDuration = -1;
		}
		myDrawInfos.clear();

		if (getPageToScrollTo() == ZLViewEnums.PageIndex.previous) {
			forward = !forward;
		}

		switch (myDirection) {
			case up:
			case rightToLeft:
				myForward = forward;
				break;
			case leftToRight:
			case down:
				myForward = !forward;
				break;
		}

		startAnimatedScrollingInternal();
	}

	public void onAnimationStart(Animator animator) {
	}
	public void onAnimationRepeat(Animator animator) {
	}
	public void onAnimationCancel(Animator animator) {
	}
	public void onAnimationEnd(Animator animator) {
		terminate();
		myView.postInvalidate();
	}

	protected abstract Animator createAnimator();

	protected void terminateAnimator(Animator animator) {
		if (animator != null) {
			animator.pause();
			animator.removeAllListeners();
		}
	}

	protected final void setAnimatorDuration(Animator animator, float part) {
		final int speed = myView.getReader().PageTurningOptions.AnimationSpeed.getValue() - 8;
		final double fullDuration = 250 * Math.pow(1.25, -speed);
		final double minDuration = 42;//250 * Math.pow(1.25, -8);
		final double duration;
		if (myManualDuration < 0) {
			duration = fullDuration;
		} else {
			duration = Math.min(fullDuration, Math.max(minDuration, myManualDuration));
		}
		animator.setDuration(Math.round(duration * part));
	}

	public void startAnimatedScrolling(ZLViewEnums.PageIndex pageIndex, Integer x, Integer y) {
		if (myMode.Auto) {
			return;
		}

		terminate();
		myMode = Mode.AnimatedScrollingForward;
		myManualDuration = -1;

		switch (myDirection) {
			case up:
			case rightToLeft:
				myForward = pageIndex == ZLViewEnums.PageIndex.next;
				break;
			case leftToRight:
			case down:
				myForward = pageIndex != ZLViewEnums.PageIndex.next;
				break;
		}
		setupAnimatedScrollingStart(x, y);
		startAnimatedScrollingInternal();
	}

	private void startAnimatedScrollingInternal() {
		final Animator animator = createAnimator();
		myAnimator = animator;
		animator.addListener(this);
		animator.start();
	}

	protected abstract void setupAnimatedScrollingStart(Integer x, Integer y);

	public boolean inProgress() {
		switch (myMode) {
			case NoScrolling:
			case PreManualScrolling:
				return false;
			default:
				return true;
		}
	}

	protected int getScrollingShift() {
		return myDirection.IsHorizontal ? myEndX - myStartX : myEndY - myStartY;
	}

	public final void setup(ZLViewEnums.Direction direction, int width, int height, Integer colorLevel) {
		myDirection = direction;
		myWidth = width;
		myHeight = height;
		myColorLevel = colorLevel;
	}

	public int getScrolledPercent() {
		final int full = myDirection.IsHorizontal ? myWidth : myHeight;
		final int shift = Math.abs(getScrollingShift());
		return 100 * shift / full;
	}

	static class DrawInfo {
		final int X, Y;
		final long Timestamp;

		DrawInfo(int x, int y) {
			X = x;
			Y = y;
			Timestamp = System.currentTimeMillis();
		}
	}

	final private List<DrawInfo> myDrawInfos = new LinkedList<DrawInfo>();

	public final void draw(Canvas canvas) {
		setFilter();
		drawInternal(canvas);
	}

	public final void drawFooterBitmap(Canvas canvas, Bitmap footerBitmap, int voffset) {
		setFilter();
		drawFooterBitmapInternal(canvas, footerBitmap, voffset);
	}

	protected abstract void setFilter();
	protected abstract void drawInternal(Canvas canvas);
	protected abstract void drawFooterBitmapInternal(Canvas canvas, Bitmap footerBitmap, int voffset);

	public abstract ZLViewEnums.PageIndex getPageToScrollTo(int x, int y);

	public final ZLViewEnums.PageIndex getPageToScrollTo() {
		return getPageToScrollTo(myEndX, myEndY);
	}

	protected Bitmap getBitmapFrom() {
		return myView.getBitmapManager().getBitmap(ZLViewEnums.PageIndex.current);
	}

	protected Bitmap getBitmapTo() {
		return myView.getBitmapManager().getBitmap(getPageToScrollTo());
	}

	protected void drawBitmapFrom(Canvas canvas, int x, int y, Paint paint) {
		myView.getBitmapManager().drawBitmap(canvas, x, y, ZLViewEnums.PageIndex.current, paint);
	}

	protected void drawBitmapTo(Canvas canvas, int x, int y, Paint paint) {
		myView.getBitmapManager().drawBitmap(canvas, x, y, getPageToScrollTo(), paint);
	}
}
