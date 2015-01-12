package com.kos.ktodo.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.kos.ktodo.R;
import com.kos.ktodo.widget.UpdateService;

/**
 * Preferences screen.
 */
public class Preferences extends PreferenceActivity {
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
		UpdateService.requestUpdateAll(this);
		startService(new Intent(this, UpdateService.class));
		super.onStop();
	}
}