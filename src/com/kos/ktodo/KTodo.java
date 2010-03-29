package com.kos.ktodo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class KTodo extends ListActivity {
	private static final String TAG = "KTodo";
	@SuppressWarnings({"FieldCanBeLocal"})
	private final int EDIT_TAGS_MENU_ITEM = Menu.FIRST;

	private TodoItemsStorage todoItemsStorage;
	private TagsStorage tagsStorage;
	private SimpleCursorAdapter tagsAdapter;
	private Cursor currentTagItemsCursor;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		todoItemsStorage = new TodoItemsStorage(this);
		todoItemsStorage.open();
		tagsStorage = new TagsStorage(this);
		tagsStorage.open();

		final Cursor allTagsCursor = tagsStorage.getAllTagsCursor();
		startManagingCursor(allTagsCursor);
		tagsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item,
				allTagsCursor,
				new String[]{DBHelper.TAG_TAG}, new int[]{android.R.id.text1});
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		getTagsWidget().setAdapter(tagsAdapter);

		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		setCurrentTag(preferences.getInt("currentTag", 0));

		getAddTaskButton().setOnClickListener(new View.OnClickListener() {
			public void onClick(final View view) {
				addTodoItem();
			}
		});

		onTagSelected();

		getTagsWidget().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				onTagSelected();
			}

			public void onNothingSelected(final AdapterView<?> parent) {
				onTagSelected();
			}
		});

		getMyListView().setDeleteItemListener(new MyListView.DeleteItemListener() {
			public void deleteItem(final long id) {
				todoItemsStorage.deleteTodoItem(id);
				updateView();
			}
		});

		getMyListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				final TodoItem todoItem = todoItemsStorage.loadTodoItem(id);
				todoItem.done = !todoItem.done;
				todoItemsStorage.saveTodoItem(todoItem);
				updateView();
			}
		});
	}

	private void onTagSelected() {
		if (currentTagItemsCursor != null) {
			stopManagingCursor(currentTagItemsCursor);
			currentTagItemsCursor.close();
		}
		currentTagItemsCursor = todoItemsStorage.getByTagCursor(getCurrentTagID());
		final int doneIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_DONE);
		startManagingCursor(currentTagItemsCursor);
		final ListAdapter todoAdapter = new SimpleCursorAdapter(
				this, android.R.layout.simple_list_item_checked,
				currentTagItemsCursor,
				new String[]{DBHelper.TODO_SUMMARY}, new int[]{android.R.id.text1}) {
			@Override
			public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
				final View view = super.newView(context, cursor, parent);
				final CheckedTextView ctv = (CheckedTextView) view;
				ctv.setChecked(cursor.getInt(doneIndex) != 0);
				return view;
			}

			@Override
			public void bindView(final View view, final Context context, final Cursor cursor) {
				super.bindView(view, context, cursor);
				view.getId();
				final CheckedTextView ctv = (CheckedTextView) view;
				ctv.setChecked(cursor.getInt(doneIndex) != 0);

			}
		};

		setListAdapter(todoAdapter);
		updateView();
	}

	@Override
	protected void onPause() {
		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		preferences.edit().putInt("currentTag", getTagsWidget().getSelectedItemPosition()).commit();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		todoItemsStorage.close();
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
		onTagSelected();
	}

	private void addTodoItem() {
		final long currentTagID = getCurrentTagID();
		if (currentTagID == AdapterView.INVALID_ROW_ID) {
			final AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setMessage(R.string.intro);
			b.show();
		} else {
			final EditText et = getAddTaskWidget();
			final String st = et.getText().toString();
			if (st.length() > 0) {
				todoItemsStorage.addTodoItem(new TodoItem(
						0, currentTagID, false, st, null));
				et.setText("");
				et.requestFocus();
				updateView();
			}
		}
	}

	private void updateView() {
		currentTagItemsCursor.requery();
		final boolean b = tagsAdapter.getCount() > 0;
//		getAddTaskButton().setEnabled(b);
	}

	private void setCurrentTag(final int idx) {
		if (idx < tagsAdapter.getCount())
			getTagsWidget().setSelection(idx);
	}

	private long getCurrentTagID() {
		return getTagsWidget().getSelectedItemId();
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

	private Button getAddTaskButton() {
		return (Button) findViewById(R.id.add_task_button);
	}

	private MyListView getMyListView() {
		return (MyListView) findViewById(android.R.id.list);
	}
}
