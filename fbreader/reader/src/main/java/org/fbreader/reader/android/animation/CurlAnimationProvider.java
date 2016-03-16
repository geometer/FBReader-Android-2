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

import android.animation.*;
import android.graphics.*;
import android.view.animation.AccelerateInterpolator;

import org.fbreader.reader.android.MainView;
import org.fbreader.reader.android.view.ViewUtil;

import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;

import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil;

public final class CurlAnimationProvider extends AnimationProvider {
	private final Paint myPaint = new Paint();
	private final Paint myBackPaint = new Paint();
	private final Paint myEdgePaint = new Paint();

	final Path myFgPath = new Path();
	final Path myEdgePath = new Path();
	final Path myQuadPath = new Path();

	public CurlAnimationProvider(MainView view) {
		super(view);

		myBackPaint.setAntiAlias(true);
		myBackPaint.setAlpha(0x40);

		myEdgePaint.setAntiAlias(true);
		myEdgePaint.setStyle(Paint.Style.FILL);
		myEdgePaint.setShadowLayer(15, 0, 0, 0xC0000000);
	}

	private Bitmap myBuffer;
	private volatile boolean myUseCanvasHack = false;

	@Override
	protected void drawInternal(Canvas canvas) {
		if (myUseCanvasHack) {
			// This is a hack that disables hardware acceleration
			//   1) for GLES20Canvas we got an UnsupportedOperationException in clipPath
			//   2) View.setLayerType(LAYER_TYPE_SOFTWARE) does not work properly in some cases
			if (myBuffer == null ||
				myBuffer.getWidth() != myWidth ||
				myBuffer.getHeight() != myHeight) {
				myBuffer = BitmapUtil.createBitmap(myWidth, myHeight, getBitmapTo().getConfig());
			}
			final Canvas softCanvas = new Canvas(myBuffer);
			drawInternalNoHack(softCanvas);
			canvas.drawBitmap(myBuffer, 0, 0, myPaint);
		} else {
			try {
				drawInternalNoHack(canvas);
			} catch (UnsupportedOperationException e) {
				myUseCanvasHack = true;
				drawInternal(canvas);
			}
		}
	}

	private void drawInternalNoHack(Canvas canvas) {
		drawBitmapTo(canvas, 0, 0, myPaint);

		final int cornerX = myStartX > myWidth / 2 ? myWidth : 0;
		final int cornerY = myStartY > myHeight / 2 ? myHeight : 0;
		final int oppositeX = Math.abs(myWidth - cornerX);
		final int oppositeY = Math.abs(myHeight - cornerY);
		final int x, y;
		if (myDirection.IsHorizontal) {
			x = myEndX;
			if (getMode().Auto) {
				y = myEndY;
			} else {
				if (cornerY == 0) {
					y = Math.max(1, Math.min(myHeight / 2, myEndY));
				} else {
					y = Math.max(myHeight / 2, Math.min(myHeight - 1, myEndY));
				}
			}
		} else {
			y = myEndY;
			if (getMode().Auto) {
				x = myEndX;
			} else {
				if (cornerX == 0) {
					x = Math.max(1, Math.min(myWidth / 2, myEndX));
				} else {
					x = Math.max(myWidth / 2, Math.min(myWidth - 1, myEndX));
				}
			}
		}
		final int dX = Math.max(1, Math.abs(x - cornerX));
		final int dY = Math.max(1, Math.abs(y - cornerY));

		final int x1 = cornerX == 0
			? (dY * dY / dX + dX) / 2
			: cornerX - (dY * dY / dX + dX) / 2;
		final int y1 = cornerY == 0
			? (dX * dX / dY + dY) / 2
			: cornerY - (dX * dX / dY + dY) / 2;

		float sX, sY;
		{
			final float d1 = x - x1;
			final float d2 = y - cornerY;
			sX = (float)Math.sqrt(d1 * d1 + d2 * d2) / 2;
			if (cornerX == 0) {
				sX = -sX;
			}
		}
		{
			final float d1 = x - cornerX;
			final float d2 = y - y1;
			sY = (float)Math.sqrt(d1 * d1 + d2 * d2) / 2;
			if (cornerY == 0) {
				sY = -sY;
			}
		}

		myFgPath.rewind();
		myFgPath.moveTo(x, y);
		myFgPath.lineTo((x + cornerX) / 2, (y + y1) / 2);
		myFgPath.quadTo(cornerX, y1, cornerX, y1 - sY);
		if (Math.abs(y1 - sY - cornerY) < myHeight) {
			myFgPath.lineTo(cornerX, oppositeY);
		}
		myFgPath.lineTo(oppositeX, oppositeY);
		if (Math.abs(x1 - sX - cornerX) < myWidth) {
			myFgPath.lineTo(oppositeX, cornerY);
		}
		myFgPath.lineTo(x1 - sX, cornerY);
		myFgPath.quadTo(x1, cornerY, (x + x1) / 2, (y + cornerY) / 2);

		myQuadPath.moveTo(x1 - sX, cornerY);
		myQuadPath.quadTo(x1, cornerY, (x + x1) / 2, (y + cornerY) / 2);
		canvas.drawPath(myQuadPath, myEdgePaint);
		myQuadPath.rewind();
		myQuadPath.moveTo((x + cornerX) / 2, (y + y1) / 2);
		myQuadPath.quadTo(cornerX, y1, cornerX, y1 - sY);
		canvas.drawPath(myQuadPath, myEdgePaint);
		myQuadPath.rewind();

		canvas.save();
		canvas.clipPath(myFgPath);
		drawBitmapFrom(canvas, 0, 0, myPaint);
		canvas.restore();

		myEdgePaint.setColor(ZLAndroidColorUtil.rgb(ZLAndroidColorUtil.getAverageColor(getBitmapFrom())));

		myEdgePath.rewind();
		myEdgePath.moveTo(x, y);
		myEdgePath.lineTo(
			(x + cornerX) / 2,
			(y + y1) / 2
		);
		myEdgePath.quadTo(
			(x + 3 * cornerX) / 4,
			(y + 3 * y1) / 4,
			(x + 7 * cornerX) / 8,
			(y + 7 * y1 - 2 * sY) / 8
		);
		myEdgePath.lineTo(
			(x + 7 * x1 - 2 * sX) / 8,
			(y + 7 * cornerY) / 8
		);
		myEdgePath.quadTo(
			(x + 3 * x1) / 4,
			(y + 3 * cornerY) / 4,
			(x + x1) / 2,
			(y + cornerY) / 2
		);

		canvas.drawPath(myEdgePath, myEdgePaint);
		/*
		canvas.save();
		canvas.clipPath(myEdgePath);
		final Matrix m = new Matrix();
		m.postScale(1, -1);
		m.postTranslate(x - cornerX, y + cornerY);
		final float angle;
		if (cornerY == 0) {
			angle = -180 / 3.1416f * (float)Math.atan2(x - cornerX, y - y1);
		} else {
			angle = 180 - 180 / 3.1416f * (float)Math.atan2(x - cornerX, y - y1);
		}
		m.postRotate(angle, x, y);
		canvas.drawBitmap(getBitmapFrom(), m, myBackPaint);
		canvas.restore();
		*/
	}

	@Override
	public ZLViewEnums.PageIndex getPageToScrollTo(int x, int y) {
		if (myDirection == null) {
			return ZLViewEnums.PageIndex.current;
		}

		switch (myDirection) {
			case leftToRight:
				return myStartX < myWidth / 2 ? ZLViewEnums.PageIndex.next : ZLViewEnums.PageIndex.previous;
			case rightToLeft:
				return myStartX < myWidth / 2 ? ZLViewEnums.PageIndex.previous : ZLViewEnums.PageIndex.next;
			case up:
				return myStartY < myHeight / 2 ? ZLViewEnums.PageIndex.previous : ZLViewEnums.PageIndex.next;
			case down:
				return myStartY < myHeight / 2 ? ZLViewEnums.PageIndex.next : ZLViewEnums.PageIndex.previous;
		}
		return ZLViewEnums.PageIndex.current;
	}

	@Override
	protected void setupAnimatedScrollingStart(Integer x, Integer y) {
		if (x == null || y == null) {
			if (myDirection.IsHorizontal) {
				x = myForward ? myWidth - 3 : 3;
				y = 1;
			} else {
				x = 1;
				y = myForward ? myHeight - 3 : 3;
			}
		} else {
			final int cornerX = x > myWidth / 2 ? myWidth : 0;
			final int cornerY = y > myHeight / 2 ? myHeight : 0;
			int deltaX = Math.min(Math.abs(x - cornerX), myWidth / 5);
			int deltaY = Math.min(Math.abs(y - cornerY), myHeight / 5);
			if (myDirection.IsHorizontal) {
				deltaY = Math.min(deltaY, deltaX / 3);
			} else {
				deltaX = Math.min(deltaX, deltaY / 3);
			}
			x = Math.abs(cornerX - deltaX);
			y = Math.abs(cornerY - deltaY);
		}
		myEndX = myStartX = x;
		myEndY = myStartY = y;
	}

	@Override
	protected Animator createAnimator() {
		final int cornerX = myStartX > myWidth / 2 ? myWidth : 0;
		final int cornerY = myStartY > myHeight / 2 ? myHeight : 0;

		int boundX, boundY;
		if (getMode() == Mode.AnimatedScrollingForward) {
			boundX = cornerX == 0 ? 2 * myWidth : -myWidth;
			boundY = cornerY == 0 ? 2 * myHeight : -myHeight;
		} else {
			boundX = cornerX;
			boundY = cornerY;
		}

		final int deltaX = Math.abs(myEndX - cornerX);
		final int deltaY = Math.abs(myEndY - cornerY);
		final int speedX, speedY;
		if (deltaX == 0) {
			boundX = myEndX;
		} else if (deltaY == 0) {
			boundY = myEndY;
		} else if (deltaX < deltaY) {
			boundX = myEndX + (boundX - myEndX) * deltaX / deltaY;
		} else {
			boundY = myEndY + (boundY - myEndY) * deltaY / deltaX;
		}

		final ValueAnimator xAnimator = ValueAnimator.ofInt(myEndX, boundX);
		final ValueAnimator yAnimator = ValueAnimator.ofInt(myEndY, boundY);
		final ValueAnimator.AnimatorUpdateListener listener =
			new ValueAnimator.AnimatorUpdateListener() {
				public void onAnimationUpdate(ValueAnimator animator) {
					final int intValue;
					try {
						intValue = (int)(Integer)animator.getAnimatedValue();
					} catch (Exception e) {
						return;
					}

					if (animator == xAnimator) {
						myEndX = intValue;
					} else {
						myEndY = intValue;
					}
					// TODO: do not send twice?
					myView.postInvalidate();
				}
			};
		xAnimator.addUpdateListener(listener);
		yAnimator.addUpdateListener(listener);
		final AnimatorSet set = new AnimatorSet();
		set.playTogether(xAnimator, yAnimator);
		setAnimatorDuration(set, Math.max(
			1f * Math.abs(myEndX - boundX) / myWidth,
			1f * Math.abs(myEndY - boundY) / myHeight
		));
		set.setInterpolator(new AccelerateInterpolator(1.2f));
		return set;
	}

	@Override
	protected void terminateAnimator(Animator animator) {
		super.terminateAnimator(animator);
		if (animator instanceof AnimatorSet) {
			for (Animator child : ((AnimatorSet)animator).getChildAnimations()) {
				if (child instanceof ValueAnimator) {
					((ValueAnimator)child).removeAllUpdateListeners();
				}
			}
		}
	}

	@Override
	public void drawFooterBitmapInternal(Canvas canvas, Bitmap footerBitmap, int voffset) {
		canvas.drawBitmap(footerBitmap, 0, voffset, myPaint);
	}

	@Override
	protected void setFilter() {
		ViewUtil.setColorLevel(myPaint, myColorLevel);
		ViewUtil.setColorLevel(myBackPaint, myColorLevel);
		ViewUtil.setColorLevel(myEdgePaint, myColorLevel);
	}
}
