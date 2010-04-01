package com.kos.ktodo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

public class PriorityCheckedTextView extends CheckedTextView {
	private String prio;
	private int paddingRight;
	private int checkMarkWidth;

	public PriorityCheckedTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public void setPrio(final int prio) {
		this.prio = Integer.toString(prio);
//		this.prio = Integer.toString((int) (System.currentTimeMillis() % 10));
	}

	@Override
	public void setPadding(final int left, final int top, final int right, final int bottom) {
		super.setPadding(left, top, right, bottom);
		paddingRight = right;
	}

	@Override
	public void setCheckMarkDrawable(final Drawable d) {
		super.setCheckMarkDrawable(d);
		if (d != null)
			checkMarkWidth = d.getIntrinsicWidth();
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		final Paint p = new Paint(getPaint());
//		p.setColor(Color.CYAN);
//		final int pl = getPaddingLeft();
//		Log.i("foo", "checkMarkWidth=" + checkMarkWidth);
		final int sz = checkMarkWidth / 2;
		p.setTextSize(sz - 2);
		final int pl = getWidth() - checkMarkWidth - paddingRight - sz;
		canvas.drawPosText(prio, new float[]{pl, sz + 2}, p);
	}
}
