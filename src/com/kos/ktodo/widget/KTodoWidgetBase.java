package com.kos.ktodo.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.kos.ktodo.*;

public abstract class KTodoWidgetBase extends AppWidgetProvider {
	private static final String TAG = "KTodoWidgetBase";

	protected static final int[] ITEMS = new int[]{
			R.id.widget_item1,
			R.id.widget_item2,
			R.id.widget_item3,
			R.id.widget_item4,
			R.id.widget_item5,
			R.id.widget_item6,
			R.id.widget_item7,
			R.id.widget_item8,
			R.id.widget_item9,
			R.id.widget_item10,
			R.id.widget_item11,
			R.id.widget_item12,
			R.id.widget_item13,
			R.id.widget_item14,
			R.id.widget_item15,
			R.id.widget_item16,
			R.id.widget_item17,
			R.id.widget_item18,
	};
	protected static final int[] LINES = new int[]{
			R.id.widget_item1_line,
			R.id.widget_item2_line,
			R.id.widget_item3_line,
			R.id.widget_item4_line,
			R.id.widget_item5_line,
			R.id.widget_item6_line,
			R.id.widget_item7_line,
			R.id.widget_item8_line,
			R.id.widget_item9_line,
			R.id.widget_item10_line,
			R.id.widget_item11_line,
			R.id.widget_item12_line,
			R.id.widget_item13_line,
			R.id.widget_item14_line,
			R.id.widget_item15_line,
			R.id.widget_item16_line,
			R.id.widget_item17_line,
			R.id.widget_item17_line,
	};
	//implement real content provider?
	protected static final String AUTHORITY = "com.kos.ktodo";
	protected static final Uri WIDGET_URI = Uri.parse("content://" + AUTHORITY + "/appwidgets");

	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (appWidgetIds == null)
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, KTodoWidget22.class));
		UpdateService.requestUpdate(appWidgetIds);
		context.startService(new Intent(context, UpdateService.class));
	}

	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		final WidgetSettingsStorage wss = new WidgetSettingsStorage(context);
		wss.open();
		for (final int widgetId : appWidgetIds)
			wss.delete(widgetId);
		wss.close();
	}

	public static RemoteViews buildUpdate(final Context context, final int widgetId, final AppWidgetProviderInfo providerInfo) {
		final WidgetSizeInfo widgetSizeInfo = getWidgetSizeInfo(context, providerInfo);
		final int layout = widgetSizeInfo.getLayout();
		final int numLines = widgetSizeInfo.getNumLines();
		final RemoteViews views = new RemoteViews(context.getPackageName(), layout);

		views.setImageViewResource(R.id.widget_app_icon, R.drawable.icon_small);
		views.setImageViewResource(R.id.widget_setup_icon, R.drawable.settings);
		for (int i = 0; i < LINES.length; i++) {
			views.setViewVisibility(LINES[i], View.INVISIBLE);
			views.setViewVisibility(ITEMS[i], View.INVISIBLE);
		}

		final WidgetSettingsStorage settingsStorage = new WidgetSettingsStorage(context);
		settingsStorage.open();
		final WidgetSettings s = settingsStorage.load(widgetId);

		final Resources r = context.getResources();

		final TagsStorage tagsStorage = new TagsStorage(context);
		tagsStorage.open();
		int tagID = s.tagID;
		String tagName = tagsStorage.getTag(tagID);
		if (tagName == null) {
			tagID = DBHelper.ALL_TAGS_METATAG_ID;
			s.tagID = tagID;
			settingsStorage.save(s);
		}
		if (tagID == DBHelper.ALL_TAGS_METATAG_ID)
			tagName = r.getString(R.string.all);
		else if (tagID == DBHelper.UNFILED_METATAG_ID)
			tagName = r.getString(R.string.unfiled);

		views.setTextViewText(R.id.widget_tag, tagName);
		tagsStorage.close();
		settingsStorage.close();

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final int todayDueDateColor = prefs.getInt("dueTodayColor", r.getColor(R.color.today_due_date_color));
		final int expiredDueDateColor = prefs.getInt("overdueColor", r.getColor(R.color.expired_due_date_color));
		final int completedColor = r.getColor(R.color.widget_completed);
		final int defaultColor = r.getColor(R.color.white);

		final TodoItemsStorage itemsStorage = new TodoItemsStorage(context);
		itemsStorage.open();

		try {
			final Cursor c = itemsStorage.getByTagCursor(tagID, s.sortingMode);
			int i = 0;
			if (c.moveToFirst()) {
				do {
					final TodoItem item = itemsStorage.loadTodoItemFromCursor(c);
					if (!showItem(item, s))
						continue;
					if (i > 0)
						views.setViewVisibility(LINES[i - 1], View.VISIBLE);

					views.setImageViewResource(LINES[i], R.drawable.line);
					views.setTextViewText(ITEMS[i], item.summary);
					views.setTextColor(ITEMS[i], getItemColor(defaultColor, completedColor, todayDueDateColor, expiredDueDateColor, item));
					views.setViewVisibility(ITEMS[i], View.VISIBLE);
					if (++i >= numLines) break;
				} while (c.moveToNext());
			}
			c.close();
		} finally {
			itemsStorage.close();
		}

		final int FLAG_ACTIVITY_CLEAR_TASK = 32768; //In intent since API level 11

		final Intent configureIntent = new Intent(context, ConfigureActivity.class);
		configureIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		configureIntent.setData(ContentUris.withAppendedId(WIDGET_URI, widgetId));
		configureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
		final PendingIntent configurePendingIntent = PendingIntent.getActivity(context, 0, configureIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_setup_icon, configurePendingIntent);

		final Intent showTagIntent = new Intent(context, KTodo.class);
		showTagIntent.setAction(KTodo.SHOW_WIDGET_DATA);
		showTagIntent.setData(ContentUris.withAppendedId(WIDGET_URI, widgetId));
		showTagIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
		final PendingIntent showTagPendingIntent = PendingIntent.getActivity(context, 0, showTagIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_list, showTagPendingIntent);
		views.setOnClickPendingIntent(R.id.widget, showTagPendingIntent);

		return views;
	}

	private static WidgetSizeInfo getWidgetSizeInfo(final Context context, final AppWidgetProviderInfo providerInfo) {
		final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		final int mh = (int) (72f * displayMetrics.density);
		final int ratio = providerInfo.minHeight / mh;

		final int numLines; // = small ? 3 : LINES.length;
		final int layout; // = small ? R.layout.widget_2x1 : R.layout.widget_2x2;
		switch (ratio) {
			case 1:
				numLines = 3;
				layout = R.layout.widget_2x1;
				break;
			case 2:
				numLines = 8;
				layout = R.layout.widget_2x2;
				break;
			case 4:
				numLines = LINES.length;
				layout = R.layout.widget_2x4;
				break;
			default:
				Log.i(TAG, "can't recognize widget size, minHeight: " + providerInfo.minHeight + ", mh: " + mh + ", r: " + ratio);
				numLines = LINES.length;
				layout = R.layout.widget_2x4;
		}
//		Log.i(TAG, "ratio: " + ratio);
//		Log.i(TAG, "widget, small: " + small + ", medium: " + medium + ", minHeight: " + providerInfo.minHeight + ", mh -> " + mh);
		return new WidgetSizeInfo(layout, numLines);
	}

	private static int getItemColor(final int defaultColor, final int completedColor,
	                                final int dueTodayColor, final int expiredColor,
	                                final TodoItem i) {
		switch (Util.getDueStatus(i.getDueDate())) {
			case EXPIRED:
				return expiredColor;
			case TODAY:
				return dueTodayColor;
			default:
				if (i.isDone()) return completedColor;
				return defaultColor;
		}
	}

	private static boolean showItem(final TodoItem i, final WidgetSettings s) {
		if (s.hideCompleted && i.isDone()) return false;
		final int opts = (s.showOnlyDue ? 1 : 0) << 1 | (s.showOnlyDueIn == -1 ? 0 : 1);
		final Integer dd = Util.getDueInDays(i.getDueDate());
		switch (opts) {
			case 0:
				return true;
			case 1:
				return dd != null && dd >= 0 && dd <= s.showOnlyDueIn;
			case 2:
				return dd != null && dd < 0;
			case 3:
				return dd != null && (dd < 0 || (dd <= s.showOnlyDueIn));
			default:
				return true; //unreachable
		}
	}

	private static class WidgetSizeInfo {
		private final int numLines;
		private final int layout;

		private WidgetSizeInfo(final int layout, final int numLines) {
			this.layout = layout;
			this.numLines = numLines;
		}

		public int getNumLines() {
			return numLines;
		}

		public int getLayout() {
			return layout;
		}
	}
}
