package com.kos.ktodo;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.kos.ktodo.widget.WidgetItemOnClickAction;

/**
 * SQLite helper.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class DBHelper extends SQLiteOpenHelper {
	private static final String TAG = "DBHelper";
	private static final String DB_NAME = "ktodo.db";
	private static final int DB_VERSION = 9;

	public static final int ALL_TAGS_METATAG_ID = 1;
	public static final int UNFILED_METATAG_ID = 2;
	public static final int TODAY_METATAG_ID = -1;

	public static final String TAG_TABLE_NAME = "tag";
	public static final String TAG_ID = "_id";
	public static final String TAG_TAG = "tag";
	public static final String TAG_ORDER = "tag_order"; //not visible anywhere. Used to make "All" first and "Unfiled" last

	public static final String TODO_TABLE_NAME = "todo";
	public static final String TODO_ID = "_id";
	public static final String TODO_TAG_ID = "tag_id";
	public static final String TODO_DONE = "done";
	public static final String TODO_SUMMARY = "summary";
	public static final String TODO_BODY = "body";
	public static final String TODO_PRIO = "prio";
	public static final String TODO_PROGRESS = "progress";
	public static final String TODO_DUE_DATE = "due_date";
	public static final String TODO_CARET_POSITION = "caret_pos";

	public static final String WIDGET_TABLE_NAME = "widget";
	public static final String WIDGET_ID = "_id";
	public static final String WIDGET_TAG_ID = "tag_id";
	public static final String WIDGET_HIDE_COMPLETED = "hide_completed";
	public static final String WIDGET_SHOW_ONLY_DUE = "show_only_due";
	public static final String WIDGET_SHOW_ONLY_DUE_IN = "show_only_due_in";
	public static final String WIDGET_SORTING_MODE = "sorting_mode";
	public static final String WIDGET_CONFIGURED = "configured";
	public static final String WIDGET_ITEM_ON_CLICK_ACTION = "item_on_click_action";

	private static final String CREATE_TAG_TABLE = "create table if not exists " + TAG_TABLE_NAME +
	                                               " (" + TAG_ID + " integer primary key autoincrement, " +
	                                               TAG_ORDER + " integer default 10 not null, " +
	                                               TAG_TAG + " text not null);";

	private static final String CREATE_TODO_TABLE = "create table if not exists " + TODO_TABLE_NAME +
	                                                " (" + TODO_ID + " integer primary key autoincrement, " +
	                                                TODO_TAG_ID + " integer not null, " +
	                                                TODO_DONE + " boolean not null, " +
	                                                TODO_SUMMARY + " text not null, " +
	                                                TODO_PRIO + " integer default 1 not null, " +
	                                                TODO_PROGRESS + " integer default 0 not null, " +
	                                                TODO_DUE_DATE + " integer, " +
	                                                TODO_BODY + " text, " +
	                                                TODO_CARET_POSITION + " integer default 0);";

	private static final String CREATE_WIDGET_TABLE = "create table if not exists " + WIDGET_TABLE_NAME +
	                                                  " (" + WIDGET_ID + " integer primary key autoincrement, " +
	                                                  WIDGET_TAG_ID + " integer default 1 not null, " +
	                                                  WIDGET_CONFIGURED + " boolean default 0 not null, " +
	                                                  WIDGET_SHOW_ONLY_DUE + " boolean default 0 not null, " +
	                                                  WIDGET_SHOW_ONLY_DUE_IN + " integer default -1 not null, " +
	                                                  WIDGET_SORTING_MODE + " integer default " + TodoItemsSortingMode.PRIO_DUE_SUMMARY.ordinal() + " not null, " +
	                                                  WIDGET_ITEM_ON_CLICK_ACTION + " integer default " + WidgetItemOnClickAction.DEFAULT.ordinal() + " not null, " +
	                                                  WIDGET_HIDE_COMPLETED + " boolean default 1 not null);";

//	private static DBHelper instance = null;

	private final Context context;
	private boolean needToRecreateAllItems = false;

	private boolean closed = false;
	@SuppressWarnings("ThrowableInstanceNeverThrown")
	private Throwable createdST = new Throwable();

	private DBHelper(final Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	public static DBHelper getInstance(final Context context) {
//		if (instance == null) {
//			instance = new DBHelper(context.getApplicationContext());
//		}
//
//		return instance;
		return new DBHelper(context.getApplicationContext());
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (!closed)
			Log.w(getClass().getName(), "Finalizing non-closed db helper", createdST);
	}

	@Override
	public synchronized void close() {
		super.close();
		closed = true;
	}

	@Override
	public void onCreate(final SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL(CREATE_TAG_TABLE);
		sqLiteDatabase.execSQL(CREATE_TODO_TABLE);
		sqLiteDatabase.execSQL(CREATE_WIDGET_TABLE);

		final ContentValues cv = new ContentValues();
		cv.put(TAG_ID, ALL_TAGS_METATAG_ID);
		cv.put(TAG_TAG, context.getString(R.string.all_untranslated)); //will be localized in KTodo
		cv.put(TAG_ORDER, 1);
		sqLiteDatabase.insert(TAG_TABLE_NAME, null, cv);
		cv.put(TAG_ID, TODAY_METATAG_ID);
		cv.put(TAG_TAG, context.getString(R.string.today_untranslated)); //will be localized in KTodo
		cv.put(TAG_ORDER, 99);
		sqLiteDatabase.insert(TAG_TABLE_NAME, null, cv);
		cv.put(TAG_ID, UNFILED_METATAG_ID);
		cv.put(TAG_TAG, context.getString(R.string.unfiled_untranslated)); //will be localized in KTodo
		cv.put(TAG_ORDER, 100);
		sqLiteDatabase.insert(TAG_TABLE_NAME, null, cv);

		final Resources r = context.getResources();
		cv.clear();
		cv.put(TODO_TAG_ID, UNFILED_METATAG_ID);
		cv.put(TODO_DONE, false);
		cv.put(TODO_SUMMARY, r.getString(R.string.help_summary));
		cv.put(TODO_BODY, r.getString(R.string.help_body));
		cv.put(TODO_PRIO, 1);
		cv.put(TODO_PROGRESS, 0);
		cv.putNull(TODO_DUE_DATE);
		sqLiteDatabase.insert(TODO_TABLE_NAME, null, cv);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int oldv, final int newv) {
		Log.i(TAG, "onUpgrade: " + oldv + " -> " + newv);
		if (oldv == 1)
			sqLiteDatabase.execSQL("alter table " + TODO_TABLE_NAME + " add " + TODO_DUE_DATE + " integer;");
		if (oldv <= 2)
			sqLiteDatabase.execSQL(CREATE_WIDGET_TABLE);
		if (oldv == 3)
			sqLiteDatabase.execSQL("alter table " + WIDGET_TABLE_NAME + " add " + WIDGET_SORTING_MODE +
			                       " integer default " + TodoItemsSortingMode.PRIO_DUE_SUMMARY.ordinal() + " not null");
		if (oldv <= 4)
			sqLiteDatabase.execSQL("alter table " + TODO_TABLE_NAME + " add " + TODO_CARET_POSITION + " integer default 0;");
		if (oldv < 7)
			needToRecreateAllItems = true;
		if (oldv < 8)
			sqLiteDatabase.execSQL("alter table " + WIDGET_TABLE_NAME + " add " + WIDGET_ITEM_ON_CLICK_ACTION +
			                       " integer default " + WidgetItemOnClickAction.DEFAULT.ordinal() + " not null");
		if (oldv < 9) {
			final ContentValues cv = new ContentValues();
			cv.put(TAG_ID, TODAY_METATAG_ID);
			cv.put(TAG_TAG, context.getString(R.string.today_untranslated)); //will be localized in KTodo
			cv.put(TAG_ORDER, 99);
			sqLiteDatabase.insert(TAG_TABLE_NAME, null, cv);
		}
	}

	boolean isNeedToRecreateAllItems() {
		return needToRecreateAllItems;
	}

	void resetNeedToRecreateAllItems() {
		this.needToRecreateAllItems = false;
	}

	public static boolean isNullable(final String tableName, final String columnName) {
		//noinspection SimplifiableIfStatement
		if (TODO_TABLE_NAME.equals(tableName))
			return TODO_DUE_DATE.equals(columnName) || TODO_BODY.equals(columnName) || TODO_CARET_POSITION.equals(columnName);
		return false;
	}
}
