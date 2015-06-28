package com.kos.ktodo;

import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.widget.CursorAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class CursorAdapterManagingLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
	private final CursorAdapter cursorAdapter;

	private List<Runnable> actionsOnLoadFinished = new ArrayList<>(); // actions to run on load finished
	private boolean loaded = false;

	public CursorAdapterManagingLoaderCallbacks(final CursorAdapter cursorAdapter) {
		this.cursorAdapter = cursorAdapter;
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		cursorAdapter.swapCursor(data);
		synchronized (this) {
			loaded = true;
			for (Runnable action : actionsOnLoadFinished) {
				action.run();
			}
			actionsOnLoadFinished.clear();
		}
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		cursorAdapter.swapCursor(null);
		synchronized (this) {
			loaded = false;
		}
	}

	public synchronized void addOnLoadFinishedAction(Runnable action) {
		if (loaded)
			action.run();
		else
			actionsOnLoadFinished.add(action);
	}
}
