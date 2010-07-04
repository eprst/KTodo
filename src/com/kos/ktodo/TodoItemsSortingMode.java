package com.kos.ktodo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import static com.kos.ktodo.DBHelper.*;

public enum TodoItemsSortingMode {
	PRIO_THEN_DUE, DUE_THEN_PRIO;

	private static final String PRIO_SORT = TODO_PRIO + " ASC";
	private static final String DUE_SORT = "(" + TODO_DUE_DATE + " is null) ASC, " + TODO_DUE_DATE + " ASC";

	public String getOrderBy() {
		switch (this) {
			case PRIO_THEN_DUE:
				return PRIO_SORT + ", " + DUE_SORT;
			case DUE_THEN_PRIO:
				return DUE_SORT + ", " + PRIO_SORT;
			default:
				throw new IllegalArgumentException("Unknown sorting mode: " + this);
		}
	}

	public static TodoItemsSortingMode fromOrdinal(final int ord) {
		return TodoItemsSortingMode.values()[ord];
	}

	public static void selectSortingMode(final Context c, final TodoItemsSortingMode def, final Callback1<TodoItemsSortingMode> callback) {
		final AlertDialog.Builder b = new AlertDialog.Builder(c);
		b.setTitle(R.string.sorting);
		b.setSingleChoiceItems(new CharSequence[]{
				c.getString(R.string.prio_then_due),
				c.getString(R.string.due_then_prio)},
				def.ordinal(),
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int which) {
						callback.call(fromOrdinal(which));
						dialog.dismiss();
					}
				});
		b.show();
	}
}
