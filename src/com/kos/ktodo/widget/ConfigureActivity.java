package com.kos.ktodo.widget;

import android.app.Activity;
import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.kos.ktodo.*;

public class ConfigureActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
	@SuppressWarnings("UnusedDeclaration")
	private static final String TAG = "ConfigureActivity";
	public static final int TAGS_LOADER_ID = 0;

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private WidgetSettingsStorage settingsStorage;
	private WidgetSettings settings;
	private SimpleCursorAdapter tagsAdapter;

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

		tagsAdapter = Util.createTagsAdapter2(this, null, android.R.layout.simple_spinner_item);
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		settings = settingsStorage.load(appWidgetId); // TODO this should be done using Loaders too

		setConfigureResult(Activity.RESULT_CANCELED);
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}

		initHideCompleted();
		initDue();
		initDueIn();
		initSortingButton();
		initOKButton();

		getLoaderManager().initLoader(TAGS_LOADER_ID, null, this);
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

	private void initSortingButton() {
		updateSortingButtonText();
		final Button b = (Button) findViewById(R.id.conf_sorting);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				TodoItemsSortingMode.selectSortingMode(ConfigureActivity.this, settings.sortingMode, new Callback1<TodoItemsSortingMode, Unit>() {
					public Unit call(final TodoItemsSortingMode arg) {
						settings.sortingMode = arg;
						updateSortingButtonText();
						return Unit.u;
					}
				});
			}
		});
	}

	private Button updateSortingButtonText() {
		final Button b = (Button) findViewById(R.id.conf_sorting);
		b.setText(settings.sortingMode.getNameResId());
		return b;
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
		findViewById(R.id.conf_ok).setEnabled(false);
	}

	@Override
	protected void onDestroy() {
		settingsStorage.close();
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

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		switch (id) {
			case TAGS_LOADER_ID:
				return new CustomCursorLoader(this, TagsStorage.CHANGE_NOTIFICATION_URI) {
					private TagsStorage tagsStorage;

					@Override
					public Cursor createCursor() {
						tagsStorage = new TagsStorage(ConfigureActivity.this);
						tagsStorage.open();

						return tagsStorage.getAllTagsCursor();
					}

					@Override
					protected void onReset() {
						super.onReset();
						if (tagsStorage != null)
							tagsStorage.close();
					}
				};
			default:
				return null;
		}
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
		tagsAdapter.swapCursor(cursor);
		findViewById(R.id.conf_ok).setEnabled(true);
		initTagsSelector();
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		tagsAdapter.swapCursor(null);
	}
}
