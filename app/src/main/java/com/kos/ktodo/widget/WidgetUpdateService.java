package com.kos.ktodo.widget;


import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.RemoteViews;
import com.kos.ktodo.R;
import org.joda.time.DateTime;


public class WidgetUpdateService extends JobIntentService {
	public static final String ACTION_UPDATE_ALL = "com.kos.ktodo.widget.UPDATE_ALL";
	public static final String ACTION_UPDATE_WIDGETS = "com.kos.ktodo.widget.UPDATE_WIDGET";
	public static final String WIDGET_IDS_EXTRA = "com.kos.ktodo.widget.WIDGET_IDS";

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
	protected void onHandleWork(@NonNull Intent intent) {
		onHandleIntent(intent);
	}

	protected void onHandleIntent(@Nullable Intent intent) {
		if (intent != null) {
			if (ACTION_UPDATE_ALL.equals(intent.getAction())) {
				updateAll(this);
				// WakefulBroadcastReceiver.completeWakefulIntent(intent);
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
			updateWidgets(settingsStorage, widgetIds);
		} finally {
			settingsStorage.close();
		}
		scheduleMidnightUpdate();
	}

	private void updateWidget(int widgetId) {
		final WidgetSettingsStorage settingsStorage = new WidgetSettingsStorage(this);
		settingsStorage.open();
		try {
			updateWidgets(settingsStorage, new int[]{widgetId});
		} finally {
			settingsStorage.close();
		}
		scheduleMidnightUpdate();
	}

	private void updateWidgets(WidgetSettingsStorage settingsStorage, final int[] widgetIds) {
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
		for (int widgetId : widgetIds) {
			final WidgetSettings s = settingsStorage.load(widgetId);
			if (s.configured) {
				widgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list);
				final RemoteViews updViews = KTodoWidgetProvider.buildUpdate(this, widgetId);
				if (updViews != null) {
					widgetManager.partiallyUpdateAppWidget(widgetId, updViews);
				}
			}
		}
	}

	private void scheduleMidnightUpdate() {
		// todo use android.app.job.JobScheduler once we switch to Oreo
		// or, instead, use android:updatePeriodMillis in widget metadata?

		//schedule next update at midnight
		// todo switch to Java 8 LocalDateTime on Oreo
		DateTime today = new DateTime().withTimeAtStartOfDay();
		DateTime tomorrow = today.plusDays(1);

		final Intent intent = new Intent(ACTION_UPDATE_ALL);
		intent.setClass(this, WidgetUpdateReceiver.class);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		final AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		if (alarmMgr != null) {
			alarmMgr.set(AlarmManager.RTC_WAKEUP, tomorrow.getMillis(), pendingIntent);
		// todo change to this once on Marshmallow
//			alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

			Log.i(getClass().getName(), "Scheduled next widgets update at " + tomorrow);
		}
	}
}
