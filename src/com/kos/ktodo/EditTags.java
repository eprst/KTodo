package com.kos.ktodo;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class EditTags extends ListActivity {
	private TagsStorage tagsStorage;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_tags);

		tagsStorage = new TagsStorage(this, false);
		tagsStorage.open();
		final Cursor allTagsCursor = tagsStorage.getAllTagsCursor();
		startManagingCursor(allTagsCursor);
		final ListAdapter tagsAdapter = new SimpleCursorAdapter(
				this, android.R.layout.simple_list_item_1,
				allTagsCursor,
				new String[]{TagsStorage.TAG_NAME}, new int[]{android.R.layout.simple_list_item_1});

		setListAdapter(tagsAdapter);

		findViewById(R.id.add_tag).setOnClickListener(new View.OnClickListener() {
			public void onClick(final View view) {
				addTag();
			}
		});
	}

	private void addTag() {
		final LayoutInflater inf = LayoutInflater.from(this);
		final View textEntryView = inf.inflate(R.layout.alert_text_entry, null);

		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(getString(R.string.add_tag));
		b.setMessage(getString(R.string.add_tag_msg));
		b.setCancelable(true);
		b.setView(textEntryView);
		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialogInterface, final int i) {
				final EditText et = (EditText) textEntryView.findViewById(R.id.text_entry);
				final String st = et.getText().toString();
				tagsStorage.addTag(st);
				updateView();
			}
		});
		b.show();
	}

	private void updateView() {
		//getListView().re
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		tagsStorage.close();
	}
}
