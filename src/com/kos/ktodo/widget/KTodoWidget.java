package com.kos.ktodo.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import com.kos.ktodo.*;

public class KTodoWidget extends AppWidgetProvider {
	private static final String TAG = "KTodoWidget";
	private static final int[] ITEMS = new int[]{
			R.id.widget_item1,
			R.id.widget_item2,
			R.id.widget_item3,
			R.id.widget_item4,
			R.id.widget_item5,
			R.id.widget_item6,
			R.id.widget_item7,
			R.id.widget_item8,
	};
	private static final int[] LINES = new int[]{
			R.id.widget_item1_line,
			R.id.widget_item2_line,
			R.id.widget_item3_line,
			R.id.widget_item4_line,
			R.id.widget_item5_line,
			R.id.widget_item6_line,
			R.id.widget_item7_line,
			R.id.widget_item7_line,
	};
	//implement real content provider?
	private static final String AUTHORITY = "com.kos.ktodo";
	private static final Uri WIDGET_URI = Uri.parse("content://" + AUTHORITY + "/appwidgets");

	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (appWidgetIds == null)
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, KTodoWidget.class));
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

	public static RemoteViews buildUpdate(final Context context, final int widgetId) {
		final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

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

		final TodoItemsStorage itemsStorage = new TodoItemsStorage(context);
		itemsStorage.open();

		try {
			final Cursor c = itemsStorage.getByTagCursor(tagID);
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
					views.setTextColor(ITEMS[i], getItemColor(r, item));
					views.setViewVisibility(ITEMS[i], View.VISIBLE);
					if (++i >= ITEMS.length) break;
				} while (c.moveToNext());
			}
			c.close();
		} finally {
			itemsStorage.close();
		}


		final Intent configureIntent = new Intent(context, ConfigureActivity.class);
		configureIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		configureIntent.setData(ContentUris.withAppendedId(WIDGET_URI, widgetId));
		configureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
		final PendingIntent configurePendingIntent = PendingIntent.getActivity(context, 0, configureIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_setup_icon, configurePendingIntent);

		final Intent showTagIntent = new Intent(context, KTodo.class);
		showTagIntent.setAction(KTodo.SHOW_WIDGET_DATA);
		showTagIntent.setData(ContentUris.withAppendedId(WIDGET_URI, widgetId));
		showTagIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
		final PendingIntent showTagPendingIntent = PendingIntent.getActivity(context, 0, showTagIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_list, showTagPendingIntent);

		return views;
	}

	private static int getItemColor(final Resources r, final TodoItem i) {
		switch (Util.getDueStatus(i.dueDate)) {
			case EXPIRED:
				return r.getColor(R.color.expired_due_date_color);
			case TODAY:
				return r.getColor(R.color.today_due_date_color);
			default:
				if (i.done) return r.getColor(R.color.widget_completed);
				return r.getColor(R.color.white);
		}
	}

	private static boolean showItem(final TodoItem i, final WidgetSettings s) {
		if (s.hideCompleted && i.done) return false;
		final int opts = (s.showOnlyDue ? 1 : 0) << 1 | (s.showOnlyDueIn == -1 ? 0 : 1);
		final Integer dd = Util.getDueInDays(i.dueDate);
		switch (opts) {
			case 0: return true;
			case 1: return dd != null && dd >= 0 && dd <= s.showOnlyDueIn;
			case 2: return dd != null && dd < 0;
			case 3: return dd != null && (dd < 0 || (dd <= s.showOnlyDueIn));
			default: return true; //unreachable
		}
	}
}
