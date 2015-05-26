package com.kos.ktodo.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
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
			// todo cache?
			final Resources r = context.getResources();
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			final int todayDueDateColor = prefs.getInt("dueTodayColor", r.getColor(R.color.today_due_date_color));
			final int expiredDueDateColor = prefs.getInt("overdueColor", r.getColor(R.color.expired_due_date_color));
			final int completedColor = r.getColor(R.color.widget_completed);
			final int defaultColor = r.getColor(R.color.white);


			TodoItem item = items.get(position);

			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item);
			rv.setTextViewText(R.id.widget_item, item.summary);
			rv.setTextColor(R.id.widget_item, getItemColor(defaultColor, completedColor, todayDueDateColor, expiredDueDateColor, item));

//			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item_bitmap);
//
//			LayoutInflater inflater = LayoutInflater.from(context);
//			@SuppressLint("InflateParams")
//			TodoItemView itemView = (TodoItemView) inflater.inflate(R.layout.todo_item, null); // todo cache it?
//			itemView.setText(item.summary);
//			itemView.setPrio(item.prio);
//			itemView.setProgress(item.getProgress());
////			itemView.setCheckMarkDrawable(null); // show checked still?
//			Long dueDate = item.getDueDate();
//			item.setDueDate(dueDate);
//			if (dueDate != null) {
//				itemView.setDueDate(Util.showDueDate(context, dueDate), Util.getDueStatus(dueDate));
//			}
//			itemView.measure(500, 200);
//			int width = 500; // itemView.getMeasuredWidth
//			int height = 200; // itemView.getMeasuredHeight
//			itemView.layout(0, 0, width, height);
//
//			final Bitmap bm = Bitmap.createBitmap(itemView.getMeasuredWidth(), itemView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
//			final Canvas bitmapCanvas = new Canvas(bm);
//			itemView.draw(bitmapCanvas);
////			itemView.setDrawingCacheEnabled(true);
////			Bitmap bitmap = itemView.getDrawingCache();
//			rv.setImageViewBitmap(R.id.widget_item_bitmap, bm);


//			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.todo_item);// todo: separate item?
//			rv.setTextViewText(R.id.todo_item, item.summary);
//			rv.setInt(R.id.todo_item, "setPrio", item.prio);
//			rv.setInt(R.id.todo_item, "setProgress", item.getProgress());
//			rv.setBoolean(R.id.todo_item, "setShowNotesMark", false);
//
//			Long dueDate = item.getDueDate();
//			if (dueDate != null) {
//				rv.setString(R.id.todo_item, "setDueDate", Util.showDueDate(context, dueDate));
//				rv.setString(R.id.todo_item, "setDueDateStatus", Util.getDueStatus(dueDate).name());
//			}


			// todo set onclick listener?
/*			final Intent fillInIntent = new Intent();
			fillInIntent.setAction(WidgetProvider.ACTION_TOAST);
			final Bundle bundle = new Bundle();
			bundle.putString(WidgetProvider.EXTRA_STRING,
					mCollections.get(position));
			fillInIntent.putExtras(bundle);
			mView.setOnClickFillInIntent(android.R.id.text1, fillInIntent);*/

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
