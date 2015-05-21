package com.kos.ktodo.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.kos.ktodo.R;
import com.kos.ktodo.TodoItem;
import com.kos.ktodo.TodoItemsStorage;
import com.kos.ktodo.Util;

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
			TodoItem item = items.get(position);
			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.todo_item);// todo: separate item?
			rv.setTextViewText(R.id.todo_item, item.summary);
			rv.setInt(R.id.todo_item, "setPrio", item.prio);
			rv.setInt(R.id.todo_item, "setProgress", item.getProgress());
			rv.setBoolean(R.id.todo_item, "setShowNotesMark", false);

			Long dueDate = item.getDueDate();
			if (dueDate != null) {
				rv.setString(R.id.todo_item, "setDueDate", Util.showDueDate(context, dueDate));
				rv.setString(R.id.todo_item, "setDueDateStatus", Util.getDueStatus(dueDate).name());
			}

			return rv;
		}
	}

	@Override
	public RemoteViews getLoadingView() {
		return new RemoteViews(context.getPackageName(), R.layout.widget_loading);
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
