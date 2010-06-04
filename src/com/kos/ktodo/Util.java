package com.kos.ktodo;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

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
		if (dueDate == null) return DueStatus.NONE;
		final Calendar due = Calendar.getInstance();
		due.setTimeInMillis(dueDate);
		final Calendar now = Calendar.getInstance();
		if (due.get(Calendar.YEAR) < now.get(Calendar.YEAR)) return DueStatus.EXPIRED;
		if (due.get(Calendar.YEAR) > now.get(Calendar.YEAR)) return DueStatus.FUTURE;
		if (due.get(Calendar.MONTH) < now.get(Calendar.MONTH)) return DueStatus.EXPIRED;
		if (due.get(Calendar.MONTH) > now.get(Calendar.MONTH)) return DueStatus.FUTURE;
		if (due.get(Calendar.DAY_OF_MONTH) < now.get(Calendar.DAY_OF_MONTH)) return DueStatus.EXPIRED;
		if (due.get(Calendar.DAY_OF_MONTH) > now.get(Calendar.DAY_OF_MONTH)) return DueStatus.FUTURE;
		return DueStatus.TODAY;
	}

	public static String showDueDate(final Context ctx, final Long dueDate) {
		if (dueDate == null) return null;
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
	private static DateFormat shortFormat = null;
	private static Locale shortFormatLocale = null;

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
				shortFormat = new SimpleDateFormat(m.group(1));
			else {
				pat = new StringBuffer(pat).reverse().toString();
				m = leadingYearCut.matcher(pat);
				if (m.matches()) {
					pat = m.group(1);
					pat = new StringBuffer(pat).reverse().toString();
					shortFormat = new SimpleDateFormat(pat);
				}
			}
			if (shortFormat == null)
				shortFormat = new SimpleDateFormat(ctx.getString(R.string.due_date_format_short));
			Log.i("Util", "short pat for " + Locale.getDefault() + " : " + ((SimpleDateFormat) shortFormat).toPattern());
		} else {
			shortFormat = new SimpleDateFormat(ctx.getString(R.string.due_date_format_short));
		}
		shortFormatLocale = Locale.getDefault();
		return shortFormat;
	}

	private static DateFormat getLongFormat() {
		return DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
	}
}
