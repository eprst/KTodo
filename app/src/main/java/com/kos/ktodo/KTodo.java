package com.kos.ktodo;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import com.kos.ktodo.impex.XmlExporter;
import com.kos.ktodo.impex.XmlImporter;
import com.kos.ktodo.menu.MenuAdapter;
import com.kos.ktodo.menu.MenuItemModel;
import com.kos.ktodo.preferences.Preferences;
import com.kos.ktodo.widget.WidgetSettingsStorage;
import com.kos.ktodo.widget.WidgetUpdateService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class KTodo extends ListActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
	public static final String SHOW_WIDGET_DATA = "com.kos.ktodo.SHOW_WIDGET_DATA";
	public static final String SHOW_WIDGET_ITEM_DATA = "com.kos.ktodo.SHOW_WIDGET_ITEM_DATA";

	private static final String TAG = "KTodo";
	private static final boolean TRACE = false; //enables method tracing
	private static final int HIDE_UNDELETE_AFTER = 4000; //ms
	public static final int VOICE_RECOGNITION_REQUEST_CODE = 123422;

	private static final int IMPORT_DATA_PERMISSION_REQUEST_CODE = 1;
	private static final int EXPORT_DATA_PERMISSION_REQUEST_CODE = 2;

	private final int EDIT_ITEM_CONTEXT_MENU_ITEM = Menu.FIRST;
	private final int CHANGE_TAG_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 1;
	private final int CHANGE_PRIO_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 2;
	private final int CHANGE_PROGRESS_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 3;
	private final int CHANGE_DUE_DATE_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 4;
	private final int DELETE_ITEM_CONTEXT_MENU_ITEM = EDIT_ITEM_CONTEXT_MENU_ITEM + 5;

	private static final int ALL_TAGS_LOADER_ID = 0;
	private static final int CURRENT_TAG_ITEMS_LOADER_ID = 1;
	private static final int EDITING_ITEM_TAGS_LOADER_ID = 2;

	private Intent voiceRecognitionIntent = null;
	private Drawable voiceDrawable = null;

	private Handler handler;
	private TodoItemsStorage todoItemsStorage;
	private TagsStorage tagsStorage;
	private SimpleCursorAdapter tagsAdapter;
	private SimpleCursorAdapter editingItemTagsAdapter;
	@Nullable
	private SimpleCursorAdapter todoAdapter;

	private AllTagsLoaderCallbacks allTagsLoaderCallbacks;
	private CurrentTagItemsLoaderCallbacks currentTagItemsLoaderCallbacks;
	@SuppressWarnings("FieldCanBeLocal")
	private EditingItemTagsLoaderCallbacks editingItemTagsLoaderCallbacks;

	private boolean hidingCompleted;
	private long defaultDue;
	private int defaultPrio;
	private TodoItemsSortingMode sortingMode;

	private ActionBar actionBar = null;
	private ActionBarDrawerToggle drawerToggle;

	private long currentTagId;

	@Nullable
	private TodoItem editingItem;

	@Nullable
	private TodoItem lastDeletedItem;

	private final TodoItemsListView.DeleteItemListener deleteItemListener;
	private final SlideLeftListener slideLeftListener;

	private SparseArray<Runnable> permissionRequests = new SparseArray<>();

	//prefs
	private Float listFontSize;
	private boolean clickAnywhereToCheck = true;
	private boolean keepKbdOpen;
	private boolean useVoiceInput;

	private final ContentObserver contentObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			onDataChanged();
		}
	};

	public KTodo() {
		deleteItemListener = new TodoItemsListView.DeleteItemListener() {
			public void deleteItem(final long id) {
				if (todoItemsStorage != null) {
					lastDeletedItem = todoItemsStorage.loadTodoItem(id);
					todoItemsStorage.deleteTodoItem(id);
					showUndeleteButton();
				}
			}
		};
		slideLeftListener = new SlideLeftListener() {
			@Override
			public boolean canSlideLeft() {
				return !isDrawerOpen();
			}

			public void slideLeftStarted(final long id) {
				hideSoftKeyboard();
				startEditingItem(id);
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);
			}

			public void onSlideBack() {
				//Log.i(TAG, "slide back");
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
				KTodo.this.stopEditingItem(false);
			}
		};
	}

	private void hideSoftKeyboard() {
		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(getSlidingView().getWindowToken(), 0);
	}

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

		tagsAdapter = Util.createTagsAdapter(this, null, R.layout.tag_list_item);
		tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		editingItemTagsAdapter = Util.createTagsAdapter(this, null, android.R.layout.simple_spinner_item);
		editingItemTagsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		getEditItemTagsWidget().setAdapter(editingItemTagsAdapter);

		setupDrawer();

		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		final long currentTag;
		final Intent intent = getIntent();

		Integer tagId = extractTagIdFromIntent(intent);
		currentTag = tagId != null ? tagId : preferences.getLong("currentTag", DBHelper.ALL_TAGS_METATAG_ID);

		hidingCompleted = preferences.getBoolean("hidingCompleted", false);
		setDefaultDue(preferences.getLong("defaultDue", -1));
		setDefaultPrio(preferences.getInt("defaultPrio", 1));
		sortingMode = TodoItemsSortingMode.fromOrdinal(preferences.getInt("sortingMode", TodoItemsSortingMode.PRIO_DUE_SUMMARY.ordinal()));

		setupFirstScreenWidgets();
		setupSecondScreenWidgets();

		registerForContextMenu(getMyListView());

		// should be done from onStart, depends on useVoiceInput preference value
//		setupVoiceRecognition();
//		setupAddButtonIcon();

//		reloadTodoItems(); //will be called from spinner.onMeasure->fireOnSelected->KTodo$4.onItemSelected

		updateTitle();
		setResult(Activity.RESULT_OK);

		allTagsLoaderCallbacks = new AllTagsLoaderCallbacks(this, tagsAdapter);
		currentTagItemsLoaderCallbacks = new CurrentTagItemsLoaderCallbacks(this);
		editingItemTagsLoaderCallbacks = new EditingItemTagsLoaderCallbacks(this, editingItemTagsAdapter);

		final LoaderManager loaderManager = getLoaderManager();
		loaderManager.initLoader(ALL_TAGS_LOADER_ID, null, allTagsLoaderCallbacks);
		loaderManager.initLoader(CURRENT_TAG_ITEMS_LOADER_ID, null, currentTagItemsLoaderCallbacks);
		loaderManager.initLoader(EDITING_ITEM_TAGS_LOADER_ID, null, editingItemTagsLoaderCallbacks);

		setCurrentTag(currentTag, false);

		final ContentResolver contentResolver = getApplicationContext().getContentResolver();
		contentResolver.registerContentObserver(TodoItemsStorage.CHANGE_NOTIFICATION_URI, false, contentObserver);
		contentResolver.registerContentObserver(TagsStorage.CHANGE_NOTIFICATION_URI, false, contentObserver);

		runIntent(intent);
	}

	private void setupDrawer() {
		actionBar = getActionBar();
		if (actionBar == null) throw new NullPointerException("Action bar must be present");

		drawerToggle = new ActionBarDrawerToggle(
				this,
				getDrawerLayout(),
				R.string.drawer_open,
				R.string.drawer_close) {
			@Override
			public void onDrawerOpened(View drawerView) {
				hideUndeleteButton(false);
				hideSoftKeyboard(); // any way to hide it faster?
				super.onDrawerOpened(drawerView);
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				hideUndeleteButton(false);
				hideSoftKeyboard();
				super.onDrawerSlide(drawerView, slideOffset);
			}
		};
		getDrawerLayout().addDrawerListener(drawerToggle);

		getTagsList().setAdapter(tagsAdapter);
		getTagsList().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				closeDrawer();
				setCurrentTag(id, true);
			}
		});

		getDrawerLayout().setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);

		// add leftPadding to the logo
		Resources resources = getResources();
		int leftPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, resources.getDisplayMetrics());
		ImageView view = findViewById(android.R.id.home);
		view.setPadding(leftPadding, 0, 0, 0);

		setupActionBarMenu();
	}

	private void setupActionBarMenu() {
		initActionBarMenuItems();

		getDrawerMenu().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				closeDrawer();
				invokeMenuAction(position);
			}
		});
	}

	private void invokeMenuAction(int position) {
		switch (position) {
			case 0:
				hidingCompleted = !hidingCompleted;
				getLoaderManager().restartLoader(CURRENT_TAG_ITEMS_LOADER_ID, null, currentTagItemsLoaderCallbacks);
				initActionBarMenuItems(); // menu item changes text
				break;
			case 1:
				TodoItemsSortingMode.selectSortingMode(this, sortingMode, new Callback1<TodoItemsSortingMode, Unit>() {
					public Unit call(final TodoItemsSortingMode arg) {
						sortingMode = arg;
						getLoaderManager().restartLoader(CURRENT_TAG_ITEMS_LOADER_ID, null, currentTagItemsLoaderCallbacks);
						return Unit.u;
					}
				});
				break;
			case 2:
				startActivity(new Intent(KTodo.this, EditTags.class));
				break;
			case 3:
				exportData(getBaseContext());
				break;
			case 4:
				importData(getBaseContext());
				break;
			case 5:
				startActivity(new Intent(getBaseContext(), Preferences.class));
				break;
			default:
				Log.e(TAG, "Unexpected menu item position: " + position);
		}
	}

	private void initActionBarMenuItems() {
		List<MenuItemModel> menuItems = new ArrayList<>(6);
		menuItems.add(new MenuItemModel(R.drawable.ic_menu_view, hidingCompleted ? R.string.show_completed_items : R.string.hide_completed_items));
		menuItems.add(new MenuItemModel(R.drawable.ic_menu_sort_alphabetically, R.string.sorting));
		menuItems.add(new MenuItemModel(R.drawable.ic_menu_edit, R.string.edit_tags));
		menuItems.add(new MenuItemModel(R.drawable.ic_menu_back, R.string.export));
		menuItems.add(new MenuItemModel(R.drawable.ic_menu_forward, R.string._import));
		menuItems.add(new MenuItemModel(R.drawable.ic_menu_preferences, R.string.prefs_title));
		getDrawerMenu().setAdapter(new MenuAdapter(this, menuItems));
	}

	private void setupAddButtonIcon() {
		if (voiceDrawable != null) {
			getAddTaskWidget().addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
				}

				@Override
				public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
				}

				@Override
				public void afterTextChanged(final Editable s) {
					if (s.length() == 0)
						getAddTaskButton().setImageDrawable(voiceDrawable);
					else
						//requires API 21
						getAddTaskButton().setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_mark));
				}
			});
			getAddTaskButton().setImageDrawable(voiceDrawable);
		} else {
			getAddTaskButton().setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_mark)); // TODO ..,null (theme) (here and above)
		}
	}

//	@Override
//	protected void onNewIntent(final Intent intent) {
//		Log.i(TAG, "onNewIntent: " + intent);
//		final Integer tagId = extractTagIdFromIntent(intent);
//		if (tagId != null) {
//			handler.post(new Runnable() {
//				public void run() {
//					setCurrentTag(tagId.longValue());
//					getLoaderManager().restartLoader(CURRENT_TAG_ITEMS_LOADER_ID, null, currentTagItemsLoaderCallbacks);
//				}
//			});
//			setIntent(intent);
//		}
//	}

	private void runIntent(final Intent intent) {
		String action = intent.getAction();
		if (action != null) {
			switch (intent.getAction()) {
				case SHOW_WIDGET_ITEM_DATA:
					// do we have to switch current tag?
					Uri data = intent.getData();
					if (data != null) {
						Long itemId = Long.parseLong(data.getLastPathSegment());
						editItem(itemId);
					}
					break;
				// SHOW_WIDGET_DATA is processed in onCreate by selecting correct initial currentTag
			}
		}
	}

	private void editItem(Long itemId) {
		startEditingItem(itemId);
		handler.postDelayed(new Runnable() {
			public void run() {
				getSlidingView().switchRight();
			}
		}, 100);
	}

	@Nullable
	private Integer extractTagIdFromIntent(final Intent intent) {
		if (intent != null) {
			int widgetId = -1;
			String action = intent.getAction();
			if (action != null) {
				switch (action) {
					case SHOW_WIDGET_DATA:
						widgetId = (int) ContentUris.parseId(intent.getData());
						break;
					case SHOW_WIDGET_ITEM_DATA:
						Uri data = intent.getData();
						if (data != null) {
							List<String> pathSegments = data.getPathSegments();
							if (pathSegments.size() == 3) {
								widgetId = Integer.parseInt(pathSegments.get(1));
							}
						}
						break;
				}
			}
			if (widgetId != -1) {
				final WidgetSettingsStorage wss = new WidgetSettingsStorage(this);
				wss.open();
				int tagId = wss.load(widgetId).tagID;
				wss.close();
				return tagId;
			}
			return null;
		} else
			return null;
	}

	private void stopEditingItem(boolean slideBack) {
		if (slideBack)
			getSlidingView().switchLeft();

		saveItemBeingEdited();
		updateTitle();
		unlockDrawer();
		getAddTaskWidget().requestFocus();
		editingItem = null;
	}

	private void hideUndeleteButton(boolean animate) {
		if (animate)
			getUndeleteButton().setVisibility(View.INVISIBLE);
		else
			getUndeleteButton().hideNoAnimation();
	}

	private void setupFirstScreenWidgets() {
		final SlideLeftImageButton addTaskButton = getAddTaskButton();
		addTaskButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View view) {
				if (getAddTaskWidget().getText().toString().length() == 0)
					startVoiceRecognition();
				else {
					final long id = addTodoItem();
					if (id != -1)
						addTaskButton.setItemID(id);
				}
			}
		});

		getAddTaskWidget().setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					addTodoItem();
					return keepKbdOpen; // return true to keep kbd
				} else {
					return false;
				}
			}
		});

		getEditItemTagsWidget().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				if (editingItem != null) {
					editingItem.tagID = id;
				}
			}

			public void onNothingSelected(final AdapterView<?> parent) {
			}
		});

		final DueDateSelector dueDateSelector = new DueDateSelector() {
			@Override
			public void onDueDateSelected(final Long dueDate) {
				setDefaultDue(dueDate == null ? -1 : dueDate);
			}

			@Override
			public Long getCurrentDueDate() {
				return defaultDue == -1 ? null : defaultDue;
			}
		};
		getDefaultDueTxtButton().setOnClickListener(dueDateSelector);
		getDefaultDueImgButton().setOnClickListener(dueDateSelector);

		getDefaultPrioButton().setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				selectPrio(new Callback1<Integer, Unit>() {
					public Unit call(final Integer prio) {
						setDefaultPrio(prio);
						return Unit.u;
					}
				});
			}
		});

		getUndeleteButton().setOnClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				undelete();
			}
		});
		Drawable background = getUndeleteButton().getBackground();
		if (background != null)
			background.setColorFilter(0xFFCAFF4C, PorterDuff.Mode.MULTIPLY); //add green tint
	}

	private void setupSecondScreenWidgets() {
		final SliderButton prioSliderButton = getPrioSliderButton();

		prioSliderButton.setOnChangeListener(new Callback1<String, Unit>() {
			public Unit call(final String newValue) {
				if (editingItem != null) {
					editingItem.prio = Integer.parseInt(newValue);
				}
				return Unit.u;
			}
		});

		prioSliderButton.setOnTrackballListener(new Callback1<MotionEvent, Boolean>() {
			public Boolean call(final MotionEvent evt) {
				if (evt.getX() < 0) {
					stopEditingItem(true);
					return Boolean.TRUE;
				}
				return Boolean.FALSE;
			}
		});

		getProgressSliderButton().setOnChangeListener(new Callback1<String, Unit>() {
			public Unit call(final String newValue) {
				if (editingItem != null) {
					editingItem.setProgress(Integer.parseInt(newValue));
				}
				return Unit.u;
			}
		});

		final DueDateSelector dueDateSelector = new DueDateSelector() {
			@Override
			public void onDueDateSelected(final Long dueDate) {
				if (editingItem != null) {
					editingItem.setDueDate(dueDate);
				}
				updateDueDateButton();
			}

			@Override
			public Long getCurrentDueDate() {
				if (editingItem != null) {
					return editingItem.getDueDate();
				} else {
					return null;
				}
			}
		};
		getDueDateTxtButton().setOnClickListener(dueDateSelector);
		getDueDateImgButton().setOnClickListener(dueDateSelector);

		getEditBodyWidget().setScrollbarFadingEnabled(true);
		getEditBodyWidget().setVerticalFadingEdgeEnabled(true);
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
						hideSoftKeyboard();
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
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	private void updateTitle() {
		String tagName = tagsStorage.getTag(getCurrentTagID());
		actionBar.setTitle(tagName);
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getSlidingView().fixAfterOrientationChange();
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onStart() {
		super.onStart();
		//re-load settings
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		final TodoItemsListView listView = getMyListView();
		listView.setDeleteItemListener(prefs.getBoolean(Preferences.DELETE_BY_FLING, true) ? deleteItemListener : null);

		if (prefs.getBoolean(Preferences.SLIDE_TO_EDIT, true)) {
			getAddTaskButton().setSlideLeftInfo(getSlidingView(), slideLeftListener);
			listView.setSlideLeftInfo(getSlidingView(), slideLeftListener);
		} else {
			getAddTaskButton().setSlideLeftInfo(null, null);
			listView.setSlideLeftInfo(null, null);
		}

		final String _default = "default";
		final String fontSize = prefs.getString(Preferences.MAIN_LIST_FONT_SIZE, _default);
		assert fontSize != null;
		if (!fontSize.equals(_default)) {
			try {
				listFontSize = Float.parseFloat(fontSize);
			} catch (NumberFormatException e) {
				listFontSize = null;
			}
		} else listFontSize = null;

		clickAnywhereToCheck = prefs.getBoolean(Preferences.CLICK_ANYWHERE_TO_CHECK, true);
		keepKbdOpen = prefs.getBoolean(Preferences.KEEP_KBD_OPEN, false);
		useVoiceInput = prefs.getBoolean(Preferences.USE_VOICE_INPUT, true);

		setupVoiceRecognition();
		setupAddButtonIcon();

		reloadTodoItemsFromUIThread(); // for font size changes or 'show as days left' to kick in
	}

	private void startEditingItem(final long id) {
		if (todoItemsStorage != null) {
			editingItem = todoItemsStorage.loadTodoItem(id);

			if (editingItem != null) {
				hideUndeleteButton(false);

				actionBar.setTitle(R.string.edit_item);
				getEditSummaryWidget().setText(editingItem.summary);
				getEditBodyWidget().setText(editingItem.body);

				editingItemTagsLoaderCallbacks.addOnLoadFinishedAction(new Runnable() {
					@Override
					public void run() {
						final Spinner spinner = getEditItemTagsWidget();
						final int position = Util.getItemPosition(editingItemTagsAdapter, editingItem.tagID);
						if (position != -1)
							spinner.setSelection(position);
						else
							Log.w(TAG, "Can't find spinner position for tag " + editingItem.tagID);
					}
				});

				getPrioSliderButton().setSelection(editingItem.prio - 1);
				getProgressSliderButton().setSelection(editingItem.getProgress() / 10);
				updateDueDateButton();
				getSlidingView().setListener(new SlidingView.Listener() {
					public void slidingFinished() {
						final EditText editText = getEditBodyWidget();
						if (editingItem != null && editingItem.caretPos != null) {
							final Editable text = editText.getText();
							final int savedCaretPos = editingItem.caretPos;
							if (savedCaretPos >= 0 && savedCaretPos < text.length())
								Selection.setSelection(text, savedCaretPos);
						}
						editText.requestFocus();
						lockDrawer();
					}
				});
			}
		}
	}

	private void updateDueDateButton() {
		if (editingItem != null) {
			final Long dueDate = editingItem.getDueDate();
			updateImgButton(getDueDateTxtButton(), getDueDateImgButton(), dueDate == null ? null : Util.showDueDate(this, dueDate));
		}
	}

	private void updateImgButton(final Button txtButton, final ImageButton imgButton, final String text) {
		if (text == null) {
			txtButton.setVisibility(View.GONE);
			imgButton.setVisibility(View.VISIBLE);
		} else {
			txtButton.setText(text);
			txtButton.setVisibility(View.VISIBLE);
			imgButton.setVisibility(View.GONE);
		}
//		txtButton.invalidate();
//		imgButton.invalidate();
	}

	private void saveItemBeingEdited() {
		final String summary = getEditSummaryWidget().getText().toString();
		if (editingItem != null && todoItemsStorage != null && summary.length() > 0) {
			editingItem.summary = summary;
			final Editable editBodyText = getEditBodyWidget().getText();
			editingItem.body = editBodyText.toString();
			editingItem.caretPos = Selection.getSelectionEnd(editBodyText);
			todoItemsStorage.saveTodoItem(editingItem);
		}
	}

	private void reloadTodoItemsFromUIThread() {
		// otherwise this happens: Can't create handler inside thread that has not called Looper.prepare()
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getLoaderManager().restartLoader(CURRENT_TAG_ITEMS_LOADER_ID, null, currentTagItemsLoaderCallbacks);
			}
		});
	}

	private void reloadTodoItems(@NotNull final Cursor currentTagItemsCursor) {
		getLoaderManager().restartLoader(ALL_TAGS_LOADER_ID, null, allTagsLoaderCallbacks);

		final int doneIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_DONE);
		final int prioIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_PRIO);
		final int progressIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_PROGRESS);
		final int bodyIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_BODY);
		final int dueDateIndex = currentTagItemsCursor.getColumnIndexOrThrow(DBHelper.TODO_DUE_DATE);
		if (todoAdapter == null) {
			todoAdapter = new SimpleCursorAdapter(
					this, R.layout.todo_item,
					currentTagItemsCursor,
					new String[]{DBHelper.TODO_SUMMARY}, new int[]{R.id.todo_item}, 0) {
				@Override
				public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
					final View view = super.newView(context, cursor, parent);
					initView((TodoItemView) view, cursor);
					return view;
				}

				@Override
				public void bindView(@NotNull final View view, final Context context, @NotNull final Cursor cursor) {
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
					if (listFontSize != null)
						ctv.setTextSize(listFontSize);
				}
			};
			setListAdapter(todoAdapter);
		} else {
			todoAdapter.swapCursor(currentTagItemsCursor);
		}
		getMyListView().updateChildren();
	}

	@Override
	public void onBackPressed() {
		if (isDrawerOpen()) {
			closeDrawer();
		} else {
			super.onBackPressed();
		}
	}

	private boolean isDrawerOpen() {
		return getDrawerLayout().isDrawerOpen(Gravity.LEFT);
	}

	private void closeDrawer() {
		getDrawerLayout().closeDrawer(Gravity.LEFT);
	}

	private void lockDrawer() {
		getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		drawerToggle.setDrawerIndicatorEnabled(false);
		drawerToggle.syncState();
	}

	private void unlockDrawer() {
		getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		drawerToggle.setDrawerIndicatorEnabled(true);
		drawerToggle.syncState();
	}

	@Override
	protected void onPause() {
		final SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		final SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("currentTag", getCurrentTagID());
		editor.putLong("defaultDue", defaultDue);
		editor.putInt("defaultPrio", defaultPrio);
		editor.putInt("sortingMode", sortingMode.ordinal());
		editor.putBoolean("hidingCompleted", hidingCompleted);
		editor.apply();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		final LoaderManager loaderManager = getLoaderManager();
		loaderManager.destroyLoader(CURRENT_TAG_ITEMS_LOADER_ID);
		loaderManager.destroyLoader(ALL_TAGS_LOADER_ID);
		loaderManager.destroyLoader(EDITING_ITEM_TAGS_LOADER_ID);
		allTagsLoaderCallbacks.shutdown();
		currentTagItemsLoaderCallbacks.shutdown();
		editingItemTagsLoaderCallbacks.shutdown();

		todoItemsStorage.close();
		tagsStorage.close();

		getApplicationContext().getContentResolver().unregisterContentObserver(contentObserver);

		if (TRACE) Debug.stopMethodTracing();
	}

	private void onDataChanged() {
		if (!tagsStorage.hasTag(KTodo.this.currentTagId)) {
			// current tag has been removed, switch to 'all tags'
			setCurrentTag(DBHelper.ALL_TAGS_METATAG_ID, true);
		}
		startService(new Intent(this, WidgetUpdateService.class));
		WidgetUpdateService.requestUpdateAll(this);
		LastModifiedState.touch(this);
		new BackupManager(this).dataChanged();
	}

	@Override
	protected void onSaveInstanceState(@NotNull final Bundle outState) {
		outState.putLong("currentTag", getCurrentTagID());
		outState.putBoolean("hidingCompleted", hidingCompleted);
		outState.putLong("defaultDue", defaultDue);
		outState.putInt("defaultPrio", defaultPrio);
		outState.putInt("sortingMode", sortingMode.ordinal());
		final EditText addTask = getAddTaskWidget();
		outState.putString("addTaskText", addTask.getText().toString());
		outState.putInt("addTaskSelStart", addTask.getSelectionStart());
		outState.putInt("addTaskSelEnd", addTask.getSelectionEnd());
		final boolean onLeft = getSlidingView().isOnLeft();
		outState.putBoolean("onLeft", onLeft);
		if (!onLeft && editingItem != null) {
			outState.putLong("itemBeingEditedID", editingItem.id);
			saveItemBeingEdited();
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(@NotNull final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		setCurrentTag(savedInstanceState.getLong("currentTag"), false);
		hidingCompleted = savedInstanceState.getBoolean("hidingCompleted");
		setDefaultDue(savedInstanceState.getLong("defaultDue", -1));
		setDefaultPrio(savedInstanceState.getInt("defaultPrio"));
		sortingMode = TodoItemsSortingMode.fromOrdinal(savedInstanceState.getInt("sortingMode", TodoItemsSortingMode.PRIO_DUE_SUMMARY.ordinal()));
		final String addTaskText = savedInstanceState.getString("addTaskText");
		if (addTaskText != null) {
			final EditText taskWidget = getAddTaskWidget();
			taskWidget.setText(addTaskText);
			taskWidget.setSelection(savedInstanceState.getInt("addTaskSelStart"), savedInstanceState.getInt("addTaskSelEnd"));
		}
		final boolean onLeft = savedInstanceState.getBoolean("onLeft");
		if (onLeft)
			getSlidingView().switchLeft();
		else
			editItem(savedInstanceState.getLong("itemBeingEditedID"));
//		reloadTodoItems();
		getLoaderManager().restartLoader(CURRENT_TAG_ITEMS_LOADER_ID, null, currentTagItemsLoaderCallbacks);
	}

	@Override
	protected void onListItemClick(ListView listView, View v, int position, long id) {
		if (todoItemsStorage != null && (clickAnywhereToCheck || ((TodoItemsListView) listView).isClickedOnCheckMark())) {
			final TodoItem todoItem = todoItemsStorage.loadTodoItem(id);
			todoItem.setDone(!todoItem.isDone());
			todoItemsStorage.saveTodoItem(todoItem);
		}
	}

	@Override
	public boolean onKeyDown(final int keyCode, @NotNull final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (getMyListView().handleBack())
				return true;
//			if (lastDeletedItem != null && System.currentTimeMillis() - lastDeletedTimestamp < 3000) {
//				undelete();
//				return true;
//			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showUndeleteButton() {
		getUndeleteButton().setVisibility(View.VISIBLE);
		getUndeleteButton().hideAfter(HIDE_UNDELETE_AFTER);
	}

	private void undelete() {
		if (lastDeletedItem != null && todoItemsStorage != null) {
			todoItemsStorage.addTodoItem(lastDeletedItem);
			lastDeletedItem = null;
		}
		getUndeleteButton().hideNoAnimation();
	}

	private long addTodoItem() {
		final String st = getAddTaskWidget().getText().toString();
		return addTodoItem(st);
	}

	private long addTodoItem(final String st) {
		final EditText et = getAddTaskWidget();
		if (todoItemsStorage != null && st.length() > 0) {
			long currentTagId = getCurrentTagID();
			if (currentTagId == DBHelper.ALL_TAGS_METATAG_ID || currentTagId == DBHelper.TODAY_METATAG_ID) {
				currentTagId = DBHelper.UNFILED_METATAG_ID;
			}

			final Long due;
			if (defaultDue == -1) {
				if (this.currentTagId == DBHelper.TODAY_METATAG_ID) {
					due = Calendar.getInstance().getTimeInMillis();
				} else {
					due = null;
				}
			} else {
				due = defaultDue;
			}

			final TodoItem todoItem = todoItemsStorage.addTodoItem(new TodoItem(-1, currentTagId, false, st, null, defaultPrio, 0, due, null));
			et.setText("");
			et.requestFocus();
			return todoItem.id;
		}
		return -1;
	}

	private void setDefaultDue(final long d) {
		if (defaultDue != d) {
			defaultDue = d;
			updateImgButton(getDefaultDueTxtButton(), getDefaultDueImgButton(), d == -1 ? null : Util.showDueDate(this, d));
		}
	}

	@SuppressLint("SetTextI18n")
	private void setDefaultPrio(final int p) {
		if (defaultPrio != p) {
			defaultPrio = p;
			final Button button = getDefaultPrioButton();
			button.setText(Integer.toString(p));
			button.invalidate();
		}
	}

	private void setCurrentTag(final long id, boolean reload) {
		currentTagId = id;
		updateTitle();
		if (reload)
			getLoaderManager().restartLoader(CURRENT_TAG_ITEMS_LOADER_ID, null, currentTagItemsLoaderCallbacks);
	}

	private long getCurrentTagID() {
		return currentTagId;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (editingItem != null) {
			stopEditingItem(true);
			return true;
		} else {
			return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
		}
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
		if (info == null || todoItemsStorage == null) return false;
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
				todoItemsStorage.deleteTodoItem(id);
				showUndeleteButton();
				return true;
			case CHANGE_PRIO_CONTEXT_MENU_ITEM:
				selectPrio(new Callback1<Integer, Unit>() {
					public Unit call(final Integer prio) {
						todoItem.prio = prio;
						todoItemsStorage.saveTodoItem(todoItem);
						return Unit.u;
					}
				});
				return true;
			case CHANGE_PROGRESS_CONTEXT_MENU_ITEM:
				b = new AlertDialog.Builder(this);
				b.setTitle(R.string.select_progress_title);
				b.setItems(new CharSequence[]{"0%", "10%", "20%", "30%", "40%", "50%", "60%", "70%", "80%", "90%", "100%"},
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog, final int which) {
								todoItem.setProgress(which * 10);
								todoItemsStorage.saveTodoItem(todoItem);
							}
						});
				b.show();
				return true;
			case CHANGE_DUE_DATE_CONTEXT_MENU_ITEM:
				new DueDateSelector() {
					@Override
					public void onDueDateSelected(final Long dueDate) {
						todoItem.setDueDate(dueDate);
						todoItemsStorage.saveTodoItem(todoItem);
					}

					@Override
					public Long getCurrentDueDate() {
						return todoItem.getDueDate();
					}
				}.onClick(getMyListView());
				return true;
			case CHANGE_TAG_CONTEXT_MENU_ITEM:
				b = new AlertDialog.Builder(this);
				b.setTitle(R.string.select_tag_title);
				final Cursor cursor = tagsStorage.getAllTagsExceptCursor(todoItem.tagID,
						DBHelper.ALL_TAGS_METATAG_ID, DBHelper.TODAY_METATAG_ID);
				final ListAdapter adapter = Util.createTagsAdapter(this, cursor, android.R.layout.simple_dropdown_item_1line);

				b.setAdapter(adapter, new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int which) {
						todoItem.tagID = adapter.getItemId(which);
						todoItemsStorage.saveTodoItem(todoItem);
						cursor.close();
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

	private void selectPrio(final Callback1<Integer, Unit> cb) {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.select_prio_title);
		b.setItems(new CharSequence[]{"1", "2", "3", "4", "5"}, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				cb.call(which + 1);
			}
		});
		b.show();
	}

	private void exportData(final Context context) {
		doWithPermission(context, EXPORT_DATA_PERMISSION_REQUEST_CODE, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.req_storage_write, new Runnable() {
			@Override
			public void run() {
				exportDataWithPermissionsGranted();
			}
		});
	}

	private void exportDataWithPermissionsGranted() { //any good reason to export/import in background? It's very quick anyways
		final LayoutInflater inf = LayoutInflater.from(this);
		@SuppressLint("InflateParams")
		final View textEntryView = inf.inflate(R.layout.alert_text_entry, null);
		final File currentPath = new File(Environment.getExternalStorageDirectory(), "ktodo.xml");
		final String currentName = currentPath.getAbsolutePath(); //todo use real Save As dialog
		final EditText editText = textEntryView.findViewById(R.id.text_entry);
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
							// TODO show non-intrusive "done" message
						} catch (final IOException e) {
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

	private void importData(final Context context) {
		doWithPermission(context, IMPORT_DATA_PERMISSION_REQUEST_CODE, Manifest.permission.READ_EXTERNAL_STORAGE, R.string.req_storage_read, new Runnable() {
			@Override
			public void run() {
				importDataWithPermissionsGranted();
			}
		});
	}

	private void importDataWithPermissionsGranted() {
		final LayoutInflater inf = LayoutInflater.from(this);
		@SuppressLint("InflateParams") final View dialogView = inf.inflate(R.layout.import_dialog, null);
		final File currentPath = new File(Environment.getExternalStorageDirectory(), "ktodo.xml");
		final String currentName = currentPath.getAbsolutePath(); //todo use real Open dialog
		final EditText editText = dialogView.findViewById(R.id.text_entry);
		editText.setText(currentName);
		final CheckBox wipe = dialogView.findViewById(R.id.wipe_checkbox);

		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string._import);
		b.setMessage(R.string.import_file_name);
		b.setCancelable(true);
		b.setView(dialogView);
		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialogInterface, final int i) {
				if (wipe.isChecked() && todoItemsStorage != null) { //additional warning?
					setCurrentTag(DBHelper.ALL_TAGS_METATAG_ID, true);
					todoItemsStorage.deleteAllTodoItems();
					tagsStorage.deleteAllTags();
				}
				final String st = editText.getText().toString();
				runAsynchronously(R.string._import, R.string.importing_data, new Runnable() {
					public void run() {
						try {
							XmlImporter.importData(KTodo.this, new File(st), false);
						} catch (final IOException e) {
							Log.e(TAG, "error importing data", e);
							showErrorFromAnotherThread(e.toString());
						}
						reloadTodoItemsFromUIThread();
					}
				});
			}
		});

		final AlertDialog dialog = b.create();
		Util.setupEditTextEnterListener(editText, dialog);
		dialog.show();
	}

	private void doWithPermission(final Context context, final int permissionRequestCode, final String permissionName, int explanation, final Runnable callback) {
		int permissionCheckResult = ContextCompat.checkSelfPermission(this, permissionName);
		if (permissionCheckResult != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionName)) {
				final AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setTitle(R.string.missing_permission);
				b.setMessage(explanation);
				b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						permissionRequests.put(permissionRequestCode, callback);
						ActivityCompat.requestPermissions(KTodo.this, new String[]{permissionName}, permissionRequestCode);
					}
				});
				b.setCancelable(false);
				b.create().show();
			} else {
				final AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setTitle(R.string.missing_permission);
				b.setMessage(explanation);
				b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.setData(Uri.parse("package:" + context.getPackageName()));
						startActivity(intent);
					}
				});
				b.setCancelable(false);
				b.create().show();
			}
		} else {
			callback.run();
		}
	}

	@Override
	public void onRequestPermissionsResult(int permissionRequestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
		if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			Runnable callback = permissionRequests.get(permissionRequestCode);
			permissionRequests.remove(permissionRequestCode);
			if (callback != null) {
				callback.run();
			}
		}
	}

	private void showErrorFromAnotherThread(final String msg) {
		handler.post(new Runnable() {
			public void run() {
				new AlertDialog.Builder(KTodo.this).setTitle(R.string.error).setPositiveButton(android.R.string.ok, null).setMessage(msg).show();
			}
		});
	}

	private void startVoiceRecognition() {
		if (voiceRecognitionIntent != null)
			startActivityForResult(voiceRecognitionIntent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	private void setupVoiceRecognition() {
		voiceDrawable = null;
		voiceRecognitionIntent = null;
		if (useVoiceInput) {
			final PackageManager pm = getPackageManager();

			voiceRecognitionIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			voiceRecognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			voiceRecognitionIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_rec_prompt));
			voiceRecognitionIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

			if (pm.resolveActivity(voiceRecognitionIntent, PackageManager.MATCH_DEFAULT_ONLY) == null)
				voiceRecognitionIntent = null;
			else
				//requires API 21
				voiceDrawable = getResources().getDrawable(android.R.drawable.ic_btn_speak_now);
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
//		Log.i(TAG, "onActivityResult " + requestCode + ", data: " + (data == null ? "null" : data.toURI()));
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
//			Log.i(TAG, "onActivityResult resultCode " + resultCode);
			if (resultCode == RESULT_OK) {
				assert data != null;
				final List<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//				Log.i(TAG, "onActivityResult matches:" + matches.size());
				final AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setCancelable(true);
				b.setNeutralButton(R.string.try_again, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						startVoiceRecognition();
					}
				});
				b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						dialog.cancel();
					}
				});
				if (matches.size() == 1) {
					b.setTitle(R.string.voice_rec_confirm_title);
					final String bestMatch = matches.get(0);
					b.setMessage(bestMatch);
					b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							dialog.dismiss();
							addTodoItem(bestMatch);
						}
					});
				} else {
					b.setTitle(R.string.did_you_mean);
//					final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, matches);
//					b.setAdapter(adapter, new DialogInterface.OnClickListener() {
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//							dialog.dismiss();
//							addTodoItem(matches.get(which));
//						}
//					});
					b.setItems(matches.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							dialog.dismiss();
							addTodoItem(matches.get(which));
						}
					});
				}
				b.show();
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void runAsynchronously(final int title, final int message, final Runnable r) {
		final ProgressDialog pg = ProgressDialog.show(this, getString(title), getString(message), true, false);

		new Thread() {
			@Override
			public void run() {
				r.run();
				pg.dismiss();
			}
		}.start();
	}

	private DrawerLayout getDrawerLayout() {
		return (DrawerLayout) findViewById(R.id.drawer_layout);
	}

	private ListView getTagsList() {
		return (ListView) findViewById(R.id.tags_list);
	}

	private ListView getDrawerMenu() {
		return (ListView) findViewById(R.id.menu_list);
	}

	private EditText getAddTaskWidget() {
		return (EditText) findViewById(R.id.add_task);
	}

	private SlideLeftImageButton getAddTaskButton() {
		return (SlideLeftImageButton) findViewById(R.id.add_task_button);
	}

	private TodoItemsListView getMyListView() {
		return (TodoItemsListView) findViewById(android.R.id.list);
	}

	private AnimatedVisibilityButton getUndeleteButton() {
		return (AnimatedVisibilityButton) findViewById(R.id.undelete_button);
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

	private Button getDefaultDueTxtButton() {
		return (Button) findViewById(R.id.default_due_txt_button);
	}

	private ImageButton getDefaultDueImgButton() {
		return (ImageButton) findViewById(R.id.default_due_img_button);
	}

	private Button getDefaultPrioButton() {
		return (Button) findViewById(R.id.default_prio_button);
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

	private ImageButton getDueDateImgButton() {
		return (ImageButton) findViewById(R.id.due_date_img_button);
	}

	private Button getDueDateTxtButton() {
		return (Button) findViewById(R.id.due_date_txt_button);
	}

	private class AllTagsLoaderCallbacks extends CursorAdapterManagingLoaderCallbacks {
		private final Context ctx;
		private TagsStorage loaderTagsStorage;

		private AllTagsLoaderCallbacks(final Context ctx, final CursorAdapter cursorAdapter) {
			super(cursorAdapter);
			this.ctx = ctx;
			loaderTagsStorage = new TagsStorage(ctx);
			loaderTagsStorage.open();
		}

		@Override
		public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
			return new AllTagsLoader(ctx, loaderTagsStorage);
		}

		void shutdown() {
			loaderTagsStorage.close();
		}
	}

	private class CurrentTagItemsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
		private final Context ctx;
		private TodoItemsStorage todoItemsStorage;

		private CurrentTagItemsLoaderCallbacks(final Context ctx) {
			this.ctx = ctx;
			todoItemsStorage = new TodoItemsStorage(ctx);
			todoItemsStorage.open();
		}

		@Override
		public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
			return new CurrentTagItemsLoader(ctx, todoItemsStorage, getCurrentTagID(), hidingCompleted, sortingMode);
		}

		@Override
		public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
			reloadTodoItems(data);
		}

		@Override
		public void onLoaderReset(final Loader<Cursor> loader) {
			if (todoAdapter != null) todoAdapter.swapCursor(null);
		}

		void shutdown() {
			todoItemsStorage.close();
		}
	}

	private class EditingItemTagsLoaderCallbacks extends CursorAdapterManagingLoaderCallbacks {
		private final Context ctx;
		private TagsStorage loaderTagStorage;

		private EditingItemTagsLoaderCallbacks(final Context ctx, final CursorAdapter cursorAdapter) {
			super(cursorAdapter);
			this.ctx = ctx;
			loaderTagStorage = new TagsStorage(ctx);
			loaderTagStorage.open();
		}

		@Override
		public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
			return new TagsCursorLoader(ctx, loaderTagStorage);
		}

		void shutdown() {
			loaderTagStorage.close();
		}
	}

	private static class CurrentTagItemsLoader extends CustomCursorLoader {
		private final TodoItemsStorage todoItemsStorage;
		private final long currentTagId;
		private final boolean hidingCompleted;
		private final TodoItemsSortingMode sortingMode;

		private CurrentTagItemsLoader(Context ctx, TodoItemsStorage todoItemsStorage, long currentTagId, boolean hidingCompleted, TodoItemsSortingMode sortingMode) {
			super(ctx, TodoItemsStorage.CHANGE_NOTIFICATION_URI);
			this.todoItemsStorage = todoItemsStorage;
			this.currentTagId = currentTagId;
			this.hidingCompleted = hidingCompleted;
			this.sortingMode = sortingMode;
		}

		@Override
		public Cursor createCursor() {
			if (hidingCompleted)
				return todoItemsStorage.getByTagCursorExcludingCompleted(currentTagId, sortingMode);
			else
				return todoItemsStorage.getByTagCursor(currentTagId, sortingMode);
		}
	}

	private static class TagsCursorLoader extends CustomCursorLoader {
		private final TagsStorage tagStorage;

		TagsCursorLoader(Context context, TagsStorage tagStorage) {
			super(context, TagsStorage.CHANGE_NOTIFICATION_URI);
			this.tagStorage = tagStorage;
		}

		@Override
		public Cursor createCursor() {
			return tagStorage.getAllTagsExceptCursor(DBHelper.ALL_TAGS_METATAG_ID, DBHelper.TODAY_METATAG_ID);
		}
	}
}
