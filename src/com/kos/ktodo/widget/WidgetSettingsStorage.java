package com.kos.ktodo.widget;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.kos.ktodo.DBHelper;
import com.kos.ktodo.TagWidgetStorage;
import com.kos.ktodo.TodoItemsSortingMode;

import static com.kos.ktodo.DBHelper.*;

/**
 * Widget settings storage.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class WidgetSettingsStorage {
	private static final String TAG = "WidgetSettingsStorage";

	private SQLiteDatabase db;
	private DBHelper helper;

	private WidgetSettingsStorage(final SQLiteDatabase db) {
		this.db = db;
	}

	public WidgetSettingsStorage(final Context context) {
		helper = new DBHelper(context);
	}

	public void open() {
		db = helper.getWritableDatabase();
	}

	public void close() {
		helper.close();
	}

	public void save(final WidgetSettings s) {
		final ContentValues cv = new ContentValues();
		new TagWidgetStorage(db).setWidgetTags(s.widgetID, s.tagIDs);
		cv.put(WIDGET_HIDE_COMPLETED, s.hideCompleted);
		cv.put(WIDGET_SHOW_ONLY_DUE, s.showOnlyDue);
		cv.put(WIDGET_SHOW_ONLY_DUE_IN, s.showOnlyDueIn);
		cv.put(WIDGET_CONFIGURED, s.configured);
		cv.put(WIDGET_ID, s.widgetID);
		cv.put(WIDGET_SORTING_MODE, s.sortingMode.ordinal());
		db.replace(WIDGET_TABLE_NAME, null, cv);
	}

	public boolean delete(final int widgetID) {
		new TagWidgetStorage(db).deleteByWidget(widgetID);
		return db.delete(WIDGET_TABLE_NAME, getWhere(widgetID), null) > 0;
	}

	private String getWhere(final int widgetID) {return WIDGET_ID + "=" + widgetID;}

	public WidgetSettings load(final int widgetID) {
		final Cursor c = db.query(WIDGET_TABLE_NAME, new String[]
				{WIDGET_CONFIGURED, WIDGET_HIDE_COMPLETED, WIDGET_SHOW_ONLY_DUE, WIDGET_SHOW_ONLY_DUE_IN, WIDGET_SORTING_MODE},
				getWhere(widgetID), null, null, null, null);
		try {
			final WidgetSettings res = new WidgetSettings(widgetID);
			if (c.moveToFirst()) {
				res.configured = c.getInt(0) != 0;
				res.hideCompleted = c.getInt(1) != 0;
				res.showOnlyDue = c.getInt(2) != 0;
				res.showOnlyDueIn = c.getInt(3);
				res.sortingMode = TodoItemsSortingMode.fromOrdinal(c.getInt(5));
				res.tagIDs = new TagWidgetStorage(db).getWidgetTags(widgetID);
			} else Log.i(TAG, "widget not found: " + widgetID);
			return res;
		} finally {c.close();}
	}

	public static void upgradeToV8(SQLiteDatabase db) {
		final WidgetSettingsStorage s = new WidgetSettingsStorage(db);

		@SuppressWarnings("deprecation")
		final String[] ALL_OLD_COLUMNS = new String[]{WIDGET_ID, WIDGET_CONFIGURED, WIDGET_HIDE_COMPLETED, WIDGET_SHOW_ONLY_DUE, WIDGET_SHOW_ONLY_DUE_IN, WIDGET_SORTING_MODE, WIDGET_TAG_ID};

		final Cursor cursor = db.query(TODO_TABLE_NAME, ALL_OLD_COLUMNS, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			final WidgetSettings ws = new WidgetSettings(cursor.getInt(0));

			long oldTagId = cursor.getInt(7);
			@SuppressWarnings("deprecation")
			long[] tagIDs = (cursor.isNull(7) || oldTagId == DBHelper.UNFILED_METATAG_ID) ? new long[0] : new long[]{oldTagId};

			ws.configured = cursor.getInt(1) != 0;
			ws.hideCompleted = cursor.getInt(2) != 0;
			ws.showOnlyDue = cursor.getInt(3) != 0;
			ws.showOnlyDueIn = cursor.getInt(4);
			ws.sortingMode = TodoItemsSortingMode.fromOrdinal(cursor.getInt(6));
			ws.tagIDs = tagIDs;

			s.save(ws);
			cursor.moveToNext();
		}
		cursor.close();
	}
}
