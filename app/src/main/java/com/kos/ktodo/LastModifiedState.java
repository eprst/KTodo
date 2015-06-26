package com.kos.ktodo;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Holds the 'last modified' marker for backup/restore.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com" title="">Konstantin Sobolev</a>
 * @version $Revision$
 */
public class LastModifiedState {
	public static final String TAG = "LastModifiedState";
	public static final String FILE_NAME = "last_modified";

	private static File getLastModFile(final Context ctx) {
		return new File(ctx.getFilesDir(), FILE_NAME);
	}

	public static void touch(final Context ctx) {
		final File lastModFile = getLastModFile(ctx);
		if (!lastModFile.exists())
			try {
				if (!lastModFile.createNewFile())
					Log.i(TAG, "Failed to create last mod file");
			} catch (final IOException e) {
				Log.i(TAG, "Error creating last mod file: " + e);
			}
		else if (!lastModFile.setLastModified(System.currentTimeMillis()))
			Log.i(TAG, "touch failed");
	}

	public static Long getLastModified(final Context ctx) {
		final File lastModFile = getLastModFile(ctx);
		return lastModFile.lastModified();
	}
}
