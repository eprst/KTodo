package com.kos.ktodo.widget;

import com.kos.ktodo.TodoItemsSortingMode;
import com.kos.ktodo.Util;

public class WidgetSettings {
	public final int widgetID;
	public long[] tagIDs = new long[0];
	public boolean configured = false;
	public boolean hideCompleted = true;
	public boolean showOnlyDue = false;
	public int showOnlyDueIn = -1;
	public TodoItemsSortingMode sortingMode = TodoItemsSortingMode.PRIO_DUE_SUMMARY;

	public WidgetSettings(final int widgetID) {
		this.widgetID = widgetID;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("WidgetSettings");
		sb.append("{configured=").append(configured);
		sb.append(", widgetID=").append(widgetID);
		sb.append(", tagIDs=").append(Util.separate("[", "]", ",", Util.toString(tagIDs)));
		sb.append(", hideCompleted=").append(hideCompleted);
		sb.append(", showOnlyDue=").append(showOnlyDue);
		sb.append(", showOnlyDueIn=").append(showOnlyDueIn);
		sb.append(", sortingMode=").append(sortingMode);
		sb.append('}');
		return sb.toString();
	}
}
