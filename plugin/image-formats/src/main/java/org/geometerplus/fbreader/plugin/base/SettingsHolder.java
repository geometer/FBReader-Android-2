package org.geometerplus.fbreader.plugin.base;

import org.fbreader.reader.options.ColorProfile;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.*;
import org.geometerplus.zlibrary.core.view.ZLViewEnums;

public class SettingsHolder {
	SettingsHolder(ZLibrary zlibrary) {
		final int dpi = zlibrary.getDisplayDPI();
		final int x = zlibrary.getWidthInPixels();
		final int y = zlibrary.getHeightInPixels();

		final int horMargin = Math.min(dpi / 5, Math.min(x, y) / 30);
		TwoColumnView =
			new ZLBooleanOption("Options", "TwoColumnView", x * x + y * y >= 42 * dpi * dpi);
		LeftMargin =
			new ZLIntegerRangeOption("Options", "LeftMargin", 0, 100, horMargin);
		RightMargin =
			new ZLIntegerRangeOption("Options", "RightMargin", 0, 100, horMargin);
		TopMargin =
			new ZLIntegerRangeOption("Options", "TopMargin", 0, 100, 15);
		BottomMargin =
			new ZLIntegerRangeOption("Options", "BottomMargin", 0, 100, 20);
		SpaceBetweenColumns =
			new ZLIntegerRangeOption("Options", "SpaceBetweenColumns", 0, 300, 3 * horMargin);
		ScrollbarType =
			new ZLIntegerRangeOption("Options", "ScrollbarType", 0, 4, SCROLLBAR_SHOW_AS_FOOTER);
		FooterHeight =
			new ZLIntegerRangeOption("Options", "FooterHeight", 8, dpi / 8, dpi / 20);
		ColorProfileName =
			new ZLStringOption("Options", "ColorProfile", ColorProfile.DAY);
		ColorProfileName.setSpecialName("colorProfile");
		ShowTOCMarks =
			new ZLBooleanOption("Options", "FooterShowTOCMarks", true);
		ShowClock =
			new ZLBooleanOption("Options", "ShowClockInFooter", true);
		ShowBattery =
			new ZLBooleanOption("Options", "ShowBatteryInFooter", true);
		ShowProgress =
			new ZLBooleanOption("Options", "ShowProgressInFooter", true);
		Font =
			new ZLStringOption("Options", "FooterFont", "Droid Sans");
	}

	// View
	public final ZLBooleanOption TwoColumnView;
	public final ZLIntegerRangeOption ScrollbarType;
	public final ZLIntegerRangeOption FooterHeight;
	public final ZLStringOption ColorProfileName;

	public final ZLIntegerRangeOption LeftMargin;
	public final ZLIntegerRangeOption RightMargin;
	public final ZLIntegerRangeOption TopMargin;
	public final ZLIntegerRangeOption BottomMargin;
	public final ZLIntegerRangeOption SpaceBetweenColumns;

	public static final int SCROLLBAR_HIDE = 0;
	public static final int SCROLLBAR_SHOW = 1;
	public static final int SCROLLBAR_SHOW_AS_PROGRESS = 2;
	public static final int SCROLLBAR_SHOW_AS_FOOTER = 3;
	public static final int SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE = 4;

	// Footer
	public final ZLBooleanOption ShowTOCMarks;
	public final ZLBooleanOption ShowClock;
	public final ZLBooleanOption ShowBattery;
	public final ZLBooleanOption ShowProgress;
	public final ZLStringOption Font;

	// Color
	private ColorProfile myColorProfile;
	public ColorProfile getColorProfile() {
		final String name = ColorProfileName.getValue();
		if (myColorProfile == null || !name.equals(myColorProfile.Name)) {
			myColorProfile = ColorProfile.get(name);
		}
		return myColorProfile;
	}
}
