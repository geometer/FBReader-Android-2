package org.geometerplus.fbreader.plugin.base;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;

import org.geometerplus.fbreader.plugin.base.reader.PluginView;
import org.geometerplus.fbreader.plugin.base.document.DocumentHolder;

public class BookSettingsDB {//TODO: do not store default values
	private final SQLiteDatabase myDb;
	private final DBHelper myHelper;

	public BookSettingsDB(Context c) {
		myHelper = new DBHelper(c);
		myDb = myHelper.getWritableDatabase();
	}

	public void close() {
		if (!myDb.isOpen()) {
			return;
		}
		synchronized (myDb) {
			if (!myDb.isOpen()) {
				return;
			}
			myHelper.close();
			myDb.close();
		}
	}

	private static String DATABASE_NAME = "BookSettings.db";

	private void storeCrops(String id, PluginView view) {
		final DocumentHolder.CropInfo cropInfo = view.getDocument().getCropInfo();
		if (cropInfo == DocumentHolder.CropInfo.NULL) {
			return;
		}

		final SQLiteStatement insertStmt =
			myDb.compileStatement("insert or replace into crops (id,top,bottom,left,right) values (?,?,?,?,?)");
		insertStmt.bindString(1, id);
		insertStmt.bindLong(2, cropInfo.TopPercent);
		insertStmt.bindLong(3, cropInfo.BottomPercent);
		insertStmt.bindLong(4, cropInfo.LeftPercent);
		insertStmt.bindLong(5, cropInfo.RightPercent);
		insertStmt.execute();
		insertStmt.close();
	}

	private void storeZoom(String id, PluginView view) {
		final PluginView.ZoomMode z = view.getZoomMode();

		final SQLiteStatement insertStmt =
			myDb.compileStatement("insert or replace into zoom (id, mode, zoom) values (?,?,?)");
		insertStmt.bindString(1, id);
		insertStmt.bindLong(2, z.Mode);
		insertStmt.bindLong(3, z.Percent);
		insertStmt.execute();
		insertStmt.close();
	}

	private void storeWay(String id, PluginView view) {
		final boolean horiz = view.isHorizontalFirst();

		final SQLiteStatement insertStmt =
			myDb.compileStatement("insert or replace into way (id, horiz) values (?,?)");
		insertStmt.bindString(1, id);
		insertStmt.bindLong(2, horiz ? 1 : 0);
		insertStmt.execute();
		insertStmt.close();
	}

	private void storeOverlap(String id, PluginView view) {
		final PluginView.IntersectionsHolder i = view.getIntersections();

		final SQLiteStatement insertStmt =
			myDb.compileStatement("insert or replace into overlap (id, x, y) values (?,?,?)");
		insertStmt.bindString(1, id);
		insertStmt.bindLong(2, i.XPercent);
		insertStmt.bindLong(3, i.YPercent);
		insertStmt.execute();
		insertStmt.close();
	}

	private void storeBackground(String id, PluginView view) {
		final boolean use = view.useWallPaper();

		final SQLiteStatement insertStmt =
			myDb.compileStatement("insert or replace into background (id, use) values (?,?)");
		insertStmt.bindString(1, id);
		insertStmt.bindLong(2, use ? 1 : 0);
		insertStmt.execute();
		insertStmt.close();
	}

	public void storeAll(ViewHolder vh) {
		if (!myDb.isOpen()) {
			return;
		}

		synchronized (myDb) {
			if (!myDb.isOpen()) {
				return;
			}
			final ViewHolder.BookInfo bookInfo = vh.getBookInfo();
			if (bookInfo == null) {
				return;
			}
			final String id = bookInfo.DcId;
			final PluginView view = vh.getView();

			storeCrops(id, view);
			storeOverlap(id, view);
			storeWay(id, view);
			storeZoom(id, view);
			storeBackground(id, view);
		}
	}

	public void loadAll(ViewHolder vh) {
		if (!myDb.isOpen()) {
			return;
		}

		synchronized (myDb) {
			if (!myDb.isOpen()) {
				return;
			}
			final ViewHolder.BookInfo bookInfo = vh.getBookInfo();
			if (bookInfo == null) {
				return;
			}
			final String id = bookInfo.DcId;
			final PluginView view = vh.getView();

			Cursor cursor = myDb.rawQuery("SELECT top,bottom,left,right FROM crops WHERE id = ?", new String[] { id });
			if (cursor.moveToFirst()) {
				view.getDocument().setCropInfo(
					cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getInt(3)
				);
			} else {
				view.getDocument().setCropInfo(0, 0, 0, 0);
			}
			cursor.close();
			cursor = myDb.rawQuery("SELECT zoom, mode FROM zoom WHERE id = ?", new String[] { id });
			if (cursor.moveToFirst()) {
				int zoom = cursor.getInt(0);
				int mode = cursor.getInt(1);
				view.setZoomMode(new PluginView.ZoomMode(mode, zoom));
			} else {
				view.setZoomMode(new PluginView.ZoomMode(PluginView.ZoomMode.FREE_ZOOM, 100));
			}
			cursor.close();
			cursor = myDb.rawQuery("SELECT horiz FROM way WHERE id = ?", new String[] { id });
			if (cursor.moveToFirst()) {
				int horiz = cursor.getInt(0);
				view.setHorizontalFirst(horiz == 1);
			}
			cursor.close();
			cursor = myDb.rawQuery("SELECT x, y FROM overlap WHERE id = ?", new String[] { id });
			if (cursor.moveToFirst()) {
				int x = cursor.getInt(0);
				int y = cursor.getInt(1);
				view.setIntersections(new PluginView.IntersectionsHolder(x, y));
			}
			cursor.close();
			cursor = myDb.rawQuery("SELECT use FROM background WHERE id = ?", new String[] { id });
			if (cursor.moveToFirst()) {
				int use = cursor.getInt(0);
				view.useWallPaper(use == 1);
			}
			cursor.close();
		}
	}

	private static class DBHelper extends SQLiteOpenHelper {
		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, 3);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS crops (id TEXT PRIMARY KEY, top INTEGER NOT NULL, bottom INTEGER NOT NULL, right INTEGER NOT NULL, left INTEGER NOT NULL)");
			db.execSQL("CREATE TABLE IF NOT EXISTS zoom (id TEXT PRIMARY KEY, mode INTEGER NOT NULL, zoom INTEGER NOT NULL)");
			db.execSQL("CREATE TABLE IF NOT EXISTS way (id TEXT PRIMARY KEY, horiz INTEGER NOT NULL)");
			db.execSQL("CREATE TABLE IF NOT EXISTS overlap (id TEXT PRIMARY KEY, x INTEGER NOT NULL, y INTEGER NOT NULL)");
			db.execSQL("CREATE TABLE IF NOT EXISTS background (id TEXT PRIMARY KEY, use INTEGER NOT NULL)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			switch (oldVersion) {
				case 1:
				case 2:
					db.execSQL("DROP TABLE IF EXISTS landscape");
				default:
					onCreate(db);
			}
		}
	}
}
