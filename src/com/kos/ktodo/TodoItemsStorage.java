package com.kos.ktodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import static com.kos.ktodo.DBHelper.*;

/**
 * TodoItems storage.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class TodoItemsStorage {
	@SuppressWarnings("UnusedDeclaration")
	private static final String TAG = "TodoItemsStorage";

	private static final String[] ALL_COLUMNS = new String[]{
			TODO_ID, TODO_TAG_ID, TODO_DONE, TODO_SUMMARY, TODO_BODY, TODO_PRIO, TODO_PROGRESS, TODO_DUE_DATE,
			TODO_CARET_POSITION};

	private SQLiteDatabase db;
	private DBHelper helper;
	private boolean modifiedDB;
//	private SQLiteStatement toggleDoneStmt1;
//	private SQLiteStatement toggleDoneStmt2;

	public TodoItemsStorage(final Context context) {
		helper = new DBHelper(context);
	}

	public void open() {
		db = helper.getWritableDatabase();
//		toggleDoneStmt1 = db.compileStatement("UPDATE " + TODO_TABLE_NAME + " SET " + TODO_DONE + " = NOT " + TODO_DONE + " WHERE " + TODO_ID + "=?");
//		toggleDoneStmt2 = db.compileStatement("UPDATE " + TODO_TABLE_NAME + " SET " + TODO_PROGRESS + " = 0 " + " WHERE " +
//		                                      TODO_ID + "=? AND " + TODO_PROGRESS + " = 100 AND " + TODO_DONE + " = 0");
		modifiedDB = false;
		if (helper.isNeedToRecreateAllItems()) {
			recreateAllItems();
			helper.resetNeedToRecreateAllItems();
		}
	}

	public void close() {
		helper.close();
	}

	public TodoItem addTodoItem(final TodoItem item) {
		modifiedDB = true;
		final ContentValues cv = fillValues(item);
		final long id = db.insert(TODO_TABLE_NAME, null, cv);
		return new TodoItem(id, item.tagID, item.isDone(), item.summary, item.body, item.prio, item.getProgress(), item.getDueDate(), item.caretPos);
	}

	private ContentValues fillValues(final TodoItem item) {
		final ContentValues cv = new ContentValues();
		if (item.id != -1)
			cv.put(TODO_ID, item.id);
		cv.put(TODO_TAG_ID, item.tagID);
		cv.put(TODO_DONE, item.isDone());
		cv.put(TODO_SUMMARY, item.summary);
		cv.put(TODO_BODY, item.body);
		cv.put(TODO_PRIO, item.prio);
		cv.put(TODO_PROGRESS, item.getProgress());
		if (item.getDueDate() != null)
			cv.put(TODO_DUE_DATE, item.getDueDate());
		else
			cv.putNull(TODO_DUE_DATE);
		cv.put(TODO_CARET_POSITION, item.caretPos);
		return cv;
	}

	public boolean saveTodoItem(final TodoItem item) {
		modifiedDB = true;
		final ContentValues cv = fillValues(item);
		return db.update(TODO_TABLE_NAME, cv, TODO_ID + "=" + item.id, null) > 0;
	}

//	public void toggleDone(final long id) {
//		modifiedDB = true;
//		toggleDoneStmt1.bindLong(1, id);
//		toggleDoneStmt1.execute();
//		toggleDoneStmt2.bindLong(1, id);
//		toggleDoneStmt2.execute();
//	}

	public void moveTodoItems(final long fromTag, final long toTag) {
		modifiedDB = true;
		final ContentValues cv = new ContentValues();
		cv.put(TODO_TAG_ID, toTag);
		db.update(TODO_TABLE_NAME, cv, TODO_TAG_ID + "=" + fromTag, null);
	}

	public boolean deleteTodoItem(final long id) {
		modifiedDB = true;
		return db.delete(TODO_TABLE_NAME, TODO_ID + "=" + id, null) > 0;
	}

	public void deleteAllTodoItems() {
		modifiedDB = true;
		db.delete(TODO_TABLE_NAME, null, null);
	}

	public int deleteByTag(final long tagID) {
		modifiedDB = true;
		return db.delete(TODO_TABLE_NAME, TODO_TAG_ID + "=" + tagID, null);
	}

	public Cursor getByTagCursor(final long tagID, final TodoItemsSortingMode sortingMode) {
		return db.query(TODO_TABLE_NAME, ALL_COLUMNS, getTagConstraint(tagID), null, null, null, sortingMode.getOrderBy());
	}

	public Cursor getByTagCursorExcludingCompleted(final long tagID, final TodoItemsSortingMode sortingMode) {
		final String tagConstraint = getTagConstraint(tagID);
		final String doneConstraint = TODO_DONE + " = 0";
		final String constraint = tagConstraint == null ? doneConstraint :
		                          tagConstraint + " AND " + doneConstraint;
		return db.query(TODO_TABLE_NAME, ALL_COLUMNS, constraint, null, null, null, sortingMode.getOrderBy());
	}

	private String getTagConstraint(final long tagID) {
		return tagID == DBHelper.ALL_TAGS_METATAG_ID ? null : TODO_TAG_ID + "=" + tagID;
	}

	public TodoItem loadTodoItem(final long id) {
		final Cursor cursor = db.query(TODO_TABLE_NAME, ALL_COLUMNS, TODO_ID + "=" + id, null, null, null, TODO_PRIO);
		TodoItem res = null;
		if (cursor.moveToFirst())
			res = loadTodoItemFromCursor(cursor);
		cursor.close();
		return res;
	}

	public TodoItem loadTodoItemFromCursor(final Cursor cursor) {
		return new TodoItem(
				cursor.getInt(0),
				cursor.getInt(1),
				cursor.getInt(2) != 0,
				cursor.getString(3),
				cursor.getString(4),
				cursor.getInt(5),
				cursor.getInt(6),
				cursor.isNull(7) ? null : cursor.getLong(7),
				cursor.isNull(8) ? null : cursor.getInt(8)
		);
	}

	public void recreateAllItems() {
		final Cursor cursor = db.query(TODO_TABLE_NAME, ALL_COLUMNS, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			final TodoItem item = loadTodoItemFromCursor(cursor);
			saveTodoItem(item);
			cursor.moveToNext();
		}
		cursor.close();
	}

	public boolean hasModifiedDB() {
		return modifiedDB;
	}

	public void resetModifiedDB() {
		modifiedDB = false;
	}
}
