package com.kos.ktodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TagsStorage {
	private static final String TAG = "TagsStorage";
	private static final String DB_NAME = "ktodo.db";
	private static final String TABLE_NAME = "tag";
	private static final int DB_VERSION = 1;

	public static final String ID_NAME = "_id";
	public static final String TAG_NAME = "tag";

	private static final String DB_CREATE = "create table " + TABLE_NAME +
	                                        " (" + ID_NAME + " integer primary key autoincrement, " +
	                                        TAG_NAME + " text not null);";

	private SQLiteDatabase db;
	private final boolean readOnly;
	private DBHelper helper;

	public TagsStorage(final Context context, final boolean readOnly) {
		this.readOnly = readOnly;
		helper = new DBHelper(context, DB_NAME, null, DB_VERSION);
	}

	public void open() {
		if (readOnly)
			db = helper.getWritableDatabase();
		else
			db = helper.getReadableDatabase();
	}

	public void close() {
		helper.close();
	}

	public long addTag(final String tag) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_NAME, tag);
		return db.insert(TABLE_NAME, null, cv);
	}

	public boolean renameTag(final String oldName, final String newName) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_NAME, newName);
		return db.update(TABLE_NAME, cv, TAG_NAME + "=" + oldName, null) > 0;
	}

	public boolean renameTag(final long id, final String newName) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_NAME, newName);
		return db.update(TABLE_NAME, cv, ID_NAME + "=" + id, null) > 0;
	}

	public boolean deleteTag(final String tag) {
		return db.delete(TABLE_NAME, TAG_NAME + "=" + tag, null) > 0;
	}

	public boolean deleteTag(final long id) {
		return db.delete(TABLE_NAME, ID_NAME + "=" + id, null) > 0;
	}

	public Cursor getAllTagsCursor() {
		return db.query(TABLE_NAME, new String[]{ID_NAME, TAG_NAME}, null, null, null, null, null);
	}

	public boolean hasTag(final String tag) {
		final Cursor cursor = db.query(TABLE_NAME, new String[]{ID_NAME}, TAG_NAME + "=\"" + tag + "\"", null, null, null, null);
		final boolean res = cursor.getCount() > 0;
		cursor.close();
		return res;
	}

	public String getTag(final long id) {
		final Cursor cursor = db.query(TABLE_NAME, new String[]{TAG_NAME}, ID_NAME + "=" + id, null, null, null, null);
		final String res = cursor.moveToFirst() ? cursor.getString(0) : null;
		cursor.close();
		return res;
	}

	private static class DBHelper extends SQLiteOpenHelper {
		public DBHelper(final Context context, final String name, final SQLiteDatabase.CursorFactory factory, final int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(final SQLiteDatabase sqLiteDatabase) {
			sqLiteDatabase.execSQL(DB_CREATE);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int oldv, final int newv) {
			Log.w(TAG, "DB upgrade not supported");
		}
	}
}
