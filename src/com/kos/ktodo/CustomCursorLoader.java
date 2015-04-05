package com.kos.ktodo;

import android.content.Context;
import android.content.CursorLoader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

public abstract class CustomCursorLoader extends CursorLoader {
	private final Uri notificationUri;
	private final ContentObserver observer;

	public CustomCursorLoader(final Context context, final Uri notificationUri) {
		super(context);
		this.notificationUri = notificationUri;
		observer = new ForceLoadContentObserver();
	}

	public abstract Cursor createCursor();

	@Override
	public Cursor loadInBackground() {
		final Cursor cursor = createCursor();

		if (notificationUri != null) {
			// Ensure the cursor window is filled
			cursor.getCount();
			cursor.registerContentObserver(observer);
			cursor.setNotificationUri(getContext().getContentResolver(), notificationUri);
		}

		return cursor;
	}
}
