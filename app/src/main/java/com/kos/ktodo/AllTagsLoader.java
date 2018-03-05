package com.kos.ktodo;


import android.content.Context;
import android.database.Cursor;


public class AllTagsLoader extends CustomCursorLoader {
	private final TagsStorage loaderTagsStorage;

	public AllTagsLoader(Context ctx, TagsStorage loaderTagsStorage) {
		super(ctx, TagsStorage.CHANGE_NOTIFICATION_URI);
		this.loaderTagsStorage = loaderTagsStorage;
	}

	@Override
	public Cursor createCursor() {
		return loaderTagsStorage.getAllTagsCursor();
	}
}
