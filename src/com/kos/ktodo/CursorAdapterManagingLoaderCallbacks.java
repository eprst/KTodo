package com.kos.ktodo;

import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.widget.CursorAdapter;

public abstract class CursorAdapterManagingLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
	private final CursorAdapter cursorAdapter;

	public CursorAdapterManagingLoaderCallbacks(final CursorAdapter cursorAdapter) {
		this.cursorAdapter = cursorAdapter;
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		cursorAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		cursorAdapter.swapCursor(null);
	}
}
