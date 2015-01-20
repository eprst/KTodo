package com.kos.ktodo;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;

public abstract class CustomCursorLoader extends CursorLoader {
	private final Uri notificationUri;

	public CustomCursorLoader(Context context, Uri notificationUri) {
		super(context);
		this.notificationUri = notificationUri;
	}

	public abstract Cursor createCursor();

	@Override
	public Cursor loadInBackground() {
		Cursor cursor = createCursor();
		if (notificationUri != null)
			cursor.setNotificationUri(getContext().getContentResolver(), notificationUri);

		return cursor;
	}
}
