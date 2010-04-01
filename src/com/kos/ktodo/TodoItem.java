package com.kos.ktodo;

public class TodoItem {
	public final long id;
	public long tagID;
	public boolean done;
	public String summary;
	public String body;
	public int prio = 1;

	public TodoItem(final long id, final long tagID) {
		this.id = id;
		this.tagID = tagID;
	}

	public TodoItem(final long id, final long tagID, final boolean done, final String summary, final String body, final int prio) {
		this.id = id;
		this.tagID = tagID;
		this.done = done;
		this.summary = summary;
		this.body = body;
		this.prio = prio;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TodoItem");
		sb.append("{body='").append(body).append('\'');
		sb.append(", id=").append(id);
		sb.append(", tagID=").append(tagID);
		sb.append(", done=").append(done);
		sb.append(", summary='").append(summary).append('\'');
		sb.append(", prio=").append(prio);
		sb.append('}');
		return sb.toString();
	}
}
