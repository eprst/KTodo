package com.kos.ktodo;

import android.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class KTodo extends ListActivity {
	private static final String TAG = "KTodo";
	@SuppressWarnings({"FieldCanBeLocal"})
	private final int EDIT_TAGS_MENU_ITEM = Menu.FIRST;
	private final int SHOW_HIDE_COMPLETED_MENU_ITEM = EDIT_TAGS_MENU_ITEM + 1;

	private final int EDIT_ITEM_CONTEXT_MENU_ITEM = Menu.FIRST;
	private final int DELETE_ITEM_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 1;

	private TodoItemsStorage todoItemsStorage;
	private TagsStorage tagsStorage;
	private SimpleCursorAdapter tagsAdapter;
	private Cursor currentTagItemsCursor;
	private boolean hidingCompleted;
	private TodoItem editingItem;

	private TodoItem lastDeletedItem;
	private long lastDeletedTimestamp;

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
		hidingCompleted = preferences.getBoolean("hidingCompleted", false);

		getAddTaskButton().setOnClickListener(new View.OnClickListener() {
			public void onClick(final View view) {
				addTodoItem();
			}
		});

		reloadTodoItems();

		getTagsWidget().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				reloadTodoItems();
			}

			public void onNothingSelected(final AdapterView<?> parent) {
				reloadTodoItems();
			}
		});

		final MyListView listView = getMyListView();
		listView.setDeleteItemListener(new MyListView.DeleteItemListener() {
			public void deleteItem(final long id) {
				lastDeletedItem = todoItemsStorage.loadTodoItem(id);
				lastDeletedTimestamp = System.currentTimeMillis();
				todoItemsStorage.deleteTodoItem(id);
				updateView();
			}
		});

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				final TodoItem todoItem = todoItemsStorage.loadTodoItem(id);
				todoItem.done = !todoItem.done;
				todoItemsStorage.saveTodoItem(todoItem);
				updateView();
			}
		});

		listView.setSlideLeftInfo(getSlidingView(), new MyListView.SlideLeftListener() {
			public void slideLeftStarted(final long id) {
				startEditingItem(id);
			}

			public void onSlideBack() {
				//Log.i(TAG, "slide back");
				final String summary = getEditSummaryWidget().getText().toString();
				if (editingItem != null && summary.length() > 0) {
					editingItem.summary = summary;
					editingItem.body = getEditBodyWidget().getText().toString();
					todoItemsStorage.saveTodoItem(editingItem);
					updateView();
				}
			}
		});

		registerForContextMenu(listView);
	}

	private void startEditingItem(final long id) {
		editingItem = todoItemsStorage.loadTodoItem(id);
		getEditSummaryWidget().setText(editingItem.summary);
		getEditBodyWidget().setText(editingItem.body);
	}

	private void reloadTodoItems() {
		if (currentTagItemsCursor != null) {
			stopManagingCursor(currentTagItemsCursor);
			currentTagItemsCursor.close();
		}

		if (hidingCompleted)
			currentTagItemsCursor = todoItemsStorage.getByTagCursorExcludingCompleted(getCurrentTagID());
		else
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
		final SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("currentTag", getTagsWidget().getSelectedItemPosition());
		editor.putBoolean("hidingCompleted", hidingCompleted).commit();
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
		outState.putBoolean("hidingCompleted", hidingCompleted);
		final EditText addTask = getAddTaskWidget();
		outState.putString("addTaskText", addTask.getText().toString());
		outState.putInt("addTaskSelStart", addTask.getSelectionStart());
		outState.putInt("addTaskSelEnd", addTask.getSelectionEnd());
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		setCurrentTag(savedInstanceState.getInt("currentTag"));
		hidingCompleted = savedInstanceState.getBoolean("hidingCompleted");
		final String addTaskText = savedInstanceState.getString("addTaskText");
		if (addTaskText != null) {
			final EditText taskWidget = getAddTaskWidget();
			taskWidget.setText(addTaskText);
			taskWidget.setSelection(savedInstanceState.getInt("addTaskSelStart"), savedInstanceState.getInt("addTaskSelEnd"));
		}
		reloadTodoItems();
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (getMyListView().handleBack())
				return true;
			if (lastDeletedItem != null && System.currentTimeMillis() - lastDeletedTimestamp < 3000) {
				todoItemsStorage.addTodoItem(lastDeletedItem);
				lastDeletedItem = null;
				updateView();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
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
						-1, currentTagID, false, st, null));
				et.setText("");
				et.requestFocus();
				updateView();
			}
		}
	}

	private void updateView() {
		currentTagItemsCursor.requery();
//		final boolean b = tagsAdapter.getCount() > 0;
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
	public boolean onPrepareOptionsMenu(final Menu menu) {
		menu.clear();
		final MenuItem item = menu.add(0, EDIT_TAGS_MENU_ITEM, Menu.NONE, R.string.edit_tags);
		item.setIntent(new Intent(this, EditTags.class));
		menu.add(0, SHOW_HIDE_COMPLETED_MENU_ITEM, Menu.NONE,
				hidingCompleted ? R.string.show_completed_items : R.string.hide_completed_items);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == SHOW_HIDE_COMPLETED_MENU_ITEM) {
			hidingCompleted = !hidingCompleted;
			reloadTodoItems();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_ITEM_CONTEXT_MENU_ITEM, Menu.NONE, R.string.edit);
		menu.add(0, DELETE_ITEM_CONTEXT_MENU_ITEM, Menu.NONE, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		if (info == null) return false;
		final long id = getListAdapter().getItemId(info.position);
		switch (item.getItemId()) {
			case EDIT_ITEM_CONTEXT_MENU_ITEM:
				startEditingItem(id);
				getSlidingView().switchRight();
				return true;
			case DELETE_ITEM_CONTEXT_MENU_ITEM:
				todoItemsStorage.deleteTodoItem(id);
				updateView();
				return true;
		}
		return super.onContextItemSelected(item);
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

	private SlidingView getSlidingView() {
		return (SlidingView) findViewById(R.id.sliding_view);
	}

	private EditText getEditSummaryWidget() {
		return (EditText) findViewById(R.id.edit_task_summary);
	}

	private EditText getEditBodyWidget() {
		return (EditText) findViewById(R.id.edit_task_body);
	}
}
