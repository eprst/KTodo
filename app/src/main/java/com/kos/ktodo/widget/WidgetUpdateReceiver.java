package com.kos.ktodo.widget;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import static com.kos.ktodo.widget.WidgetUpdateService.ACTION_UPDATE_ALL;


public class WidgetUpdateReceiver extends BroadcastReceiver {
    private final int JOB_ID = 837662934;
    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                ACTION_UPDATE_ALL.equals(intent.getAction())) {

            Intent updateAllIntent = new Intent(context, WidgetUpdateService.class);
            updateAllIntent.setAction(ACTION_UPDATE_ALL);

//			 todo replace with JobScheduler once we're on Oreo
//			WakefulBroadcastReceiver.startWakefulService(context, updateAllIntent);


            ComponentName comp = new ComponentName(context.getPackageName(),
                    WidgetUpdateService.class.getName());
            WidgetUpdateService.enqueueWork(context, comp, JOB_ID, intent);
        }
    }
}
