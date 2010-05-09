package com.kos.ktodo;

import android.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

	public static boolean isDue(final Long dueDate) {
		if (dueDate == null) return false;
		final Calendar due = Calendar.getInstance();
		due.setTimeInMillis(dueDate);
		final Calendar now = Calendar.getInstance();
		if (due.get(Calendar.YEAR) < now.get(Calendar.YEAR)) return true;
		if (due.get(Calendar.YEAR) > now.get(Calendar.YEAR)) return false;
		if (due.get(Calendar.MONTH) < now.get(Calendar.MONTH)) return true;
		if (due.get(Calendar.MONTH) > now.get(Calendar.MONTH)) return false;
		return due.get(Calendar.DAY_OF_MONTH) < now.get(Calendar.DAY_OF_MONTH);
	}

	public static String showDueDate(final Long dueDate) {
		if (dueDate == null) return null;
		final Calendar due = Calendar.getInstance();
		due.setTimeInMillis(dueDate);
		//there's no sane way to get locale-aware formatted date without a year
		final String r1 = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).
				format(new Date(due.getTimeInMillis()));
		final Calendar now = Calendar.getInstance();
		final int year = due.get(Calendar.YEAR);
		if (now.get(Calendar.YEAR) != year)
			return r1;
		//try to cut off the year
		final String y = Integer.toString(year);
		final String y2 = y.substring(2); //last 2 digits of year
		if (r1.endsWith(y2) && !r1.endsWith(y))
			return r1.substring(0, r1.length() - 3);
		else
			return r1;

	}
}
