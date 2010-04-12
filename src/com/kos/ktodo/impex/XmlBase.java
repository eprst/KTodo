package com.kos.ktodo.impex;

/**
 * Base class for XML importer and exporter.
 * 
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class XmlBase {
	public static final String DATABASE_TAG = "database";
	public static final String TABLE_TAG = "table";
	public static final String NAME_ATTR = "name";
	public static final String PK_ATTR = "pk";
	public static final String ROW_TAG = "row";
	public static final String COLUMN_TAG = "col";

	public static String escape(final String str) {
		if (str == null || str.length() == 0)
			return "";

		final StringBuilder buf = new StringBuilder();
		final int len = str.length();
		for (int i = 0; i < len; i++)
			escapeChar(str.charAt(i), buf);
		return buf.toString();
	}

	private static void escapeChar(final char c, final StringBuilder buf) {
		switch (c) {
			case '&':
				buf.append("&amp;");
				break;
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '"':
				buf.append("&quot;");
				break;
			case '\'':
				buf.append("&apos;");
				break;
			default:
				buf.append(c);
				break;
		}
	}

	public static String unescape(final String str) {
		if (str == null || str.length() == 0)
			return "";

		final StringBuilder buf = new StringBuilder();
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			final char c = str.charAt(i);
			if (c == '&') {
				final int pos = str.indexOf(";", i);
				if (pos == -1) {
					buf.append('&');
				} else if (str.charAt(i + 1) == '#') {
					final int val = Integer.parseInt(str.substring(i + 2, pos), 16);
					buf.append((char) val);
					i = pos;
				} else {
					final String substr = str.substring(i, pos + 1);
					if ("&amp;".equals(substr))
						buf.append('&');
					else if ("&lt;".equals(substr))
						buf.append('<');
					else if ("&gt;".equals(substr))
						buf.append('>');
					else if ("&quot;".equals(substr))
						buf.append('"');
					else if ("&apos;".equals(substr))
						buf.append('\'');
					else
						buf.append(substr);
					i = pos;
				}
			} else
				buf.append(c);
		}
		return buf.toString();
	}
}
