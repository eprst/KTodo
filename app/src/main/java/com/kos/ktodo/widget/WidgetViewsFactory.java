package com.kos.ktodo.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.kos.ktodo.R;
import com.kos.ktodo.TodoItem;
import com.kos.ktodo.TodoItemsStorage;
import com.kos.ktodo.Util;
import com.kos.ktodo.preferences.Preferences;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	private final Context context;
	private final int widgetId;

	@Nullable
	private WidgetSettingsStorage widgetSettingsStorage;
	@Nullable
	private TodoItemsStorage todoItemsStorage;
	@Nullable
	private List<TodoItem> items;

	public WidgetViewsFactory(Context context, Intent intent) {
		this.context = context;
		this.widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	@Override
	public void onCreate() {
		widgetSettingsStorage = new WidgetSettingsStorage(context);
		widgetSettingsStorage.open();
		todoItemsStorage = new TodoItemsStorage(context);
		todoItemsStorage.open();
	}

	@Override
	public void onDataSetChanged() {
		if (widgetSettingsStorage != null &&
				todoItemsStorage != null &&
				widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {

			WidgetSettings widgetSettings = widgetSettingsStorage.load(widgetId);
			int tagID = widgetSettings.tagID;

			items = new ArrayList<>();

			Cursor cursor = todoItemsStorage.getByTagCursor(tagID, widgetSettings.sortingMode);
			if (cursor.moveToFirst()) {
				do {
					TodoItem item = todoItemsStorage.loadTodoItemFromCursor(cursor);
					if (showItem(item, widgetSettings)) {
						items.add(item);
					}
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
	}

	@Override
	public void onDestroy() {
		if (widgetSettingsStorage != null) {
			widgetSettingsStorage.close();
			widgetSettingsStorage = null;
		}

		if (todoItemsStorage != null) {
			todoItemsStorage.close();
			todoItemsStorage = null;
		}

		items = null;
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

	@Override
	public int getCount() {
		return items == null ? 0 : items.size();
	}

	@Override
	public RemoteViews getViewAt(int position) {
		if (items == null || position < 0 || position >= items.size()) {
			return null;
		} else {
			// todo cache?
			final Resources r = context.getResources();
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			final int todayDueDateColor = prefs.getInt(Preferences.DUE_TODAY_COLOR, r.getColor(R.color.today_due_date_color));
			final int expiredDueDateColor = prefs.getInt(Preferences.OVERDUE_COLOR, r.getColor(R.color.expired_due_date_color));
			final int completedColor = r.getColor(R.color.widget_completed);
			final int defaultColor = r.getColor(R.color.white);

			TodoItem item = items.get(position);

			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item);
			rv.setTextViewText(R.id.widget_item, item.summary);
			rv.setTextColor(R.id.widget_item, getItemColor(defaultColor, completedColor, todayDueDateColor, expiredDueDateColor, item));

			final Intent fillInIntent = new Intent();
			final Bundle bundle = new Bundle();
			bundle.putLong(KTodoWidgetProvider.ON_ITEM_CLICK_ITEM_ID_EXTRA, item.id);
			bundle.putLong(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
			fillInIntent.putExtras(bundle);
			rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

			return rv;
		}
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

	@Override
	public RemoteViews getLoadingView() {
		return null; // todo something better?
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}
}
