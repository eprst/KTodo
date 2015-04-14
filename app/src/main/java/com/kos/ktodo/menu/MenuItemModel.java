package com.kos.ktodo.menu;

public class MenuItemModel {
	private final int icon;
	private final int text;

	public MenuItemModel(int icon, int text) {
		this.icon = icon;
		this.text = text;
	}

	public int getIcon() {
		return icon;
	}

	public int getText() {
		return text;
	}
}
