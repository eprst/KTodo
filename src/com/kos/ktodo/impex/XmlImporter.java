package com.kos.ktodo.impex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Xml;
import com.kos.ktodo.DBHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.*;

import static com.kos.ktodo.Util.assumeEquals;

/**
 * Tables to XML importer.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class XmlImporter extends XmlBase {
	private static final String TAG = "XmlImporter";

	public static void importData(final Context ctx, final File f, final boolean overwrite) throws IOException {
		final FileInputStream fis = new FileInputStream(f);
		try {
			importData(ctx, fis, overwrite);
		} finally {
			fis.close();
		}
	}

	public static void importData(final Context ctx, final InputStream is, final boolean overwrite) throws IOException {
		final InputStreamReader isr = new InputStreamReader(is, "UTF8");
		final DBHelper dbh = new DBHelper(ctx);
		final SQLiteDatabase db = dbh.getWritableDatabase();
		try {
			final XmlPullParser p = Xml.newPullParser();
			p.setInput(isr);
			readTag(p, DATABASE_TAG, false);

			final HashMap<Long, Long> tagIdRemapping = importTable(db, p, DBHelper.TAG_TABLE_NAME, overwrite, null, DBHelper.TAG_TAG);
			final HashMap<Long, Long> todoIdRemapping = importTable(db, p, DBHelper.TODO_TABLE_NAME, overwrite, null, DBHelper.TODO_SUMMARY);

			final FkRemapping remapping = new FkRemapping();
			remapping.addRemapping(DBHelper.TAG_TODO_TAG_ID, tagIdRemapping);
			remapping.addRemapping(DBHelper.TAG_TODO_TODO_ID, todoIdRemapping);

			importTable(db, p, DBHelper.TAG_TODO_TABLE_NAME, overwrite, remapping);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "error parsing backup file", e);
		} finally {
			db.close();
			dbh.close();
			isr.close();
		}
	}

	/**
	 * Imports a table that has one primary key column and possibly has one foreign key column. Can update existing rows
	 * instead of creating new ones (merge data), existing rows will be spotted by using <code>mergeByColumn</code>.
	 *
	 * @param db            open database to use
	 * @param p             XML parser to use
	 * @param expectedName  expected table name
	 * @param overwrite     if existing data (with the same PK values) should be overwritten
	 * @param fkRemapping   FK columns remapping tables
	 * @param mergeByColumn an optional list of columns to merge by. If there already exists a row where all the values for
	 *                      these columns are the same as in the import file then this row will be updated and no
	 *                      new row will be created.
	 *
	 * @return remapping table for PK column or <code>null</code> if <code>overwrite</code> is <code>true</code>.
	 *
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Nullable
	private static HashMap<Long, Long> importTable(final SQLiteDatabase db, final XmlPullParser p, final String expectedName, final boolean overwrite,
	                                               @Nullable final FkRemapping fkRemapping,
	                                               final String... mergeByColumn) throws IOException, XmlPullParserException {
		readTag(p, TABLE_TAG, false);
		assumeEquals(expectedName, p.getAttributeValue(null, NAME_ATTR));

		@Nullable
		final String pkColumn = p.getAttributeValue(null, PK_ATTR);
		final HashMap<Long, Long> remapping = overwrite ? null : new HashMap<Long, Long>();
		final HashSet<String> mb = new HashSet<String>(mergeByColumn.length);
		mb.addAll(Arrays.asList(mergeByColumn));

		while (true) {
			final int et = p.nextTag();
			if (et == XmlPullParser.END_TAG) {
				assumeEquals(TABLE_TAG, p.getName());
				return remapping;
			} else {
				assumeEquals(XmlPullParser.START_TAG, et);
				assumeEquals(ROW_TAG, p.getName());
				importRow(db, p, expectedName, pkColumn, remapping, fkRemapping, mb);
			}
		}
	}

	private static void importRow(final SQLiteDatabase db, final XmlPullParser p, final String tableName,
	                              @Nullable final String pkColumn, @Nullable final HashMap<Long, Long> pkRemapping, //we fill pkRemapping
	                              @Nullable final FkRemapping fkRemapping, //we use fkRemapping,
	                              final Set<String> mergeByColumn) throws IOException, XmlPullParserException {
		final ContentValues cv = new ContentValues();
		int mergeCnt = 0;
		final StringBuilder mergeCond = new StringBuilder();
		final String[] mergeCols = new String[mergeByColumn.size()]; //spotted mergeByColumn names
		final String[] mergeArgs = new String[mergeByColumn.size()]; //corresponding values

		Long oldPK = null;
		while (true) {
			final int et = p.nextTag();
			if (et == XmlPullParser.END_TAG) {
				if (ROW_TAG.equals(p.getName()))
					break;
				assumeEquals(COLUMN_TAG, p.getName());
			} else {
				assumeEquals(XmlPullParser.START_TAG, et);
				assumeEquals(COLUMN_TAG, p.getName());
				final String cname = p.getAttributeValue(null, NAME_ATTR);
				final String val = valueForDB(tableName, cname, unescape(p.nextText()));

				//read column value
				if (pkRemapping == null || !cname.equals(pkColumn))
					cv.put(cname, val);
				if (fkRemapping != null && fkRemapping.hasRemappingsFor(cname)) {
					//this is FK, use remapping (which should have remappings for all values)
					final Long oldFK = Long.parseLong(val);
					final Long newFK = fkRemapping.getRemappingsFor(cname).get(oldFK);
					if (newFK != null && !newFK.equals(oldFK)) {
//						Log.i(TAG, "remapped " + tableName + '.' + fkColumn + ": " + oldFK + " -> " + newFK);
						cv.put(cname, newFK);
					} else {
						Log.w(TAG, "can't find remapping for " + oldFK);
						cv.put(cname, oldFK);
					}
				}
				if (cname.equals(pkColumn)) {
					//this is PK, save it's value for remapping
					if (oldPK != null)
						Log.w(TAG, "already seen primary column {" + pkColumn + "} with value " + oldPK + "; will use " + val + " instead");
					oldPK = Long.parseLong(val);
				}
				if (mergeByColumn.contains(cname)) {
					//we're merging by this column, add "AND colName=val" to merge clause
					if (mergeCnt > 0)
						mergeCond.append(" AND ");
					mergeCond.append(cname).append("=?");
					mergeCols[mergeCnt] = cname;
					mergeArgs[mergeCnt++] = val;
				}
			}
		}
		//done reading data, update merge arguments if FK is among merge columns
		if (fkRemapping != null)
			for (int i = 0; i < mergeCnt; i++) {
				if (fkRemapping.hasRemappingsFor(mergeCols[i])) {
					final Long oldFK = Long.parseLong(mergeArgs[i]);
					final Long newFK = fkRemapping.getRemappingsFor(mergeCols[i]).get(oldFK);
					if (newFK != null && !newFK.equals(oldFK))
						mergeArgs[i] = newFK.toString();
				}
			}
		if (mergeCnt > 0) {
			final String mergeCondStr = mergeCond.toString();
//			Log.i(TAG, "merge: " + mergeCondStr + ", " + Arrays.toString(mergeArgs));
			final int updated = db.update(tableName, cv, mergeCondStr, mergeArgs);
			//try to write new data into existing row first
			if (updated > 1) {
				if (pkRemapping != null)
					Log.w(TAG, "Can't do PK remapping, updated more than 1 row");
				return;
			}
			if (updated == 1) {
				//OK, updated just one row. Now get it's PK value and add to our remapping table
				if (pkRemapping != null) {
					final Cursor cursor = db.query(tableName, new String[]{pkColumn}, mergeCondStr, mergeArgs, null, null, null);
					final int cnt = cursor.getCount();
					if (cnt != 1)
						Log.w(TAG, "Can't do PK remapping, error finding updated row; cnt=" + cnt);
					else {
						cursor.moveToFirst();
						pkRemapping.put(oldPK, cursor.getLong(0));
						cursor.close();
					}
				}
//				Log.i(TAG, "Merged!");
				return;
			}
			//updated 0 = do an insert
		}
		//no merging, just create new row and add it's ID to PK remapping table
		final long pk = db.replace(tableName, null, cv);
		if (oldPK != null && pkRemapping != null)
			pkRemapping.put(oldPK, pk);
	}

	private static void readTag(final XmlPullParser p, final String expectedName, final boolean closing) throws IOException, XmlPullParserException {
		p.nextTag();
		p.require(closing ? XmlPullParser.END_TAG : XmlPullParser.START_TAG, null, expectedName);
	}

	private static String valueForDB(final String tableName, final String cname, final String valFromXML) {
		if (valFromXML.length() > 0) return valFromXML;
		//write nullable/not null into XML?
		if (DBHelper.isNullable(tableName, cname))
			return null;
		return valFromXML;
	}

	private final static class FkRemapping {
		final Map<String, Map<Long, Long>> remappings = new HashMap<String, Map<Long, Long>>();

		public boolean hasRemappingsFor(@NotNull String columnName) {
			return remappings.containsKey(columnName);
		}

		@NotNull
		public Map<Long, Long> getRemappingsFor(@NotNull String columnName) {
			Map<Long, Long> res = remappings.get(columnName);
			if (res == null) {
				res = new HashMap<Long, Long>();
				remappings.put(columnName, res);
			}
			return res;
		}

		public void addRemapping(@NotNull String columnName, Map<Long, Long> remapping) {
			remappings.put(columnName, remapping);
		}
	}
}
