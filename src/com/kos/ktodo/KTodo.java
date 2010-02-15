package com.kos.ktodo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class KTodo extends Activity {
	private final int EDIT_TAGS_MENU_ITEM = Menu.FIRST;
	private List<String> allTags;

	public KTodo() {
		allTags = new ArrayList<String>();
		allTags.add("tag1");
		allTags.add("tag2");
		allTags.add("tag3");
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		final ArrayAdapter<String> tagsAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, getAllTags());
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		getTagsWidget().setAdapter(tagsAdapter);

		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		getTagsWidget().setSelection(preferences.getInt("currentTag", 0));
	}

	@Override
	protected void onPause() {
		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		preferences.edit().putInt("currentTag", getTagsWidget().getSelectedItemPosition()).commit();
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
		getTagsWidget().setSelection(savedInstanceState.getInt("currentTag"));
		final String addTaskText = savedInstanceState.getString("addTaskText");
		if (addTaskText != null) {
			final EditText taskWidget = getAddTaskWidget();
			taskWidget.setText(addTaskText);
			taskWidget.setSelection(savedInstanceState.getInt("addTaskSelStart"), savedInstanceState.getInt("addTaskSelEnd"));
		}
		loadList();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuItem item = menu.add(0, EDIT_TAGS_MENU_ITEM, Menu.NONE, R.string.edit_tags);
		item.setIntent(new Intent(this, EditTags.class));
		return true;
	}

	private List<String> getAllTags() {
		return allTags;
	}

	private EditText getAddTaskWidget() {
		return (EditText) findViewById(R.id.add_task);
	}

	private Spinner getTagsWidget() {
		return (Spinner) findViewById(R.id.tags);
	}

	private String getCurrentTag() {
		final Object selectedItem = getTagsWidget().getSelectedItem();
		if (selectedItem == null) return null;
		return selectedItem.toString();
	}

	private void loadList() {
	}
}
