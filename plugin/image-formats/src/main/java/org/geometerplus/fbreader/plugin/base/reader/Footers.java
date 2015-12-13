package org.geometerplus.fbreader.plugin.base.reader;

import java.util.*;

import android.text.format.DateFormat;

import org.fbreader.reader.TOCTree;

import org.geometerplus.zlibrary.core.fonts.FontEntry;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.fbreader.plugin.base.SettingsHolder;

class Footers {
	abstract public interface FooterArea {
		int getHeight();
		void paint(ZLPaintContext context);
	}

	public abstract static class Footer implements FooterArea {
		Runnable UpdateTask = new Runnable() {
			public void run() {
				myPluginView.postInvalidate();
			}
		};

		protected final PluginView myPluginView;
		protected ArrayList<TOCTree> myTOCMarks;

		Footer(PluginView pluginView) {
			myPluginView = pluginView;
		}

		public int getHeight() {
			return myPluginView.getSettings().FooterHeight.getValue();
		}

		public synchronized void resetTOCMarks() {
			myTOCMarks = null;
		}

		private final int MAX_TOC_MARKS_NUMBER = 100;
		protected synchronized void updateTOCMarks() {
			myTOCMarks = new ArrayList<TOCTree>();
			TOCTree toc = myPluginView.getTOCTree();
			if (toc == null) {
				return;
			}
			int maxLevel = Integer.MAX_VALUE;
			if (toc.getSize() >= MAX_TOC_MARKS_NUMBER) {
				final int[] sizes = new int[10];
				for (TOCTree tocItem : toc) {
					if (tocItem.Level < 10) {
						++sizes[tocItem.Level];
					}
				}
				for (int i = 1; i < sizes.length; ++i) {
					sizes[i] += sizes[i - 1];
				}
				for (maxLevel = sizes.length - 1; maxLevel >= 0; --maxLevel) {
					if (sizes[maxLevel] < MAX_TOC_MARKS_NUMBER) {
						break;
					}
				}
			}
			for (TOCTree tocItem : toc.allSubtrees(maxLevel)) {
				myTOCMarks.add(tocItem);
			}
		}

		protected String buildInfoString(String separator) {
			final StringBuilder info = new StringBuilder();
			if (myPluginView.getSettings().ShowProgress.getValue()) {
				info.append(myPluginView.getCurPageNo() + 1);
				info.append("/");
				info.append(myPluginView.getPagesNum());
			}
			if (myPluginView.getSettings().ShowClock.getValue()) {
				if (info.length() > 0) {
					info.append(separator);
				}
				info.append(DateFormat.getTimeFormat(myPluginView.getContext()).format(new Date()));
			}
			if (myPluginView.getSettings().ShowBattery.getValue()) {
				if (info.length() > 0) {
					info.append(separator);
				}
				info.append(myPluginView.getBatteryLevel());
				info.append("%");
			}
			return info.toString();
		}

		private List<FontEntry> myFontEntry;
		private Map<String,Integer> myHeightMap = new HashMap<String,Integer>();
		private Map<String,Integer> myCharHeightMap = new HashMap<String,Integer>();
		protected synchronized int setFont(ZLPaintContext context, int height, boolean bold) {
			final String family = myPluginView.getSettings().Font.getValue();
			if (myFontEntry == null || !family.equals(myFontEntry.get(0).Family)) {
				myFontEntry = Collections.singletonList(FontEntry.systemEntry(family));
			}
			final String key = family + (bold ? "N" : "B") + height;
			final Integer cached = myHeightMap.get(key);
			if (cached != null) {
				context.setFont(myFontEntry, cached, bold, false, false, false);
				final Integer charHeight = myCharHeightMap.get(key);
				return charHeight != null ? charHeight : height;
			} else {
				int h = height + 2;
				int charHeight = height;
				final int max = height < 9 ? height - 1 : height - 2;
				for (; h > 5; --h) {
					context.setFont(myFontEntry, h, bold, false, false, false);
					charHeight = context.getCharHeight('H');
					if (charHeight <= max) {
						break;
					}
				}
				myHeightMap.put(key, h);
				myCharHeightMap.put(key, charHeight);
				return charHeight;
			}
		}
	}

	static class FooterOldStyle extends Footer {
		FooterOldStyle(PluginView pluginView) {
			super(pluginView);
		}

		public synchronized void paint(ZLPaintContext context) {
//			final ZLFile wallpaper = getWallpaperFile();
//			if (wallpaper != null) {
//				context.clear(wallpaper, getFillMode());
//			} else {
//				context.clear(getBackgroundColor());
//			}

//			final ZLColor bgColor = getBackgroundColor();
//			TODO: separate color option for footer color
			final SettingsHolder settings = myPluginView.getSettings();
			final ZLColor fgColor = settings.getColorProfile().RegularTextOption.getValue();
			final ZLColor fillColor = settings.getColorProfile().FooterFillOption.getValue();

			final int left = settings.LeftMargin.getValue();
			final int right = context.getWidth() - settings.RightMargin.getValue();
			final int height = getHeight();
			final int lineWidth = height <= 10 ? 1 : 2;
			final int delta = height <= 10 ? 0 : 1;
			setFont(context, height, height > 10);

			// draw info text
			final String infoString = buildInfoString(" ");
			final int infoWidth = context.getStringWidth(infoString);
			context.setTextColor(fgColor);
			context.drawString(right - infoWidth, height - delta, infoString);

			// draw gauge
			final int gaugeRight = right - (infoWidth == 0 ? 0 : infoWidth + 10);
			final int gaugeWidth = gaugeRight - left - 2 * lineWidth;

			context.setLineColor(fgColor);
			context.setLineWidth(lineWidth);
			context.drawLine(left, lineWidth, left, height - lineWidth);
			context.drawLine(left, height - lineWidth, gaugeRight, height - lineWidth);
			context.drawLine(gaugeRight, height - lineWidth, gaugeRight, lineWidth);
			context.drawLine(gaugeRight, lineWidth, left, lineWidth);

			final int gaugeInternalRight =
				left + lineWidth + (int)(1.0 * gaugeWidth * (myPluginView.getCurPageNo() + 1) / myPluginView.getPagesNum());

			context.setFillColor(fillColor);
			context.fillRectangle(left + 1, height - 2 * lineWidth, gaugeInternalRight, lineWidth + 1);

			if (settings.ShowTOCMarks.getValue()) {
				if (myTOCMarks == null) {
					updateTOCMarks();
				}
				final int fullLength = myPluginView.getPagesNum();
				for (TOCTree tocItem : myTOCMarks) {
					if (tocItem.Reference != null && tocItem.Reference != -1) {
						final int refCoord = tocItem.Reference;
						final int xCoord =
							left + 2 * lineWidth + (int)(1.0 * gaugeWidth * refCoord / fullLength);
						context.drawLine(xCoord, height - lineWidth, xCoord, lineWidth);
					}
				}
			}
		}
	}

	static class FooterNewStyle extends Footer {
		FooterNewStyle(PluginView pluginView) {
			super(pluginView);
		}

		public synchronized void paint(ZLPaintContext context) {
			context.clear(new ZLColor(0x44, 0x44, 0x44));

			final ZLColor textColor = new ZLColor(0xBB, 0xBB, 0xBB);
			final ZLColor readColor = new ZLColor(0xBB, 0xBB, 0xBB);
			final ZLColor unreadColor = new ZLColor(0x77, 0x77, 0x77);

			final SettingsHolder settings = myPluginView.getSettings();
			final int left = settings.LeftMargin.getValue();
			final int right = context.getWidth() - settings.RightMargin.getValue();
			final int height = getHeight();
			final int lineWidth = height <= 12 ? 1 : 2;
			final int charHeight = setFont(context, height, height > 12);

			// draw info text
			final String infoString = buildInfoString("  ");
			final int infoWidth = context.getStringWidth(infoString);
			context.setTextColor(textColor);
			context.drawString(right - infoWidth, (height + charHeight + 1) / 2, infoString);

			// draw gauge
			final int gaugeRight = right - (infoWidth == 0 ? 0 : infoWidth + 10);
			final int gaugeInternalRight =
				left + (int)(1.0 * (gaugeRight - left) * (myPluginView.getCurPageNo() + 1) / myPluginView.getPagesNum() + 0.5);
			final int v = height / 2;

			context.setLineWidth(lineWidth);
			context.setLineColor(readColor);
			context.drawLine(left, v, gaugeInternalRight, v);
			if (gaugeInternalRight < gaugeRight) {
				context.setLineColor(unreadColor);
				context.drawLine(gaugeInternalRight + 1, v, gaugeRight, v);
			}

			// draw labels
			if (settings.ShowTOCMarks.getValue()) {
				final TreeSet<Integer> labels = new TreeSet<Integer>();
				labels.add(left);
				labels.add(gaugeRight);
				if (myTOCMarks == null) {
					updateTOCMarks();
				}
				final int fullLength = myPluginView.getPagesNum();
				for (TOCTree tocItem : myTOCMarks) {
					if (tocItem.Reference != null && tocItem.Reference != -1) {
						labels.add(left + (int)(1.0 * (gaugeRight - left) * tocItem.Reference / fullLength + 0.5));
					}
				}
				for (int l : labels) {
					context.setLineColor(l <= gaugeInternalRight ? readColor : unreadColor);
					context.drawLine(l, v + 3, l, v - lineWidth - 2);
				}
			}
		}
	}
}
