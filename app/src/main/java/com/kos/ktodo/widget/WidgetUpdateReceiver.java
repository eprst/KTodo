package com.kos.ktodo.widget;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.kos.ktodo.widget.WidgetUpdateService.ACTION_UPDATE_ALL;


public class WidgetUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
				Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
				ACTION_UPDATE_ALL.equals(intent.getAction())) {

			Log.i("WidgetUpdateReceiver", "KSS: onReceive: " + intent.getAction());
			Intent updateAllIntent = new Intent(context, WidgetUpdateService.class);
			updateAllIntent.setAction(ACTION_UPDATE_ALL);

//            ComponentName comp = new ComponentName(
//                    context.getPackageName(),
//                    WidgetUpdateService.class.getName()
//            );
			WidgetUpdateService.enqueueWork(context, WidgetUpdateService.class, WidgetUpdateService.JOB_ID, intent);
		}
	}
}
