package com.kos.ktodo.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kos.ktodo.R;

import java.util.ArrayList;
import java.util.List;

public class MenuAdapter extends ArrayAdapter<MenuItemModel> {
	private final Context context;
	private final List<MenuItemModel> modelsList;

	private final List<View> viewsCache;

	public MenuAdapter(Context context, List<MenuItemModel> modelsList) {
		super(context, R.layout.menu_item, modelsList);
		this.context = context;
		this.modelsList = modelsList;
		viewsCache = new ArrayList<>(modelsList.size());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = viewsCache.size() >= position ? null : viewsCache.get(position);

		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.menu_item, parent, false);
			viewsCache.add(position, rowView);
		}

		ImageView imgView = (ImageView) rowView.findViewById(R.id.item_icon);
		TextView titleView = (TextView) rowView.findViewById(R.id.item_title);

		imgView.setImageResource(modelsList.get(position).getIcon());
		titleView.setText(modelsList.get(position).getText());
		return rowView;
	}
}
