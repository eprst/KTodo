package com.kos.ktodo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import static com.kos.ktodo.R.string.*;

import static com.kos.ktodo.DBHelper.*;

public enum TodoItemsSortingMode {
	PRIO_DUE_SUMMARY(prio_due_summary, prio_due_summary_t, TODO_DONE, TODO_PRIO, TODO_DUE_DATE, TODO_SUMMARY),
	DUE_PRIO_SUMMARY(due_prio_summary, due_prio_summary_t, TODO_DONE, TODO_DUE_DATE, TODO_PRIO, TODO_SUMMARY),
	PRIO_SUMMARY_DUE(prio_summary_due, prio_summary_due_t, TODO_DONE, TODO_SUMMARY, TODO_DUE_DATE),
	SUMMARY_PRIO_DUE(summary_prio_due, summary_prio_due_t, TODO_DONE, TODO_SUMMARY, TODO_PRIO, TODO_DUE_DATE),
	DUE_SUMMARY_PRIO(due_summary_prio, due_summary_prio_t, TODO_DONE, TODO_DUE_DATE, TODO_SUMMARY, TODO_PRIO),
	SUMMARY_DUE_PRIO(summary_due_prio, summary_due_prio_t, TODO_DONE, TODO_SUMMARY, TODO_DUE_DATE, TODO_PRIO);

	private final int nameResId;
	private final int titleResId;
	private final String orderBy;

	private TodoItemsSortingMode(final int nameResId, final int titleResId, final String... cols) {
		this.nameResId = nameResId;
		this.titleResId = titleResId;
		final StringBuilder sb = new StringBuilder();
		for (final String col : cols) {
			if (sb.length() != 0)
				sb.append(", ");
			if (col.equals(TODO_DUE_DATE))
				sb.append("(").append(TODO_DUE_DATE).append(" is null) ASC, ");
			sb.append(col);
			sb.append(" ASC");
		}
		orderBy = sb.toString();
	}

	public int getNameResId() {
		return nameResId;
	}

	public int getTitleResId() {
		return titleResId;
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
			items[i] = c.getString(values()[i].nameResId);
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
