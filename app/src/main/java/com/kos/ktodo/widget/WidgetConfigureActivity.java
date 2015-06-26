package com.kos.ktodo.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.kos.ktodo.CursorAdapterManagingLoaderCallbacks;
import com.kos.ktodo.CustomCursorLoader;
import com.kos.ktodo.R;
import com.kos.ktodo.TagsStorage;
import com.kos.ktodo.TodoItemsSortingMode;
import com.kos.ktodo.Util;

public class WidgetConfigureActivity extends Activity {
	@SuppressWarnings("UnusedDeclaration")
	private static final String TAG = "ConfigureActivity";
	public static final int TAGS_LOADER_ID = 0;

	@SuppressWarnings("RedundantFieldInitialization")
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private WidgetSettingsStorage settingsStorage;
	private WidgetSettings settings;
	private SimpleCursorAdapter tagsAdapter;
	private TagsLoaderCallbacks tagsLoaderCallbacks;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure);

		final Uri data = getIntent().getData();
		if (data != null)
			appWidgetId = (int) ContentUris.parseId(data);
		else
			appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

		settingsStorage = new WidgetSettingsStorage(this);
		settingsStorage.open();

		tagsAdapter = Util.createTagsAdapter(this, null, android.R.layout.simple_spinner_item);
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		settings = settingsStorage.load(appWidgetId); // TODO this should be done using Loaders too

		setConfigureResult(Activity.RESULT_CANCELED);
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}

		initItemOnClickActionSelector();
		initHideCompleted();
		initDue();
		initDueIn();
		initSortingSpinner();
		initOKButton();

		tagsLoaderCallbacks = new TagsLoaderCallbacks(this, tagsAdapter);
		getLoaderManager().initLoader(TAGS_LOADER_ID, null, tagsLoaderCallbacks);
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

	private void initItemOnClickActionSelector() {
		final Spinner spinner = getItemOnClickActionWidget();
		spinner.setSelection(settings.itemOnClickAction.ordinal());

		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				settings.itemOnClickAction = WidgetItemOnClickAction.fromOrdinal((int) spinner.getSelectedItemId());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

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

		final ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
				android.R.layout.simple_spinner_item, new Integer[]{0, 1, 2, 3, 4, 5, 6, 7});
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

	private void initSortingSpinner() {
		final Spinner s = (Spinner) findViewById(R.id.conf_sorting);
		String[] items = {
				getString(TodoItemsSortingMode.PRIO_DUE_SUMMARY.getNameResId()),
				getString(TodoItemsSortingMode.DUE_PRIO_SUMMARY.getNameResId()),
				getString(TodoItemsSortingMode.PRIO_SUMMARY_DUE.getNameResId()),
				getString(TodoItemsSortingMode.SUMMARY_PRIO_DUE.getNameResId()),
				getString(TodoItemsSortingMode.DUE_SUMMARY_PRIO.getNameResId()),
				getString(TodoItemsSortingMode.SUMMARY_DUE_PRIO.getNameResId())
		};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);
		s.setSelection(settings.sortingMode.ordinal());

		s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				settings.sortingMode = TodoItemsSortingMode.fromOrdinal(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

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
				WidgetUpdateService.requestUpdate(new int[]{settings.widgetID});
				startService(new Intent(WidgetConfigureActivity.this, WidgetUpdateService.class));
				setConfigureResult(Activity.RESULT_OK);
				finish();
			}
		});
		findViewById(R.id.conf_ok).setEnabled(false);
	}

	@Override
	protected void onDestroy() {
		settingsStorage.close();
		tagsLoaderCallbacks.shutdown();
		getLoaderManager().destroyLoader(TAGS_LOADER_ID);
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

	private Spinner getItemOnClickActionWidget() {
		return (Spinner) findViewById(R.id.conf_on_item_click);
	}

	private class TagsLoaderCallbacks extends CursorAdapterManagingLoaderCallbacks {
		private final Context ctx;
		private final TagsStorage tagsStorage;

		private TagsLoaderCallbacks(final Context ctx, final CursorAdapter cursorAdapter) {
			super(cursorAdapter);
			this.ctx = ctx;
			tagsStorage = new TagsStorage(ctx);
			tagsStorage.open();
		}

		@Override
		public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
			return new CustomCursorLoader(ctx, TagsStorage.CHANGE_NOTIFICATION_URI) {

				@Override
				public Cursor createCursor() {
					return tagsStorage.getAllTagsCursor();
				}
			};
		}

		@Override
		public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
			super.onLoadFinished(loader, data);
			findViewById(R.id.conf_ok).setEnabled(true);
			initTagsSelector();
		}

		public void shutdown() {
			tagsStorage.close();
		}
	}
}
