/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.config;

import java.util.*;

import android.app.Service;
import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.geometerplus.android.fbreader.api.FBReaderIntents;

final class SQLiteConfig extends ConfigInterface.Stub {
	private final Service myService;

	private final SQLiteDatabase myDatabase;

	public SQLiteConfig(Service service) {
		myService = service;
		myDatabase = service.openOrCreateDatabase("config.db", Context.MODE_PRIVATE, null);
		switch (myDatabase.getVersion()) {
			case 0:
				myDatabase.execSQL("CREATE TABLE IF NOT EXISTS config (groupName VARCHAR, name VARCHAR, value VARCHAR, PRIMARY KEY(groupName, name) )");
				break;
			case 1:
				myDatabase.beginTransaction();
				final SQLiteStatement removeStatement = myDatabase.compileStatement(
					"DELETE FROM config WHERE name = ? AND groupName LIKE ?"
				);
				try {
					removeStatement.bindString(2, "/%");
					removeStatement.bindString(1, "Size"); removeStatement.execute();
					removeStatement.bindString(1, "Title"); removeStatement.execute();
					removeStatement.bindString(1, "Language"); removeStatement.execute();
					removeStatement.bindString(1, "Encoding"); removeStatement.execute();
					removeStatement.bindString(1, "AuthorSortKey"); removeStatement.execute();
					removeStatement.bindString(1, "AuthorDisplayName"); removeStatement.execute();
					removeStatement.bindString(1, "EntriesNumber"); removeStatement.execute();
					removeStatement.bindString(1, "TagList"); removeStatement.execute();
					removeStatement.bindString(1, "Sequence"); removeStatement.execute();
					removeStatement.bindString(1, "Number in seq"); removeStatement.execute();
				} finally {
					removeStatement.close();
				}
				myDatabase.execSQL(
					"DELETE FROM config WHERE name LIKE 'Entry%' AND groupName LIKE '/%'"
				);
				myDatabase.setTransactionSuccessful();
				myDatabase.endTransaction();
				myDatabase.execSQL("VACUUM");
				SQLiteDatabase.releaseMemory();
				break;
		}
		myDatabase.setVersion(2);
	}

	@Override
	synchronized public List<String> listGroups() {
		final LinkedList<String> list = new LinkedList<String>();
		final Cursor cursor = myDatabase.rawQuery("SELECT DISTINCT groupName FROM config", null);
		try {
			while (cursor.moveToNext()) {
				list.add(cursor.getString(0));
			}
		} finally {
			cursor.close();
		}
		return list;
	}

	@Override
	synchronized public List<String> listNames(String group) {
		final LinkedList<String> list = new LinkedList<String>();
		final Cursor cursor = myDatabase.rawQuery("SELECT name FROM config WHERE groupName = ?", new String[] { group });
		try {
			while (cursor.moveToNext()) {
				list.add(cursor.getString(0));
			}
		} finally {
			cursor.close();
		}
		return list;
	}

	@Override
	synchronized public void removeGroup(String name) {
		myDatabase.delete("config", "groupName=?", new String[] { name });
	}

	@Override
	synchronized public List<String> requestAllValuesForGroup(String group) {
		try {
			final List<String> pairs = new LinkedList<String>();
			final Cursor cursor = myDatabase.rawQuery(
				"SELECT name,value FROM config WHERE groupName = ?",
				new String[] { group }
			);
			try {
				while (cursor.moveToNext()) {
					pairs.add(cursor.getString(0) + "\000" + cursor.getString(1));
				}
			} finally {
				cursor.close();
			}
			return pairs;
		} catch (SQLException e) {
			return Collections.emptyList();
		}
	}

	@Override
	synchronized public String getValue(String group, String name) {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT value FROM config WHERE groupName=? AND name=?",
			new String[] { group, name }
		);
		try {
			return cursor.moveToNext() ? cursor.getString(0) : null;
		} finally {
			cursor.close();
		}
	}

	@Override
	synchronized public void setValue(String group, String name, String value) {
		final ContentValues values = new ContentValues();
		values.put("groupName", group);
		values.put("name", name);
		values.put("value", value);
		myDatabase.insertWithOnConflict("config", null, values, SQLiteDatabase.CONFLICT_REPLACE);
		sendChangeEvent(group, name, value);
	}

	@Override
	synchronized public void unsetValue(String group, String name) {
		myDatabase.delete("config", "groupName=? AND name=?", new String[] { group, name });
		sendChangeEvent(group, name, null);
	}

	private void sendChangeEvent(String group, String name, String value) {
		myService.sendBroadcast(
			new Intent(FBReaderIntents.Event.CONFIG_OPTION_CHANGE)
				.putExtra("group", group)
				.putExtra("name", name)
				.putExtra("value", value)
		);
	}
}
