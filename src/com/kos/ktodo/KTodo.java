package com.kos.ktodo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.kos.ktodo.impex.XmlExporter;
import com.kos.ktodo.impex.XmlImporter;
import com.kos.ktodo.widget.UpdateService;
import com.kos.ktodo.widget.WidgetSettingsStorage;

import java.io.File;
import java.io.IOException;

public class KTodo extends ListActivity {
	public static final String SHOW_WIDGET_DATA = "com.kos.ktodo.SHOW_WIDGET_DATA";

	private static final String TAG = "KTodo";
	private static final boolean TRACE = false; //enables method tracing
	@SuppressWarnings({"FieldCanBeLocal"})
	private final int EDIT_TAGS_MENU_ITEM = Menu.FIRST;
	private final int SHOW_HIDE_COMPLETED_MENU_ITEM = EDIT_TAGS_MENU_ITEM + 1;
	private final int SORTING_MENU_ITEM = EDIT_TAGS_MENU_ITEM + 2;
	private final int EXPORT_MENU_ITEM = EDIT_TAGS_MENU_ITEM + 3;
	private final int IMPORT_MENU_ITEM = EDIT_TAGS_MENU_ITEM + 4;

	private final int EDIT_ITEM_CONTEXT_MENU_ITEM = Menu.FIRST;
	private final int CHANGE_TAG_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 1;
	private final int CHANGE_PRIO_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 2;
	private final int CHANGE_PROGRESS_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 3;
	private final int CHANGE_DUE_DATE_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 4;
	private final int DELETE_ITEM_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 5;

//	private final Random rnd = new Random();
//	private final HashMap<Integer, SubActivityCallback> subCallbacks = new HashMap<Integer, SubActivityCallback>(5);

	private Handler handler;
	private TodoItemsStorage todoItemsStorage;
	private TagsStorage tagsStorage;
	private SimpleCursorAdapter tagsAdapter;
	private Cursor allTagsCursor;
	private Cursor currentTagItemsCursor;
	private boolean hidingCompleted;
	private int defaultPrio;
	private TodoItemsSortingMode sortingMode;

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
		if (TRACE) Debug.startMethodTracing("ktodo");
		setContentView(R.layout.main);
		handler = new Handler();

		todoItemsStorage = new TodoItemsStorage(this);
		todoItemsStorage.open();
		tagsStorage = new TagsStorage(this);
		tagsStorage.open();

		allTagsCursor = tagsStorage.getAllTagsCursor();
		startManagingCursor(allTagsCursor);
		tagsAdapter = Util.createTagsAdapter(this, allTagsCursor, android.R.layout.simple_spinner_item);
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		getTagsWidget().setAdapter(tagsAdapter);

		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		final long currentTag;
		final Intent intent = getIntent();
		if (intent != null && SHOW_WIDGET_DATA.equals(intent.getAction())) {
			final int widgetId = (int) ContentUris.parseId(intent.getData());
			final WidgetSettingsStorage wss = new WidgetSettingsStorage(this);
			wss.open();
			currentTag = wss.load(widgetId).tagID;
			wss.close();
		} else
			currentTag = preferences.getLong("currentTag", 0);
		setCurrentTag(currentTag);
		hidingCompleted = preferences.getBoolean("hidingCompleted", false);
		setDefaultPrio(preferences.getInt("defaultPrio", 1));
		sortingMode = TodoItemsSortingMode.fromOrdinal(preferences.getInt("sortingMode", TodoItemsSortingMode.PRIO_THEN_DUE.ordinal()));

		setupFirstScreenWidgets();
		setupSecondScreenWidgets();

		registerForContextMenu(getMyListView());

//		reloadTodoItems(); //will be called from spinner.onMeasure->fireOnSelected->KTodo$4.onItemSelected

		setResult(Activity.RESULT_OK);
	}

//	private boolean isShowingWidgetData() {
//		return getIntent() != null && SHOW_WIDGET_DATA.equals(getIntent().getAction());
//	}

	@Override
	protected void onNewIntent(final Intent intent) {
		if (intent != null && SHOW_WIDGET_DATA.equals(intent.getAction())) {
			final int widgetId = (int) ContentUris.parseId(intent.getData());
			final WidgetSettingsStorage wss = new WidgetSettingsStorage(this);
			wss.open();
			final int currentTag = wss.load(widgetId).tagID;
			wss.close();
			handler.post(new Runnable() {
				public void run() {
					setCurrentTag(currentTag);
					reloadTodoItems();
				}
			});
			setIntent(intent);
		}
	}

	private void setupFirstScreenWidgets() {
		getAddTaskButton().setOnClickListener(new View.OnClickListener() {
			public void onClick(final View view) {
				addTodoItem();
			}
		});
		getAddTaskWidget().setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					addTodoItem();
					return true;
				}
				return false;
			}
		});

		getTagsWidget().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				reloadTodoItems();
			}

			public void onNothingSelected(final AdapterView<?> parent) {
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
				//making it async (uncommenting this code) seems to make it visually worse
//				final TodoItemView tiv = (TodoItemView) view;
//				tiv.toggle();

//				handler.post(new Runnable() {
//					public void run() {
//				final TodoItem todoItem = todoItemsStorage.loadTodoItem(id);
//				todoItem.setDone(!todoItem.done);
//				todoItemsStorage.saveTodoItem(todoItem);
				todoItemsStorage.toggleDone(id);
				updateView();
//					}
//				});
			}
		});

		listView.setSlideLeftInfo(getSlidingView(), new MyListView.SlideLeftListener() {
			public void slideLeftStarted(final long id) {
				startEditingItem(id);
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);
			}

			public void onSlideBack() {
				//Log.i(TAG, "slide back");
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
				saveItemBeingEdited();
				updateView();
			}
		});

		getEditItemTagsWidget().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				editingItem.tagID = id;
			}

			public void onNothingSelected(final AdapterView<?> parent) {}
		});
	}

	private void setupSecondScreenWidgets() {
		getPrioButton().setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				selectPrio(new Callback1<Integer>() {
					public void call(final Integer prio) {
						setDefaultPrio(prio);
					}
				});
			}
		});

		getPrioSliderButton().setOnChangeListener(new SliderButton.OnChangeListener() {
			public void valueChanged(final String newValue) {
				editingItem.prio = Integer.parseInt(newValue);
			}
		});

		getProgressSliderButton().setOnChangeListener(new SliderButton.OnChangeListener() {
			public void valueChanged(final String newValue) {
				editingItem.setProgress(Integer.parseInt(newValue));
			}
		});

		getDueDateButton().setOnClickListener(new DueDateSelector() {
			@Override
			public void onDueDateSelected(final Long dueDate) {
				editingItem.dueDate = dueDate;
				updateDueDateButton();
			}

			@Override
			public Long getCurrentDueDate() {
				return editingItem.dueDate;
			}
		});

		setupBodyWidgetFlingDetector();
	}

	private void setupBodyWidgetFlingDetector() {
		final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
				if (Math.abs(e1.getY() - e2.getY()) > 250)
					return false;

				if (e2.getX() - e1.getX() > 120 && Math.abs(velocityX) > 200) {
					final long now = SystemClock.uptimeMillis();
					final MotionEvent cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
					getEditBodyWidget().onTouchEvent(cancelEvent);
					final View focus = getSlidingView().findFocus();
					if (focus != null) {
						final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
					}
					getMyListView().handleBack();
					return true;
				}
				return false;
			}
		});
		final View.OnTouchListener gestureListener = new View.OnTouchListener() {
			public boolean onTouch(final View v, final MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		getEditBodyWidget().setOnTouchListener(gestureListener);
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getSlidingView().fixAfterOrientationChange();
	}

	private void startEditingItem(final long id) {
		editingItem = todoItemsStorage.loadTodoItem(id);
		getEditSummaryWidget().setText(editingItem.summary);
		getEditBodyWidget().setText(editingItem.body);

		if (edititgItemTagsCursor != null)
			edititgItemTagsCursor.close();
		edititgItemTagsCursor = tagsStorage.getAllTagsExceptCursor(DBHelper.ALL_TAGS_METATAG_ID);
		startManagingCursor(edititgItemTagsCursor);
		final SimpleCursorAdapter editingItemTagsAdapter = Util.createTagsAdapter(this, edititgItemTagsCursor, android.R.layout.simple_spinner_item);
		editingItemTagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		final Spinner spinner = getEditItemTagsWidget();
		spinner.setAdapter(editingItemTagsAdapter);
		final int position = Util.getItemPosition(editingItemTagsAdapter, editingItem.tagID);
		if (position != -1)
			spinner.setSelection(position);

		getPrioSliderButton().setSelection(editingItem.prio - 1);
		getProgressSliderButton().setSelection(editingItem.getProgress() / 10);
		updateDueDateButton();
	}

	private void updateDueDateButton() {
		if (editingItem.dueDate == null)
			getDueDateButton().setText(R.string.due_date);
		else
			getDueDateButton().setText(Util.showDueDate(this, editingItem.dueDate));
	}

	private void saveItemBeingEdited() {
		final String summary = getEditSummaryWidget().getText().toString();
		if (editingItem != null && summary.length() > 0) {
			editingItem.summary = summary;
			editingItem.body = getEditBodyWidget().getText().toString();
			todoItemsStorage.saveTodoItem(editingItem);
		}
	}


	private void reloadTodoItemsFromAnotherThread() {
		handler.post(new Runnable() {
			public void run() {
				reloadTodoItems();
			}
		});
	}

	private void reloadTodoItems() {
//		new Exception("reloadTodoItems").printStackTrace();
		allTagsCursor.requery();

		if (currentTagItemsCursor != null) {
			stopManagingCursor(currentTagItemsCursor);
			currentTagItemsCursor.close();
		}

		if (hidingCompleted)
			currentTagItemsCursor = todoItemsStorage.getByTagCursorExcludingCompleted(getCurrentTagID(), sortingMode);
		else
			currentTagItemsCursor = todoItemsStorage.getByTagCursor(getCurrentTagID(), sortingMode);

		final int doneIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_DONE);
		final int prioIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_PRIO);
		final int progressIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_PROGRESS);
		final int bodyIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_BODY);
		final int dueDateIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_DUE_DATE);
		startManagingCursor(currentTagItemsCursor);
		final ListAdapter todoAdapter = new SimpleCursorAdapter(
				this, R.layout.todo_item,
				currentTagItemsCursor,
				new String[]{DBHelper.TODO_SUMMARY}, new int[]{R.id.todo_item}) {
			@Override
			public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
				final View view = super.newView(context, cursor, parent);
				initView((TodoItemView) view, cursor);
				return view;
			}

			@Override
			public void bindView(final View view, final Context context, final Cursor cursor) {
				super.bindView(view, context, cursor);
				view.getId();
				initView((TodoItemView) view, cursor);
			}

			private void initView(final TodoItemView ctv, final Cursor cursor) {
				final boolean done = cursor.getInt(doneIndex) != 0;
				ctv.setChecked(done);
				ctv.setPrio(cursor.getInt(prioIndex));
				ctv.setProgress(done ? 100 : cursor.getInt(progressIndex));
				final String body = cursor.getString(bodyIndex);
				ctv.setShowNotesMark(body != null && body.length() > 0);
				if (cursor.isNull(dueDateIndex))
					ctv.setDueDate(null, DueStatus.NONE);
				else {
					final Long dd = cursor.getLong(dueDateIndex);
					ctv.setDueDate(Util.showDueDate(KTodo.this, dd), Util.getDueStatus(dd));
				}
			}
		};

		setListAdapter(todoAdapter);
		updateView();
	}

	@Override
	protected void onPause() {
		updateWidgetsIfNeeded();
		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		final SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("currentTag", getCurrentTagID());
		editor.putInt("defaultPrio", defaultPrio);
		editor.putInt("sortingMode", sortingMode.ordinal());
		editor.putBoolean("hidingCompleted", hidingCompleted).commit();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		updateWidgetsIfNeeded();

		todoItemsStorage.close();
		tagsStorage.close();
		allTagsCursor.close();
		super.onDestroy();
		if (TRACE) Debug.stopMethodTracing();
	}

	private void updateWidgetsIfNeeded() {
		if (todoItemsStorage.hasModifiedDB() || tagsStorage.hasModifiedDB()) {
			UpdateService.requestUpdateAll(this);
			startService(new Intent(this, UpdateService.class));
			todoItemsStorage.resetModifiedDB();
			tagsStorage.resetModifiedDB();
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		outState.putLong("currentTag", getCurrentTagID());
		outState.putBoolean("hidingCompleted", hidingCompleted);
		outState.putInt("defaultPrio", defaultPrio);
		outState.putInt("sortingMode", sortingMode.ordinal());
		final EditText addTask = getAddTaskWidget();
		outState.putString("addTaskText", addTask.getText().toString());
		outState.putInt("addTaskSelStart", addTask.getSelectionStart());
		outState.putInt("addTaskSelEnd", addTask.getSelectionEnd());
		final boolean onLeft = getSlidingView().isOnLeft();
		outState.putBoolean("onLeft", onLeft);
		if (!onLeft) {
			outState.putLong("itemBeingEditedID", editingItem.id);
			saveItemBeingEdited();
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		setCurrentTag(savedInstanceState.getLong("currentTag"));
		hidingCompleted = savedInstanceState.getBoolean("hidingCompleted");
		setDefaultPrio(savedInstanceState.getInt("defaultPrio"));
		sortingMode = TodoItemsSortingMode.fromOrdinal(savedInstanceState.getInt("sortingMode", TodoItemsSortingMode.PRIO_THEN_DUE.ordinal()));
		final String addTaskText = savedInstanceState.getString("addTaskText");
		if (addTaskText != null) {
			final EditText taskWidget = getAddTaskWidget();
			taskWidget.setText(addTaskText);
			taskWidget.setSelection(savedInstanceState.getInt("addTaskSelStart"), savedInstanceState.getInt("addTaskSelEnd"));
		}
		final boolean onLeft = savedInstanceState.getBoolean("onLeft");
		if (onLeft)
			getSlidingView().switchLeft();
		else {
			startEditingItem(savedInstanceState.getLong("itemBeingEditedID"));
			handler.postDelayed(new Runnable() {
				public void run() {
					getSlidingView().switchRight();
				}
			}, 100);
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
			currentTagID = DBHelper.UNFILED_METATAG_ID;
		}
		final EditText et = getAddTaskWidget();
		final String st = et.getText().toString();
		if (st.length() > 0) {
			todoItemsStorage.addTodoItem(new TodoItem(-1, currentTagID, false, st, null, defaultPrio, 0, null));
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
		allTagsCursor.requery();
		currentTagItemsCursor.requery();
	}

	private void setCurrentTag(final long id) {
		final int position = Util.getItemPosition(tagsAdapter, id);
		if (position != -1)
			getTagsWidget().setSelection(position);
	}

	private long getCurrentTagID() {
		return getTagsWidget().getSelectedItemId();
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		menu.clear();
		menu.add(0, SHOW_HIDE_COMPLETED_MENU_ITEM, Menu.NONE,
				hidingCompleted ? R.string.show_completed_items : R.string.hide_completed_items);
		menu.add(0, SORTING_MENU_ITEM, Menu.NONE, R.string.sorting);
		final MenuItem item = menu.add(0, EDIT_TAGS_MENU_ITEM, Menu.NONE, R.string.edit_tags);
		item.setIntent(new Intent(this, EditTags.class));
		menu.add(0, EXPORT_MENU_ITEM, Menu.NONE, R.string.export);
		menu.add(0, IMPORT_MENU_ITEM, Menu.NONE, R.string._import);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case SHOW_HIDE_COMPLETED_MENU_ITEM:
				hidingCompleted = !hidingCompleted;
				reloadTodoItems();
				return true;
			case SORTING_MENU_ITEM:
				TodoItemsSortingMode.selectSortingMode(this, sortingMode, new Callback1<TodoItemsSortingMode>() {
					public void call(final TodoItemsSortingMode arg) {
						sortingMode = arg;
						reloadTodoItems();
					}
				});
				return true;
			case EXPORT_MENU_ITEM:
				exportData();
				return true;
			case IMPORT_MENU_ITEM:
				importData();
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
		menu.add(0, CHANGE_PROGRESS_CONTEXT_MENU_ITEM, Menu.NONE, R.string.change_progress);
		menu.add(0, CHANGE_DUE_DATE_CONTEXT_MENU_ITEM, Menu.NONE, R.string.change_due_date);
		menu.add(0, DELETE_ITEM_CONTEXT_MENU_ITEM, Menu.NONE, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		if (info == null) return false;
		final long id = getListAdapter().getItemId(info.position);
		final TodoItem todoItem = todoItemsStorage.loadTodoItem(id);
		final AlertDialog.Builder b;
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
				selectPrio(new Callback1<Integer>() {
					public void call(final Integer prio) {
						todoItem.prio = prio;
						todoItemsStorage.saveTodoItem(todoItem);
						updateView();
					}
				});
				return true;
			case CHANGE_PROGRESS_CONTEXT_MENU_ITEM:
				b = new AlertDialog.Builder(this);
				b.setTitle(R.string.select_progress_title);
				b.setItems(new CharSequence[]{
						"0%", "10%", "20%", "30%", "40%", "50%",
						"60%", "70%", "80%", "90%", "100%"}, new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int which) {
						todoItem.setProgress(which * 10);
						todoItemsStorage.saveTodoItem(todoItem);
						updateView();
					}
				});
				b.show();
				return true;
			case CHANGE_DUE_DATE_CONTEXT_MENU_ITEM:
				new DueDateSelector() {
					@Override
					public void onDueDateSelected(final Long dueDate) {
						todoItem.dueDate = dueDate;
						todoItemsStorage.saveTodoItem(todoItem);
						updateView();
					}

					@Override
					public Long getCurrentDueDate() {
						return todoItem.dueDate;
					}
				}.onClick(getMyListView());
				return true;
			case CHANGE_TAG_CONTEXT_MENU_ITEM:
				b = new AlertDialog.Builder(this);
				b.setTitle(R.string.select_tag_title);
				final Cursor cursor = tagsStorage.getAllTagsExceptCursor(todoItem.tagID, DBHelper.ALL_TAGS_METATAG_ID);
				final ListAdapter adapter = Util.createTagsAdapter(this, cursor, android.R.layout.simple_dropdown_item_1line);

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
				b.show();
				return true;
		}
		return super.onContextItemSelected(item);
	}

	private void selectPrio(final Callback1<Integer> cb) {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.select_prio_title);
		b.setItems(new CharSequence[]{"1", "2", "3", "4", "5"}, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				cb.call(which + 1);
			}
		});
		b.show();
	}

	private void exportData() { //any good reason to export/import in background? It's very quick anyways
		final LayoutInflater inf = LayoutInflater.from(this);
		final View textEntryView = inf.inflate(R.layout.alert_text_entry, null);
		final String currentName = "/sdcard/ktodo.xml"; //todo use real Save As dialog
		final EditText editText = (EditText) textEntryView.findViewById(R.id.text_entry);
		editText.setText(currentName);

		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.export);
		b.setMessage(R.string.export_file_name);
		b.setCancelable(true);
		b.setView(textEntryView);
		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialogInterface, final int i) {
				final String st = editText.getText().toString();
				runAsynchronously(R.string.export, R.string.exporting_data, new Runnable() {
					public void run() {
						try {
							XmlExporter.exportData(KTodo.this, new File(st));
						} catch (IOException e) {
							Log.e(TAG, "error exporting data", e);
							showErrorFromAnotherThread(e.toString());
						}
					}
				});
			}
		});

		final AlertDialog dialog = b.create();
		Util.setupEditTextEnterListener(editText, dialog);
		dialog.show();
	}

	private void showErrorFromAnotherThread(final String msg) {
		handler.post(new Runnable() {
			public void run() {
				new AlertDialog.Builder(KTodo.this).setTitle(R.string.error).setMessage(msg).show();
			}
		});
	}

	private void importData() {
		final LayoutInflater inf = LayoutInflater.from(this);
		final View dialogView = inf.inflate(R.layout.import_dialog, null);
		final String currentName = "/sdcard/ktodo.xml"; //todo use real Open dialog
		final EditText editText = (EditText) dialogView.findViewById(R.id.text_entry);
		editText.setText(currentName);
		final CheckBox wipe = (CheckBox) dialogView.findViewById(R.id.wipe_checkbox);

		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string._import);
		b.setMessage(R.string.import_file_name);
		b.setCancelable(true);
		b.setView(dialogView);
		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialogInterface, final int i) {
				if (wipe.isChecked()) { //additional warning?
					todoItemsStorage.deleteAllTodoItems();
					tagsStorage.deleteAllTags();
				}
				final String st = editText.getText().toString();
				runAsynchronously(R.string._import, R.string.importing_data, new Runnable() {
					public void run() {
						try {
							XmlImporter.importData(KTodo.this, new File(st), false);
						} catch (IOException e) {
							Log.e(TAG, "error importing data", e);
							showErrorFromAnotherThread(e.toString());
						}
						reloadTodoItemsFromAnotherThread();
					}
				});
			}
		});

		final AlertDialog dialog = b.create();
		Util.setupEditTextEnterListener(editText, dialog);
		dialog.show();
	}

	private void runAsynchronously(final int title, final int message, final Runnable r) {
		final ProgressDialog pg = ProgressDialog.show(this, getString(title), getString(message), true);
		final Handler h = new Handler() {
			@Override
			public void handleMessage(final Message msg) {
				pg.dismiss();
			}
		};
		final Runnable r2 = new Runnable() {
			public void run() {
				r.run();
				h.sendEmptyMessage(0);
			}
		};
		new Thread(r2).start();
	}

/*	private void startSubActivity(final Class subActivityClass, final SubActivityCallback callback, final Bundle params) {
		final int i = rnd.nextInt();
		subCallbacks.put(i, callback);
		final Intent intent = new Intent(this, subActivityClass);
		if (params != null)
			intent.getExtras().putAll(params);
		startActivityForResult(intent, i);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		final SubActivityCallback callback = subCallbacks.remove(requestCode);
		if (callback != null && resultCode == Activity.RESULT_OK)
			callback.onResultOK(data);
	}*/

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

	private SliderButton getPrioSliderButton() {
		return (SliderButton) findViewById(R.id.prio_sliding_button);
	}

	private SliderButton getProgressSliderButton() {
		return (SliderButton) findViewById(R.id.progress_sliding_button);
	}

	private Button getDueDateButton() {
		return (Button) findViewById(R.id.due_date_button);
	}

/*	private interface SubActivityCallback {
		void onResultOK(final Intent data);
	}*/
}
