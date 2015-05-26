package com.kos.ktodo.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.widget.RemoteViews;

import com.kos.ktodo.DBHelper;
import com.kos.ktodo.R;
import com.kos.ktodo.TagsStorage;

public class KTodoWidget extends AppWidgetProvider {
	private static final String TAG = "KTodoWidgetBase";

	//implement real content provider?
	protected static final String AUTHORITY = "com.kos.ktodo";
	protected static final Uri WIDGET_URI = Uri.parse("content://" + AUTHORITY + "/appwidgets");

	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (appWidgetIds == null)
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, KTodoWidget.class));
		WidgetUpdateService.requestUpdate(appWidgetIds);
		context.startService(new Intent(context, WidgetUpdateService.class));
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
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

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

		remoteViews.setTextViewText(R.id.widget_tag, tagName);

		tagsStorage.close();
		settingsStorage.close();

		// update list items

		Intent intent = new Intent(context, WidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

		remoteViews.setRemoteAdapter(R.id.widget_list, intent);
		return remoteViews;
	}
}
