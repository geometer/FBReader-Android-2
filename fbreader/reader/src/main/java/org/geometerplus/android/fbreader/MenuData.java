/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader;

import java.util.*;

import android.os.Build;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.ZLIntegerOption;

import org.fbreader.reader.ActionCode;
import org.fbreader.reader.R;

import org.geometerplus.android.fbreader.api.MenuNode;
import org.geometerplus.android.util.DeviceType;

public abstract class MenuData {
	private static List<MenuNode> ourNodes;
	private static final Map<String,ZLIntegerOption> ourNodeOptions =
		new HashMap<String,ZLIntegerOption>();
	private static final Map<Location,Integer> ourSizes = new HashMap<Location,Integer>();
	private static final Map<String,Integer> ourDefaultValues = new HashMap<String,Integer>();
	private static final Map<String,String> ourConfigCodes = new HashMap<String,String>();
	private static final Map<String,Set<Location>> ourGroups = new HashMap<String,Set<Location>>();

	private static final String CONFIG_CODE_DAY_NIGHT = "dayNight";
	private static final String CONFIG_CODE_CHANGE_FONT_SIZE = "changeFontSize";
	private static final String CONFIG_CODE_PREMIUM = "premium";

	public enum Location {
		bookMenuUpperSection(0),
		bookMenuLowerSection(1000),
		toolbarOrMainMenu(2000),
		mainMenu(3000),
		disabled(100000);

		static Location byIndex(int index) {
			Location previous = disabled;
			for (Location location : values()) {
				if (index < location.StartIndex) {
					return previous;
				}
				previous = location;
			}
			return disabled;
		}

		public final int StartIndex;

		private Location(int startIndex) {
			StartIndex = startIndex;
		}

		public String resourceKey() {
			return name();
		}

		static final Set<Location> GroupAny = Collections.unmodifiableSet(
			new HashSet<Location>(Arrays.asList(Location.values()))
		);
		static final Set<Location> GroupAlwaysEnabled;
		static {
			final Set<Location> group = new HashSet<Location>(GroupAny);
			group.remove(Location.disabled);
			GroupAlwaysEnabled = Collections.unmodifiableSet(group);
		}
		static final Set<Location> GroupMainMenuOnly = Collections.unmodifiableSet(
			new HashSet<Location>(Arrays.asList(Location.mainMenu, Location.disabled))
		);
	}

	private static void addToplevelNode(MenuNode node, Location location) {
		addToplevelNode(node, node.Code, location, Location.GroupAny);
	}

	private static void addToplevelNode(MenuNode node, String configCode, Location location, Set<Location> group) {
		ourConfigCodes.put(node.Code, configCode);
		if (!ourDefaultValues.containsKey(configCode)) {
			Integer index = ourSizes.get(location);
			if (index == null) {
				index = 0;
			}
			ourDefaultValues.put(configCode, location.StartIndex + index);
			ourSizes.put(location, index + 1);
		}
		ourNodes.add(node);
		ourGroups.put(configCode, group);
	}

	private static synchronized List<MenuNode> allTopLevelNodes() {
		if (ourNodes == null) {
			ourNodes = new ArrayList<MenuNode>();

			addToplevelNode(
				new MenuNode.Item(ActionCode.SWITCH_TO_NIGHT_PROFILE, R.drawable.ic_menu_night),
				CONFIG_CODE_DAY_NIGHT,
				Location.toolbarOrMainMenu,
				Location.GroupAny
			);
			addToplevelNode(
				new MenuNode.Item(ActionCode.SWITCH_TO_DAY_PROFILE, R.drawable.ic_menu_day),
				CONFIG_CODE_DAY_NIGHT,
				Location.toolbarOrMainMenu,
				Location.GroupAny
			);
			addToplevelNode(new MenuNode.Item(ActionCode.SEARCH, R.drawable.abc_ic_search_api_mtrl_alpha), Location.toolbarOrMainMenu);
			if (DeviceType.Instance() == DeviceType.YOTA_PHONE) {
				addToplevelNode(new MenuNode.Item(ActionCode.YOTA_SWITCH_TO_BACK_SCREEN, R.drawable.ic_menu_p2b), Location.toolbarOrMainMenu);
				//addToplevelNode(new MenuNode.Item(ActionCode.YOTA_SWITCH_TO_FRONT_SCREEN, R.drawable.ic_menu_p2b), Location.toolbarOrMainMenu);
			}

			addToplevelNode(
				new MenuNode.Item(ActionCode.SHOW_BOOK_INFO, R.drawable.ic_menu_info),
				Location.bookMenuUpperSection
			);
			addToplevelNode(
				new MenuNode.Item(ActionCode.SHOW_TOC, R.drawable.ic_menu_toc),
				Location.bookMenuUpperSection
			);
			addToplevelNode(
				new MenuNode.Item(ActionCode.SHOW_BOOKMARKS, R.drawable.ic_menu_bookmarks),
				Location.bookMenuUpperSection
			);
			addToplevelNode(
				new MenuNode.Item(ActionCode.SHARE_BOOK, R.drawable.ic_menu_share),
				Location.bookMenuUpperSection
			);
			addToplevelNode(
				new MenuNode.Item(ActionCode.GOTO_PAGE_NUMBER, R.drawable.ic_menu_goto),
				Location.bookMenuUpperSection
			);

			addToplevelNode(
				new MenuNode.Item(ActionCode.SHOW_LIBRARY, R.drawable.ic_menu_library),
				Location.bookMenuLowerSection
			);
			addToplevelNode(
				new MenuNode.Item(ActionCode.SHOW_NETWORK_LIBRARY, R.drawable.ic_menu_networklibrary),
				Location.bookMenuLowerSection
			);

			final MenuNode.Submenu orientations = new MenuNode.Submenu("screenOrientation", R.drawable.ic_menu_orientation);
			orientations.Children.add(new MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM));
			orientations.Children.add(new MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_SENSOR));
			orientations.Children.add(new MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT));
			orientations.Children.add(new MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE));
			if (ZLibrary.Instance().supportsAllOrientations()) {
				orientations.Children.add(new MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT));
				orientations.Children.add(new MenuNode.Item(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
			}
			addToplevelNode(orientations, orientations.Code, Location.mainMenu, Location.GroupMainMenuOnly);
			addToplevelNode(
				new MenuNode.Item(ActionCode.INCREASE_FONT, R.drawable.ic_menu_zoom_in),
				CONFIG_CODE_CHANGE_FONT_SIZE,
				Location.mainMenu,
				Location.GroupAny
			);
			addToplevelNode(
				new MenuNode.Item(ActionCode.DECREASE_FONT, R.drawable.ic_menu_zoom_out),
				CONFIG_CODE_CHANGE_FONT_SIZE,
				Location.mainMenu,
				Location.GroupAny
			);
			addToplevelNode(
				new MenuNode.Item(ActionCode.SHOW_PREFERENCES, R.drawable.ic_menu_settings),
				ActionCode.SHOW_PREFERENCES,
				Location.mainMenu,
				Location.GroupAlwaysEnabled
			);
			addToplevelNode(new MenuNode.Item(ActionCode.INSTALL_PLUGINS, R.drawable.ic_menu_plugins), Location.mainMenu);
			addToplevelNode(new MenuNode.Item(ActionCode.SHOW_WHATSNEW_DIALOG, R.drawable.ic_menu_whatsnew), Location.mainMenu);
			addToplevelNode(new MenuNode.Item(ActionCode.OPEN_WEB_HELP, R.drawable.ic_menu_help), Location.mainMenu);
			addToplevelNode(new MenuNode.Item(ActionCode.OPEN_START_SCREEN, R.drawable.ic_menu_home), Location.mainMenu);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				addToplevelNode(
					new MenuNode.Item(ActionCode.INSTALL_PREMIUM, R.drawable.ic_menu_premium),
					CONFIG_CODE_PREMIUM,
					Location.mainMenu,
					Location.GroupAny
				);
				addToplevelNode(
					new MenuNode.Item(ActionCode.OPEN_PREMIUM, R.drawable.ic_menu_premium),
					CONFIG_CODE_PREMIUM,
					Location.mainMenu,
					Location.GroupAny
				);
			}
			ourNodes = Collections.unmodifiableList(ourNodes);
		}
		return ourNodes;
	}

	public static String code(MenuNode node) {
		return ourConfigCodes.get(node.Code);
	}

	private static class MenuComparator implements Comparator<MenuNode> {
		@Override
		public int compare(MenuNode lhs, MenuNode rhs) {
			return nodeOption(code(lhs)).getValue() - nodeOption(code(rhs)).getValue();
		}
	}

	public static synchronized List<MenuNode> topLevelNodes(Location location) {
		final List<MenuNode> nodes = new ArrayList<MenuNode>();
		for (MenuNode n : allTopLevelNodes()) {
			if (Location.byIndex(nodeOption(code(n)).getValue()) == location) {
				nodes.add(n);
			}
		}
		Collections.<MenuNode>sort(nodes, new MenuComparator());
		return nodes;
	}

	public static ZLIntegerOption nodeOption(String code) {
		synchronized (ourNodeOptions) {
			ZLIntegerOption option = ourNodeOptions.get(code);
			if (option == null) {
				final Integer defaultValue = ourDefaultValues.get(code);
				option = new ZLIntegerOption(
					"ReadingModeMenu",
					code,
					defaultValue != null ? defaultValue : Location.disabled.StartIndex
				);
				ourNodeOptions.put(code, option);
			}
			return option;
		}
	}

	public static Set<Location> locationGroup(String code) {
		final Set<Location> group = ourGroups.get(code);
		return group != null ? group : Collections.singleton(Location.disabled);
	}
}
