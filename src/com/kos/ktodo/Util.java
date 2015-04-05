package com.kos.ktodo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various utils.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class Util {
	public static void assume(final boolean c) {
		if (!c) throw new RuntimeException("assertion failed");
	}

	public static void assumeEquals(final String expected, final String found) {
		if (!expected.equals(found))
			throw new RuntimeException("assertion failed; expected '" + expected + "', found '" + found + "'");
	}

	public static void assumeEquals(final int expected, final int found) {
		if (expected != found)
			throw new RuntimeException("assertion failed; expected " + expected + ", found " + found);
	}

	public static void setupEditTextEnterListener(final EditText et, final AlertDialog dlg) {
		et.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					dlg.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
					return true;
				}
				return false;
			}
		});
	}

	public static DueStatus getDueStatus(final Long dueDate) {
		final Integer dueInDays = getDueInDays(dueDate);
		if (dueInDays == null) return DueStatus.NONE;
		if (dueInDays == 0)
			return DueStatus.TODAY;
		if (dueInDays < 0)
			return DueStatus.EXPIRED;
		return DueStatus.FUTURE;
//		if (dueDate == null) return DueStatus.NONE;
//		final Calendar due = Calendar.getInstance();
//		due.setTimeInMillis(dueDate);
//		final Calendar now = Calendar.getInstance();
//		if (due.get(Calendar.YEAR) < now.get(Calendar.YEAR)) return DueStatus.EXPIRED;
//		if (due.get(Calendar.YEAR) > now.get(Calendar.YEAR)) return DueStatus.FUTURE;
//		if (due.get(Calendar.MONTH) < now.get(Calendar.MONTH)) return DueStatus.EXPIRED;
//		if (due.get(Calendar.MONTH) > now.get(Calendar.MONTH)) return DueStatus.FUTURE;
//		if (due.get(Calendar.DAY_OF_MONTH) < now.get(Calendar.DAY_OF_MONTH)) return DueStatus.EXPIRED;
//		if (due.get(Calendar.DAY_OF_MONTH) > now.get(Calendar.DAY_OF_MONTH)) return DueStatus.FUTURE;
//		return DueStatus.TODAY;
	}

	public static Integer getDueInDays(final Long dueDate) {
		if (dueDate == null) return null;
		final Calendar due = Calendar.getInstance();
		due.setTimeInMillis(dueDate);
		killTime(due);
		final Calendar now = Calendar.getInstance();
		killTime(now);
		final long millisDiff = due.getTimeInMillis() - now.getTimeInMillis();
		final long daysDiff = millisDiff / DateUtils.DAY_IN_MILLIS;
		return (int) daysDiff;
	}

	private static void killTime(final Calendar due) {
		due.set(Calendar.HOUR_OF_DAY, 0);
		due.set(Calendar.MINUTE, 0);
		due.set(Calendar.SECOND, 0);
		due.set(Calendar.MILLISECOND, 0);
	}

	public static String showDueDate(final Context ctx, final Long dueDate) {
		if (dueDate == null) return null;

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		if (prefs.getBoolean("dueAsDaysLeft", false)) {
			final Integer dueInDays = getDueInDays(dueDate);
			switch (Math.abs(dueInDays)) {
				case 0:  return ctx.getString(R.string.day0, dueInDays);
				case 1:  return ctx.getString(R.string.day1, dueInDays);
				case 2:  return ctx.getString(R.string.day2, dueInDays);
				case 3:  return ctx.getString(R.string.day3, dueInDays);
				case 4:  return ctx.getString(R.string.day4, dueInDays);
				default: return ctx.getString(R.string.day5, dueInDays);
			}
		}

		final Calendar due = Calendar.getInstance();
		due.setTimeInMillis(dueDate);
		final Calendar now = Calendar.getInstance();

		final int year = due.get(Calendar.YEAR);
		if (now.get(Calendar.YEAR) != year)
			return getLongFormat().format(dueDate);
		else
			return getShortFormat(ctx).format(dueDate);
	}

	private static final Pattern leadingYearCut = Pattern.compile(".[yY]+.(.*)");
	private static DateFormat shortFormat;
	private static Locale shortFormatLocale;

	private static DateFormat getShortFormat(final Context ctx) {
		//there's no sane way to get locale-aware formatted date without a year
		final Locale l = Locale.getDefault();
		if (l.equals(shortFormatLocale))
			return shortFormat;

		final DateFormat full = getLongFormat();
		if (full instanceof SimpleDateFormat) {
			final SimpleDateFormat sdf = (SimpleDateFormat) full;
			String pat = sdf.toPattern();
			shortFormat = null;
			Matcher m = leadingYearCut.matcher(pat);
			if (m.matches())
				shortFormat = new SimpleDateFormat(m.group(1), Locale.getDefault());
			else {
				pat = new StringBuffer(pat).reverse().toString();
				m = leadingYearCut.matcher(pat);
				if (m.matches()) {
					pat = m.group(1);
					pat = new StringBuffer(pat).reverse().toString();
					shortFormat = new SimpleDateFormat(pat, Locale.getDefault());
				}
			}
			synchronized (Util.class) {
				if (shortFormat == null)
					shortFormat = new SimpleDateFormat(ctx.getString(R.string.due_date_format_short), Locale.getDefault());
			}
//			Log.i("Util", "short pat for " + Locale.getDefault() + " : " + ((SimpleDateFormat) shortFormat).toPattern());
		} else {
			shortFormat = new SimpleDateFormat(ctx.getString(R.string.due_date_format_short), Locale.getDefault());
		}
		shortFormatLocale = Locale.getDefault();
		return shortFormat;
	}

	private static DateFormat getLongFormat() {
		return DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
	}

	public static SimpleCursorAdapter createTagsAdapter(final Context ctx, final Cursor cursor, final int layout) {
		return new SimpleCursorAdapter(ctx, layout,
				cursor,
				new String[]{DBHelper.TAG_TAG}, new int[]{android.R.id.text1},
				0) {
			private int tagIDIndex = -1;

			@Override
			public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
				final View view = super.newView(context, cursor, parent);
				maybeLocalizeViewText(view, cursor);
				return view;
			}

			@Override
			public void bindView(@NotNull final View view, final Context context, @NotNull final Cursor cursor) {
				super.bindView(view, context, cursor);
				maybeLocalizeViewText(view, cursor);
			}

			private void maybeLocalizeViewText(final View view, final Cursor cursor) {
				if (tagIDIndex == -1) // technically we should invalidate it on swapCursor, but this index never changes
					tagIDIndex = cursor.getColumnIndexOrThrow(DBHelper.TAG_ID);

				if (view instanceof TextView) {
					final int tagID = cursor.getInt(tagIDIndex);
					if (tagID == DBHelper.ALL_TAGS_METATAG_ID)
						((TextView) view).setText(R.string.all);
					else if (tagID == DBHelper.UNFILED_METATAG_ID)
						((TextView) view).setText(R.string.unfiled);
				}
			}
		};
	}

	public static int getItemPosition(final CursorAdapter a, final long id) {
		final int cnt = a.getCount();
		for (int i = 0; i < cnt; i++)
			if (a.getItemId(i) == id)
				return i;
		return -1;
	}
}
