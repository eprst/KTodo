package com.kos.ktodo;

import java.util.Calendar;
import java.util.Date;

public class TodoItem {
	public final long id;
	public long tagID;
	public boolean done;
	public String summary;
	public String body;
	public int prio = 1;
	public int progress = 0;
	public Long dueDate = null;
	public Integer caretPos = null;

//	public TodoItem(final long id, final long tagID) {
//		this.id = id;
//		this.tagID = tagID;
//	}

	public TodoItem(final long id, final long tagID, final boolean done, final String summary, final String body, final int prio, final int progress,
	                final Long dueDate, final Integer caretPos) {
		this.id = id;
		this.tagID = tagID;
		this.done = done;
		this.summary = summary;
		this.body = body;
		this.prio = prio;
		this.progress = progress;
		this.dueDate = resetTime(dueDate);
		this.caretPos = caretPos;
	}

	private Long resetTime(final Long date) {
		if (date == null) return null;
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(date);
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.HOUR_OF_DAY, 0);
		return c.getTimeInMillis();
	}

	public void setDone(final boolean done) {
		if (this.done != done) {
			this.done = done;
			if (!done && progress == 100)
				progress = 0;
//			progress = done ? 100 : 0;
		}
	}

	public void setProgress(final int progress) {
		if (this.progress != progress) {
			this.progress = progress;
			done = progress == 100;
		}
	}

	public int getProgress() {
		if (done) return 100;
		return progress;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TodoItem");
		sb.append("{body='").append(body).append('\'');
		sb.append(", id=").append(id);
		sb.append(", tagID=").append(tagID);
		sb.append(", done=").append(done);
		sb.append(", summary='").append(summary).append('\'');
		sb.append(", prio=").append(prio);
		sb.append(", progress=").append(prio);
		if (dueDate != null)
			sb.append(", dueDate=").append(new Date(dueDate));
		if (caretPos != null)
			sb.append(", caretPos=").append(caretPos);
		sb.append('}');
		return sb.toString();
	}
}
