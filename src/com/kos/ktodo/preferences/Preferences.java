package com.kos.ktodo.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.widget.Button;
import com.kos.ktodo.R;

/**
 * Preferences screen.
 */
public class Preferences extends PreferenceActivity {
	public Preferences() {
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

	}
}