package com.kos.ktodo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.kos.ktodo.widget.WidgetSettingsStorage;
import org.intellij.lang.annotations.Language;

/**
 * SQLite helper.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class DBHelper extends SQLiteOpenHelper {
	private static final String TAG = "DBHelper";
	private static final String DB_NAME = "ktodo.db";
	private static final int DB_VERSION = 8;

	@Deprecated
	public static final int ALL_TAGS_METATAG_ID = 1;
	@Deprecated
	public static final int UNFILED_METATAG_ID = 2;

	public static final String TAG_TABLE_NAME = "tag";
	public static final String TAG_ID = "_id";
	public static final String TAG_TAG = "tag";
	@Deprecated
	public static final String TAG_ORDER = "tag_order"; //not visible anywhere. Used to make "All" first and "Unfiled" last; unneeded since v.8

	public static final String TODO_TABLE_NAME = "todo";
	public static final String TODO_ID = "_id";
	@Deprecated
	public static final String TODO_TAG_ID = "tag_id"; //old tag column, unused in >=v8
	public static final String TODO_DONE = "done";
	public static final String TODO_SUMMARY = "summary";
	public static final String TODO_BODY = "body";
	public static final String TODO_PRIO = "prio";
	public static final String TODO_PROGRESS = "progress";
	public static final String TODO_DUE_DATE = "due_date";
	public static final String TODO_CARET_POSITION = "caret_pos";

	public static final String WIDGET_TABLE_NAME = "widget";
	public static final String WIDGET_ID = "_id";
	@Deprecated
	public static final String WIDGET_TAG_ID = "tag_id"; //old tag column, unused in >=v8
	public static final String WIDGET_HIDE_COMPLETED = "hide_completed";
	public static final String WIDGET_SHOW_ONLY_DUE = "show_only_due";
	public static final String WIDGET_SHOW_ONLY_DUE_IN = "show_only_due_in";
	public static final String WIDGET_SORTING_MODE = "sorting_mode";
	public static final String WIDGET_CONFIGURED = "configured";

	public static final String TAG_WIDGET_TABLE_NAME = "tag_widget";
	public static final String TAG_WIDGET_TAG_ID = "tag_id";
	public static final String TAG_WIDGET_WIDGET_ID = "widget_id";
	public static final String TAG_WIDGET_INDEX_NAME = "tag_widget_idx";

	public static final String TAG_TODO_TABLE_NAME = "tag_todo";
	public static final String TAG_TODO_TAG_ID = "tag_id";
	public static final String TAG_TODO_TODO_ID = "todo_id";
	public static final String TAG_TODO_INDEX_NAME = "tag_todo_idx";

	@Language("SQLite")
	private static final String CREATE_TAG_TABLE = "CREATE TABLE IF NOT EXISTS " + TAG_TABLE_NAME +
	                                               " (" + TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                                               TAG_TAG + " TEXT NOT NULL);";

	@Language("SQLite")
	private static final String CREATE_TODO_TABLE = "CREATE TABLE IF NOT EXISTS " + TODO_TABLE_NAME +
	                                                " (" + TODO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                                                TODO_DONE + " BOOLEAN NOT NULL, " +
	                                                TODO_SUMMARY + " TEXT NOT NULL, " +
	                                                TODO_PRIO + " INTEGER DEFAULT 1 NOT NULL, " +
	                                                TODO_PROGRESS + " INTEGER DEFAULT 0 NOT NULL, " +
	                                                TODO_DUE_DATE + " INTEGER DEFAULT NULL, " +
	                                                TODO_BODY + " TEXT DEFAULT NULL, " +
	                                                TODO_CARET_POSITION + " INTEGER DEFAULT 0);";

	@Language("SQLite")
	private static final String CREATE_WIDGET_TABLE = "CREATE TABLE IF NOT EXISTS " + WIDGET_TABLE_NAME +
	                                                  " (" + WIDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                                                  WIDGET_CONFIGURED + " BOOLEAN DEFAULT 0 NOT NULL, " +
	                                                  WIDGET_SHOW_ONLY_DUE + " BOOLEAN DEFAULT 0 NOT NULL, " +
	                                                  WIDGET_SHOW_ONLY_DUE_IN + " INTEGER DEFAULT -1 NOT NULL, " +
	                                                  WIDGET_SORTING_MODE + " INTEGER DEFAULT " + TodoItemsSortingMode.PRIO_DUE_SUMMARY.ordinal() + " NOT NULL, " +
	                                                  WIDGET_HIDE_COMPLETED + " BOOLEAN DEFAULT 1 NOT NULL);";

	@Language("SQLite")
	private static final String CREATE_TAG_WIDGET_TABLE = "CREATE TABLE IF NOT EXISTS " + TAG_WIDGET_TABLE_NAME +
	                                                      " (" + TAG_WIDGET_TAG_ID + " INTEGER NOT NULL, " +
	                                                      TAG_WIDGET_WIDGET_ID + " INTEGER NOT NULL);";
	@Language("SQLite")
	public static final String CREATE_TAG_WIDGET_INDEX = "CREATE INDEX IF NOT EXISTS " + TAG_WIDGET_INDEX_NAME +
	                                                     " ON " + TAG_WIDGET_TABLE_NAME +
	                                                     " (" + TAG_WIDGET_TAG_ID + ", " +
	                                                     TAG_WIDGET_WIDGET_ID + " );";

	@Language("SQLite")
	private static final String CREATE_TAG_TODO_TABLE = "CREATE TABLE IF NOT EXISTS " + TAG_TODO_TABLE_NAME +
	                                                    " (" + TAG_TODO_TAG_ID + " INTEGER NOT NULL, " +
	                                                    TAG_TODO_TODO_ID + " INTEGER NOT NULL);";
	@Language("SQLite")
	public static final String CREATE_TAG_TODO_INDEX = "CREATE INDEX IF NOT EXISTS " + TAG_TODO_INDEX_NAME +
	                                                   " ON " + TAG_TODO_TABLE_NAME +
	                                                   " (" + TAG_TODO_TAG_ID + ", " +
	                                                   TAG_TODO_TODO_ID + " );";

	@SuppressWarnings("FieldCanBeLocal")
	private final Context context;

	public DBHelper(final Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		db.execSQL(CREATE_TAG_TABLE);
		db.execSQL(CREATE_TODO_TABLE);
		db.execSQL(CREATE_WIDGET_TABLE);
		db.execSQL(CREATE_TAG_WIDGET_TABLE);
		db.execSQL(CREATE_TAG_WIDGET_INDEX);
		db.execSQL(CREATE_TAG_TODO_TABLE);
		db.execSQL(CREATE_TAG_TODO_INDEX);

//		final ContentValues cv = new ContentValues();
//		cv.put(TAG_ID, ALL_TAGS_METATAG_ID);
//		cv.put(TAG_TAG, context.getString(R.string.all_untranslated)); //will be localized in KTodo
//		cv.put(TAG_ORDER, 1);
//		db.insert(TAG_TABLE_NAME, null, cv);
//		cv.put(TAG_ID, UNFILED_METATAG_ID);
//		cv.put(TAG_TAG, context.getString(R.string.unfiled_untranslated)); //will be localized in KTodo
//		cv.put(TAG_ORDER, 100);
//		db.insert(TAG_TABLE_NAME, null, cv);
//
//		final Resources r = context.getResources();
//		cv.clear();
//		cv.put(TODO_TAG_ID, UNFILED_METATAG_ID);
//		cv.put(TODO_DONE, false);
//		cv.put(TODO_SUMMARY, r.getString(R.string.help_summary));
//		cv.put(TODO_BODY, r.getString(R.string.help_body));
//		cv.put(TODO_PRIO, 1);
//		cv.put(TODO_PROGRESS, 0);
//		cv.putNull(TODO_DUE_DATE);
//		db.insert(TODO_TABLE_NAME, null, cv);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldv, final int newv) {
		Log.i(TAG, "onUpgrade: " + oldv + " -> " + newv);
		if (oldv == 1)
			db.execSQL("ALTER TABLE " + TODO_TABLE_NAME + " ADD " + TODO_DUE_DATE + " INTEGER;");
		if (oldv <= 2)
			db.execSQL(CREATE_WIDGET_TABLE);
		if (oldv == 3)
			db.execSQL("ALTER TABLE " + WIDGET_TABLE_NAME + " ADD " + WIDGET_SORTING_MODE +
			           " INTEGER DEFAULT " + TodoItemsSortingMode.PRIO_DUE_SUMMARY.ordinal() + " NOT NULL");
		if (oldv <= 4)
			db.execSQL("ALTER TABLE " + TODO_TABLE_NAME + " ADD " + TODO_CARET_POSITION + " INTEGER DEFAULT 0;");

		if (oldv < 8) {
			//create new tables & indices
			//recreate items
			//drop old columns
			db.execSQL(CREATE_TAG_WIDGET_TABLE);
			db.execSQL(CREATE_TAG_WIDGET_INDEX);
			db.execSQL(CREATE_TAG_TODO_TABLE);
			db.execSQL(CREATE_TAG_TODO_INDEX);

			TodoItemsStorage.upgradeToV8(db);
			WidgetSettingsStorage.upgradeToV8(db);
			//noinspection deprecation
			db.execSQL("DELETE FROM " + TAG_TODO_TABLE_NAME + " WHERE " + TAG_ID + " = " + ALL_TAGS_METATAG_ID +
			           " OR " + TAG_ID + " = " + UNFILED_METATAG_ID);


//			db.execSQL("alter table ");
		}
	}

	public static boolean isNullable(final String tableName, final String columnName) {
		if (TODO_TABLE_NAME.equals(tableName))
			return TODO_DUE_DATE.equals(columnName) || TODO_BODY.equals(columnName) || TODO_CARET_POSITION.equals(columnName);
		//noinspection SimplifiableIfStatement
//		if (WIDGET_TABLE_NAME.equals(tableName))
//			return WIDGET_TAG_IDS.equals(columnName);
		return false;
	}
}
