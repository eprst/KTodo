package com.kos.ktodo;

import android.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

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
}
