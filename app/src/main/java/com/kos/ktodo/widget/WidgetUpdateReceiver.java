package com.kos.ktodo.widget;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import static com.kos.ktodo.widget.WidgetUpdateService.ACTION_UPDATE_ALL;


public class WidgetUpdateReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {

		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
				ACTION_UPDATE_ALL.equals(intent.getAction())) {

			Intent updateAllIntent = new Intent(context, WidgetUpdateService.class);
			updateAllIntent.setAction(ACTION_UPDATE_ALL);

			// todo replace with JobScheduler once we're on Oreo
			WakefulBroadcastReceiver.startWakefulService(context, updateAllIntent);
		}
	}
}
