package com.kos.ktodo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

public class TagsStorage {
	private static final String TAG = "TagsStorage";
	private static final String DB_NAME = "ktodo.db";
	private static final String TABLE_NAME = "tag";
	private static final int DB_VERSION = 1;

	public static final String KEY_ID = "_id";

	public static final String TAG_NAME = "tag";
	public static final int TAG_COLUMN = 1;

	private static final String DB_CREATE = "create table " + TABLE_NAME +
	                                        " (" + KEY_ID + " integer primary key autoincrement, " +
	                                        TAG_NAME + " text not null);";

	private SQLiteDatabase db;
	private final Context context;
	private DBHelper helper;

	public TagsStorage(final Context context) {
		this.context = context;
		helper = new DBHelper(context, DB_NAME, null, DB_VERSION);
	}

	public 

	public List<String> getTags() {
		return null;
	}

	public void addTag(final String tag) {

	}

	public void renameTag(final String oldName, final String newName) {

	}

	public void deleteTag(final String tag) {

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
