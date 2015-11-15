package org.geometerplus.fbreader.plugin.base.document;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.*;

import org.geometerplus.zlibrary.core.util.BitmapUtil;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil;

import org.fbreader.common.options.ColorProfile;

import org.geometerplus.fbreader.plugin.base.SettingsHolder;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

public abstract class DocumentUtil {
	private static String ourWallpaperFile;
	private static Bitmap ourWallpaper;
	private static ZLPaintContext.FillMode ourFillMode;
	public static void drawWallpaper(PluginView view, Canvas canv, float x, float y, float z, boolean useXfer) {
		if (view == null || !view.useWallPaper()) {
			return;
		}

		final SettingsHolder settings = view.getSettings();
		final String wallpaperFile = settings.getColorProfile().WallpaperOption.getValue();
		if (wallpaperFile == null || "".equals(wallpaperFile)) {
			return;
		}
		final ZLPaintContext.FillMode mode = wallpaperFile.startsWith("/")
			? settings.getColorProfile().FillModeOption.getValue()
			: ZLPaintContext.FillMode.tileMirror;

		if (mode != ourFillMode || !wallpaperFile.equals(ourWallpaperFile)) {
			ourFillMode = mode;
			ourWallpaperFile = wallpaperFile;
			ourWallpaper = null;
		}

		Bitmap wallpaper = ourWallpaper;
		if (wallpaper == null) {
			final Bitmap fileBitmap;
			if (wallpaperFile.startsWith("/")) {
				fileBitmap = BitmapFactory.decodeFile(wallpaperFile);
			} else {
				fileBitmap = getBitmapFromAsset(view.getContext(), wallpaperFile);
			}

			if (mode == ZLPaintContext.FillMode.tileMirror) {
				final int w = fileBitmap.getWidth();
				final int h = fileBitmap.getHeight();
				wallpaper = BitmapUtil.createBitmap(
					2 * w, 2 * h, fileBitmap.getConfig()
				);
				final Canvas wallpaperCanvas = new Canvas(wallpaper);
				final Paint wallpaperPaint = new Paint();

				final Matrix m = new Matrix();
				wallpaperCanvas.drawBitmap(fileBitmap, m, wallpaperPaint);
				m.preScale(-1, 1);
				m.postTranslate(2 * w, 0);
				wallpaperCanvas.drawBitmap(fileBitmap, m, wallpaperPaint);
				m.preScale(1, -1);
				m.postTranslate(0, 2 * h);
				wallpaperCanvas.drawBitmap(fileBitmap, m, wallpaperPaint);
				m.preScale(-1, 1);
				m.postTranslate(-2 * w, 0);
				wallpaperCanvas.drawBitmap(fileBitmap, m, wallpaperPaint);
			} else {
				wallpaper = fileBitmap;
			}
			ourWallpaper = wallpaper;
		}

		if (wallpaper == null) {
			return;
		}

		final Paint paint = new Paint();
		if (useXfer) {
			if (ColorProfile.NIGHT.equals(settings.ColorProfileName.getValue())) {
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
			} else {
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
			}
		}

		final int w = wallpaper.getWidth();
		final int h = wallpaper.getHeight();
		final int screenW = view.getWidth();
		final int screenH = view.getHeight();
		final Matrix m = new Matrix();

		switch (mode) {
			case fullscreen:
				m.preScale(1f * screenW / w * z, 1f * screenH / h * z);
				m.postTranslate(x, y);
				canv.drawBitmap(wallpaper, m, paint);
				break;
			case stretch:
				final float sw = 1f * screenW / w;
				final float sh = 1f * screenH / h;
				float scale;
				if (sw < sh) {
					scale = sh;
					x -= z * (scale * w - screenW) / 2;
				} else {
					scale = sw;
					y -= z * (scale * h - screenH) / 2;
				}
				scale *= z;
				m.preScale(scale, scale);
				m.postTranslate(x, y);
				canv.drawBitmap(wallpaper, m, paint);
				break;
			case tileVertically:
				while (y > 0) {
					y -= h;
				}
				m.preScale(1f * screenW / w * z, 1);
				m.postTranslate(x, y);
				for (float ch = 0; ch + y < canv.getHeight(); ch += h) {
					canv.drawBitmap(wallpaper, m, paint);
					m.postTranslate(0, h);
				}
				break;
			case tileHorizontally:
				while (x > 0) {
					x -= w;
				}
				m.preScale(1, 1f * screenH / h * z);
				m.postTranslate(x, y);
				for (float cw = 0; cw + x < canv.getWidth(); cw += w) {
					canv.drawBitmap(wallpaper, m, paint);
					m.postTranslate(w, 0);
				}
				break;
			case tile:
			case tileMirror:
				while (x > 0) {
					x -= w;
				}
				while (y > 0) {
					y -= h;
				}
				for (int cw = 0; cw + x < canv.getWidth(); cw += w) {
					for (int ch = 0; ch + y < canv.getHeight(); ch += h) {
						canv.drawBitmap(wallpaper, cw + x, ch + y, paint);
					}
				}
				break;
			default:
				break;
		}
	}

	public static int getAverageBgColor() {
		final Bitmap wp = ourWallpaper;
		return wp != null ? ZLAndroidColorUtil.rgb(ZLAndroidColorUtil.getAverageColor(wp)) : Color.GRAY;
	}

	private static Bitmap getBitmapFromAsset(Context context, String strName) {
		final AssetManager assetManager = context.getAssets();

		InputStream istr;
		Bitmap bitmap = null;
		try {
			istr = assetManager.open(strName);
			bitmap = BitmapFactory.decodeStream(istr);
		} catch (IOException e) {
			return null;
		}

		return bitmap;
	}
}
