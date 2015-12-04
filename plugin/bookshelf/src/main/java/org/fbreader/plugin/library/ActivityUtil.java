package org.fbreader.plugin.library;

import android.annotation.TargetApi;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.os.Build;
import android.util.TypedValue;

public abstract class ActivityUtil {
	public static int setup(Activity activity, boolean dialog) {
		return selectTheme(activity, dialog);
	}

	public static int currentThemeId(Activity activity, boolean dialog) {
		final String name = PreferenceManager.getDefaultSharedPreferences(activity)
			.getString("color_scheme", "indigo");

		switch (name) {
			default:
				return dialog ? R.style.Theme_Library_Indigo_Dialog : R.style.Theme_Library_Indigo;
			case "pink":
				return dialog ? R.style.Theme_Library_Pink_Dialog : R.style.Theme_Library_Pink;
			case "teal":
				return dialog ? R.style.Theme_Library_Teal_Dialog : R.style.Theme_Library_Teal;
			case "green":
				return dialog ? R.style.Theme_Library_Green_Dialog : R.style.Theme_Library_Green;
			case "grey_light":
				return dialog ? R.style.Theme_Library_Grey_Dialog : R.style.Theme_Library_Grey;
			case "grey_dark":
				return dialog ? R.style.Theme_Library_Dark_Dialog : R.style.Theme_Library_Dark;
		}
	}

	public static int getColorFromAttribute(Activity activity, int attributeId) {
		final TypedValue typedValue = new TypedValue();
		activity.getTheme().resolveAttribute(attributeId, typedValue, true);
		return typedValue.data;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static void setStatusBarColor(Activity activity) {
		activity.getWindow().setStatusBarColor(
			getColorFromAttribute(activity, android.R.attr.colorPrimaryDark)
		);
	}

	private static int selectTheme(Activity activity, boolean dialog) {
		final int themeId = currentThemeId(activity, dialog);

		activity.setTheme(themeId);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			setStatusBarColor(activity);
		}

		return themeId;
	}
}
