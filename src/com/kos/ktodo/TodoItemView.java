package com.kos.ktodo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

public class TodoItemView extends CheckedTextView {
	private String prio;
	private int paddingRight;
	private int checkMarkWidth;

	private TwoColorDrawable tcd;
	private int progress;

	public TodoItemView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TodoItemView);
		final int c1 = ta.getColor(R.styleable.TodoItemView_progress0Color, Color.BLACK);
		final int c2 = ta.getColor(R.styleable.TodoItemView_progress100Color, Color.GRAY);
		tcd = new TwoColorDrawable(c1, c2);
		ta.recycle();
		setBackgroundDrawable(tcd);
	}

	public void setPrio(final int prio) {
		this.prio = Integer.toString(prio);
	}

	public void setProgress(final int progress) {
		this.progress = progress;
		tcd.setPercent(progress);
	}

	@Override
	public void setPadding(final int left, final int top, final int right, final int bottom) {
		super.setPadding(left, top, right, bottom);
		paddingRight = right;
	}

	@Override
	public void setCheckMarkDrawable(final Drawable d) {
		super.setCheckMarkDrawable(d);
		if (d != null) {
			checkMarkWidth = d.getIntrinsicWidth();
			mPaddingRight += 20;
		}
	}

	@Override
	public void setSelected(final boolean selected) {
		super.setSelected(selected);
		updateBackground();
	}

	@Override
	public void setPressed(final boolean pressed) {
		super.setPressed(pressed);
		updateBackground();
	}

	private void updateBackground() {
		if (isSelected() || isPressed())
			setBackgroundDrawable(null);
		else
			setBackgroundDrawable(tcd);
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
