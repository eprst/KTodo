package com.kos.ktodo.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import com.kos.ktodo.R;
import com.kos.ktodo.widget.WidgetUpdateService;

/**
 * Preferences screen.
 *
 * TODO: refactor into fragment-based preferences.
 * Color picker: https://github.com/lomza/android-color-picker
 */
public class Preferences extends PreferenceActivity {
	// preference names
	public static final String DELETE_BY_FLING = "delByFling";
	public static final String FLING_GRAVITY = "flingGravity";
	public static final String SLIDE_TO_EDIT = "slideToEdit";
	public static final String MAIN_LIST_FONT_SIZE = "mainListFontSize";
	public static final String CLICK_ANYWHERE_TO_CHECK = "clickAnywhereToCheck";

	public static final String DUE_TODAY_COLOR = "dueTodayColor";
	public static final String OVERDUE_COLOR = "overdueColor";

	public static final String PRIO1_COLOR = "prio1Color";
	public static final String PRIO2_COLOR = "prio2Color";
	public static final String PRIO3_COLOR = "prio3Color";
	public static final String PRIO4_COLOR = "prio4Color";
	public static final String PRIO5_COLOR = "prio5Color";

	public static final String PROGRESS_COLOR = "progressColor";
	public static final String DUE_DATE_COLOR = "dueDateColor";

	public static final String DUE_AS_DAYS_LEFT = "dueAsDaysLeft";

	private final int RESET_TO_DEFAULTS_MENU_ITEM = Menu.FIRST;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	private void resetToDefaults() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		final SharedPreferences.Editor editor = prefs.edit();
		editor.clear();
		editor.apply();
		startActivity(getIntent());
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.clear();
		menu.add(0, RESET_TO_DEFAULTS_MENU_ITEM, Menu.NONE, R.string.prefs_reset_to_defaults);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case RESET_TO_DEFAULTS_MENU_ITEM:
				resetToDefaults();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		WidgetUpdateService.requestUpdateAll(this);
		startService(new Intent(this, WidgetUpdateService.class));
		super.onStop();
	}
}