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

	private String dueDate;
	private DueStatus dueStatus;
	private float dueDateWidth = -1;

	private int paddingRight;
	private int checkMarkWidth;
	private int minHeight;

	private final TwoColorDrawable tcd;
	private final Drawable notesDrawable;
	private final int dueDateColor, todayDueDateColor, expiredDueDateColor;

	public TodoItemView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TodoItemView);
		final int c1 = ta.getColor(R.styleable.TodoItemView_progress0Color, Color.BLACK);
		final int c2 = ta.getColor(R.styleable.TodoItemView_progress100Color, Color.GRAY);
		dueDateColor = ta.getColor(R.styleable.TodoItemView_dueDateColor, Color.WHITE);
		todayDueDateColor = ta.getColor(R.styleable.TodoItemView_todayDueDateColor, Color.YELLOW);
		expiredDueDateColor = ta.getColor(R.styleable.TodoItemView_expiredDueDateColor, Color.RED);
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

	public void setDueDate(final String dueDate, final DueStatus dueStatus) {
		this.dueDate = dueDate;
		this.dueStatus = dueStatus;
		dueDateWidth = -1;
		updateSuperPadding();
	}

	@Override
	public void setPadding(final int left, final int top, final int right, final int bottom) {
		super.setPadding(left, top, right, bottom);
		paddingRight = right;
	}

	@Override
	public void setCheckMarkDrawable(final Drawable d) {
		super.setCheckMarkDrawable(d);
		checkMarkWidth = d != null ? d.getIntrinsicWidth() : 0;
		updateSuperPadding();
	}

	private void updateSuperPadding() {
		int p = 2 * checkMarkWidth;
		if (dueDate != null) {
			final Paint paint = new Paint(getPaint());
			final int sz = checkMarkWidth / 2;
			paint.setTextSize(sz - 2);
			dueDateWidth = paint.measureText(dueDate) + sz;
			p += dueDateWidth;
		}
		mPaddingRight = p;
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
	public void setMinHeight(final int minHeight) {
		//this allows to workaround the fact that setCheckmarkDrawable sets min heigh to 72 (check mark height)
		//essentially we make the value set in constructor (and thus obtained from xml) to be the final one
		if (this.minHeight == 0) {
			this.minHeight = minHeight;
			super.setMinHeight(minHeight);
		}
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		final Paint p = new Paint(getPaint());
		final int sz = checkMarkWidth / 2;
		p.setTextSize(sz - 2);
		final int pl = getWidth() - checkMarkWidth - paddingRight - sz;
		canvas.drawText(prio, pl, sz + 2, p);
		if (showNotesMark && notesDrawable != null) {
			final int ih = notesDrawable.getIntrinsicHeight();
			final int iw = notesDrawable.getIntrinsicWidth();
			final int height = getHeight();
			notesDrawable.setBounds(pl - 2, height - ih - 4, pl - 2 + iw, height - 4);
			notesDrawable.draw(canvas);
		}
		if (dueDate != null) {
//			p.setTextSize(getPaint().getTextSize());
			int color = dueDateColor;
			if (!isChecked()) {
				if (dueStatus == DueStatus.TODAY)
					color = todayDueDateColor;
				else if (dueStatus == DueStatus.EXPIRED)
					color = expiredDueDateColor;
			}

			p.setColor(color);
//			final float textHeight = p.getFontMetrics().top;
//			final float y = (getHeight() - textHeight) / 2;
			canvas.drawText(dueDate, pl - dueDateWidth, getHeight() - 6, p);
		}
	}
}
