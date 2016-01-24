package org.fbreader.plugin.library;

import android.annotation.TargetApi;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.os.Build;
import android.util.TypedValue;

public abstract class ActivityUtil {
	public enum ActivityType {
		Dialog,
		Full,
		Main
	}

	public static int setup(Activity activity, ActivityType type) {
		return selectTheme(activity, type);
	}

	public static int currentThemeId(Activity activity, ActivityType type) {
		final String name = PreferenceManager.getDefaultSharedPreferences(activity)
			.getString("color_scheme", "indigo");

		switch (type) {
			default:
			case Full:
				switch (name) {
					default:
						return R.style.Theme_Library_Indigo;
					case "pink":
						return R.style.Theme_Library_Pink;
					case "teal":
						return R.style.Theme_Library_Teal;
					case "green":
						return R.style.Theme_Library_Green;
					case "grey_light":
						return R.style.Theme_Library_Grey;
					case "grey_dark":
						return R.style.Theme_Library_Dark;
				}
			case Dialog:
				switch (name) {
					default:
						return R.style.Theme_Library_Indigo_Dialog;
					case "pink":
						return R.style.Theme_Library_Pink_Dialog;
					case "teal":
						return R.style.Theme_Library_Teal_Dialog;
					case "green":
						return R.style.Theme_Library_Green_Dialog;
					case "grey_light":
						return R.style.Theme_Library_Grey_Dialog;
					case "grey_dark":
						return R.style.Theme_Library_Dark_Dialog;
				}
			case Main:
				switch (name) {
					default:
						return R.style.Theme_Library_Main_Indigo;
					case "pink":
						return R.style.Theme_Library_Main_Pink;
					case "teal":
						return R.style.Theme_Library_Main_Teal;
					case "green":
						return R.style.Theme_Library_Main_Green;
					case "grey_light":
						return R.style.Theme_Library_Main_Grey;
					case "grey_dark":
						return R.style.Theme_Library_Main_Dark;
				}
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

	private static int selectTheme(Activity activity, ActivityType type) {
		final int themeId = currentThemeId(activity, type);

		activity.setTheme(themeId);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			setStatusBarColor(activity);
		}

		return themeId;
	}
}
