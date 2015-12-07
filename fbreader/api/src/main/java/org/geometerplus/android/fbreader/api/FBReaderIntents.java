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

package org.geometerplus.android.fbreader.api;

import java.lang.reflect.Field;

import android.content.Intent;

import org.geometerplus.fbreader.book.*;

public abstract class FBReaderIntents {
	public static String DEFAULT_PACKAGE = "org.geometerplus.zlibrary.ui.android";

	public abstract static class Action {
		public static String API                              = "android.fbreader.action.API";
		public static String API_CALLBACK                     = "android.fbreader.action.API_CALLBACK";
		public static String VIEW                             = "android.fbreader.action.VIEW";
		public static String CANCEL_MENU                      = "android.fbreader.action.CANCEL_MENU";
		public static String CONFIG_SERVICE                   = "android.fbreader.action.CONFIG_SERVICE";
		public static String LIBRARY_SERVICE                  = "android.fbreader.action.LIBRARY_SERVICE";
		public static String BOOK_INFO                        = "android.fbreader.action.BOOK_INFO";
		public static String LIBRARY                          = "android.fbreader.action.LIBRARY";
		public static String EXTERNAL_LIBRARY                 = "android.fbreader.action.EXTERNAL_LIBRARY";
		public static String BOOKMARKS                        = "android.fbreader.action.BOOKMARKS";
		public static String EXTERNAL_BOOKMARKS               = "android.fbreader.action.EXTERNAL_BOOKMARKS";
		public static String PREFERENCES                      = "android.fbreader.action.PREFERENCES";
		public static String NETWORK_LIBRARY                  = "android.fbreader.action.NETWORK_LIBRARY";
		public static String OPEN_NETWORK_CATALOG             = "android.fbreader.action.OPEN_NETWORK_CATALOG";
		public static String ERROR                            = "android.fbreader.action.ERROR";
		public static String CRASH                            = "android.fbreader.action.CRASH";
		public static String PLUGIN                           = "android.fbreader.action.PLUGIN";
		public static String CLOSE                            = "android.fbreader.action.CLOSE";
		public static String PLUGIN_CRASH                     = "android.fbreader.action.PLUGIN_CRASH";
		public static String EDIT_STYLES                      = "android.fbreader.action.EDIT_STYLES";
		public static String EDIT_BOOKMARK                    = "android.fbreader.action.EDIT_BOOKMARK";
		public static String SWITCH_YOTA_SCREEN               = "android.fbreader.action.SWITCH_YOTA_SCREEN";

		public static String SYNC_START                       = "android.fbreader.action.sync.START";
		public static String SYNC_STOP                        = "android.fbreader.action.sync.STOP";
		public static String SYNC_SYNC                        = "android.fbreader.action.sync.SYNC";
		public static String SYNC_QUICK_SYNC                  = "android.fbreader.action.sync.QUICK_SYNC";

		public static String PLUGIN_VIEW                      = "android.fbreader.action.plugin.VIEW";
		public static String PLUGIN_KILL                      = "android.fbreader.action.plugin.KILL";
		public static String PLUGIN_CONNECT_COVER_SERVICE     = "android.fbreader.action.plugin.CONNECT_COVER_SERVICE";
	}

	public abstract static class Event {
		public static String CONFIG_OPTION_CHANGE             = "fbreader.config_service.option_change_event";

		public static String LIBRARY_BOOK                     = "fbreader.library_service.book_event";
		public static String LIBRARY_BUILD                    = "fbreader.library_service.build_event";
		public static String LIBRARY_COVER_READY              = "fbreader.library_service.cover_ready";

		public static String SYNC_UPDATED                     = "android.fbreader.event.sync.UPDATED";
	}

	public interface Key {
		String BOOK                             = "fbreader.book";
		String BOOKMARK                         = "fbreader.bookmark";
		String PLUGIN                           = "fbreader.plugin";
		String TYPE                             = "fbreader.type";
	}

	public static void initPremium() {
		DEFAULT_PACKAGE = FBReaderIntentsPremium.DEFAULT_PACKAGE;
		try {
			for (Field field : Action.class.getFields()) {
				field.set(
					null, FBReaderIntentsPremium.Action.class.getField(field.getName()).get(null)
				);
			}
			for (Field field : Event.class.getFields()) {
				field.set(
					null, FBReaderIntentsPremium.Event.class.getField(field.getName()).get(null)
				);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static Intent defaultInternalIntent(String action) {
		return internalIntent(action).addCategory(Intent.CATEGORY_DEFAULT);
	}

	public static Intent internalIntent(String action) {
		return new Intent(action).setPackage(DEFAULT_PACKAGE);
	}

	public static void putBookExtra(Intent intent, String key, Book book) {
		intent.putExtra(key, SerializerUtil.serialize(book));
	}

	public static void putBookExtra(Intent intent, Book book) {
		putBookExtra(intent, Key.BOOK, book);
	}

	public static <B extends AbstractBook> B getBookExtra(Intent intent, String key, AbstractSerializer.BookCreator<B> creator) {
		return SerializerUtil.deserializeBook(intent.getStringExtra(key), creator);
	}

	public static <B extends AbstractBook> B getBookExtra(Intent intent, AbstractSerializer.BookCreator<B> creator) {
		return getBookExtra(intent, Key.BOOK, creator);
	}

	public static void putBookmarkExtra(Intent intent, String key, Bookmark bookmark) {
		intent.putExtra(key, SerializerUtil.serialize(bookmark));
	}

	public static void putBookmarkExtra(Intent intent, Bookmark bookmark) {
		putBookmarkExtra(intent, Key.BOOKMARK, bookmark);
	}

	public static Bookmark getBookmarkExtra(Intent intent, String key) {
		return SerializerUtil.deserializeBookmark(intent.getStringExtra(key));
	}

	public static Bookmark getBookmarkExtra(Intent intent) {
		return getBookmarkExtra(intent, Key.BOOKMARK);
	}
}
