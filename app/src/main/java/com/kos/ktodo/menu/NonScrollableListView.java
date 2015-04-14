package com.kos.ktodo.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Non-scrollable list view, used inside a ScrollView by the left side drawer.
 */
public class NonScrollableListView extends ListView {
	public NonScrollableListView(Context context) {
		super(context);
	}

	public NonScrollableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NonScrollableListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int heightMeasureSpec_custom = MeasureSpec.makeMeasureSpec(
				Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec_custom);
		ViewGroup.LayoutParams params = getLayoutParams();
		params.height = getMeasuredHeight();
	}
}
