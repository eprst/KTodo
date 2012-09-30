package com.kos.ktodo.impex;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.kos.ktodo.DBHelper;
import com.kos.ktodo.Util;
import org.jetbrains.annotations.Nullable;

import java.io.*;

/**
 * Tables to XML file exporter.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class XmlExporter extends XmlBase {
	public static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private final BufferedWriter w;

	public XmlExporter(final File outFile) throws IOException { //I hate checked exceptions
		this(new FileOutputStream(outFile));
	}

	public XmlExporter(final OutputStream os) throws IOException {
		w = new BufferedWriter(new OutputStreamWriter(os, "UTF8"));
		w.append(HEADER);
		w.newLine();
		openTag(DATABASE_TAG);
		w.newLine();
	}

	public void close() throws IOException {
		closeTag(DATABASE_TAG);
		w.close();
	}

	public void writeTable(final SQLiteDatabase db, final String tableName, @Nullable final String pkColumn) throws IOException {
		openTag(TABLE_TAG, NAME_ATTR, tableName, PK_ATTR, pkColumn);
		w.newLine();
		final Cursor c = db.rawQuery("select * from " + tableName, null);
		if (c.moveToFirst()) {
			final int cols = c.getColumnCount();
			do {
				openTag(ROW_TAG);
				w.newLine();
				for (int i = 0; i < cols; i++) {
					openTag(COLUMN_TAG, NAME_ATTR, c.getColumnName(i));
					final String s = escape(c.getString(i));
					w.append(s);
					closeTag(COLUMN_TAG);
					w.newLine();
				}
				closeTag(ROW_TAG);
				w.newLine();
			} while (c.moveToNext());
		}
		c.close();
		closeTag(TABLE_TAG);
		w.newLine();
	}

	public static void exportData(final Context ctx, final File f) throws IOException {
		final DBHelper hlp = new DBHelper(ctx);
		final SQLiteDatabase database = hlp.getReadableDatabase();
		try {
			exportData(database, f);
		} finally {
			database.close();
			hlp.close();
		}
	}

	private static void exportData(final SQLiteDatabase database, final File f) throws IOException {
		final XmlExporter x = new XmlExporter(f);
		exportDataAndCloseExporter(database, x);
	}

	private static void exportData(final SQLiteDatabase database, final OutputStream os) throws IOException {
		final XmlExporter x = new XmlExporter(os);
		exportDataAndCloseExporter(database, x);
	}

	private static void exportDataAndCloseExporter(SQLiteDatabase database, XmlExporter x) throws IOException {
		x.writeTable(database, DBHelper.TAG_TABLE_NAME, DBHelper.TAG_ID);
		x.writeTable(database, DBHelper.TODO_TABLE_NAME, DBHelper.TODO_ID);
		x.writeTable(database, DBHelper.TAG_TODO_TABLE_NAME, null);
		x.close();
	}

	public static void exportData(final Context ctx, final OutputStream os) throws IOException {
		final DBHelper hlp = new DBHelper(ctx);
		final SQLiteDatabase database = hlp.getReadableDatabase();
		try {
			exportData(database, os);
		} finally {
			database.close();
			hlp.close();
		}
	}

	protected Writer openTag(final String tag, final String... attrs) throws IOException {
		w.append('<').append(tag);
		if (attrs.length == 0) {
			w.append('>');
			return w;
		}
		Util.assume(attrs.length % 2 == 0);
		for (int i = 0; i < attrs.length; i++) {
			w.append(' ').append(attrs[i++]).append("=\"").append(attrs[i]).append('"');
		}
		w.append('>');
		return w;
	}

	protected Writer closeTag(final String tag) throws IOException {
		w.append("</").append(tag).append('>');
		return w;
	}
}
