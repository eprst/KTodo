package com.kos.ktodo;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.kos.ktodo.DBHelper.TAG_ID;
import static com.kos.ktodo.DBHelper.TAG_ORDER;
import static com.kos.ktodo.DBHelper.TAG_TABLE_NAME;
import static com.kos.ktodo.DBHelper.TAG_TAG;


/**
 * Tags storage.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class TagsStorage extends ContentProvider {
	@SuppressWarnings("UnusedDeclaration")
	private static final String TAG = "TagsStorage";

	public static final String AUTHORITY = "com.kos.ktodo.tags";
	public static final Uri CHANGE_NOTIFICATION_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).build();

	private DBHelper helper;
	private final Context context;

	public TagsStorage(final Context context) {
		this.context = context;
	}

	public TagsStorage() {
		this(null);
	}

	public synchronized void open() {
		helper = DBHelper.getInstance(context);
	}

	// todo remove
	private Context context() {
		return context == null ? getContext() : context;
	}

	private SQLiteDatabase readableDb() {
		return helper.getReadableDatabase();
	}

	private SQLiteDatabase writableDb() {
		return helper.getWritableDatabase();
	}

	public synchronized void close() {
		helper.close();
	}

	private void notifyChange() {
		context().getContentResolver().notifyChange(CHANGE_NOTIFICATION_URI, null);
	}

	public synchronized long addTag(final String tag) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_TAG, tag);
		final long res = writableDb().insert(TAG_TABLE_NAME, null, cv);
		notifyChange();
		return res;
	}

	public synchronized boolean renameTag(final String oldName, final String newName) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_TAG, newName);
		final boolean res = writableDb().update(TAG_TABLE_NAME, cv, TAG_TAG + "=" + oldName, null) > 0;
		notifyChange();
		return res;
	}

	public synchronized boolean renameTag(final long id, final String newName) {
		final ContentValues cv = new ContentValues();
		cv.put(TAG_TAG, newName);
		final boolean res = writableDb().update(TAG_TABLE_NAME, cv, TAG_ID + "=" + id, null) > 0;
		notifyChange();
		return res;
	}

//	public boolean deleteTag(final String tag) {
//		return db.delete(TABLE_NAME, TAG_NAME + "=" + tag, null) > 0;
//	}

	public synchronized boolean deleteTag(final long id) {
		final boolean res = writableDb().delete(TAG_TABLE_NAME, TAG_ID + "=" + id, null) > 0;
		notifyChange();
		return res;
	}

	public synchronized void deleteAllTags() {
		writableDb().delete(TAG_TABLE_NAME,
		          TAG_ID + "<>" + DBHelper.ALL_TAGS_METATAG_ID + " AND " +
				          DBHelper.TAG_ID + "<>" + DBHelper.TODAY_METATAG_ID + " AND " +
				          DBHelper.TAG_ID + "<>" + DBHelper.UNFILED_METATAG_ID,
		          null);
		notifyChange();
	}

	public synchronized Cursor getAllTagsCursor() {
		return readableDb().query(TAG_TABLE_NAME, new String[]{TAG_ID, TAG_TAG}, null, null, null, null, TAG_ORDER + " ASC");
	}

	public synchronized Cursor getAllTagsExceptCursor(final long... except) {
		final StringBuilder where = new StringBuilder();
		for (final long e : except) {
			if (where.length() != 0)
				where.append(" AND ");
			where.append(TAG_ID).append("<>").append(e);
		}
		return readableDb().query(TAG_TABLE_NAME, new String[]{TAG_ID, TAG_TAG},
		                where.toString(), null, null, null, TAG_ORDER + " ASC");
	}

	public synchronized boolean hasTag(final String tag) {
		final Cursor cursor = readableDb().query(TAG_TABLE_NAME, new String[]{TAG_ID}, TAG_TAG + "=\"" + tag + "\"", null, null, null, null);
		final boolean res = cursor.getCount() > 0;
		cursor.close();
		return res;
	}

	public synchronized boolean hasTag(final long id) {
		final Cursor cursor = readableDb().query(TAG_TABLE_NAME, new String[]{TAG_ID}, TAG_ID + "=" + id, null, null, null, null);
		final boolean res = cursor.getCount() > 0;
		cursor.close();
		return res;
	}

	public synchronized String getTag(final long id) {
		final Cursor cursor = readableDb().query(TAG_TABLE_NAME, new String[]{TAG_TAG}, TAG_ID + "=" + id, null, null, null, null);
		final String res = cursor.moveToFirst() ? cursor.getString(0) : null;
		cursor.close();
		return res;
	}

	// content provider stuff
	// todo implement it actually

	@Override
	public boolean onCreate() {
		return true;
	}

	@Nullable
	@Override
	public Cursor query(@NotNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		return null;
	}

	@Nullable
	@Override
	public String getType(@NotNull Uri uri) {
		return null;
	}

	@Nullable
	@Override
	public Uri insert(@NotNull Uri uri, @Nullable ContentValues values) {
		return null;
	}

	@Override
	public int delete(@NotNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(@NotNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
		return 0;
	}
}
