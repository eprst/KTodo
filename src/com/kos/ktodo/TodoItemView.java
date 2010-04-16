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
	private boolean showNotesMark;
	private int paddingRight;
	private int checkMarkWidth;

	private final TwoColorDrawable tcd;
	private final Drawable notesDrawable;

	private final float[] ar = new float[2];

	public TodoItemView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TodoItemView);
		final int c1 = ta.getColor(R.styleable.TodoItemView_progress0Color, Color.BLACK);
		final int c2 = ta.getColor(R.styleable.TodoItemView_progress100Color, Color.GRAY);
		tcd = new TwoColorDrawable(c1, c2);
		notesDrawable = ta.getDrawable(R.styleable.TodoItemView_notesDrawable);
		ta.recycle();
		setBackgroundDrawable(tcd);
	}

	public void setPrio(final int prio) {
		this.prio = Integer.toString(prio);
	}

	public void setProgress(final int progress) {
		tcd.setPercent(progress);
	}

	public void setShowNotesMark(final boolean showNotesMark) {
		this.showNotesMark = showNotesMark;
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
			mPaddingRight = 2 * checkMarkWidth;
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
		final int sz = checkMarkWidth / 2;
		p.setTextSize(sz - 2);
		final int pl = getWidth() - checkMarkWidth - paddingRight - sz;
		ar[0] = pl;
		ar[1] = sz + 2;
		canvas.drawPosText(prio, ar, p);
		if (showNotesMark && notesDrawable != null) {
			final int ih = notesDrawable.getIntrinsicHeight();
			final int iw = notesDrawable.getIntrinsicWidth();
			final int height = getHeight();
			notesDrawable.setBounds(pl - 2, height - ih - 4, pl - 2 + iw, height - 4);
			notesDrawable.draw(canvas);
		}
	}
}
