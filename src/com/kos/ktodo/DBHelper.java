package com.kos.ktodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	private static final String TAG = "DBHelper";
	private static final String DB_NAME = "ktodo.db";
	private static final int DB_VERSION = 1;

	public static final int ALL_TAGS_METATAG_ID = 1;
	public static final int UNFILED_METATAG_ID = 2;

	public static final String TAG_TABLE_NAME = "tag";
	public static final String TAG_ID = "_id";
	public static final String TAG_TAG = "tag";
	public static final String TAG_ORDER = "tag_order"; //not visible anywhere. Used to make All first and Unfiled last

	public static final String TODO_TABLE_NAME = "todo";
	public static final String TODO_ID = "_id";
	public static final String TODO_TAG_ID = "tag_id";
	public static final String TODO_DONE = "done";
	public static final String TODO_SUMMARY = "summary";
	public static final String TODO_BODY = "body";
	public static final String TODO_PRIO = "prio";
	public static final String TODO_PROGRESS = "progress";

	private final Context context;

	public DBHelper(final Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(final SQLiteDatabase sqLiteDatabase) {
		final String CREATE_TAG_TABLE = "create table if not exists " + TAG_TABLE_NAME +
		                                " (" + TAG_ID + " integer primary key autoincrement, " +
		                                TAG_ORDER + " integer default 10 not null, " +
		                                TAG_TAG + " text not null);";
		final String CREATE_TODO_TABLE = "create table if not exists " + TODO_TABLE_NAME +
		                                 " (" + TODO_ID + " integer primary key autoincrement, " +
		                                 TODO_TAG_ID + " integer not null, " +
		                                 TODO_DONE + " boolean not null, " +
		                                 TODO_SUMMARY + " text not null, " +
		                                 TODO_PRIO + " integer default 1 not null, " +
		                                 TODO_PROGRESS + " integer default 0 not null, " +
		                                 TODO_BODY + " text nullable);";
		sqLiteDatabase.execSQL(CREATE_TAG_TABLE);
		sqLiteDatabase.execSQL(CREATE_TODO_TABLE);

		final ContentValues cv = new ContentValues();
		cv.put(TAG_ID, ALL_TAGS_METATAG_ID);
		cv.put(TAG_TAG, context.getString(R.string.all)); //will be localized in KTodo anyways
		cv.put(TAG_ORDER, 1);
		sqLiteDatabase.insert(TAG_TABLE_NAME, null, cv);
		cv.put(TAG_ID, UNFILED_METATAG_ID);
		cv.put(TAG_TAG, context.getString(R.string.unfiled)); //will be localized in KTodo anyways
		cv.put(TAG_ORDER, 100);
		sqLiteDatabase.insert(TAG_TABLE_NAME, null, cv);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int oldv, final int newv) {
//		Log.i(TAG, "onUpgrade: " + oldv + "->" + newv);
//		Log.i(TAG, "upgrading...");
//		sqLiteDatabase.execSQL("alter table " + TODO_TABLE_NAME + " add " + TODO_PRIO + " integer default 1 not null;");
	}
}
