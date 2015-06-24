package com.kos.ktodo.widget;

public enum WidgetItemOnClickAction {
	NONE,
	MARK_DONE,
	EDIT_ITEM,
	OPEN_TAG;

	public static final WidgetItemOnClickAction DEFAULT = OPEN_TAG;

	public static WidgetItemOnClickAction fromOrdinal(final int ord) {
		return WidgetItemOnClickAction.values()[ord];
	}
}
