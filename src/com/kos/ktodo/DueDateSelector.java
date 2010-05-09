package com.kos.ktodo;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Due date selector. Activated by <code>onClick</code>.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public abstract class DueDateSelector implements View.OnClickListener {
	public void onClick(final View v) {
		final Context c = v.getContext();
		final AlertDialog.Builder b = new AlertDialog.Builder(c);
		b.setItems(new CharSequence[]{
				c.getString(R.string.today),
				c.getString(R.string.tomorrow),
				c.getString(R.string.one_week_later),
				c.getString(R.string.no_date),
				c.getString(R.string.choose_date)
		}, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				final Calendar now = Calendar.getInstance();
				switch (which) {
					case 0: //today
						onDueDateSelected(now.getTimeInMillis());
						break;
					case 1: //tomorrow
						now.add(Calendar.DAY_OF_MONTH, 1);
						onDueDateSelected(now.getTimeInMillis());
						break;
					case 2: //next week
						now.add(Calendar.DAY_OF_MONTH, 7);
						onDueDateSelected(now.getTimeInMillis());
						break;
					case 3: //no due date
						onDueDateSelected(null);
						break;
					default:
						chooseDate(c);
						break;
				}
			}
		});
		b.show();
	}

	private void chooseDate(final Context c) {
		final Calendar cur = Calendar.getInstance();
		final Long curMillis = getCurrentDueDate();
		if (curMillis != null)
			cur.setTimeInMillis(curMillis);
		new DatePickerDialog(c, new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
				final Calendar due = Calendar.getInstance();
				due.set(Calendar.YEAR, year);
				due.set(Calendar.MONTH, monthOfYear);
				due.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				onDueDateSelected(due.getTimeInMillis());
			}
		}, cur.get(Calendar.YEAR), cur.get(Calendar.MONTH), cur.get(Calendar.DAY_OF_MONTH)).show();
	}

	public abstract Long getCurrentDueDate();

	public abstract void onDueDateSelected(final Long dueDate);
}
