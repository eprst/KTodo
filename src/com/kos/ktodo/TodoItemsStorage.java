package com.kos.ktodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TodoItemsStorage {
	private static final String TAG = "TodoItemsStorage";
	private static final String DB_NAME = "ktodo.db";
	private static final String TABLE_NAME = "todo";
	private static final int DB_VERSION = 1;

	public static final String ID_NAME = "_id";
	public static final String TAG_ID_NAME = "tag_id";
	public static final String DONE_NAME = "done";
	public static final String SUMMARY_NAME = "summary";
	public static final String BODY_NAME = "body";

	private static final String DB_CREATE = "create table " + TABLE_NAME +
	                                        " (" + ID_NAME + " integer primary key autoincrement, " +
	                                        TAG_ID_NAME + " integer not null, " +
	                                        DONE_NAME + " boolean not null, " +
	                                        SUMMARY_NAME + " text not null, " +
	                                        BODY_NAME + " text nullable);";

	private SQLiteDatabase db;
	private final boolean readOnly;
	private DBHelper helper;

	public TodoItemsStorage(final Context context, final boolean readOnly) {
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

	public TodoItem addTodoItem(final TodoItem item) {
		final ContentValues cv = fillValues(item);
		final long id = db.insert(TABLE_NAME, null, cv);
		return new TodoItem(id, item.tagID, item.done, item.summary, item.body);
	}

	private ContentValues fillValues(final TodoItem item) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_ID_NAME, item.tagID);
		cv.put(DONE_NAME, item.done);
		cv.put(SUMMARY_NAME, item.summary);
		cv.put(BODY_NAME, item.body);
		return cv;
	}

	public boolean saveTodoItem(final TodoItem item) {
		final ContentValues cv = fillValues(item);
		return db.update(TABLE_NAME, cv, ID_NAME + "=" + item.id, null) > 0;
	}

	public boolean deleteTodoItem(final long id) {
		return db.delete(TABLE_NAME, ID_NAME + "=" + id, null) > 0;
	}

	public int deleteByTag(final long tagID) {
		return db.delete(TABLE_NAME, TAG_ID_NAME + "=" + tagID, null);
	}

	public Cursor getByTagCursor(final long tagID) {
		return db.query(TABLE_NAME, new String[]{ID_NAME, DONE_NAME, BODY_NAME, SUMMARY_NAME},
				TAG_ID_NAME + "=" + tagID, null, null, null, null);
	}

	public TodoItem loadTodoItem(final long id) {
		final Cursor cursor = db.query(TABLE_NAME, new String[]{
				TAG_ID_NAME, DONE_NAME, BODY_NAME, SUMMARY_NAME},
				ID_NAME + "=" + id, null, null, null, null);
		TodoItem res = null;
		if (cursor.moveToFirst()) {
			res = new TodoItem(
					id,
					cursor.getInt(0),
					cursor.getInt(1) != 0,
					cursor.getString(2),
					cursor.getString(3)
			);
		}
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