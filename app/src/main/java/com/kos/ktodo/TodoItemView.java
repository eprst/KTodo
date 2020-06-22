package com.kos.ktodo;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import com.kos.ktodo.preferences.Preferences;
import org.jetbrains.annotations.NotNull;

public class TodoItemView extends androidx.appcompat.widget.AppCompatCheckedTextView {
	private static final String[] PRIO_TO_STRING = new String[]{"0", "1", "2", "3", "4", "5"};

	private String prio;
	private boolean showNotesMark;

	private Drawable checkmark;

	private String dueDate;
	private Float dueDateWidth;
	private DueStatus dueStatus;

	private int paddingRightPx;
	private int checkMarkWidthPx;
	private int minHeight;

	private final float prioFontSizePx;
	private final float notesPrioMarginRightPx;
	private final float prioMarginTopPx;
	private final float notesMarginBottomPx;
	private final float dueMarginBottomPx;
	private final float dueFontSizePx;
	private final float dueMarginRightPx;

	private final TodoItemBackgroundDrawable tcd;
	private final Drawable notesDrawable;
	private final int dueDateColor, todayDueDateColor, expiredDueDateColor;
	private final int[] prioToColor;

	public TodoItemView(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		final TypedArray ta_tiv = context.obtainStyledAttributes(attrs, R.styleable.TodoItemView);
		@SuppressLint("CustomViewStyleable")
		final TypedArray ta_prio = context.obtainStyledAttributes(attrs, R.styleable.PrioColor);

		final int prio1Color = prefs.getInt(Preferences.PRIO1_COLOR, ta_prio.getColor(R.styleable.PrioColor_prio1Color, context.getResources().getColor(R.color.prio_1)));
		final int prio2Color = prefs.getInt(Preferences.PRIO2_COLOR, ta_prio.getColor(R.styleable.PrioColor_prio2Color, context.getResources().getColor(R.color.prio_2)));
		final int prio3Color = prefs.getInt(Preferences.PRIO3_COLOR, ta_prio.getColor(R.styleable.PrioColor_prio3Color, context.getResources().getColor(R.color.prio_3)));
		final int prio4Color = prefs.getInt(Preferences.PRIO4_COLOR, ta_prio.getColor(R.styleable.PrioColor_prio4Color, context.getResources().getColor(R.color.prio_4)));
		final int prio5Color = prefs.getInt(Preferences.PRIO5_COLOR, ta_prio.getColor(R.styleable.PrioColor_prio5Color, context.getResources().getColor(R.color.prio_5)));


		final int c1 = ta_tiv.getColor(R.styleable.TodoItemView_progress0Color, Color.TRANSPARENT);
		final int c2 = prefs.getInt(Preferences.PROGRESS_COLOR, ta_tiv.getColor(R.styleable.TodoItemView_progress100Color, Color.GRAY));
		dueDateColor = prefs.getInt(Preferences.DUE_DATE_COLOR, ta_tiv.getColor(R.styleable.TodoItemView_dueDateColor, Color.WHITE));
		todayDueDateColor = prefs.getInt(Preferences.DUE_TODAY_COLOR, ta_tiv.getColor(R.styleable.TodoItemView_todayDueDateColor, Color.YELLOW));
		expiredDueDateColor = prefs.getInt(Preferences.OVERDUE_COLOR, ta_tiv.getColor(R.styleable.TodoItemView_expiredDueDateColor, Color.RED));

		prioToColor = new int[]{
				ta_prio.getColor(R.styleable.PrioColor_prio0Color, context.getResources().getColor(R.color.prio_0)),
				prio1Color,
				prio2Color,
				prio3Color,
				prio4Color,
				prio5Color,
		};

		final int prioStripeWidthPx = (int) ta_tiv.getDimension(R.styleable.TodoItemView_prioStripeWidth, 0.0f);
		tcd = new TodoItemBackgroundDrawable(c1, c2, prioStripeWidthPx);
		notesDrawable = ta_tiv.getDrawable(R.styleable.TodoItemView_notesDrawable);

		prioFontSizePx = ta_tiv.getDimension(R.styleable.TodoItemView_prioFontSize, 0.0f);
		dueFontSizePx = ta_tiv.getDimension(R.styleable.TodoItemView_dueFontSize, 0.0f);
		notesPrioMarginRightPx = ta_tiv.getDimension(R.styleable.TodoItemView_notesPrioMarginRight, 0.0f);
		notesMarginBottomPx = ta_tiv.getDimension(R.styleable.TodoItemView_notesMarginBottom, 0.0f);
		dueMarginBottomPx = ta_tiv.getDimension(R.styleable.TodoItemView_dueMarginBottom, 0.0f);
		prioMarginTopPx = ta_tiv.getDimension(R.styleable.TodoItemView_prioMarginTop, 0.0f);
		dueMarginRightPx = ta_tiv.getDimension(R.styleable.TodoItemView_dueMarginRight, 0.0f);


		ta_tiv.recycle();
		ta_prio.recycle();

		setBackground(tcd);
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
		setDueDate(dueDate);
		this.dueStatus = dueStatus;
		dueDateWidth = null;
		updateSuperCheckmark();
	}

	// for remote views
	public void setDueDate(final String dueDate) {
		this.dueDate = dueDate;
	}
	public void setDueDateStatus(final String dueStatusName) {
		this.dueStatus = DueStatus.valueOf(dueStatusName);
	}
	//

	@Override
	public void setPadding(final int left, final int top, final int right, final int bottom) {
		super.setPadding(left, top, right, bottom);
		paddingRightPx = right;
	}

	@Override
	public void setCheckMarkDrawable(final Drawable d) {
		checkmark = d;
		checkMarkWidthPx = d != null ? d.getIntrinsicWidth() : 0;
		updateSuperCheckmark();
	}

	public int getCheckMarkWidthPx() {
		return checkMarkWidthPx;
	}

	private void updateSuperCheckmark() {
		if (checkmark == null)
			return;

		float acc = notesPrioMarginRightPx;
		if (notesDrawable != null)
			acc += notesDrawable.getIntrinsicWidth();

		if (dueDate != null)
			acc += getDueDateWidth() + dueMarginRightPx;

		final int checkmarkWidthExtra = (int) acc;

		// pretend checkmark drawable is wider by checkmarkWidthExtra
		// to accommodate more space for prio, notes mark and due date
		super.setCheckMarkDrawable(new DelegatingDrawable(checkmark) {
			@Override
			public int getIntrinsicWidth() {
				return checkmark.getIntrinsicWidth() + checkmarkWidthExtra;
			}

			@Override
			public void setBounds(int left, int top, int right, int bottom) {
				super.setBounds(left + checkmarkWidthExtra, top, right, bottom);
			}
		});
	}

	private float getDueDateWidth() {
		if (dueDateWidth != null)
			return dueDateWidth;
		if (dueDate == null)
			return 0.0f;

		final TextPaint p = new TextPaint(getPaint());
		p.setTextSize(dueFontSizePx);
		dueDateWidth = p.measureText(dueDate);
		return dueDateWidth;
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
			setBackground(null);
		else
			setBackground(tcd);
	}

	@Override
	public void setMinHeight(final int minHeight) {
		//this allows to workaround the fact that setCheckmarkDrawable sets min height to 72 (check mark height)
		//essentially we make the value set in constructor (and thus obtained from xml) to be the final one
		if (this.minHeight == 0) {
			this.minHeight = minHeight;
			super.setMinHeight(minHeight);
		}
	}

	private final Rect bounds = new Rect();
	@Override
	protected void onDraw(@NotNull final Canvas canvas) {
		super.onDraw(canvas);

		@SuppressLint("DrawAllocation")
		final Paint p = new Paint(getPaint());
		p.setTextSize(prioFontSizePx);

		final float prioWidthPx = p.measureText(prio);
		p.getTextBounds(prio, 0, prio.length(), bounds);
		final float prioHeightPx = bounds.height();

		final float widthMinusCheckmarkPadding = getWidth() - checkMarkWidthPx - paddingRightPx;
		final float prioX = widthMinusCheckmarkPadding - prioWidthPx - notesPrioMarginRightPx + prioWidthPx / 2;
		float notesX = prioX;

		canvas.drawText(prio, prioX, prioMarginTopPx + prioHeightPx, p);

		if (notesDrawable != null) {
			final int notesHeightPx = notesDrawable.getIntrinsicHeight();
			final int notesWidthPx = notesDrawable.getIntrinsicWidth();
			notesX = widthMinusCheckmarkPadding - notesWidthPx - notesPrioMarginRightPx + notesWidthPx / 2;
			if (showNotesMark) {
				notesDrawable.setBounds((int) notesX, (int) (getHeight() - notesHeightPx - notesMarginBottomPx),
						(int) (notesX + notesWidthPx), (int) (getHeight() - notesMarginBottomPx));
				notesDrawable.draw(canvas);
			}
		}
		if (dueDate != null) {
			p.setTextSize(dueFontSizePx);

			int color = dueDateColor;
			if (!isChecked()) {
				if (dueStatus == DueStatus.TODAY)
					color = todayDueDateColor;
				else if (dueStatus == DueStatus.EXPIRED)
					color = expiredDueDateColor;
			}

			p.setColor(color);

			canvas.drawText(dueDate, notesX - getDueDateWidth() - dueMarginRightPx, getHeight() - dueMarginBottomPx, p);
		}
	}
}
