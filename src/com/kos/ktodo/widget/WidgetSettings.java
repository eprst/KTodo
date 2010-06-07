package com.kos.ktodo.widget;

public class WidgetSettings {
	public final int widgetID;
	public int tagID = 1;
	public boolean configured = false;
	public boolean hideCompleted = true;
	public boolean showOnlyDue = false;
	public int showOnlyDueIn = -1;

	public WidgetSettings(final int widgetID) {
		this.widgetID = widgetID;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("WidgetSettings");
		sb.append("{configured=").append(configured);
		sb.append(", widgetID=").append(widgetID);
		sb.append(", tagID=").append(tagID);
		sb.append(", hideCompleted=").append(hideCompleted);
		sb.append(", showOnlyDue=").append(showOnlyDue);
		sb.append(", showOnlyDueIn=").append(showOnlyDueIn);
		sb.append('}');
		return sb.toString();
	}
}
