package com.kos.ktodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.kos.ktodo.DBHelper.*;

/**
 * Tags storage.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class TagsStorage {
	private static final String TAG = "TagsStorage";

	private SQLiteDatabase db;
	private DBHelper helper;
	private boolean modifiedDB;

	public TagsStorage(final Context context) {
		helper = new DBHelper(context);
	}

	public void open() {
		db = helper.getWritableDatabase();
		modifiedDB = false;
	}

	public void close() {
		helper.close();
	}

	public long addTag(final String tag) {
		modifiedDB = true;
		final ContentValues cv = new ContentValues();
		cv.put(TAG_TAG, tag);
		return db.insert(TAG_TABLE_NAME, null, cv);
	}

//	public boolean renameTag(final String oldName, final String newName) {
//		modifiedDB = true;
//		final ContentValues cv = new ContentValues();
//		cv.put(TAG_TAG, newName);
//		return db.update(TAG_TABLE_NAME, cv, TAG_TAG + "=" + oldName, null) > 0;
//	}

	public boolean renameTag(final long id, final String newName) {
		modifiedDB = true;
		final ContentValues cv = new ContentValues();
		cv.put(TAG_TAG, newName);
		return db.update(TAG_TABLE_NAME, cv, TAG_ID + "=" + id, null) > 0;
	}

//	public boolean deleteTag(final String tag) {
//		return db.delete(TABLE_NAME, TAG_NAME + "=" + tag, null) > 0;
//	}

	public boolean deleteTag(final long id) {
		modifiedDB = true;
		new TagTodoStorage(db).deleteByTag(id);
		new TagWidgetStorage(db).deleteByTag(id);
		return db.delete(TAG_TABLE_NAME, TAG_ID + "=" + id, null) > 0;
	}

	public void deleteAllTags() {
		modifiedDB = true;
		new TagTodoStorage(db).deleteAll();
		new TagWidgetStorage(db).deleteAll();
		db.delete(TAG_TABLE_NAME,
				null,
//				TAG_ID + "<>" + DBHelper.ALL_TAGS_METATAG_ID + " AND " + DBHelper.TAG_ID + "<>" + DBHelper.UNFILED_METATAG_ID,
				null);
	}

	public Cursor getAllTagsCursor() {
		return db.query(TAG_TABLE_NAME, new String[]{TAG_ID, TAG_TAG}, null, null, null, null, null);
	}

	public Cursor getAllTagsExceptCursor(final long... except) {
		final StringBuilder where = new StringBuilder();
		for (final long e : except) {
			if (where.length() != 0)
				where.append(" AND ");
			where.append(TAG_ID).append("<>").append(e);
		}
		return db.query(TAG_TABLE_NAME, new String[]{TAG_ID, TAG_TAG},
				where.toString(), null, null, null, null);
	}

	public boolean hasTag(final String tag) {
		final Cursor cursor = db.query(TAG_TABLE_NAME, new String[]{TAG_ID}, TAG_TAG + "=\"" + tag + "\"", null, null, null, null);
		final boolean res = cursor.getCount() > 0;
		cursor.close();
		return res;
	}

	public String getTag(final long id) {
		final Cursor cursor = db.query(TAG_TABLE_NAME, new String[]{TAG_TAG}, TAG_ID + "=" + id, null, null, null, null);
		final String res = cursor.moveToFirst() ? cursor.getString(0) : null;
		cursor.close();
		return res;
	}

	public String[] getTags(long... ids) {
		final String in = Util.separate("(", ")", ",", Util.toString(ids));
		final Cursor cursor = db.query(TAG_TABLE_NAME, new String[]{TAG_TAG}, TAG_ID + " IN " + in, null, null, null, null);
		List<String> res;
		if (cursor.moveToFirst()) {
			res = new ArrayList<String>(ids.length);
			while (!cursor.isAfterLast()) {
				res.add(cursor.getString(0));
			}
		} else res = Collections.emptyList();

		cursor.close();

		return res.toArray(new String[res.size()]);
	}

	public boolean hasModifiedDB() {
		return modifiedDB;
	}

	public void resetModifiedDB() {
		modifiedDB = false;
	}
}
