package com.kos.ktodo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import static com.kos.ktodo.DBHelper.TAG_TODO_TAG_ID;
import static com.kos.ktodo.DBHelper.TAG_TODO_TODO_ID;
import static com.kos.ktodo.DBHelper.TAG_TODO_TABLE_NAME;

/**
 * tag-todoitem relationship storage.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com" title="">Konstantin Sobolev</a>
 * @version $Revision$
 */
public class TagTodoStorage {
//	private static final String[] ALL_COLUMNS = new String[]{TAG_TODO_TAG_ID, TAG_TODO_TODO_ID};

	private SQLiteDatabase db;

	public TagTodoStorage(SQLiteDatabase db) {
		this.db = db;
	}

	public long[] getTodoItemTags(final long todoItemId) {
		final Cursor cursor = db.query(TAG_TODO_TABLE_NAME, new String[]{TAG_TODO_TAG_ID}, getTodoConstraint(todoItemId), null, null, null, null);
		try {
			return loadFromCursor(cursor);
		} finally {
			cursor.close();
		}
	}

	private String getTodoConstraint(long todoItemId) {return TAG_TODO_TODO_ID + "=" + todoItemId;}

	private String getTagConstraint(long tagId) {return TAG_TODO_TAG_ID + "=" + tagId;}

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

	public void setTodoItemTags(final long todoItemId, final long[] tagIds) {
		deleteByTodoItem(todoItemId);
		for (long tagId : tagIds)
			db.insert(TAG_TODO_TABLE_NAME, null, fillValues(todoItemId, tagId));
	}

	public int deleteByTodoItem(long todoItemId) {
		return db.delete(TAG_TODO_TABLE_NAME, getTodoConstraint(todoItemId), null);
	}

	public int deleteByTag(long tagId) {
		return db.delete(TAG_TODO_TABLE_NAME, getTagConstraint(tagId), null);
	}

	public void deleteAll() {
		db.delete(TAG_TODO_TABLE_NAME, null, null);
	}

	private ContentValues fillValues(long todoItemId, long tagId) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_TODO_TAG_ID, tagId);
		cv.put(TAG_TODO_TODO_ID, todoItemId);
		return cv;
	}
}
