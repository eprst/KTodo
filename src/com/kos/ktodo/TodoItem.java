package com.kos.ktodo;

public class TodoItem {
	public final long id;
	public final long tagID;
	public boolean done;
	public String summary;
	public String body;

	public TodoItem(final long id, final long tagID) {
		this.id = id;
		this.tagID = tagID;
	}

	public TodoItem(final long id, final long tagID, final boolean done, final String summary, final String body) {
		this.id = id;
		this.tagID = tagID;
		this.done = done;
		this.summary = summary;
		this.body = body;
	}
}
