package com.kos.ktodo.impex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Xml;
import com.kos.ktodo.DBHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
		final DBHelper dbh = DBHelper.getInstance(ctx);
		final SQLiteDatabase db = dbh.getWritableDatabase();
		try {
			final XmlPullParser p = Xml.newPullParser();
			p.setInput(isr);
			readTag(p, DATABASE_TAG, false);
			final LongSparseArray<Long> tagIDRemapping = importTable(db, p, DBHelper.TAG_TABLE_NAME, overwrite, null, null, DBHelper.TAG_TAG);
			importTable(db, p, DBHelper.TODO_TABLE_NAME, overwrite, DBHelper.TODO_TAG_ID, tagIDRemapping, DBHelper.TODO_TAG_ID, DBHelper.TODO_SUMMARY);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "error parsing backup file", e);
		} finally {
			db.close();
			dbh.close();
			isr.close();
		}
	}

	private static LongSparseArray<Long> importTable(final SQLiteDatabase db, final XmlPullParser p, final String expectedName, final boolean overwrite,
	                                               final String fkColumn, final LongSparseArray<Long> fkRemapping,
	                                               final String... mergeByColumn) throws IOException, XmlPullParserException {
		readTag(p, TABLE_TAG, false);
		assumeEquals(expectedName, p.getAttributeValue(null, NAME_ATTR));

		final String pkColumn = p.getAttributeValue(null, PK_ATTR);
		final LongSparseArray<Long> remapping = overwrite ? null : new LongSparseArray<Long>();
		final HashSet<String> mb = new HashSet<>(mergeByColumn.length);
		mb.addAll(Arrays.asList(mergeByColumn));

		while (true) {
			final int et = p.nextTag();
			if (et == XmlPullParser.END_TAG) {
				assumeEquals(TABLE_TAG, p.getName());
				return remapping;
			} else {
				assumeEquals(XmlPullParser.START_TAG, et);
				assumeEquals(ROW_TAG, p.getName());
				importRow(db, p, expectedName, pkColumn, remapping, fkColumn, fkRemapping, mb);
			}
		}
	}

	private static void importRow(final SQLiteDatabase db, final XmlPullParser p, final String tableName,
	                              final String pkColumn, final LongSparseArray<Long> pkRemapping, //we fill pkRemapping
	                              final String fkColumn, final LongSparseArray<Long> fkRemapping, //we use fkRemapping
	                              final Set<String> mergeByColumn) throws IOException, XmlPullParserException {
		final ContentValues cv = new ContentValues();
		int mergeCnt = 0;
		final StringBuilder mergeCond = new StringBuilder();
		final String[] mergeCols = new String[mergeByColumn.size()];
		final String[] mergeArgs = new String[mergeByColumn.size()];

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

				if (pkRemapping == null || !cname.equals(pkColumn))
					cv.put(cname, val);
				if (fkRemapping != null && cname.equals(fkColumn)) {
					final Long oldFK = Long.parseLong(val);
					final Long newFK = fkRemapping.get(oldFK);
					if (newFK != null && !newFK.equals(oldFK)) {
//						Log.i(TAG, "remapped " + tableName + '.' + fkColumn + ": " + oldFK + " -> " + newFK);
						cv.put(cname, newFK);
					} else
						Log.w(TAG, "can't find remapping for " + oldFK);
				}
				if (cname.equals(pkColumn))
					oldPK = Long.parseLong(val);
				if (mergeByColumn.contains(cname)) {
					if (mergeCnt > 0)
						mergeCond.append(" AND ");
					mergeCond.append(cname).append("=?");
					mergeCols[mergeCnt] = cname;
					mergeArgs[mergeCnt++] = val;
				}
			}
		}
		if (fkRemapping != null)
			for (int i = 0; i < mergeCnt; i++) {
				if (mergeCols[i].equals(fkColumn)) {
					final Long oldFK = Long.parseLong(mergeArgs[i]);
					final Long newFK = fkRemapping.get(oldFK);
					if (newFK != null && !newFK.equals(oldFK))
						mergeArgs[i] = newFK.toString();
				}
			}
		if (mergeCnt > 0) {
			final String mergeCondStr = mergeCond.toString();
//			Log.i(TAG, "merge: " + mergeCondStr + ", " + Arrays.toString(mergeArgs));
			final int updated = db.update(tableName, cv, mergeCondStr, mergeArgs);
			if (updated > 1) {
				if (pkRemapping != null)
					Log.w(TAG, "Can't do PK remapping, updated more than 1 row");
				return;
			}
			if (updated == 1) {
				if (pkRemapping != null) {
					final Cursor cursor = db.query(tableName, new String[]{pkColumn}, mergeCondStr, mergeArgs, null, null, null);
					final int cnt = cursor.getCount();
					if (cnt != 1)
						Log.w(TAG, "Can't do PK remapping, error finding updated row; cnt=" + cnt);
					else if (oldPK == null)
						Log.w(TAG, "Can't do PK remapping, oldPK is null; cnt=" + cnt);
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
}
