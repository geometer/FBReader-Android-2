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

import android.animation.ValueAnimator;
import android.graphics.*;

import org.fbreader.reader.android.MainView;
import org.fbreader.reader.android.view.ViewUtil;

import org.geometerplus.zlibrary.core.view.ZLViewEnums;

public final class NoneAnimationProvider extends AnimationProvider {
	private final Paint myPaint = new Paint();

	public NoneAnimationProvider(MainView view) {
		super(view);
	}

	@Override
	protected void drawInternal(Canvas canvas) {
		drawBitmapFrom(canvas, 0, 0, myPaint);
	}

	@Override
	protected ValueAnimator createAnimator() {
		return ValueAnimator.ofInt(0, 0).setDuration(0);
	}

	@Override
	protected void setupAnimatedScrollingStart(Integer x, Integer y) {
		if (myDirection.IsHorizontal) {
			myStartX = myForward ? myWidth : 0;
			myEndX = myWidth - myStartX;
			myEndY = myStartY = 0;
		} else {
			myEndX = myStartX = 0;
			myStartY = myForward ? myHeight : 0;
			myEndY = myHeight - myStartY;
		}
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
	public void drawFooterBitmapInternal(Canvas canvas, Bitmap footerBitmap, int voffset) {
		canvas.drawBitmap(footerBitmap, 0, voffset, myPaint);
	}

	@Override
	protected void setFilter() {
		ViewUtil.setColorLevel(myPaint, myColorLevel);
	}
}
