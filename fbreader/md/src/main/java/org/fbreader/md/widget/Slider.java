package org.fbreader.md.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import org.fbreader.md.R;

public class Slider extends RelativeLayout {
	public interface OnValueChangedListener {
		public void onValueChanged(int value);
	}

	private final int myActiveColor;
	private final int myInactiveColor;
	private final int myActiveBallRadius;
	private final int myLineWidth;
	private final int myBallHighlightingRadius;
	private final int myBallHighlightingColor;

	private OnValueChangedListener myOnValueChangedListener;

	private int myValue;

	private int myMin = 0;
	private int myMax = 100;
	private int myStoredMin;
	private int myStoredMax;

	private boolean myPressed = false;

	public Slider(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.sliderStyle);
	}

	public Slider(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);

        final TypedArray array =
			context.obtainStyledAttributes(attrs, R.styleable.Slider, defStyle, 0);

		myActiveColor = array.getColor(R.styleable.Slider_activeSectionColor, 0xFFFFFFFF);
		myInactiveColor = array.getColor(R.styleable.Slider_inactiveSectionColor, 0x88888888);
		myActiveBallRadius = array.getDimensionPixelSize(R.styleable.Slider_activeBallRadius, 15);
		myLineWidth = array.getDimensionPixelSize(R.styleable.Slider_sliderLineWidth, 1);

		myBallHighlightingColor = array.getColor(R.styleable.Slider_ballHighlightingColor, 0);
		myBallHighlightingRadius = array.getDimensionPixelSize(R.styleable.Slider_ballHighlightingRadius, 0);

		setMinimumWidth(array.getDimensionPixelSize(R.styleable.Slider_sliderMinWidth, 80));
		setMinimumHeight(array.getDimensionPixelSize(R.styleable.Slider_sliderMinHeight, 48));

		array.recycle();

		setBackgroundColor(Color.TRANSPARENT);
	}

	public int getMax() {
		return myMax;
	}

	public void setMax(int max) {
		myMax = max;
	}

	public int getMin() {
		return myMin;
	}

	public void setMin(int min) {
		myMin = min;
	}

	public void setOnValueChangedListener(OnValueChangedListener onValueChangedListener) {
		myOnValueChangedListener = onValueChangedListener;
	}

	public int getValue() {
		return myValue;
	}

	public void setValue(int value) {
		myValue = value;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
			{
				myPressed = true;

				final int newValue = valueFromCoord(event.getX(), myMin, myMax);
				if (myValue == newValue) {
					break;
				}

				if (myStoredMax > myStoredMin && (myMin != myStoredMin || myMax != myStoredMax)) {
					if (myValue == valueFromCoord(event.getX(), myStoredMin, myStoredMax)) {
						break;
					}
				}
				myStoredMin = myMin;
				myStoredMax = myMax;

				myValue = newValue;
				if (myOnValueChangedListener != null) {
					myOnValueChangedListener.onValueChanged(newValue);
				}
				break;
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				myStoredMin = 0;
				myStoredMax = 0;
				myPressed = false;
				break;
		}
		return true;
	}

	private int xLeft() {
		return getHeight() / 2;
	}

	private int xRange() {
		return getWidth() - getHeight();
	}

	private float xCoord() {
		final int len = myMax - myMin;
		if (len <= 0) {
			return xLeft() + .5f * xRange();
		}
		return xLeft() + 1f * xRange() * (myValue - myMin) / len;
	}

	private int valueFromCoord(float coord, int minValue, int maxValue) {
		final int range = xRange();
		if (range <= 0) {
			return myMin;
		}
		final int value = (int)((maxValue - minValue) * (coord - xLeft()) / range  + .5f);
		return Math.min(maxValue, Math.max(minValue, value));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		final Paint paint = new Paint();
		final int y = getHeight() / 2;

		paint.setColor(myInactiveColor);
		paint.setStrokeWidth(myLineWidth);
		canvas.drawLine(xLeft(), y, xLeft() + xRange(), y, paint);
		paint.setColor(myActiveColor);
		canvas.drawLine(xLeft(), y, xCoord(), y, paint);

		paint.setAntiAlias(true);
		if (myPressed) {
			paint.setColor(myBallHighlightingColor);
			canvas.drawCircle(xCoord(), y, myBallHighlightingRadius, paint);
		}
		paint.setColor(myActiveColor);
		canvas.drawCircle(xCoord(), y, myActiveBallRadius, paint);
		invalidate();
	}
}
