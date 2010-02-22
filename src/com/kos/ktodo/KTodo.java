package com.kos.ktodo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class KTodo extends Activity {
	private final int EDIT_TAGS_MENU_ITEM = Menu.FIRST;

	private TagsStorage tagsStorage;
	private SimpleCursorAdapter tagsAdapter;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		tagsStorage = new TagsStorage(this, true);
		tagsStorage.open();

		final Cursor allTagsCursor = tagsStorage.getAllTagsCursor();
		startManagingCursor(allTagsCursor);
		tagsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item,
				allTagsCursor,
				new String[]{TagsStorage.TAG_NAME}, new int[]{android.R.id.text1});
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		getTagsWidget().setAdapter(tagsAdapter);

		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		setCurrentTag(preferences.getInt("currentTag", 0));
	}

	@Override
	protected void onPause() {
		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		preferences.edit().putInt("currentTag", getTagsWidget().getSelectedItemPosition()).commit();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		tagsStorage.close();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("currentTag", getTagsWidget().getSelectedItemPosition());
		final EditText addTask = getAddTaskWidget();
		outState.putString("addTaskText", addTask.getText().toString());
		outState.putInt("addTaskSelStart", addTask.getSelectionStart());
		outState.putInt("addTaskSelEnd", addTask.getSelectionEnd());
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		setCurrentTag(savedInstanceState.getInt("currentTag"));
		final String addTaskText = savedInstanceState.getString("addTaskText");
		if (addTaskText != null) {
			final EditText taskWidget = getAddTaskWidget();
			taskWidget.setText(addTaskText);
			taskWidget.setSelection(savedInstanceState.getInt("addTaskSelStart"), savedInstanceState.getInt("addTaskSelEnd"));
		}
		loadList();
	}

	private void setCurrentTag(final int idx) {
		if (idx < tagsAdapter.getCount())
			getTagsWidget().setSelection(idx);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuItem item = menu.add(0, EDIT_TAGS_MENU_ITEM, Menu.NONE, R.string.edit_tags);
		item.setIntent(new Intent(this, EditTags.class));
		return true;
	}

	private EditText getAddTaskWidget() {
		return (EditText) findViewById(R.id.add_task);
	}

	private Spinner getTagsWidget() {
		return (Spinner) findViewById(R.id.tags);
	}

	private void loadList() {
	}
}
