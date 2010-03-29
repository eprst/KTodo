package com.kos.ktodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.AdapterView;

public class DBHelper extends SQLiteOpenHelper {
	private static final String TAG = "DBHelper";
	private static final String DB_NAME = "ktodo.db";
	private static final int DB_VERSION = 1;

	public static final String TAG_TABLE_NAME = "tag";
	public static final String TAG_ID = "_id";
	public static final String TAG_TAG = "tag";

	public static final String TODO_TABLE_NAME = "todo";
	public static final String TODO_ID = "_id";
	public static final String TODO_TAG_ID = "tag_id";
	public static final String TODO_DONE = "done";
	public static final String TODO_SUMMARY = "summary";
	public static final String TODO_BODY = "body";

	private final Context context;

	public DBHelper(final Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(final SQLiteDatabase sqLiteDatabase) {
		final String CREATE_TAG_TABLE = "create table if not exists " + TAG_TABLE_NAME +
		                                " (" + TAG_ID + " integer primary key autoincrement, " +
		                                TAG_TAG + " text not null);";
		final String CREATE_TODO_TABLE = "create table if not exists " + TODO_TABLE_NAME +
		                                 " (" + TODO_ID + " integer primary key autoincrement, " +
		                                 TODO_TAG_ID + " integer not null, " +
		                                 TODO_DONE + " boolean not null, " +
		                                 TODO_SUMMARY + " text not null, " +
		                                 TODO_BODY + " text nullable);";
		sqLiteDatabase.execSQL(CREATE_TAG_TABLE);
		sqLiteDatabase.execSQL(CREATE_TODO_TABLE);

		final ContentValues cv = new ContentValues();
		cv.put(TODO_DONE, 0);
		cv.put(TODO_SUMMARY, context.getString(R.string.intro));
		cv.put(TODO_TAG_ID, AdapterView.INVALID_ROW_ID); //hack hack hack :]
		sqLiteDatabase.insert(TODO_TABLE_NAME, null, cv);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int oldv, final int newv) {
		//todo proper upgrade
		//todo create default tag
		//Log.w(TAG, "DB upgrade not supported");
	}
}
