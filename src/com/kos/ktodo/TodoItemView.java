package com.kos.ktodo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;

import java.lang.reflect.Field;

public class TodoItemView extends CheckedTextView {
	private static final String[] PRIO_TO_STRING = new String[]{"0", "1", "2", "3", "4", "5"};
	private static final Field mPaddingRightFld;

	static {
		try {
			mPaddingRightFld = View.class.getDeclaredField("mPaddingRight");
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("failed to initialize", e);
		}
		mPaddingRightFld.setAccessible(true);
	}

	private String prio;
	private boolean showNotesMark;

	private String dueDate;
	private DueStatus dueStatus;
	private float dueDateWidth = -1;

	private int paddingRight;
	private int checkMarkWidth;
	private int minHeight;

	private final TodoItemBackgroundDrawable tcd;
	private final Drawable notesDrawable;
	private final int dueDateColor, todayDueDateColor, expiredDueDateColor;
	private final int[] prioToColor;

	public TodoItemView(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TodoItemView);

		final int prio1Color = prefs.getInt("prio1Color", ta.getColor(R.styleable.TodoItemView_prio1Color, context.getResources().getColor(R.color.prio_1)));
		final int prio2Color = prefs.getInt("prio2Color", ta.getColor(R.styleable.TodoItemView_prio2Color, context.getResources().getColor(R.color.prio_2)));
		final int prio3Color = prefs.getInt("prio3Color", ta.getColor(R.styleable.TodoItemView_prio3Color, context.getResources().getColor(R.color.prio_3)));
		final int prio4Color = prefs.getInt("prio4Color", ta.getColor(R.styleable.TodoItemView_prio4Color, context.getResources().getColor(R.color.prio_4)));
		final int prio5Color = prefs.getInt("prio5Color", ta.getColor(R.styleable.TodoItemView_prio5Color, context.getResources().getColor(R.color.prio_5)));


		final int c1 = ta.getColor(R.styleable.TodoItemView_progress0Color, Color.BLACK);
		final int c2 = prefs.getInt("progressColor", ta.getColor(R.styleable.TodoItemView_progress100Color, Color.GRAY));
		dueDateColor = prefs.getInt("dueDateColor", ta.getColor(R.styleable.TodoItemView_dueDateColor, Color.WHITE));
		todayDueDateColor = prefs.getInt("dueTodayColor", ta.getColor(R.styleable.TodoItemView_todayDueDateColor, Color.YELLOW));
		expiredDueDateColor = prefs.getInt("overdueColor", ta.getColor(R.styleable.TodoItemView_expiredDueDateColor, Color.RED));

		prioToColor = new int[]{
				ta.getColor(R.styleable.TodoItemView_prio0Color, context.getResources().getColor(R.color.prio_0)),
				prio1Color,
				prio2Color,
				prio3Color,
				prio4Color,
				prio5Color,
		};

		final int prioStripeWidth = (int) ta.getDimension(R.styleable.TodoItemView_prioStripeWidth, 2);
		tcd = new TodoItemBackgroundDrawable(c1, c2, prioStripeWidth);
		notesDrawable = ta.getDrawable(R.styleable.TodoItemView_notesDrawable);
		ta.recycle();
		setBackgroundDrawable(tcd);
	}

	public void setPrio(final int prio) {
		this.prio = PRIO_TO_STRING[prio];
		if (!isChecked())
			tcd.setPrioColor(prioToColor[prio]);
		else
			tcd.setPrioColor(prioToColor[0]);
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

	public int getCheckMarkWidth() {
		return checkMarkWidth;
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
		//mPaddingRight = p;
		try {
			mPaddingRightFld.set(this, p);
		} catch (IllegalAccessException e) {
			Log.i("KTodo", e.toString());
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
