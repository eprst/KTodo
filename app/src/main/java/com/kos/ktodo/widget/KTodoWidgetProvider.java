package com.kos.ktodo.widget;


import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import com.kos.ktodo.DBHelper;
import com.kos.ktodo.KTodo;
import com.kos.ktodo.LastModifiedState;
import com.kos.ktodo.R;
import com.kos.ktodo.TagsStorage;
import com.kos.ktodo.TodoItem;
import com.kos.ktodo.TodoItemsStorage;
import org.jetbrains.annotations.NotNull;

public class KTodoWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "KTodoWidgetProvider";

	//implement real content provider?
	protected static final String AUTHORITY = "com.kos.ktodo";
	protected static final Uri WIDGET_URI = Uri.parse("content://" + AUTHORITY + "/appwidgets");

	protected static final String ON_ITEM_CLICK = "onItemClick";
	public static final String ON_ITEM_CLICK_ITEM_ID_EXTRA = "position"; // constant for on item click intent bundle

	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// widget update flow:
		// KTodoWidgetProvider.onUpdate // this method
		// WidgetUpdateService.requestUpdate(context,ids)
		// intent(ACTION_UPDATE_WIDGETS,ids) ~> WidgetUpdateService
		// WidgetUpdateService.updateWidget(id)
		//   RemoteViews rv = KTodoWidgetProvider.buildUpdate
		//    AppWidgetManager.updateAppWidget

		if (appWidgetIds == null)
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, KTodoWidgetProvider.class));
//		AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
		WidgetUpdateService.requestUpdate(context, appWidgetIds);
	}

	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		final WidgetSettingsStorage wss = new WidgetSettingsStorage(context);
		wss.open();
		for (final int widgetId : appWidgetIds)
			wss.delete(widgetId);
		wss.close();
	}

	@Override
	public void onReceive(@NotNull Context context, @NotNull Intent intent) {
		if (ON_ITEM_CLICK.equals(intent.getAction())) {
			final int invalid = Integer.MIN_VALUE;

			long widgetId = intent.getLongExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, invalid);
			long itemId = intent.getLongExtra(ON_ITEM_CLICK_ITEM_ID_EXTRA, invalid);

			if (widgetId != invalid && itemId != invalid) {
				// unfortunately can't get it from template, it gets reused by different widgets
				WidgetSettingsStorage settingsStorage = new WidgetSettingsStorage(context);
				settingsStorage.open();
				WidgetSettings settings = settingsStorage.load((int) widgetId);
				settingsStorage.close();

				WidgetItemOnClickAction action = settings.itemOnClickAction;

				boolean sendIntent = false;
				Intent intentToFire = new Intent(context, KTodo.class);

				switch (action) {
					case NONE:
						break;
					case OPEN_TAG:
						sendIntent = true;
						intentToFire.setAction(KTodo.SHOW_WIDGET_DATA);
						intentToFire.setData(ContentUris.withAppendedId(WIDGET_URI, widgetId));
						break;
					case EDIT_ITEM:
						sendIntent = true;
						intentToFire.setAction(KTodo.SHOW_WIDGET_ITEM_DATA);
						Uri uri = ContentUris.appendId(ContentUris.appendId(WIDGET_URI.buildUpon(), widgetId), itemId).build();
						intentToFire.setData(uri);
						break;
					case MARK_DONE:
						// todo move this to a service?
						TodoItemsStorage todoItemsStorage = new TodoItemsStorage(context);
						todoItemsStorage.open();
						TodoItem todoItem = todoItemsStorage.loadTodoItem(itemId);
						todoItem.setDone(!todoItem.isDone());
						todoItemsStorage.saveTodoItem(todoItem);
						todoItemsStorage.close();
						context.startService(new Intent(context, WidgetUpdateService.class));
						WidgetUpdateService.requestUpdateAll(context);
						LastModifiedState.touch(context);
						new BackupManager(context).dataChanged();
						break;
				}

				if (sendIntent) {
					intentToFire.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					context.startActivity(intentToFire);
				}
			}
		}
		super.onReceive(context, intent);
	}

	public static RemoteViews buildUpdate(final Context context, final int widgetId) {
		Log.i(TAG, "buildUpdate: " + widgetId);
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

		final WidgetSettingsStorage settingsStorage = new WidgetSettingsStorage(context);
		settingsStorage.open();
		final WidgetSettings settings = settingsStorage.load(widgetId);

		final Resources r = context.getResources();

		final TagsStorage tagsStorage = new TagsStorage(context);
		tagsStorage.open();
		int tagID = settings.tagID;
		String tagName = tagsStorage.getTag(tagID);
		if (tagName == null) {
			tagID = DBHelper.ALL_TAGS_METATAG_ID;
			settings.tagID = tagID;
			settingsStorage.save(settings);
		}
		if (tagID == DBHelper.ALL_TAGS_METATAG_ID)
			tagName = r.getString(R.string.all);
		else if (tagID == DBHelper.TODAY_METATAG_ID)
			tagName = r.getString(R.string.today);
		else if (tagID == DBHelper.UNFILED_METATAG_ID)
			tagName = r.getString(R.string.unfiled);

		remoteViews.setTextViewText(R.id.widget_tag, tagName);

		tagsStorage.close();
		settingsStorage.close();

		// update list items

		Intent intent = new Intent(context, WidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

		Log.i(TAG, "Setting remote view adapter to " + intent);
		remoteViews.setRemoteAdapter(R.id.widget_list, intent);

		final Intent configureIntent = new Intent(context, WidgetConfigureActivity.class);
		configureIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		configureIntent.setData(ContentUris.withAppendedId(WIDGET_URI, widgetId));
		configureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		final PendingIntent configurePendingIntent = PendingIntent.getActivity(context, 0, configureIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_setup_icon, configurePendingIntent);

		final Intent showTagIntent = new Intent(context, KTodo.class);
		showTagIntent.setAction(KTodo.SHOW_WIDGET_DATA);
		showTagIntent.setData(ContentUris.withAppendedId(WIDGET_URI, widgetId));
		showTagIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		final PendingIntent showTagPendingIntent = PendingIntent.getActivity(context, 0, showTagIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget, showTagPendingIntent);

		Intent itemClickIntent = new Intent(context, KTodoWidgetProvider.class);
		itemClickIntent.setAction(ON_ITEM_CLICK);
		PendingIntent itemClickPendingIntent = PendingIntent.getBroadcast(context, 0, itemClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setPendingIntentTemplate(R.id.widget_list, itemClickPendingIntent);

		return remoteViews;
	}
}
