package com.kos.ktodo.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.kos.ktodo.*;

public class KTodoWidget extends AppWidgetProvider {
	private static final String TAG = "KTodoWidget";

	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (appWidgetIds == null)
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, KTodoWidget.class));
		UpdateService.requestUpdate(appWidgetIds);
		context.startService(new Intent(context, UpdateService.class));
		Log.i(TAG, "onUpdate...");
	}

	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		final WidgetSettingsStorage wss = new WidgetSettingsStorage(context);
		wss.open();
		for (final int widgetId : appWidgetIds) {
			Log.i(TAG, "delete: " + widgetId);
			wss.delete(widgetId);
		}
		wss.close();
	}

	public static RemoteViews buildUpdate(final Context context, final int widgetId) {
		Log.i(TAG, "update views: " + widgetId);
		final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

		final int[] items = new int[]{
				R.id.widget_item1,
				R.id.widget_item2,
				R.id.widget_item3,
				R.id.widget_item4,
				R.id.widget_item5,
				R.id.widget_item6,
				R.id.widget_item7,
				R.id.widget_item8,
		};
		final int[] lines = new int[]{
				R.id.widget_item1_line,
				R.id.widget_item2_line,
				R.id.widget_item3_line,
				R.id.widget_item4_line,
				R.id.widget_item5_line,
				R.id.widget_item6_line,
				R.id.widget_item7_line,
				R.id.widget_item7_line,
		};
		views.setImageViewResource(R.id.widget_app_icon, R.drawable.icon_small);
		views.setImageViewResource(R.id.widget_setup_icon, R.drawable.settings);
		for (int i = 0; i < lines.length; i++) {
			views.setViewVisibility(lines[i], View.INVISIBLE);
			views.setViewVisibility(items[i], View.INVISIBLE);
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
						views.setViewVisibility(lines[i - 1], View.VISIBLE);

					views.setImageViewResource(lines[i], R.drawable.line);
					views.setTextViewText(items[i], item.summary);
					views.setTextColor(items[i], getItemColor(r, item));
					views.setViewVisibility(items[i], View.VISIBLE);
					if (++i >= items.length) break;
				} while (c.moveToNext());
			}
			c.close();
		} finally {
			itemsStorage.close();
		}

		final Intent configureIntent = new Intent(context, ConfigureActivity.class);
		configureIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		configureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent configurePendingIntent = PendingIntent.getActivity(context, 0, configureIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_setup_icon, configurePendingIntent);

		final Intent showTagIntent = new Intent(context, KTodo.class);
		showTagIntent.setAction(KTodo.SHOW_WIDGET_DATA);
		showTagIntent.putExtra(KTodo.WIDGET_ID, widgetId);
		configureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
		if (!s.showOnlyDue && s.showOnlyDueIn == -1) return true;

		final IntegerPredicate onlyDuePred = new IntegerPredicate() {
			public boolean apply(final Integer i) {
				return s.showOnlyDue && i < 0;
			}
		};
		final IntegerPredicate onlyDueInPred = new IntegerPredicate() {
			public boolean apply(final Integer i) {
				return s.showOnlyDueIn != -1 && i <= s.showOnlyDueIn;
			}
		};

		final IntegerPredicate p = new AndPred(new NotNullPred(), new OrPred(onlyDuePred, onlyDueInPred));
		return p.apply(Util.getDueInDays(i.dueDate));
	}

	private static interface IntegerPredicate {
		boolean apply(final Integer i);
	}

	private static class NotNullPred implements IntegerPredicate {
		public boolean apply(final Integer i) {
			return i != null;
		}
	}

	private static class AndPred implements IntegerPredicate {
		private final IntegerPredicate p1, p2;

		private AndPred(final IntegerPredicate p1, final IntegerPredicate p2) {
			this.p1 = p1;
			this.p2 = p2;
		}

		public boolean apply(final Integer i) {
			return p1.apply(i) && p2.apply(i);
		}
	}

	private static class OrPred implements IntegerPredicate {
		private final IntegerPredicate p1, p2;

		private OrPred(final IntegerPredicate p1, final IntegerPredicate p2) {
			this.p1 = p1;
			this.p2 = p2;
		}

		public boolean apply(final Integer i) {
			return p1.apply(i) || p2.apply(i);
		}
	}
}
