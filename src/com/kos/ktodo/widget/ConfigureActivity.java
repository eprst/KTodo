package com.kos.ktodo.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.kos.ktodo.R;
import com.kos.ktodo.TagsStorage;
import com.kos.ktodo.Util;

public class ConfigureActivity extends Activity {
	private static final String TAG = "ConfigureActivity";
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private WidgetSettingsStorage settingsStorage;
	private WidgetSettings settings;
	private TagsStorage tagsStorage;
	private Cursor allTagsCursor;
	private SimpleCursorAdapter tagsAdapter;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure);

		appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		Log.i(TAG, "configure: " + appWidgetId);

		settingsStorage = new WidgetSettingsStorage(this);
		settingsStorage.open();

		tagsStorage = new TagsStorage(this);
		tagsStorage.open();

		allTagsCursor = tagsStorage.getAllTagsCursor();
		startManagingCursor(allTagsCursor);

		tagsAdapter = Util.createTagsAdapter(this, allTagsCursor, android.R.layout.simple_spinner_item);
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		settings = settingsStorage.load(appWidgetId);

		setConfigureResult(Activity.RESULT_CANCELED);
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}

		initTagsSelector();
		initHideCompleted();
		initDue();
		initDueIn();
		initOKButton();
	}

	private void initTagsSelector() {
		final Spinner tagsWidget = getTagsWidget();
		tagsWidget.setAdapter(tagsAdapter);

		final int position = Util.getItemPosition(tagsAdapter, settings.tagID);
		if (position != -1)
			tagsWidget.setSelection(position);

		tagsWidget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				settings.tagID = (int) tagsWidget.getSelectedItemId();
			}

			public void onNothingSelected(final AdapterView<?> parent) {
			}
		});
	}

	private void initHideCompleted() {
		final CheckBox cb = (CheckBox) findViewById(R.id.conf_hide_completed);
		cb.setChecked(settings.hideCompleted);
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				settings.hideCompleted = isChecked;
			}
		});
	}

	private void initDueIn() {
		final CheckBox cb = (CheckBox) findViewById(R.id.conf_show_only_due_x);
		final Spinner dd = (Spinner) findViewById(R.id.conf_days_spinner);

		final ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, new Integer[]{
				0, 1, 2, 3, 4, 5, 6, 7});
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dd.setAdapter(adapter);

		cb.setChecked(settings.showOnlyDueIn != -1);
		dd.setEnabled(settings.showOnlyDueIn != -1);

		if (settings.showOnlyDueIn != -1)
			dd.setSelection(settings.showOnlyDueIn);

		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				dd.setEnabled(isChecked);
				if (!isChecked)
					settings.showOnlyDueIn = -1;
			}
		});

		dd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				settings.showOnlyDueIn = (int) id;
			}

			public void onNothingSelected(final AdapterView<?> parent) {
			}
		});
	}

	private void initDue() {
		final CheckBox cb = (CheckBox) findViewById(R.id.conf_show_only_due);
		cb.setChecked(settings.showOnlyDue);
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				settings.showOnlyDue = isChecked;
			}
		});
	}

	private void initOKButton() {
		findViewById(R.id.conf_ok).setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				settings.configured = true;
				final CheckBox cb = (CheckBox) findViewById(R.id.conf_show_only_due_x);
				if (!cb.isChecked())
					settings.showOnlyDueIn = -1;
				settingsStorage.save(settings);
				UpdateService.requestUpdate(new int[]{settings.widgetID});
				startService(new Intent(ConfigureActivity.this, UpdateService.class));
				setConfigureResult(Activity.RESULT_OK);
				finish();
			}
		});
	}

	@Override
	protected void onDestroy() {
		settingsStorage.close();
		allTagsCursor.close();
		tagsStorage.close();
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	public void setConfigureResult(final int resultCode) {
		final Intent data = new Intent();
		data.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(resultCode, data);
	}

	private Spinner getTagsWidget() {
		return (Spinner) findViewById(R.id.conf_tags);
	}
}
