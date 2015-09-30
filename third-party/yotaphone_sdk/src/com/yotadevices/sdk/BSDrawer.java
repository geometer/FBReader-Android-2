
package com.yotadevices.sdk;

import com.yotadevices.sdk.utils.EinkUtils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;

public class BSDrawer {

	private final static String TAG = "BSDrawer";

	/**
	 * Width BS screen
	 */
	public static final int SCREEN_WIDTH = getBSScreenWidth();

	/**
	 * Height BS screen
	 */
	public static final int SCREEN_HEIGHT = getBSScreenHeight();

	public enum Waveform {
		WAVEFORM_INIT, WAVEFORM_DU, WAVEFORM_GC_FULL, WAVEFORM_GC_PARTIAL, WAVEFORM_A2;
	}

	public static final int PIP_FORMAT_1BPP = 0;
	public static final int PIP_FORMAT_2BPP = 1;
	public static final int PIP_FORMAT_4BPP = 2;

	public static final int TRANSPARENT_MODE_OFF = 0;
	public static final int TRANSPARENT_MODE_XOR = 1;
	public static final int TRANSPARENT_MODE_XOR_NOT = 2;
	public static final int TRANSPARENT_MODE_INVERT = 3;

	private static final int BS_SCREEN_WIDTH = 360;
	private static final int BS_SCREEN_HEIGHT = 640;

	public static final int RESULT_ERROR = -1;
	public static final int RESULT_OK = 0;

	public static final int RESULT_EPERM = 1;
	public static final int RESULT_EINVAL = 22;

	private BSActivity mParent;

	public BSDrawer() {
	}

	public BSDrawer(BSActivity activity) {
		mParent = activity;
	}

	private static int getBSScreenHeight() {
		return BS_SCREEN_HEIGHT;
	}

	private static int getBSScreenWidth() {
		return BS_SCREEN_WIDTH;
	}

	public int drawBitmap(int left, int top, Bitmap bitmap, Waveform waveform, int ditheringAlgorithm) {
		Log.d(TAG, "start drawing");
		if (mParent != null) {
			mParent.sendRequestToDrawBitmap(left, top, bitmap, waveform.ordinal(), ditheringAlgorithm);
		}

		return RESULT_OK;
	}

	public int drawBitmap(int left, int top, Bitmap bitmap, Waveform waveform) {
		if (bitmap == null)
			return RESULT_ERROR;
		int result = drawBitmap(left, top, bitmap, waveform, EinkUtils.NO_DITHERING);
		return result;
	}

	public int drawBitmap(int left, int top, int right, int bottom, View view, Waveform waveform) {
		if (view == null)
			return RESULT_ERROR;
		int result = drawBitmap(left, top, bitmapFromView(view, right, bottom), waveform, EinkUtils.NO_DITHERING);
		return result;
	}

	public int drawBitmap(View view, Waveform waveform, int ditheringAlgorithm) {
		if (view == null)
			return RESULT_ERROR;
		int result = drawBitmap(0, 0, bitmapFromView(view), waveform, ditheringAlgorithm);
		return result;
	}

	public int drawBitmap(View view, Waveform waveform) {
		if (view == null)
			return RESULT_ERROR;
		int result = drawBitmap(0, 0, bitmapFromView(view), waveform, EinkUtils.NO_DITHERING);
		return result;
	}

	/**
	 * Get bitmap from view with default BS screen size
	 * 
	 * @param view
	 * @return
	 */
	public static final Bitmap bitmapFromView(final View view) {
		return bitmapFromView(view, getBSScreenWidth(), getBSScreenHeight());
	}

	public static final Bitmap bitmapFromView(final View view, final int width, final int height) {
		if (view == null) {
			return null;
		}
		view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
		view.layout(0, 0, width, height);
		Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		view.draw(c);
		return bitmap;
	}

	// TODO Do we need toGrayscale method???
	public static final Bitmap toGrayscale(Bitmap bmpOriginal, int width, int height) {
		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmpGrayscale);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		paint.setFilterBitmap(true);
		c.drawBitmap(bmpOriginal, null, new Rect(0, 0, width, height), paint);
		return bmpGrayscale;
	}
}
