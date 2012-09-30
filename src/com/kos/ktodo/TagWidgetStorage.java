package com.kos.ktodo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import static com.kos.ktodo.DBHelper.TAG_WIDGET_TAG_ID;
import static com.kos.ktodo.DBHelper.TAG_WIDGET_WIDGET_ID;
import static com.kos.ktodo.DBHelper.TAG_WIDGET_TABLE_NAME;

/**
 * tag-todoitem relationship storage.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com" title="">Konstantin Sobolev</a>
 * @version $Revision$
 */
public class TagWidgetStorage {
//	private static final String[] ALL_COLUMNS = new String[]{TAG_WIDGET_TAG_ID, TAG_WIDGET_TODO_ID};

	private SQLiteDatabase db;

	public TagWidgetStorage(SQLiteDatabase db) {
		this.db = db;
	}

	public long[] getWidgetTags(final long widetId) {
		final Cursor cursor = db.query(TAG_WIDGET_TABLE_NAME, new String[]{TAG_WIDGET_TAG_ID}, getWidgetConstraint(widetId), null, null, null, null);
		try {
			return loadFromCursor(cursor);
		} finally {
			cursor.close();
		}
	}

	private String getWidgetConstraint(long widgetId) {return TAG_WIDGET_WIDGET_ID + "=" + widgetId;}

	private String getTagConstraint(long tagId) {return TAG_WIDGET_TAG_ID + "=" + tagId;}

	private long[] loadFromCursor(Cursor cursor) {
		final List<Long> res = new ArrayList<Long>(5);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			res.add(cursor.getLong(0));
		}
		final long[] r = new long[res.size()];
		int i = 0;
		for (Long l : res) {
			r[i++] = l;
		}
		return r;
	}

	public void setWidgetTags(final long widgetId, final long[] tagIds) {
		deleteByWidget(widgetId);
		for (long tagId : tagIds)
			db.insert(TAG_WIDGET_TABLE_NAME, null, fillValues(widgetId, tagId));
	}

	public int deleteByWidget(long widgetId) {
		return db.delete(TAG_WIDGET_TABLE_NAME, getWidgetConstraint(widgetId), null);
	}

	public int deleteByTag(long tagId) {
		return db.delete(TAG_WIDGET_TABLE_NAME, getTagConstraint(tagId), null);
	}

	public void deleteAll() {
		db.delete(TAG_WIDGET_TABLE_NAME, null, null);
	}

	private ContentValues fillValues(long widgetId, long tagId) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_WIDGET_TAG_ID, tagId);
		cv.put(TAG_WIDGET_WIDGET_ID, widgetId);
		return cv;
	}
}
