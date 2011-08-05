package com.kos.ktodo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import static com.kos.ktodo.DBHelper.*;

public enum TodoItemsSortingMode {
	PRIO_DUE_SUMMARY(R.string.prio_due_summary, TODO_PRIO, TODO_DUE_DATE, TODO_SUMMARY),
	DUE_PRIO_SUMMARY(R.string.due_prio_summary, TODO_DUE_DATE, TODO_PRIO, TODO_SUMMARY),
	PRIO_SUMMARY_DUE(R.string.prio_summary_due, TODO_PRIO, TODO_SUMMARY, TODO_DUE_DATE),
	SUMMARY_PRIO_DUE(R.string.summary_prio_due, TODO_SUMMARY, TODO_PRIO, TODO_DUE_DATE),
	DUE_SUMMARY_PRIO(R.string.due_summary_prio, TODO_DUE_DATE, TODO_SUMMARY, TODO_PRIO),
	SUMMARY_DUE_PRIO(R.string.summary_due_prio, TODO_SUMMARY, TODO_DUE_DATE, TODO_PRIO);

	private final int resId;
	private final String orderBy;

	private TodoItemsSortingMode(final int resId, final String... cols) {
		this.resId = resId;
		final StringBuilder sb = new StringBuilder();
		for (final String col : cols) {
			if (sb.length() != 0)
				sb.append(", ");
			if (col.equals(TODO_DUE_DATE))
				sb.append("(").append(TODO_DUE_DATE).append(" is null)");
			else
				sb.append(col);
			sb.append(" ASC");
		}
		orderBy = sb.toString();
	}

	public int getResId() {
		return resId;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public static TodoItemsSortingMode fromOrdinal(final int ord) {
		return TodoItemsSortingMode.values()[ord];
	}

	public static void selectSortingMode(final Context c, final TodoItemsSortingMode def, final Callback1<TodoItemsSortingMode, Unit> callback) {
		final CharSequence[] items = new CharSequence[values().length];
		for (int i = 0; i < values().length; i++)
			items[i] = c.getString(values()[i].resId);
		final AlertDialog.Builder b = new AlertDialog.Builder(c);
		b.setTitle(R.string.sorting);
		b.setSingleChoiceItems(
				items,
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
