package com.kos.ktodo.widget;


import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;
import com.kos.ktodo.R;


public class WidgetUpdateService extends IntentService {
	public static final String ACTION_UPDATE_ALL = "com.kos.ktodo.widget.UPDATE_ALL";
	public static final String ACTION_UPDATE_WIDGETS = "com.kos.ktodo.widget.UPDATE_WIDGET";
	public static final String WIDGET_IDS_EXTRA = "com.kos.ktodo.widget.WIDGET_IDS";

	private static final String TAG = "WidgetUpdateService";

	public WidgetUpdateService() {
		super(WidgetUpdateService.class.getName());
	}

	public static void requestUpdate(final Context context, final int[] appWidgetIds) {
		Intent intent = new Intent();
		intent.setAction(ACTION_UPDATE_WIDGETS);
		intent.putExtra(WIDGET_IDS_EXTRA, appWidgetIds);
		intent.setClass(context, WidgetUpdateService.class);

		context.startService(intent);
	}

	public static void requestUpdateAll(final Context context) {
		Intent intent = new Intent();
		intent.setAction(ACTION_UPDATE_ALL);
		intent.setClass(context, WidgetUpdateService.class);

		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(@Nullable Intent intent) {
		if (intent != null) {
			if (ACTION_UPDATE_ALL.equals(intent.getAction())) {
				updateAll(this);
			} else if (ACTION_UPDATE_WIDGETS.equals(intent.getAction())) {
				int[] widgetIds = intent.getIntArrayExtra(WIDGET_IDS_EXTRA);
				for (int widgetId : widgetIds) {
					if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
						updateWidget(widgetId);
					}
				}
			}
		}
	}

	private void updateAll(final Context ctx) {
		final AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
		final WidgetSettingsStorage settingsStorage = new WidgetSettingsStorage(this);
		int[] widgetIds = manager.getAppWidgetIds(new ComponentName(ctx, KTodoWidgetProvider.class));
		settingsStorage.open();
		try {
			for (int widgetId : widgetIds) {
				updateWidget(settingsStorage, widgetId);
			}
		} finally {
			settingsStorage.close();
		}
		scheduleMidnightUpdate();
	}

	private void updateWidget(int widgetId) {
		final WidgetSettingsStorage settingsStorage = new WidgetSettingsStorage(this);
		settingsStorage.open();
		try {
			updateWidget(settingsStorage, widgetId);
		} finally {
			settingsStorage.close();
		}
		scheduleMidnightUpdate();
	}

	// todo this should take a batch of IDs
	private void updateWidget(WidgetSettingsStorage settingsStorage, final int widgetId) {
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
		final WidgetSettings s = settingsStorage.load(widgetId);
		if (s.configured) {
			Log.i(TAG, "Updating widget " + widgetId);
			widgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list);
			final RemoteViews updViews = KTodoWidgetProvider.buildUpdate(this, widgetId);
			if (updViews != null) {
				widgetManager.partiallyUpdateAppWidget(widgetId, updViews);
			}
		}
	}

	private void scheduleMidnightUpdate() {
		// todo use android.app.job.JobScheduler once we switch to Oreo
		// or, instead, use android:updatePeriodMillis in widget metadata?

		//schedule next update at midnight
		final GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS);
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);

		final Intent intent = new Intent(ACTION_UPDATE_ALL);
		intent.setClass(this, this.getClass());
		final PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		if (alarmMgr != null) {
			alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
		}
	}
}
