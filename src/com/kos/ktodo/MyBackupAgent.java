package com.kos.ktodo;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;
import com.kos.ktodo.impex.XmlExporter;
import com.kos.ktodo.impex.XmlImporter;

import java.io.*;

/**
 * Backup agent.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com" title="">Konstantin Sobolev</a>
 * @version $Revision$
 */
public class MyBackupAgent extends BackupAgent {
	public static final String ENTITY_KEY = "main_data";
	public static final String TAG = "MyBackupAgent";

	@Override
	public void onBackup(final ParcelFileDescriptor oldState, final BackupDataOutput data, final ParcelFileDescriptor newState) throws IOException {
//		Log.i(TAG, "onBackup");
		final FileInputStream fis = new FileInputStream(oldState.getFileDescriptor());
		final DataInputStream dis = new DataInputStream(fis);
		Long lastMod = null;
		try {
			final long stMod = dis.readLong();
			lastMod = LastModifiedState.getLastModified(this);
			if (lastMod != null && lastMod == stMod) {
//				Log.i(TAG, "last mod check failed, skipping backup");
				return;
			}
		} catch (final IOException e) {
//			Log.i(TAG, "Error checking lasmod: " + e);
		} finally {
			dis.close();
			fis.close();
		}

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		XmlExporter.exportData(this, bos);
		final byte[] bytes = bos.toByteArray();

		data.writeEntityHeader(ENTITY_KEY, bytes.length);
		data.writeEntityData(bytes, bytes.length);

		if (lastMod == null) {
			LastModifiedState.touch(this);
			lastMod = LastModifiedState.getLastModified(this);
		}
		updateNewState(lastMod, newState);
//		Log.i(TAG, "wrote " + bytes.length + " bytes");
	}

	private void updateNewState(final Long lastMod, final ParcelFileDescriptor newState) throws IOException {
		if (lastMod != null) {
//			Log.i(TAG, "updated new state: " + lastMod);
			final FileOutputStream fos = new FileOutputStream(newState.getFileDescriptor());
			final DataOutputStream dos = new DataOutputStream(fos);
			dos.writeLong(lastMod);
			dos.close();
			fos.close();
		}
	}

	@Override
	public void onRestore(final BackupDataInput data, final int appVersionCode, final ParcelFileDescriptor newState) throws IOException {
//		Log.i(TAG, "onRestore");
		while (data.readNextHeader()) {
			if (ENTITY_KEY.equals(data.getKey())) {
				final int dataSize = data.getDataSize();
				final byte[] bytes = new byte[dataSize];
//				Log.i(TAG, "received " + dataSize + " bytes");
				data.readEntityData(bytes, 0, dataSize);
				final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
				XmlImporter.importData(this, bis, false);
				LastModifiedState.touch(this);
			} else
				data.skipEntityData();
		}
		updateNewState(LastModifiedState.getLastModified(this), newState);
	}
}
