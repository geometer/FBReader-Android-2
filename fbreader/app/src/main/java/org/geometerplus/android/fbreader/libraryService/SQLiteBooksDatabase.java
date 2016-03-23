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

package org.geometerplus.android.fbreader.libraryService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.*;
import android.database.SQLException;
import android.database.Cursor;

import org.fbreader.util.IOUtil;

import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.options.ZLIntegerOption;
import org.geometerplus.zlibrary.core.util.RationalNumber;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPositionWithTimestamp;

import org.geometerplus.fbreader.book.*;

import org.geometerplus.android.util.SQLiteUtil;

final class SQLiteBooksDatabase extends BooksDatabase {
	private final SQLiteDatabase myDatabase;

	SQLiteBooksDatabase(Context context) {
		myDatabase = context.openOrCreateDatabase("books.db", Context.MODE_PRIVATE, null);
		migrate();
	}

	private int myIterationCounter;
	private void releaseMemory() {
		if (++myIterationCounter % 128 == 0) {
			SQLiteDatabase.releaseMemory();
		}
	}

	@Override
	public void finalize() {
		IOUtil.closeQuietly(myDatabase);
	}

	protected void executeAsTransaction(Runnable actions) {
		boolean transactionStarted = false;
		try {
			myDatabase.beginTransaction();
			transactionStarted = true;
		} catch (Throwable t) {
		}
		try {
			actions.run();
			if (transactionStarted) {
				myDatabase.setTransactionSuccessful();
			}
		} finally {
			if (transactionStarted) {
				myDatabase.endTransaction();
			}
		}
	}

	private void migrate() {
		final int version = myDatabase.getVersion();
		final int currentVersion = 41;
		if (version >= currentVersion) {
			return;
		}

		myDatabase.beginTransaction();

		switch (version) {
			case 0:
				createTables();
			case 1:
				updateTables1();
			case 2:
				updateTables2();
			case 3:
				updateTables3();
			case 4:
				updateTables4();
			case 5:
				updateTables5();
			case 6:
				updateTables6();
			case 7:
				updateTables7();
			case 8:
				updateTables8();
			case 9:
				updateTables9();
			case 10:
				updateTables10();
			case 11:
				updateTables11();
			case 12:
				updateTables12();
			case 13:
				updateTables13();
			case 14:
				updateTables14();
			case 15:
				updateTables15();
			case 16:
				updateTables16();
			case 17:
				updateTables17();
			case 18:
				updateTables18();
			case 19:
				updateTables19();
			case 20:
				updateTables20();
			case 21:
				updateTables21();
			case 22:
				updateTables22();
			case 23:
				updateTables23();
			case 24:
				updateTables24();
			case 25:
				updateTables25();
			case 26:
				updateTables26();
			case 27:
				updateTables27();
			case 28:
				updateTables28();
			case 29:
				updateTables29();
			case 30:
				updateTables30();
			case 31:
				updateTables31();
			case 32:
				updateTables32();
			case 33:
				updateTables33();
			case 34:
				updateTables34();
			case 35:
				updateTables35();
			case 36:
				updateTables36();
			case 37:
				updateTables37();
			case 38:
				updateTables38();
			case 39:
				updateTables39();
			case 40:
				updateTables40();
		}
		myDatabase.setTransactionSuccessful();
		myDatabase.setVersion(currentVersion);
		myDatabase.endTransaction();

		myDatabase.execSQL("VACUUM");
		SQLiteDatabase.releaseMemory();
	}

	@Override
	protected String getOptionValue(String name) {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT value FROM Options WHERE name=? LIMIT 1", new String[] { name }
		);
		try {
			return cursor.moveToNext() ? cursor.getString(0) : null;
		} finally {
			cursor.close();
		}
	}

	@Override
	protected void setOptionValue(String name, String value) {
		final ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("value", value);
		myDatabase.insertWithOnConflict("Options", null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	@Override
	protected DbBook loadBook(long bookId) {
		final Cursor cursor = myDatabase.rawQuery("SELECT file_id,title,encoding,language FROM Books WHERE book_id=? LIMIT 1", new String[] { String.valueOf(bookId) });
		try {
			if (cursor.moveToNext()) {
				return createBook(
					bookId, cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)
				);
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	@Override
	protected DbBook loadBookByFile(long fileId, ZLFile file) {
		if (fileId == -1) {
			return null;
		}
		final Cursor cursor = myDatabase.rawQuery("SELECT book_id,title,encoding,language FROM Books WHERE file_id=? LIMIT 1", new String[] { String.valueOf(fileId) });
		try {
			if (cursor.moveToNext()) {
				return createBook(
					cursor.getLong(0),
					file,
					cursor.getString(1),
					cursor.getString(2),
					cursor.getString(3)
				);
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	private boolean myTagCacheIsInitialized;
	private final HashMap<Tag,Long> myIdByTag = new HashMap<Tag,Long>();
	private final HashMap<Long,Tag> myTagById = new HashMap<Long,Tag>();

	private void initTagCache() {
		if (myTagCacheIsInitialized) {
			return;
		}
		myTagCacheIsInitialized = true;

		final Cursor cursor = myDatabase.rawQuery("SELECT tag_id,parent_id,name FROM Tags ORDER BY tag_id", null);
		try {
			while (cursor.moveToNext()) {
				long id = cursor.getLong(0);
				if (myTagById.get(id) == null) {
					final Tag tag = Tag.getTag(myTagById.get(cursor.getLong(1)), cursor.getString(2));
					myIdByTag.put(tag, id);
					myTagById.put(id, tag);
				}
			}
		} finally {
			cursor.close();
		}
	}

	@Override
	protected Map<Long,DbBook> loadBooks(FileInfoSet infos, boolean existing) {
		final HashMap<Long,DbBook> booksById = new HashMap<Long,DbBook>();
		final HashMap<Long,DbBook> booksByFileId = new HashMap<Long,DbBook>();
		Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id,file_id,title,encoding,language FROM Books WHERE `exists` = " + (existing ? 1 : 0), null
		);
		try {
			while (cursor.moveToNext()) {
				final long id = cursor.getLong(0);
				final long fileId = cursor.getLong(1);
				final DbBook book = createBook(
					id, infos.getFile(fileId), cursor.getString(2), cursor.getString(3), cursor.getString(4)
				);
				if (book != null) {
					booksById.put(id, book);
					booksByFileId.put(fileId, book);
				}
			}
		} finally {
			cursor.close();
		}

		initTagCache();

		cursor = myDatabase.rawQuery(
			"SELECT author_id,name,sort_key FROM Authors", null
		);
		final HashMap<Long,Author> authorById = new HashMap<Long,Author>();
		try {
			while (cursor.moveToNext()) {
				authorById.put(cursor.getLong(0), new Author(cursor.getString(1), cursor.getString(2)));
			}
		} finally {
			cursor.close();
		}

		cursor = myDatabase.rawQuery(
			"SELECT book_id,author_id FROM BookAuthor ORDER BY author_index", null
		);
		try {
			while (cursor.moveToNext()) {
				final DbBook book = booksById.get(cursor.getLong(0));
				if (book != null) {
					Author author = authorById.get(cursor.getLong(1));
					if (author != null) {
						addAuthor(book, author);
					}
				}
			}
		} finally {
			cursor.close();
		}

		cursor = myDatabase.rawQuery("SELECT book_id,tag_id FROM BookTag", null);
		try {
			while (cursor.moveToNext()) {
				final DbBook book = booksById.get(cursor.getLong(0));
				if (book != null) {
					addTag(book, getTagById(cursor.getLong(1)));
				}
			}
		} finally {
			cursor.close();
		}

		cursor = myDatabase.rawQuery(
			"SELECT series_id,name FROM Series", null
		);
		final HashMap<Long,String> seriesById = new HashMap<Long,String>();
		try {
			while (cursor.moveToNext()) {
				seriesById.put(cursor.getLong(0), cursor.getString(1));
			}
		} finally {
			cursor.close();
		}

		cursor = myDatabase.rawQuery(
			"SELECT book_id,series_id,book_index FROM BookSeries", null
		);
		try {
			while (cursor.moveToNext()) {
				final DbBook book = booksById.get(cursor.getLong(0));
				if (book != null) {
					final String series = seriesById.get(cursor.getLong(1));
					if (series != null) {
						setSeriesInfo(book, series, cursor.getString(2));
					}
				}
			}
		} finally {
			cursor.close();
		}

		cursor = myDatabase.rawQuery(
			"SELECT book_id,type,uid FROM BookUid", null
		);
		try {
			while (cursor.moveToNext()) {
				final DbBook book = booksById.get(cursor.getLong(0));
				if (book != null) {
					addUid(book, new UID(cursor.getString(1), cursor.getString(2)));
				}
			}
		} finally {
			cursor.close();
		}

		cursor = myDatabase.rawQuery(
			"SELECT BookLabel.book_id,Labels.name,BookLabel.uid FROM Labels" +
			" INNER JOIN BookLabel ON BookLabel.label_id=Labels.label_id",
			null
		);
		try {
			while (cursor.moveToNext()) {
				final DbBook book = booksById.get(cursor.getLong(0));
				if (book != null) {
					addLabel(book, new Label(cursor.getString(2), cursor.getString(1)));
				}
			}
		} finally {
			cursor.close();
		}

		cursor = myDatabase.rawQuery(
			"SELECT book_id,numerator,denominator FROM BookReadingProgress",
			null
		);
		try {
			while (cursor.moveToNext()) {
				final DbBook book = booksById.get(cursor.getLong(0));
				if (book != null) {
					setProgress(book, RationalNumber.create(cursor.getLong(1), cursor.getLong(2)));
				}
			}
		} finally {
			cursor.close();
		}

		cursor = myDatabase.rawQuery(
			"SELECT book_id FROM Bookmarks WHERE visible = 1 GROUP by book_id",
			null
		);
		try {
			while (cursor.moveToNext()) {
				final DbBook book = booksById.get(cursor.getLong(0));
				if (book != null) {
					book.HasBookmark = true;
				}
			}
		} finally {
			cursor.close();
		}

		return booksByFileId;
	}

	@Override
	protected void setExistingFlag(Collection<DbBook> books, boolean flag) {
		if (books.isEmpty()) {
			return;
		}
		final StringBuilder bookSet = new StringBuilder("(");
		boolean first = true;
		for (DbBook b : books) {
			if (first) {
				first = false;
			} else {
				bookSet.append(",");
			}
			bookSet.append(b.getId());
		}
		bookSet.append(")");
		myDatabase.execSQL(
			"UPDATE Books SET `exists` = " + (flag ? 1 : 0) + " WHERE book_id IN " + bookSet
		);
	}

	@Override
	protected void updateBookInfo(long bookId, long fileId, String encoding, String language, String title) {
		final ContentValues values = new ContentValues();
		values.put("file_id", fileId);
		values.put("encoding", encoding);
		values.put("language", language);
		values.put("title", title);
		myDatabase.updateWithOnConflict(
			"Books", values,
			"book_id=?", new String[] { String.valueOf(bookId) },
			SQLiteDatabase.CONFLICT_IGNORE
		);
	}

	@Override
	protected long insertBookInfo(ZLFile file, String encoding, String language, String title) {
		releaseMemory();

		final FileInfoSet infoSet = new FileInfoSet(this, file);
		final ContentValues values = new ContentValues();
		values.put("file_id", infoSet.getId(file));
		values.put("encoding", encoding);
		values.put("language", language);
		values.put("title", title);
		return myDatabase.insertWithOnConflict(
			"Books", null, values, SQLiteDatabase.CONFLICT_IGNORE
		);
	}

	protected void deleteAllBookAuthors(long bookId) {
		myDatabase.delete("BookAuthor", "book_id=?", new String[] { String.valueOf(bookId) });
	}

	protected void saveBookAuthorInfo(long bookId, long index, Author author) {
		long authorId = -1;

		final Cursor cursor = myDatabase.rawQuery(
			"SELECT author_id FROM Authors WHERE name=? AND sort_key=? LIMIT 1",
			new String[] { author.DisplayName, author.SortKey }
		);
		try {
			if (cursor.moveToNext()) {
				authorId = cursor.getLong(0);
			}
		} finally {
			cursor.close();
		}

		if (authorId == -1) {
			final ContentValues values = new ContentValues();
			values.put("name", author.DisplayName);
			values.put("sort_key", author.SortKey);
			authorId = myDatabase.insertWithOnConflict(
				"Authors", null, values, SQLiteDatabase.CONFLICT_IGNORE
			);
		}

		if (authorId != -1) {
			final ContentValues bookAuthorValues = new ContentValues();
			bookAuthorValues.put("book_id", bookId);
			bookAuthorValues.put("author_id", authorId);
			bookAuthorValues.put("author_index", index);
			long result = myDatabase.insertWithOnConflict(
				"BookAuthor", null, bookAuthorValues, SQLiteDatabase.CONFLICT_REPLACE
			);
		}
	}

	protected List<Author> listAuthors(long bookId) {
		final Cursor cursor = myDatabase.rawQuery("SELECT Authors.name,Authors.sort_key FROM BookAuthor INNER JOIN Authors ON Authors.author_id = BookAuthor.author_id WHERE BookAuthor.book_id = ?", new String[] { String.valueOf(bookId) });
		try {
			if (!cursor.moveToNext()) {
				return null;
			}
			final ArrayList<Author> list = new ArrayList<Author>();
			do {
				list.add(new Author(cursor.getString(0), cursor.getString(1)));
			} while (cursor.moveToNext());
			return list;
		} finally {
			cursor.close();
		}
	}

	private long getTagId(Tag tag) {
		Long id = myIdByTag.get(tag);
		if (id != null) {
			return id;
		}

		final SQLiteStatement statement = myDatabase.compileStatement(
			"SELECT tag_id FROM Tags WHERE parent_id=? AND name=? LIMIT 1"
		);
		try {
			if (tag.Parent != null) {
				statement.bindLong(1, getTagId(tag.Parent));
			} else {
				statement.bindNull(1);
			}
			statement.bindString(2, tag.Name);
			id = statement.simpleQueryForLong();
		} catch (SQLException e) {
			// ignore, id is null
		} finally {
			statement.close();
		}

		if (id == null) {
			final ContentValues values = new ContentValues();
			values.put("parent_id", tag.Parent != null ? getTagId(tag.Parent) : null);
			values.put("name", tag.Name);
			id = myDatabase.insertWithOnConflict(
				"Tags", null, values, SQLiteDatabase.CONFLICT_IGNORE
			);
		}

		if (id == null || id == -1) {
			return -1;
		}

		myIdByTag.put(tag, id);
		myTagById.put(id, tag);
		return id;
	}

	protected void deleteAllBookTags(long bookId) {
		myDatabase.delete("BookTag", "book_id=?", new String[] { String.valueOf(bookId) });
	}

	protected void saveBookTagInfo(long bookId, Tag tag) {
		final ContentValues values = new ContentValues();
		values.put("book_id", bookId);
		values.put("tag_id", getTagId(tag));
		myDatabase.insertWithOnConflict("BookTag", null, values, SQLiteDatabase.CONFLICT_IGNORE);
	}

	private Tag getTagById(long id) {
		Tag tag = myTagById.get(id);
		if (tag == null) {
			final Cursor cursor = myDatabase.rawQuery("SELECT parent_id,name FROM Tags WHERE tag_id=? LIMIT 1", new String[] { String.valueOf(id) });
			try {
				if (cursor.moveToNext()) {
					final Tag parent = cursor.isNull(0) ? null : getTagById(cursor.getLong(0));
					tag = Tag.getTag(parent, cursor.getString(1));
					myIdByTag.put(tag, id);
					myTagById.put(id, tag);
				}
			} finally {
				cursor.close();
			}
		}
		return tag;
	}

	protected List<Tag> listTags(long bookId) {
		final Cursor cursor = myDatabase.rawQuery("SELECT Tags.tag_id FROM BookTag INNER JOIN Tags ON Tags.tag_id = BookTag.tag_id WHERE BookTag.book_id = ?", new String[] { String.valueOf(bookId) });
		try {
			if (!cursor.moveToNext()) {
				return null;
			}
			final ArrayList<Tag> list = new ArrayList<Tag>();
			do {
				list.add(getTagById(cursor.getLong(0)));
			} while (cursor.moveToNext());
			return list;
		} finally {
			cursor.close();
		}
	}

	@Override
	protected List<Label> listLabels(long bookId) {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT Labels.name,BookLabel.uid FROM Labels" +
			" INNER JOIN BookLabel ON BookLabel.label_id=Labels.label_id" +
			" WHERE BookLabel.book_id=?",
			new String[] { String.valueOf(bookId) }
		);
		final LinkedList<Label> labels = new LinkedList<Label>();
		try {
			while (cursor.moveToNext()) {
				labels.add(new Label(cursor.getString(1), cursor.getString(0)));
			}
		} finally {
			cursor.close();
		}
		return labels;
	}

	@Override
	protected List<String> listLabels() {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT DISTINCT(Labels.name) FROM Labels" +
			" INNER JOIN BookLabel ON BookLabel.label_id=Labels.label_id" +
			" INNER JOIN Books ON BookLabel.book_id=Books.book_id" +
			" WHERE Books.`exists`=1",
			null
		);
		final LinkedList<String> names = new LinkedList<String>();
		try {
			while (cursor.moveToNext()) {
				names.add(cursor.getString(0));
			}
		} finally {
			cursor.close();
		}
		return names;
	}

	protected void deleteAllBookUids(long bookId) {
		myDatabase.delete("BookUid", "book_id=?", new String[] { String.valueOf(bookId) });
	}

	@Override
	protected void saveBookUid(long bookId, UID uid) {
		final ContentValues values = new ContentValues();
		values.put("book_id", bookId);
		values.put("type", uid.Type);
		values.put("uid", uid.Id);
		myDatabase.insertWithOnConflict("BookUid", null, values, SQLiteDatabase.CONFLICT_IGNORE);
	}

	@Override
	protected List<UID> listUids(long bookId) {
		final ArrayList<UID> list = new ArrayList<UID>();
		final Cursor cursor = myDatabase.rawQuery("SELECT type,uid FROM BookUid WHERE book_id = ?", new String[] { String.valueOf(bookId) });
		try {
			while (cursor.moveToNext()) {
				list.add(new UID(cursor.getString(0), cursor.getString(1)));
			}
		} finally {
			cursor.close();
		}
		return list;
	}

	@Override
	protected Long bookIdByUid(UID uid) {
		final Cursor cursor = myDatabase.rawQuery("SELECT book_id FROM BookUid WHERE type=? AND uid=? LIMIT 1", new String[] { uid.Type, uid.Id });
		try {
			if (cursor.moveToNext()) {
				return cursor.getLong(0);
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	protected void saveBookSeriesInfo(long bookId, SeriesInfo seriesInfo) {
		if (seriesInfo == null) {
			myDatabase.delete("BookSeries", "book_id=?", new String[] { String.valueOf(bookId) });
		} else {
			long seriesId = -1;

			final Cursor cursor = myDatabase.rawQuery(
				"SELECT series_id FROM Series WHERE name=? LIMIT 1",
				new String[] { seriesInfo.Series.getTitle() }
			);
			try {
				if (cursor.moveToNext()) {
					seriesId = cursor.getLong(0);
				}
			} finally {
				cursor.close();
			}

			if (seriesId == -1) {
				final ContentValues values = new ContentValues();
				values.put("name", seriesInfo.Series.getTitle());
				seriesId = myDatabase.insertWithOnConflict(
					"Series", null, values, SQLiteDatabase.CONFLICT_IGNORE
				);
			}

			if (seriesId == -1) {
				return;
			}

			final ContentValues values = new ContentValues();
			values.put("book_id", bookId);
			values.put("series_id", seriesId);
			values.put(
				"book_index", seriesInfo.Index != null ? seriesInfo.Index.toPlainString() : null
			);
			myDatabase.insertWithOnConflict(
				"BookSeries", null, values, SQLiteDatabase.CONFLICT_REPLACE
			);
		}
	}

	protected SeriesInfo getSeriesInfo(long bookId) {
		final Cursor cursor = myDatabase.rawQuery("SELECT Series.name,BookSeries.book_index FROM BookSeries INNER JOIN Series ON Series.series_id=BookSeries.series_id WHERE BookSeries.book_id=? LIMIT 1", new String[] { String.valueOf(bookId) });
		try {
			if (cursor.moveToNext()) {
				return SeriesInfo.createSeriesInfo(cursor.getString(0), cursor.getString(1));
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	protected void removeFileInfo(long fileId) {
		if (fileId == -1) {
			return;
		}
		myDatabase.delete("Files", "file_id=?", new String[] { String.valueOf(fileId) });
	}

	protected void saveFileInfo(FileInfo fileInfo) {
		final ContentValues values = new ContentValues();
		values.put("name", fileInfo.Name);
		final FileInfo parent = fileInfo.Parent;
		values.put("parent_id", parent != null ? parent.Id : null);
		final Long size = fileInfo.FileSize;
		values.put("size", size != -1 ? size : null);

		if (fileInfo.Id == -1) {
			fileInfo.Id = myDatabase.insertWithOnConflict(
				"Files", null, values, SQLiteDatabase.CONFLICT_IGNORE
			);
		} else {
			myDatabase.update(
				"Files", values, "file_id=?", new String[] { String.valueOf(fileInfo.Id) }
			);
		}
	}

	protected Collection<FileInfo> loadFileInfos() {
		final HashMap<Long,FileInfo> infosById = new HashMap<Long,FileInfo>();
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT file_id,name,parent_id,size FROM Files", null
		);
		try {
			while (cursor.moveToNext()) {
				final long id = cursor.getLong(0);
				final FileInfo info = createFileInfo(id,
					cursor.getString(1),
					cursor.isNull(2) ? null : infosById.get(cursor.getLong(2))
				);
				if (!cursor.isNull(3)) {
					info.FileSize = cursor.getLong(3);
				}
				infosById.put(id, info);
			}
		} finally {
			cursor.close();
		}
		return infosById.values();
	}

	protected Collection<FileInfo> loadFileInfos(ZLFile file) {
		final LinkedList<ZLFile> fileStack = new LinkedList<ZLFile>();
		for (; file != null; file = file.getParent()) {
			fileStack.addFirst(file);
		}

		final ArrayList<FileInfo> infos = new ArrayList<FileInfo>(fileStack.size());
		final String[] parameters = { null };
		FileInfo current = null;
		for (ZLFile f : fileStack) {
			parameters[0] = f.getLongName();
			final Cursor cursor = myDatabase.rawQuery(
				current == null ?
					"SELECT file_id,size FROM Files WHERE name=? LIMIT 1" :
					"SELECT file_id,size FROM Files WHERE parent_id=" + current.Id + " AND name=? LIMIT 1",
				parameters
			);
			try {
				if (cursor.moveToNext()) {
					current = createFileInfo(cursor.getLong(0), parameters[0], current);
					if (!cursor.isNull(1)) {
						current.FileSize = cursor.getLong(1);
					}
					infos.add(current);
				} else {
					break;
				}
			} finally {
				cursor.close();
			}
		}

		return infos;
	}

	protected Collection<FileInfo> loadFileInfos(long fileId) {
		final ArrayList<FileInfo> infos = new ArrayList<FileInfo>();
		while (fileId != -1) {
			final Cursor cursor = myDatabase.rawQuery(
				"SELECT name,size,parent_id FROM Files WHERE file_id=? LIMIT 1",
				new String[] { String.valueOf(fileId) }
			);
			try {
				if (cursor.moveToNext()) {
					FileInfo info = createFileInfo(fileId, cursor.getString(0), null);
					if (!cursor.isNull(1)) {
						info.FileSize = cursor.getLong(1);
					}
					infos.add(0, info);
					fileId = cursor.isNull(2) ? -1 : cursor.getLong(2);
				} else {
					fileId = -1;
				}
			} finally {
				cursor.close();
			}
		}
		for (int i = 1; i < infos.size(); ++i) {
			final FileInfo oldInfo = infos.get(i);
			final FileInfo newInfo = createFileInfo(oldInfo.Id, oldInfo.Name, infos.get(i - 1));
			newInfo.FileSize = oldInfo.FileSize;
			infos.set(i, newInfo);
		}
		return infos;
	}

	@Override
	protected void addBookHistoryEvent(long bookId, int event) {
		final ContentValues values = new ContentValues();
		values.put("book_id", bookId);
		values.put("timestamp", System.currentTimeMillis());
		values.put("event", event);
		myDatabase.insert("BookHistory", null, values);
	}

	@Override
	protected void removeBookHistoryEvents(long bookId, int event) {
		myDatabase.delete(
			"BookHistory",
			"book_id=? and event=?",
			new String[] { String.valueOf(bookId), String.valueOf(event) }
		);
	}

	@Override
	protected List<Long> loadRecentBookIds(int event, int limit) {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id FROM BookHistory WHERE event=? GROUP BY book_id ORDER BY timestamp DESC LIMIT ?",
			new String[] { String.valueOf(event), String.valueOf(limit) }
		);
		final LinkedList<Long> ids = new LinkedList<Long>();
		try {
			while (cursor.moveToNext()) {
				ids.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		return ids;
	}

	@Override
	protected void addLabel(long bookId, Label label) {
		final ContentValues values = new ContentValues();
		values.put("name", label.Name);
		values.put("uid", uuidByString(label.Name));
		myDatabase.insertWithOnConflict("Labels", null, values, SQLiteDatabase.CONFLICT_IGNORE);

		final SQLiteStatement statement = myDatabase.compileStatement(
			"INSERT OR IGNORE INTO BookLabel(label_id,book_id,uid,timestamp)" +
			" SELECT label_id,?,?,? FROM Labels WHERE name=?"
		);
		try {
			statement.bindLong(1, bookId);
			statement.bindString(2, label.Uid);
			statement.bindLong(3, System.currentTimeMillis());
			statement.bindString(4, label.Name);
			statement.execute();
		} finally {
			statement.close();
		}
	}

	@Override
	protected long bookIdByLabelUuid(String uuid) {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id FROM BookLabel WHERE uid=? LIMIT 1", new String[] { uuid }
		);
		try {
			return cursor.moveToNext() ? cursor.getLong(0) : -1;
		} finally {
			cursor.close();
		}
	}

	@Override
	protected void removeLabel(long bookId, Label label) {
		final int count = myDatabase.delete(
			"BookLabel",
			"book_id=? AND uid=?",
			new String[] { String.valueOf(bookId), label.Uid }
		);

		if (count > 0) {
			final ContentValues values = new ContentValues();
			values.put("uid", label.Uid);
			myDatabase.insertWithOnConflict(
				"DeletedBookLabelIds", null, values, SQLiteDatabase.CONFLICT_IGNORE
			);
		}
	}

	@Override
	protected List<String> deletedBookLabelUids(int limit, int page) {
		final List<String> uids = new ArrayList<String>(limit);
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT uid FROM DeletedBookLabelIds LIMIT " + limit * page + "," + limit, null
		);
		try {
			while (cursor.moveToNext()) {
				uids.add(cursor.getString(0));
			}
		} finally {
			cursor.close();
		}
		return uids;
	}

	@Override
	protected void purgeBookLabels(List<String> uids) {
		final SQLiteStatement statement =
			myDatabase.compileStatement("DELETE FROM DeletedBookLabelIds WHERE uid=?");
		try {
			for (String u : uids) {
				statement.bindString(1, u);
				statement.execute();
			}
		} finally {
			statement.close();
		}
	}

	@Override
	protected boolean hasVisibleBookmark(long bookId) {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT bookmark_id FROM Bookmarks WHERE book_id=? AND visible=1 LIMIT 1",
			new String[] { String.valueOf(bookId) }
		);
		try {
			return cursor.moveToNext();
		} finally {
			cursor.close();
		}
	}

	@Override
	protected List<Bookmark> loadBookmarks(BookmarkQuery query) {
		final LinkedList<Bookmark> list = new LinkedList<Bookmark>();
		final StringBuilder sql = new StringBuilder("SELECT")
			.append(" bm.bookmark_id,bm.uid,bm.version_uid,")
			.append("bm.book_id,b.title,bm.bookmark_text,bm.original_text,")
			.append("bm.creation_time,bm.modification_time,bm.access_time,")
			.append("bm.model_id,bm.paragraph,bm.word,bm.char,")
			.append("bm.end_paragraph,bm.end_word,bm.end_character,")
			.append("bm.style_id")
			.append(" FROM Bookmarks AS bm")
			.append(" INNER JOIN Books AS b ON b.book_id = bm.book_id")
			.append(" WHERE");
		if (query.Book != null) {
			sql.append(" b.book_id = " + query.Book.getId() +" AND");
		}
		sql
			.append(" bm.visible = " + (query.Visible ? 1 : 0))
			.append(" ORDER BY bm.bookmark_id")
			.append(" LIMIT " + query.Limit * query.Page + "," + query.Limit);
		final Cursor cursor = myDatabase.rawQuery(sql.toString(), null);
		try {
			while (cursor.moveToNext()) {
				list.add(createBookmark(
					cursor.getLong(0),
					cursor.getString(1),
					cursor.getString(2),
					cursor.getLong(3),
					cursor.getString(4),
					cursor.getString(5),
					cursor.isNull(6) ? null : cursor.getString(6),
					cursor.getLong(7),
					cursor.isNull(8) ? null : cursor.getLong(8),
					cursor.isNull(9) ? null : cursor.getLong(9),
					cursor.getString(10),
					(int)cursor.getLong(11),
					(int)cursor.getLong(12),
					(int)cursor.getLong(13),
					(int)cursor.getLong(14),
					cursor.isNull(15) ? -1 : (int)cursor.getLong(15),
					cursor.isNull(16) ? -1 : (int)cursor.getLong(16),
					query.Visible,
					(int)cursor.getLong(17)
				));
			}
		} finally {
			cursor.close();
		}
		return list;
	}

	@Override
	protected List<HighlightingStyle> loadStyles() {
		final LinkedList<HighlightingStyle> list = new LinkedList<HighlightingStyle>();
		final String sql = "SELECT style_id,timestamp,name,bg_color,fg_color FROM HighlightingStyle";
		final Cursor cursor = myDatabase.rawQuery(sql, null);
		try {
			while (cursor.moveToNext()) {
				final String name = cursor.getString(2);
				final int bgColor = (int)cursor.getLong(3);
				final int fgColor = (int)cursor.getLong(4);
				list.add(createStyle(
					(int)cursor.getLong(0),
					cursor.getLong(1),
					name.length() > 0 ? name : null,
					bgColor != -1 ? new ZLColor(bgColor) : null,
					fgColor != -1 ? new ZLColor(fgColor) : null
				));
			}
		} finally {
			cursor.close();
		}
		return list;
	}

	protected void saveStyle(HighlightingStyle style) {
		final ContentValues values = new ContentValues();
		values.put("style_id", style.Id);
		final String name = style.getNameOrNull();
		values.put("name", name != null ? name : "");
		final ZLColor bgColor = style.getBackgroundColor();
		values.put("bg_color", bgColor != null ? bgColor.intValue() : -1);
		final ZLColor fgColor = style.getForegroundColor();
		values.put("fg_color", fgColor != null ? fgColor.intValue() : -1);
		values.put("timestamp", System.currentTimeMillis());
		myDatabase.insertWithOnConflict(
			"HighlightingStyle", null, values, SQLiteDatabase.CONFLICT_REPLACE
		);
	}

	// this is workaround for working with old format plugins;
	// it should never go via the third way with new versions
	private String uid(Bookmark bookmark) {
		if (bookmark.Uid != null) {
			return bookmark.Uid;
		}
		if (bookmark.getId() == -1) {
			return UUID.randomUUID().toString();
		}

		final Cursor cursor = myDatabase.rawQuery(
			"SELECT uid FROM Bookmarks WHERE bookmark_id=? LIMIT 1",
			new String[] { String.valueOf(bookmark.getId()) }
		);
		try {
			if (cursor.moveToNext()) {
				return cursor.getString(0);
			}
		} finally {
			cursor.close();
		}

		return UUID.randomUUID().toString();
	}

	@Override
	protected long saveBookmark(Bookmark bookmark) {
		final ContentValues values = new ContentValues();
		values.put("uid", uid(bookmark));
		values.put("version_uid", bookmark.getVersionUid());
		values.put("book_id", bookmark.BookId);
		values.put("bookmark_text", bookmark.getText());
		values.put("original_text", bookmark.getOriginalText());
		values.put("creation_time", bookmark.getTimestamp(Bookmark.DateType.Creation));
		values.put("modification_time", bookmark.getTimestamp(Bookmark.DateType.Modification));
		values.put("access_time", bookmark.getTimestamp(Bookmark.DateType.Access));
		values.put("model_id", bookmark.ModelId);
		values.put("paragraph", bookmark.ParagraphIndex);
		values.put("word", bookmark.ElementIndex);
		values.put("char", bookmark.CharIndex);
		final ZLTextPosition end = bookmark.getEnd();
		values.put("end_paragraph", end != null ? end.getParagraphIndex() : bookmark.getLength());
		values.put("end_word", end != null ? end.getElementIndex() : null);
		values.put("end_character", end != null ? end.getCharIndex() : null);
		values.put("visible", bookmark.IsVisible ? 1 : 0);
		values.put("style_id", bookmark.getStyleId());

		final long bookmarkId = bookmark.getId();
		if (bookmarkId == -1) {
			return myDatabase.insert("Bookmarks", null, values);
		} else {
			myDatabase.update(
				"Bookmarks", values, "bookmark_id=?", new String[] { String.valueOf(bookmarkId) }
			);
			return bookmarkId;
		}
	}

	@Override
	protected void deleteBookmark(Bookmark bookmark) {
		final String uuid = uid(bookmark);
		final int count = myDatabase.delete("Bookmarks", "uid=?", new String[] { uuid });

		final ContentValues values = new ContentValues();
		values.put("uid", uuid);
		myDatabase.insertWithOnConflict("DeletedBookmarkIds", null, values, SQLiteDatabase.CONFLICT_IGNORE);
	}

	@Override
	protected List<String> deletedBookmarkUids() {
		final LinkedList<String> uids = new LinkedList<String>();
		final Cursor cursor = myDatabase.rawQuery("SELECT uid FROM DeletedBookmarkIds", null);
		try {
			while (cursor.moveToNext()) {
				uids.add(cursor.getString(0));
			}
		} finally {
			cursor.close();
		}
		return uids;
	}

	@Override
	protected void purgeBookmarks(List<String> uids) {
		final SQLiteStatement statement =
			myDatabase.compileStatement("DELETE FROM DeletedBookmarkIds WHERE uid=?");
		try {
			for (String u : uids) {
				statement.bindString(1, u);
				statement.execute();
			}
		} finally {
			statement.close();
		}
	}

	protected ZLTextPositionWithTimestamp getStoredPosition(long bookId) {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT paragraph,word,char,timestamp FROM BookState WHERE book_id=? LIMIT 1",
			new String[] { String.valueOf(bookId) }
		);
		try {
			if (cursor.moveToNext()) {
				return new ZLTextPositionWithTimestamp(
					(int)cursor.getLong(0),
					(int)cursor.getLong(1),
					(int)cursor.getLong(2),
					cursor.getLong(3)
				);
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	protected void storePosition(long bookId, ZLTextPositionWithTimestamp position) {
		final ContentValues values = new ContentValues();
		values.put("book_id", bookId);
		values.put("paragraph", position.Position.ParagraphIndex);
		values.put("word", position.Position.ElementIndex);
		values.put("char", position.Position.CharIndex);
		values.put("timestamp", position.Timestamp);
		myDatabase.insertWithOnConflict("BookState", null, values, SQLiteDatabase.CONFLICT_REPLACE);

		releaseMemory();
	}

	private void deleteVisitedHyperlinks(long bookId) {
		myDatabase.delete(
			"VisitedHyperlinks", "book_id=?", new String[] { String.valueOf(bookId) }
		);
	}

	protected void addVisitedHyperlink(long bookId, String hyperlinkId) {
		final ContentValues values = new ContentValues();
		values.put("book_id", bookId);
		values.put("hyperlink_id", hyperlinkId);
		myDatabase.insertWithOnConflict(
			"VisitedHyperlinks", null, values, SQLiteDatabase.CONFLICT_IGNORE
		);
	}

	protected Collection<String> loadVisitedHyperlinks(long bookId) {
		final TreeSet<String> links = new TreeSet<String>();
		final Cursor cursor = myDatabase.rawQuery("SELECT hyperlink_id FROM VisitedHyperlinks WHERE book_id=?", new String[] { String.valueOf(bookId) });
		try {
			while (cursor.moveToNext()) {
				links.add(cursor.getString(0));
			}
		} finally {
			cursor.close();
		}
		return links;
	}

	@Override
	protected void saveBookProgress(long bookId, RationalNumber progress) {
		final ContentValues values = new ContentValues();
		values.put("book_id", bookId);
		values.put("numerator", progress.Numerator);
		values.put("denominator", progress.Denominator);
		myDatabase.insertWithOnConflict(
			"BookReadingProgress", null, values, SQLiteDatabase.CONFLICT_IGNORE
		);
	}

	@Override
	protected RationalNumber getProgress(long bookId) {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT numerator,denominator FROM BookReadingProgress WHERE book_id=? LIMIT 1",
			new String[] { String.valueOf(bookId) }
		);
		try {
			if (cursor.moveToNext()) {
				return RationalNumber.create(cursor.getLong(0), cursor.getLong(1));
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	@Override
	protected String getHash(long bookId, long lastModified) throws NotAvailable {
		try {
			releaseMemory();

			final Cursor cursor = myDatabase.rawQuery(
				"SELECT hash FROM BookHash WHERE book_id=? AND timestamp>? LIMIT 1",
				new String[] { String.valueOf(bookId), String.valueOf(lastModified) }
			);
			try {
				return cursor.moveToNext() ? cursor.getString(0) : null;
			} finally {
				cursor.close();
			}
		} catch (Throwable t) {
			throw new NotAvailable();
		}
	}

	@Override
	protected void setHash(long bookId, String hash) throws NotAvailable {
		try {
			final ContentValues values = new ContentValues();
			values.put("book_id", bookId);
			values.put("timestamp", System.currentTimeMillis());
			values.put("hash", hash);
			myDatabase.insertWithOnConflict("BookHash", null, values, SQLiteDatabase.CONFLICT_REPLACE);
		} catch (Throwable t) {
			throw new NotAvailable();
		}
	}

	@Override
	protected List<Long> bookIdsByHash(String hash) {
		final List<Long> bookIds = new LinkedList<Long>();
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id FROM BookHash WHERE hash=?", new String[] { hash }
		);
		try {
			while (cursor.moveToNext()) {
				bookIds.add(cursor.getLong(0));
			}
		} finally {
			cursor.close();
		}
		return bookIds;
	}

	@Override
	protected void deleteBook(long bookId) {
		myDatabase.beginTransaction();
		final String[] params = new String[] { String.valueOf(bookId) };
		myDatabase.delete("BookHistory", "book_id=", params);
		myDatabase.delete("BookHash", "book_id=", params);
		myDatabase.delete("BookAuthor", "book_id=", params);
		myDatabase.delete("BookLabel", "book_id=", params);
		myDatabase.delete("BookReadingProgress", "book_id=", params);
		myDatabase.delete("BookSeries", "book_id=", params);
		myDatabase.delete("BookState", "book_id=", params);
		myDatabase.delete("BookTag", "book_id=", params);
		myDatabase.delete("BookUid", "book_id=", params);
		myDatabase.delete("Bookmarks", "book_id=", params);
		myDatabase.delete("VisitedHyperlinks", "book_id=", params);
		myDatabase.delete("Books", "book_id=", params);
		myDatabase.setTransactionSuccessful();
		myDatabase.endTransaction();
	}

	private void createTables() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Books(" +
				"book_id INTEGER PRIMARY KEY," +
				"encoding TEXT," +
				"language TEXT," +
				"title TEXT NOT NULL," +
				"file_name TEXT UNIQUE NOT NULL)");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Authors(" +
				"author_id INTEGER PRIMARY KEY," +
				"name TEXT NOT NULL," +
				"sort_key TEXT NOT NULL," +
				"CONSTRAINT Authors_Unique UNIQUE (name, sort_key))");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookAuthor(" +
				"author_id INTEGER NOT NULL REFERENCES Authors(author_id)," +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"author_index INTEGER NOT NULL," +
				"CONSTRAINT BookAuthor_Unique0 UNIQUE (author_id, book_id)," +
				"CONSTRAINT BookAuthor_Unique1 UNIQUE (book_id, author_index))");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Series(" +
				"series_id INTEGER PRIMARY KEY," +
				"name TEXT UNIQUE NOT NULL)");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookSeries(" +
				"series_id INTEGER NOT NULL REFERENCES Series(series_id)," +
				"book_id INTEGER NOT NULL UNIQUE REFERENCES Books(book_id)," +
				"book_index INTEGER)");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Tags(" +
				"tag_id INTEGER PRIMARY KEY," +
				"name TEXT NOT NULL," +
				"parent INTEGER REFERENCES Tags(tag_id)," +
				"CONSTRAINT Tags_Unique UNIQUE (name, parent))");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookTag(" +
				"tag_id INTEGER REFERENCES Tags(tag_id)," +
				"book_id INTEGER REFERENCES Books(book_id)," +
				"CONSTRAINT BookTag_Unique UNIQUE (tag_id, book_id))");
	}

	private void updateTables1() {
		myDatabase.execSQL("ALTER TABLE Tags RENAME TO Tags_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Tags(" +
				"tag_id INTEGER PRIMARY KEY," +
				"name TEXT NOT NULL," +
				"parent_id INTEGER REFERENCES Tags(tag_id)," +
				"CONSTRAINT Tags_Unique UNIQUE (name, parent_id))");
		myDatabase.execSQL("INSERT INTO Tags (tag_id,name,parent_id) SELECT tag_id,name,parent FROM Tags_Obsolete");
		myDatabase.execSQL("DROP TABLE IF EXISTS Tags_Obsolete");

		myDatabase.execSQL("ALTER TABLE BookTag RENAME TO BookTag_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookTag(" +
				"tag_id INTEGER NOT NULL REFERENCES Tags(tag_id)," +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"CONSTRAINT BookTag_Unique UNIQUE (tag_id, book_id))");
		myDatabase.execSQL("INSERT INTO BookTag (tag_id,book_id) SELECT tag_id,book_id FROM BookTag_Obsolete");
		myDatabase.execSQL("DROP TABLE IF EXISTS BookTag_Obsolete");
	}

	private void updateTables2() {
		myDatabase.execSQL("CREATE INDEX BookAuthor_BookIndex ON BookAuthor (book_id)");
		myDatabase.execSQL("CREATE INDEX BookTag_BookIndex ON BookTag (book_id)");
		myDatabase.execSQL("CREATE INDEX BookSeries_BookIndex ON BookSeries (book_id)");
	}

	private void updateTables3() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Files(" +
				"file_id INTEGER PRIMARY KEY," +
				"name TEXT NOT NULL," +
				"parent_id INTEGER REFERENCES Files(file_id)," +
				"size INTEGER," +
				"CONSTRAINT Files_Unique UNIQUE (name, parent_id))");
	}

	private void updateTables4() {
		final FileInfoSet fileInfos = new FileInfoSet(this);
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT file_name FROM Books", null
		);
		try {
			while (cursor.moveToNext()) {
				fileInfos.check(ZLFile.createFileByPath(cursor.getString(0)).getPhysicalFile(), false);
			}
		} finally {
			cursor.close();
		}
		fileInfos.save();

		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS RecentBooks(" +
				"book_index INTEGER PRIMARY KEY," +
				"book_id INTEGER REFERENCES Books(book_id))");
	}

	private void updateTables5() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Bookmarks(" +
				"bookmark_id INTEGER PRIMARY KEY," +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"bookmark_text TEXT NOT NULL," +
				"creation_time INTEGER NOT NULL," +
				"modification_time INTEGER," +
				"access_time INTEGER," +
				"access_counter INTEGER NOT NULL," +
				"paragraph INTEGER NOT NULL," +
				"word INTEGER NOT NULL," +
				"char INTEGER NOT NULL)");

		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookState(" +
				"book_id INTEGER UNIQUE NOT NULL REFERENCES Books(book_id)," +
				"paragraph INTEGER NOT NULL," +
				"word INTEGER NOT NULL," +
				"char INTEGER NOT NULL)");
		final SQLiteStatement statement = myDatabase.compileStatement("INSERT INTO BookState (book_id,paragraph,word,char) VALUES (?,?,?,?)");
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id,file_name FROM Books", null
		);
		try {
			while (cursor.moveToNext()) {
				final long bookId = cursor.getLong(0);
				final String fileName = cursor.getString(1);
				final int position = new ZLIntegerOption(fileName, "PositionInBuffer", 0).getValue();
				final int paragraph = new ZLIntegerOption(fileName, "Paragraph_" + position, 0).getValue();
				final int word = new ZLIntegerOption(fileName, "Word_" + position, 0).getValue();
				final int chr = new ZLIntegerOption(fileName, "Char_" + position, 0).getValue();
				if ((paragraph != 0) || (word != 0) || (chr != 0)) {
					statement.bindLong(1, bookId);
					statement.bindLong(2, paragraph);
					statement.bindLong(3, word);
					statement.bindLong(4, chr);
					statement.execute();
				}
				Config.Instance().removeGroup(fileName);
			}
		} finally {
			cursor.close();
		}
	}

	private void updateTables6() {
		myDatabase.execSQL(
			"ALTER TABLE Bookmarks ADD COLUMN model_id TEXT"
		);

		myDatabase.execSQL(
			"ALTER TABLE Books ADD COLUMN file_id INTEGER"
		);

		myDatabase.execSQL("DELETE FROM Files");
		final FileInfoSet infoSet = new FileInfoSet(this);
		Cursor cursor = myDatabase.rawQuery(
			"SELECT file_name FROM Books", null
		);
		try {
			while (cursor.moveToNext()) {
				infoSet.check(ZLFile.createFileByPath(cursor.getString(0)).getPhysicalFile(), false);
			}
		} finally {
			cursor.close();
		}
		infoSet.save();

		final SQLiteStatement deleteStatement = myDatabase.compileStatement("DELETE FROM Books WHERE book_id=?");
		final SQLiteStatement updateStatement = myDatabase.compileStatement("UPDATE OR IGNORE Books SET file_id=? WHERE book_id=?");
		cursor = myDatabase.rawQuery(
			"SELECT book_id,file_name FROM Books", null
		);
		try {
			while (cursor.moveToNext()) {
				final long bookId = cursor.getLong(0);
				final long fileId = infoSet.getId(ZLFile.createFileByPath(cursor.getString(1)));

				if (fileId == -1) {
					deleteStatement.bindLong(1, bookId);
					deleteStatement.execute();
				} else {
					updateStatement.bindLong(1, fileId);
					updateStatement.bindLong(2, bookId);
					updateStatement.execute();
				}
			}
		} finally {
			cursor.close();
			deleteStatement.close();
			updateStatement.close();
		}

		myDatabase.execSQL("ALTER TABLE Books RENAME TO Books_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Books(" +
				"book_id INTEGER PRIMARY KEY," +
				"encoding TEXT," +
				"language TEXT," +
				"title TEXT NOT NULL," +
				"file_id INTEGER UNIQUE NOT NULL REFERENCES Files(file_id))");
		myDatabase.execSQL("INSERT INTO Books (book_id,encoding,language,title,file_id) SELECT book_id,encoding,language,title,file_id FROM Books_Obsolete");
		myDatabase.execSQL("DROP TABLE IF EXISTS Books_Obsolete");
	}

	private void updateTables7() {
		final ArrayList<Long> seriesIDs = new ArrayList<Long>();
		Cursor cursor = myDatabase.rawQuery(
			"SELECT series_id,name FROM Series", null
		);
		try {
			while (cursor.moveToNext()) {
				if (cursor.getString(1).length() > 200) {
					seriesIDs.add(cursor.getLong(0));
				}
			}
		} finally {
			cursor.close();
		}
		if (seriesIDs.isEmpty()) {
			return;
		}

		final ArrayList<Long> bookIDs = new ArrayList<Long>();
		for (Long id : seriesIDs) {
			cursor = myDatabase.rawQuery(
				"SELECT book_id FROM BookSeries WHERE series_id=" + id, null
			);
			try {
				while (cursor.moveToNext()) {
					bookIDs.add(cursor.getLong(0));
				}
			} finally {
				cursor.close();
			}
			myDatabase.execSQL("DELETE FROM BookSeries WHERE series_id=" + id);
			myDatabase.execSQL("DELETE FROM Series WHERE series_id=" + id);
		}

		for (Long id : bookIDs) {
			myDatabase.execSQL("DELETE FROM Books WHERE book_id=" + id);
			myDatabase.execSQL("DELETE FROM BookAuthor WHERE book_id=" + id);
			myDatabase.execSQL("DELETE FROM BookTag WHERE book_id=" + id);
		}
	}

	private void updateTables8() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookList ( " +
				"book_id INTEGER UNIQUE NOT NULL REFERENCES Books (book_id))");
	}

	private void updateTables9() {
		myDatabase.execSQL("CREATE INDEX BookList_BookIndex ON BookList (book_id)");
	}

	private void updateTables10() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Favorites(" +
				"book_id INTEGER UNIQUE NOT NULL REFERENCES Books(book_id))");
	}

	private void updateTables11() {
		myDatabase.execSQL("UPDATE Files SET size = size + 1");
	}

	private void updateTables12() {
		myDatabase.execSQL("DELETE FROM Files WHERE parent_id IN (SELECT file_id FROM Files WHERE name LIKE '%.epub')");
	}

	private void updateTables13() {
		myDatabase.execSQL(
			"ALTER TABLE Bookmarks ADD COLUMN visible INTEGER DEFAULT 1"
		);
	}

	private void updateTables14() {
		myDatabase.execSQL("ALTER TABLE BookSeries RENAME TO BookSeries_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookSeries(" +
				"series_id INTEGER NOT NULL REFERENCES Series(series_id)," +
				"book_id INTEGER NOT NULL UNIQUE REFERENCES Books(book_id)," +
				"book_index REAL)");
		myDatabase.execSQL("INSERT INTO BookSeries (series_id,book_id,book_index) SELECT series_id,book_id,book_index FROM BookSeries_Obsolete");
		myDatabase.execSQL("DROP TABLE IF EXISTS BookSeries_Obsolete");
	}

	private void updateTables15() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS VisitedHyperlinks(" +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"hyperlink_id TEXT NOT NULL," +
				"CONSTRAINT VisitedHyperlinks_Unique UNIQUE (book_id, hyperlink_id))");
	}

	private void updateTables16() {
		myDatabase.execSQL(
			"ALTER TABLE Books ADD COLUMN `exists` INTEGER DEFAULT 1"
		);
	}

	private void updateTables17() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookStatus(" +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id) PRIMARY KEY," +
				"access_time INTEGER NOT NULL," +
				"pages_full INTEGER NOT NULL," +
				"page_current INTEGER NOT NULL)");
	}

	private void updateTables18() {
		myDatabase.execSQL("ALTER TABLE BookSeries RENAME TO BookSeries_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookSeries(" +
				"series_id INTEGER NOT NULL REFERENCES Series(series_id)," +
				"book_id INTEGER NOT NULL UNIQUE REFERENCES Books(book_id)," +
				"book_index TEXT)");
		final SQLiteStatement insert = myDatabase.compileStatement(
			"INSERT INTO BookSeries (series_id,book_id,book_index) VALUES (?,?,?)"
		);
		final Cursor cursor = myDatabase.rawQuery("SELECT series_id,book_id,book_index FROM BookSeries_Obsolete", null);
		try {
			while (cursor.moveToNext()) {
				insert.bindLong(1, cursor.getLong(0));
				insert.bindLong(2, cursor.getLong(1));
				final float index = cursor.getFloat(2);
				final String stringIndex;
				if (index == 0.0f) {
					stringIndex = null;
				} else {
					if (Math.abs(index - Math.round(index)) < 0.01) {
						stringIndex = String.valueOf(Math.round(index));
					} else {
						stringIndex = String.format("%.1f", index);
					}
				}
				final BigDecimal bdIndex = SeriesInfo.createIndex(stringIndex);
				SQLiteUtil.bindString(insert, 3, bdIndex != null ? bdIndex.toString() : null);
				insert.executeInsert();
			}
		} finally {
			cursor.close();
			insert.close();
		}
		myDatabase.execSQL("DROP TABLE IF EXISTS BookSeries_Obsolete");
	}

	private void updateTables19() {
		myDatabase.execSQL("DROP TABLE IF EXISTS BookList");
	}

	private void updateTables20() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Labels(" +
				"label_id INTEGER PRIMARY KEY," +
				"name TEXT NOT NULL UNIQUE)");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookLabel(" +
				"label_id INTEGER NOT NULL REFERENCES Labels(label_id)," +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"CONSTRAINT BookLabel_Unique UNIQUE (label_id,book_id))");
		final SQLiteStatement insert = myDatabase.compileStatement(
			"INSERT INTO Labels (name) VALUES ('favorite')"
		);
		final long id = insert.executeInsert();
		insert.close();
		myDatabase.execSQL("INSERT INTO BookLabel (label_id,book_id) SELECT " + id + ",book_id FROM Favorites");
		myDatabase.execSQL("DROP TABLE IF EXISTS Favorites");
	}

	private void updateTables21() {
		myDatabase.execSQL("DROP TABLE IF EXISTS BookUid");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookUid(" +
				"book_id INTEGER NOT NULL UNIQUE REFERENCES Books(book_id)," +
				"type TEXT NOT NULL," +
				"uid TEXT NOT NULL," +
				"CONSTRAINT BookUid_Unique UNIQUE (book_id,type,uid))");
	}

	private void updateTables22() {
		myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN end_paragraph INTEGER");
		myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN end_word INTEGER");
		myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN end_character INTEGER");
	}

	private void updateTables23() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS HighlightingStyle(" +
				"style_id INTEGER PRIMARY KEY," +
				"name TEXT NOT NULL," +
				"bg_color INTEGER NOT NULL)");
		myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN style_id INTEGER NOT NULL REFERENCES HighlightingStyle(style_id) DEFAULT 1");
		myDatabase.execSQL("UPDATE Bookmarks SET end_paragraph = LENGTH(bookmark_text)");
	}

	private void updateTables24() {
		myDatabase.execSQL("INSERT OR REPLACE INTO HighlightingStyle (style_id, name, bg_color) VALUES (1, '', 136*256*256 + 138*256 + 133)"); // #888a85
		myDatabase.execSQL("INSERT OR REPLACE INTO HighlightingStyle (style_id, name, bg_color) VALUES (2, '', 245*256*256 + 121*256 + 0)"); // #f57900
		myDatabase.execSQL("INSERT OR REPLACE INTO HighlightingStyle (style_id, name, bg_color) VALUES (3, '', 114*256*256 + 159*256 + 207)"); // #729fcf
	}

	private void updateTables25() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookReadingProgress(" +
				"book_id INTEGER PRIMARY KEY REFERENCES Books(book_id)," +
				"numerator INTEGER NOT NULL," +
				"denominator INTEGER NOT NULL)");
	}

	private void updateTables26() {
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookHash(" +
				"book_id INTEGER PRIMARY KEY REFERENCES Books(book_id)," +
				"timestamp INTEGER NOT NULL," +
				"hash TEXT(40) NOT NULL)"
		);
	}

	private void updateTables27() {
		myDatabase.execSQL("ALTER TABLE BookState ADD COLUMN timestamp INTEGER");
	}

	private void updateTables28() {
		myDatabase.execSQL("ALTER TABLE HighlightingStyle ADD COLUMN fg_color INTEGER NOT NULL DEFAULT -1");
	}

	private void updateTables29() {
		myDatabase.execSQL("DROP TABLE IF EXISTS BookHistory");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookHistory(" +
				"book_id INTEGER REFERENCES Books(book_id)," +
				"timestamp INTEGER NOT NULL," +
				"event INTEGER NOT NULL)"
		);

		SQLiteStatement insert = myDatabase.compileStatement(
			"INSERT OR IGNORE INTO BookHistory(book_id,timestamp,event) VALUES (?,?,?)"
		);
		insert.bindLong(3, HistoryEvent.Opened);
		int count = -1;
		Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id FROM RecentBooks ORDER BY book_index", null
		);
		try {
			while (cursor.moveToNext()) {
				insert.bindLong(1, cursor.getLong(0));
				insert.bindLong(2, count);
				try {
					insert.executeInsert();
				} catch (Throwable t) {
					// ignore
				}
				--count;
			}
		} finally {
			cursor.close();
			insert.close();
		}

		insert = myDatabase.compileStatement(
			"INSERT OR IGNORE INTO BookHistory(book_id,timestamp,event) VALUES (?,?,?)"
		);
		insert.bindLong(3, HistoryEvent.Added);
		cursor = myDatabase.rawQuery(
			"SELECT book_id FROM Books ORDER BY book_id DESC", null
		);
		try {
			while (cursor.moveToNext()) {
				insert.bindLong(1, cursor.getLong(0));
				insert.bindLong(2, count);
				try {
					insert.executeInsert();
				} catch (Throwable t) {
					// ignore
				}
				--count;
			}
		} finally {
			cursor.close();
			insert.close();
		}
	}

	private void updateTables30() {
		myDatabase.execSQL("DROP TABLE IF EXISTS RecentBooks");
	}

	private void updateTables31() {
		myDatabase.execSQL("ALTER TABLE BookLabel ADD COLUMN timestamp INTEGER NOT NULL DEFAULT -1");
	}

	private void updateTables32() {
		myDatabase.execSQL("CREATE TABLE IF NOT EXISTS Options(name TEXT PRIMARY KEY, value TEXT)");
	}

	private void updateTables33() {
		myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN uid TEXT(36)");
		final SQLiteStatement statement = myDatabase.compileStatement(
			"UPDATE Bookmarks SET uid=? WHERE bookmark_id=?"
		);
		final Cursor cursor = myDatabase.rawQuery("SELECT bookmark_id FROM Bookmarks", null);
		try {
			while (cursor.moveToNext()) {
				statement.bindString(1, UUID.randomUUID().toString());
				statement.bindLong(2, cursor.getLong(0));
				statement.execute();
			}
		} finally {
			cursor.close();
			statement.close();
		}

		myDatabase.execSQL("ALTER TABLE Bookmarks RENAME TO Bookmarks_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Bookmarks(" +
				"bookmark_id INTEGER PRIMARY KEY," +
				"uid TEXT(36) NOT NULL UNIQUE," +
				"version_uid TEXT(36)," +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"visible INTEGER DEFAULT 1," +
				"style_id INTEGER NOT NULL REFERENCES HighlightingStyle(style_id) DEFAULT 1," +
				"bookmark_text TEXT NOT NULL," +
				"creation_time INTEGER NOT NULL," +
				"modification_time INTEGER," +
				"access_time INTEGER," +
				"model_id TEXT," +
				"paragraph INTEGER NOT NULL," +
				"word INTEGER NOT NULL," +
				"char INTEGER NOT NULL," +
				"end_paragraph INTEGER," +
				"end_word INTEGER," +
				"end_character INTEGER)"
		);
		final String fields = "bookmark_id,uid,book_id,visible,style_id,bookmark_text,creation_time,modification_time,access_time,model_id,paragraph,word,char,end_paragraph,end_word,end_character";
		myDatabase.execSQL("INSERT INTO Bookmarks (" + fields + ") SELECT " + fields + " FROM Bookmarks_Obsolete");
		myDatabase.execSQL("DROP TABLE IF EXISTS Bookmarks_Obsolete");
	}

	private void updateTables34() {
		myDatabase.execSQL("CREATE TABLE IF NOT EXISTS DeletedBookmarkIds(uid TEXT(36) PRIMARY KEY)");
	}

	private void updateTables35() {
		myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN original_text TEXT DEFAULT NULL");
	}

	private int styleBg(int styleId) {
		switch (styleId) {
			case 1:
				return 0x888a85;
			case 2:
				return 0xf57900;
			case 3:
				return 0x729fcf;
			default:
				return 0;
		}
	}

	private void updateTables36() {
		myDatabase.execSQL("ALTER TABLE HighlightingStyle ADD COLUMN timestamp INTEGER DEFAULT 0");

		final SQLiteStatement statement = myDatabase.compileStatement(
			"UPDATE HighlightingStyle SET timestamp=? WHERE style_id=?"
		);
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT style_id,name,bg_color FROM HighlightingStyle", null
		);
		try {
			while (cursor.moveToNext()) {
				final int styleId = (int)cursor.getLong(0);
				if ((!cursor.isNull(1) && !"".equals(cursor.getString(1))) ||
						styleBg(styleId) != (int)cursor.getLong(2)) {
					statement.bindLong(1, System.currentTimeMillis());
					statement.bindLong(2, styleId);
					statement.execute();
				}
			}
		} finally {
			cursor.close();
			statement.close();
		}
	}

	private void updateTables37() {
		myDatabase.execSQL("ALTER TABLE Bookmarks RENAME TO Bookmarks_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Bookmarks(" +
				"bookmark_id INTEGER PRIMARY KEY," +
				"uid TEXT(36) NOT NULL UNIQUE," +
				"version_uid TEXT(36)," +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"visible INTEGER DEFAULT 1," +
				"style_id INTEGER NOT NULL REFERENCES HighlightingStyle(style_id) DEFAULT 1," +
				"bookmark_text TEXT NOT NULL," +
				"creation_time INTEGER NOT NULL," +
				"modification_time INTEGER," +
				"access_time INTEGER," +
				"model_id TEXT," +
				"paragraph INTEGER NOT NULL," +
				"word INTEGER NOT NULL," +
				"char INTEGER NOT NULL," +
				"end_paragraph INTEGER," +
				"end_word INTEGER," +
				"end_character INTEGER)"
		);
		final String fields = "bookmark_id,uid,version_uid,book_id,visible,style_id,bookmark_text,creation_time,modification_time,access_time,model_id,paragraph,word,char,end_paragraph,end_word,end_character";
		myDatabase.execSQL("INSERT INTO Bookmarks (" + fields + ") SELECT " + fields + " FROM Bookmarks_Obsolete");
		myDatabase.execSQL("DROP TABLE IF EXISTS Bookmarks_Obsolete");
	}

	private void updateTables38() {
		myDatabase.execSQL("ALTER TABLE Bookmarks ADD COLUMN original_text TEXT DEFAULT NULL");
	}

	private void updateTables39() {
		myDatabase.execSQL("ALTER TABLE BookLabel RENAME TO BookLabel_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS BookLabel(" +
				"label_id INTEGER NOT NULL REFERENCES Labels(label_id)," +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"timestamp INTEGER NOT NULL DEFAULT -1," +
				"uid TEXT(36) NOT NULL UNIQUE," +
				"CONSTRAINT BookLabel_Unique UNIQUE (label_id,book_id))");
		final SQLiteStatement statement = myDatabase.compileStatement(
			"INSERT INTO BookLabel (label_id,book_id,timestamp,uid) VALUES (?,?,?,?)"
		);
		final Cursor cursor = myDatabase.rawQuery("SELECT label_id,book_id,timestamp FROM BookLabel_Obsolete", null);
		try {
			while (cursor.moveToNext()) {
				statement.bindLong(1, cursor.getLong(0));
				statement.bindLong(2, cursor.getLong(1));
				statement.bindLong(3, cursor.getLong(2));
				statement.bindString(4, UUID.randomUUID().toString());
				statement.execute();
			}
		} finally {
			cursor.close();
			statement.close();
		}
		myDatabase.execSQL("DROP TABLE IF EXISTS BookLabel_Obsolete");

		myDatabase.execSQL("CREATE TABLE IF NOT EXISTS DeletedBookLabelIds(uid TEXT(36) PRIMARY KEY)");
	}

	private void updateTables40() {
		myDatabase.execSQL("ALTER TABLE Labels RENAME TO Labels_Obsolete");
		myDatabase.execSQL(
			"CREATE TABLE IF NOT EXISTS Labels(" +
				"label_id INTEGER PRIMARY KEY," +
				"uid TEXT(36) NOT NULL UNIQUE," +
				"name TEXT NOT NULL UNIQUE)"
		);
		final SQLiteStatement statement =
			myDatabase.compileStatement("INSERT INTO Labels (label_id,uid,name) VALUES (?,?,?)");
		final Cursor cursor =
			myDatabase.rawQuery("SELECT label_id,name FROM Labels_Obsolete", null);
		try {
			while (cursor.moveToNext()) {
				final String name = cursor.getString(1);
				final String uuid = uuidByString(name);
				statement.bindLong(1, cursor.getLong(0));
				statement.bindString(2, uuid);
				statement.bindString(3, name);
				statement.execute();
			}
		} finally {
			cursor.close();
			statement.close();
		}
		myDatabase.execSQL("DROP TABLE IF EXISTS Labels_Obsolete");
	}

	private String uuidByString(String str) {
		try {
			return UUID.nameUUIDFromBytes(str.getBytes("utf-8")).toString();
		} catch (UnsupportedEncodingException e) {
			return UUID.nameUUIDFromBytes(str.getBytes()).toString();
		}
	}
}
