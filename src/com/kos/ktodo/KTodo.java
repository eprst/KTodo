package com.kos.ktodo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
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
	private final int CHANGE_TAG_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 1;
	private final int CHANGE_PRIO_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 2;
	private final int DELETE_ITEM_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 3;

	private TodoItemsStorage todoItemsStorage;
	private TagsStorage tagsStorage;
	private SimpleCursorAdapter tagsAdapter;
	private Cursor currentTagItemsCursor;
	private boolean hidingCompleted;
	private int defaultPrio;

	private TodoItem editingItem;
	private Cursor edititgItemTagsCursor;

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
		tagsAdapter = createTagsAdapter(allTagsCursor, android.R.layout.simple_spinner_item);
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		getTagsWidget().setAdapter(tagsAdapter);

		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		setCurrentTag(preferences.getLong("currentTag", 0));
		hidingCompleted = preferences.getBoolean("hidingCompleted", false);
		setDefaultPrio(preferences.getInt("defaultPrio", 1));

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

		getEditItemTagsWidget().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				editingItem.tagID = id;
			}

			public void onNothingSelected(final AdapterView<?> parent) {
			}
		});


		getPrioButton().setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				selectPrio(new PrioSelectedCallback() {
					public void prioSelected(final int prio) {
						setDefaultPrio(prio);
					}
				});
			}
		});

		registerForContextMenu(listView);
		setupEditPrioButtons();
	}

	private SimpleCursorAdapter createTagsAdapter(final Cursor cursor, final int layout) {
		final int tagIDIndex = cursor.getColumnIndexOrThrow(DBHelper.TAG_ID);
		return new SimpleCursorAdapter(this, layout,
				cursor,
				new String[]{DBHelper.TAG_TAG}, new int[]{android.R.id.text1}) {
			@Override
			public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
				final View view = super.newView(context, cursor, parent);
				maybeLocalizeViewText(view, cursor);
				return view;
			}

			@Override
			public void bindView(final View view, final Context context, final Cursor cursor) {
				super.bindView(view, context, cursor);
				maybeLocalizeViewText(view, cursor);
			}

			private void maybeLocalizeViewText(final View view, final Cursor cursor) {
				if (view instanceof TextView) {
					final int tagID = cursor.getInt(tagIDIndex);
					if (tagID == DBHelper.ALL_TAGS_METATAG_ID)
						((TextView) view).setText(R.string.all);
					else if (tagID == DBHelper.UNFILED_TAG_ID)
						((TextView) view).setText(R.string.unfiled);
				}
			}

		};
	}

	private void startEditingItem(final long id) {
		editingItem = todoItemsStorage.loadTodoItem(id);
		getEditSummaryWidget().setText(editingItem.summary);
		getEditBodyWidget().setText(editingItem.body);

		if (edititgItemTagsCursor != null)
			edititgItemTagsCursor.close();
		edititgItemTagsCursor = tagsStorage.getAllTagsExceptCursor(DBHelper.ALL_TAGS_METATAG_ID);
		startManagingCursor(edititgItemTagsCursor);
		final SimpleCursorAdapter editingItemTagsAdapter = createTagsAdapter(edititgItemTagsCursor, android.R.layout.simple_spinner_item);
		editingItemTagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		final Spinner spinner = getEditItemTagsWidget();
		spinner.setAdapter(editingItemTagsAdapter);
		final int position = getItemPosition(editingItemTagsAdapter, editingItem.tagID);
		if (position != -1)
			spinner.setSelection(position);


		updateEditPrioButtons();
	}

	private int getItemPosition(final CursorAdapter a, final long id) {
		final int cnt = a.getCount();
		for (int i = 0; i < cnt; i++)
			if (a.getItemId(i) == id)
				return i;
		return -1;
	}

	private void setupEditPrioButtons() {
		final View.OnClickListener onClickListener = new View.OnClickListener() {
			public void onClick(final View v) {
				editingItem.prio = Integer.parseInt(((TextView) v).getText().toString());
				updateEditPrioButtons();
			}
		};
		final Button[] editPrioButtons = getEditPrioButtons();
		for (int i = 1; i < editPrioButtons.length; i++) {
			editPrioButtons[i].setOnClickListener(onClickListener);
		}
	}

	private void updateEditPrioButtons() {
		final ToggleButton[] editPrioButtons = getEditPrioButtons();
		for (int i = 1; i < editPrioButtons.length; i++)
			editPrioButtons[i].setPushed(i == editingItem.prio);
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
		final int prioIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_PRIO);
		startManagingCursor(currentTagItemsCursor);
		final ListAdapter todoAdapter = new SimpleCursorAdapter(
//				this, android.R.layout.simple_list_item_checked,
				this, R.layout.todo_item,
				currentTagItemsCursor,
				//new String[]{DBHelper.TODO_SUMMARY}, new int[]{android.R.id.text1}) {
				new String[]{DBHelper.TODO_SUMMARY}, new int[]{R.id.todo_item}) {
			@Override
			public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
				final View view = super.newView(context, cursor, parent);
				final PriorityCheckedTextView ctv = (PriorityCheckedTextView) view;
				ctv.setChecked(cursor.getInt(doneIndex) != 0);
				ctv.setPrio(cursor.getInt(prioIndex));
				return view;
			}

			@Override
			public void bindView(final View view, final Context context, final Cursor cursor) {
				super.bindView(view, context, cursor);
				view.getId();
				final PriorityCheckedTextView ctv = (PriorityCheckedTextView) view;
				ctv.setChecked(cursor.getInt(doneIndex) != 0);
				ctv.setPrio(cursor.getInt(prioIndex));
			}
		};

		setListAdapter(todoAdapter);
		updateView();
	}

	@Override
	protected void onPause() {
		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		final SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("currentTag", getCurrentTagID());
		editor.putInt("defaultPrio", defaultPrio);
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
		outState.putLong("currentTag", getCurrentTagID());
		outState.putBoolean("hidingCompleted", hidingCompleted);
		outState.putInt("defaultPrio", defaultPrio);
		final EditText addTask = getAddTaskWidget();
		outState.putString("addTaskText", addTask.getText().toString());
		outState.putInt("addTaskSelStart", addTask.getSelectionStart());
		outState.putInt("addTaskSelEnd", addTask.getSelectionEnd());
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		setCurrentTag(savedInstanceState.getLong("currentTag"));
		hidingCompleted = savedInstanceState.getBoolean("hidingCompleted");
		setDefaultPrio(savedInstanceState.getInt("defaultPrio"));
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
		long currentTagID = getCurrentTagID();
		if (currentTagID == DBHelper.ALL_TAGS_METATAG_ID) {
//			final AlertDialog.Builder b = new AlertDialog.Builder(this);
//			b.setMessage(R.string.intro);
//			b.show();
			currentTagID = DBHelper.UNFILED_TAG_ID;
		}
		final EditText et = getAddTaskWidget();
		final String st = et.getText().toString();
		if (st.length() > 0) {
			todoItemsStorage.addTodoItem(new TodoItem(
					-1, currentTagID, false, st, null, defaultPrio));
			et.setText("");
			et.requestFocus();
			updateView();
		}
	}

	private void setDefaultPrio(final int p) {
		if (defaultPrio != p) {
			defaultPrio = p;
			final Button button = getPrioButton();
			button.setText(Integer.toString(p));
			button.invalidate();
		}
	}

	private void updateView() {
		currentTagItemsCursor.requery();
//		final boolean b = tagsAdapter.getCount() > 0;
//		getAddTaskButton().setEnabled(b);
	}

	private void setCurrentTag(final long id) {
		final int position = getItemPosition(tagsAdapter, id);
		if (position != -1)
			getTagsWidget().setSelection(position);
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
		menu.add(0, CHANGE_TAG_CONTEXT_MENU_ITEM, Menu.NONE, R.string.change_tag);
		menu.add(0, CHANGE_PRIO_CONTEXT_MENU_ITEM, Menu.NONE, R.string.change_prio);
		menu.add(0, DELETE_ITEM_CONTEXT_MENU_ITEM, Menu.NONE, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		if (info == null) return false;
		final long id = getListAdapter().getItemId(info.position);
		final TodoItem todoItem = todoItemsStorage.loadTodoItem(id);
		switch (item.getItemId()) {
			case EDIT_ITEM_CONTEXT_MENU_ITEM:
				startEditingItem(id);
				getSlidingView().switchRight();
				return true;
			case DELETE_ITEM_CONTEXT_MENU_ITEM:
				lastDeletedItem = todoItemsStorage.loadTodoItem(id);
				lastDeletedTimestamp = System.currentTimeMillis();
				todoItemsStorage.deleteTodoItem(id);
				updateView();
				return true;
			case CHANGE_PRIO_CONTEXT_MENU_ITEM:
				selectPrio(new PrioSelectedCallback() {
					public void prioSelected(final int prio) {
						todoItem.prio = prio;
						todoItemsStorage.saveTodoItem(todoItem);
						updateView();
					}
				});
				return true;
			case CHANGE_TAG_CONTEXT_MENU_ITEM:
				final AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setTitle(R.string.select_tag_title);
				final Cursor cursor = tagsStorage.getAllTagsExceptCursor(todoItem.tagID, DBHelper.ALL_TAGS_METATAG_ID);
				final ListAdapter adapter = createTagsAdapter(cursor, android.R.layout.simple_dropdown_item_1line);

				b.setAdapter(adapter, new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int which) {
						todoItem.tagID = adapter.getItemId(which);
						todoItemsStorage.saveTodoItem(todoItem);
						cursor.close();
						updateView();
					}
				});
				b.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(final DialogInterface dialog) {
						cursor.close();
					}
				});
				final AlertDialog dialog = b.show();
				return true;
		}
		return super.onContextItemSelected(item);
	}

	private void selectPrio(final PrioSelectedCallback cb) {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.select_prio_title);
		b.setItems(new CharSequence[]{"1", "2", "3", "4", "5"}, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				cb.prioSelected(which + 1);
			}
		});
		b.show();
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

	private Button getPrioButton() {
		return (Button) findViewById(R.id.prio_button);
	}

	private Spinner getEditItemTagsWidget() {
		return (Spinner) findViewById(R.id.item_tag);
	}

	private ToggleButton[] getEditPrioButtons() {
		return new ToggleButton[]{null,
		                          (ToggleButton) findViewById(R.id.prio_1),
		                          (ToggleButton) findViewById(R.id.prio_2),
		                          (ToggleButton) findViewById(R.id.prio_3),
		                          (ToggleButton) findViewById(R.id.prio_4),
		                          (ToggleButton) findViewById(R.id.prio_5)
		};
	}

	private interface PrioSelectedCallback {
		void prioSelected(int prio);
	}
}
