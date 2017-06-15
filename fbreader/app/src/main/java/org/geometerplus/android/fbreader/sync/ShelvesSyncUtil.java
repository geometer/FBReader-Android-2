/*
 * Copyright (C) 2010-2017 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.sync;

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.fbreader.common.options.SyncOptions;

import org.geometerplus.zlibrary.core.network.JsonRequest2;

import org.geometerplus.fbreader.book.*;

class ShelvesSyncUtil {
	static void sync(SyncNetworkContext context, final IBookCollection<Book> collection) {
		try {
			final Map<String,Object> info = info(collection);
			final List<String> deleted = (List<String>)info.get("deleted");
			context.perform(new JsonRequest2(
				SyncOptions.BASE_URL + "sync/shelves.exchange", info
			) {
				@Override
				public void processResponse(Object response) {
					updateLocalBookmarks(collection, (Map<String,Object>)response, deleted);
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static void updateLocalBookmarks(IBookCollection<Book> collection, Map<String,Object> data, List<String> deleted) {
		for (Map<String,Object> labelInfo : (List<Map<String,Object>>)data.get("labels")) {
			final String name = (String)labelInfo.get("name");
			if (name == null) {
				continue;
			}
			for (Map<String,Object> info : (List<Map<String,Object>>)labelInfo.get("bookLabels")) {
				final String uid = (String)info.get("uid");
				if (uid == null) {
					continue;
				}
				Book book = null;
				for (String hash : (List<String>)info.get("book")) {
					book = collection.getBookByHash(hash);
					if (book != null) {
						break;
					}
				}
				if (book != null) {
					final Label label = book.findLabel(name);
					if (label == null) {
						book.addLabel(new Label(uid, name));
					} else if (!uid.equals(label.Uid)) {
						book.removeLabel(label);
						book.addLabel(new Label(uid, name));
					}
					collection.saveBook(book);
				} else {
					//System.err.println("BOOK NOT FOUND");
				}
			}
		}
		for (String uuid : (List<String>)data.get("deleted")) {
			collection.deleteBookLabelByUuid(uuid);
			deleted.add(uuid);
		}
		collection.purgeBookLabels(deleted);
	}

	private static Map<Object,Object> createMap(Object ... keysAndValues) {
		switch (keysAndValues.length) {
			case 0:
				return Collections.emptyMap();
			case 2:
				return Collections.singletonMap(keysAndValues[0], keysAndValues[1]);
		}
		if (keysAndValues.length % 2 == 1) {
			throw new IllegalArgumentException("Odd number of createMap arguments");
		}

		final Map<Object,Object> map = new HashMap<Object,Object>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			map.put(keysAndValues[i], keysAndValues[i + 1]);
		}
		return map;
	}

	private static Map<String,Object> info(IBookCollection<Book> collection) {
		final Map<String,Object> result = new HashMap<String,Object>();

		final List<Object> labels = new ArrayList<Object>();
		for (String l : collection.labels()) {
			if (!Book.FAVORITE_LABEL.equals(l) && !l.startsWith("custom_")) {
				continue;
			}
			final List<Object> infos = new ArrayList<Object>();
			for (BookQuery q = new BookQuery(new Filter.ByLabel(l), 20);; q = q.next()) {
				final List<Book> books = collection.books(q);
				if (books.isEmpty()) {
					break;
				}
				for (Book b : books) {
					for (Label bookLabel: b.labels()) {
						if (l.equals(bookLabel.Name)) {
							final String hash = collection.getHash(b, true);
							if (bookLabel.Uid != null && hash != null) {
								infos.add(createMap(
									"uid", bookLabel.Uid,
									"book", hash
								));
							}
							break;
						}
					}
				}
			}
			String uuid;
			try {
				uuid = UUID.nameUUIDFromBytes(l.getBytes("utf-8")).toString();
			} catch (UnsupportedEncodingException e) {
				uuid = UUID.nameUUIDFromBytes(l.getBytes()).toString();
			}
			labels.add(createMap(
				"uid", uuid,
				"name", l,
				"bookLabels", infos
			));
		}
		result.put("labels", labels);

		final List<String> deleted = new ArrayList<String>();
		for (int page = 0;; ++page) {
			final List<String> uids = collection.deletedBookLabelUids(50, page);
			if (uids.isEmpty()) {
				break;
			}
			deleted.addAll(uids);
		}
		result.put("deleted", deleted);

		return result;
	}
}
