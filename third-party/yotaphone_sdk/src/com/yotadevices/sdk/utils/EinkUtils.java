package com.yotadevices.sdk.utils;

import com.yotadevices.platinum.EInk;
import com.yotadevices.sdk.BSDrawer.Waveform;

import android.graphics.Bitmap;

public class EinkUtils {

	public final static int GESTURE_BACK_DOUBLE_TAP = com.yotadevices.platinum.Utils.GESTURE_BACK_DOUBLE_TAP;
	public final static int GESTURE_BACK_LONG_PRESS = com.yotadevices.platinum.Utils.GESTURE_BACK_LONG_PRESS;
	public final static int GESTURE_BACK_LRL = com.yotadevices.platinum.Utils.GESTURE_BACK_LRL;
	public final static int GESTURE_BACK_RLR = com.yotadevices.platinum.Utils.GESTURE_BACK_RLR;
	public final static int GESTURE_BACK_SCROLL_LEFT = com.yotadevices.platinum.Utils.GESTURE_BACK_SCROLL_LEFT;
	public final static int GESTURE_BACK_SCROLL_RIGHT = com.yotadevices.platinum.Utils.GESTURE_BACK_SCROLL_RIGHT;
	public final static int GESTURE_BACK_SINGLE_TAP = com.yotadevices.platinum.Utils.GESTURE_BACK_SINGLE_TAP;
	public final static int GESTURE_BACK_SWIPE_LEFT = com.yotadevices.platinum.Utils.GESTURE_BACK_SWIPE_LEFT;
	public final static int GESTURE_BACK_SWIPE_RIGHT = com.yotadevices.platinum.Utils.GESTURE_BACK_SWIPE_RIGHT;

	public final static int ATKINSON_DITHERING = com.yotadevices.platinum.EInk.ATKINSON_DITHERING;
	public final static int FLOYD_STEINBERG_DITHERING = com.yotadevices.platinum.EInk.FLOYD_STEINBERG_DITHERING;
	public final static int NO_DITHERING = com.yotadevices.platinum.EInk.NO_DITHERING;

	public static Bitmap captureScreenshot() {
		return EInk.captureScreenshot();
	}

	public static void enableGestures(int bitmask) {
		com.yotadevices.platinum.Utils.enableGestures(bitmask);
	}

	public static String getDeviceColor() {
		return com.yotadevices.platinum.Utils.getDeviceColor();
	}

	public static int setActiveApplication(int pid, int uid) {
		return com.yotadevices.platinum.Utils.setActiveApplication(pid, uid);
	}

	public static int drawBitmap(Bitmap bmp, Waveform waveform) {
		return com.yotadevices.platinum.EInk.drawBitmap(0, 0, bmp, waveform.ordinal());
	}

	public static int drawBitmap(Bitmap bmp, int l, int t, Waveform waveform) {
		return com.yotadevices.platinum.EInk.drawBitmap(l, t, bmp, waveform.ordinal());
	}

	public static int drawDitheredBitmap(Bitmap bmp, Waveform waveform, int ditheringAlgorithm) {
		return com.yotadevices.platinum.EInk.drawBitmap(0, 0, bmp, waveform.ordinal(), ditheringAlgorithm);
	}

	public static Bitmap ditherBitmap(Bitmap bmp, int ditheringAlgorithm) {
		com.yotadevices.platinum.EInk.ditherBitmap(bmp, ditheringAlgorithm);
		return bmp;
	}

}
