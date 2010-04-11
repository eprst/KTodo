package com.kos.ktodo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class SliderButton extends Button {
	private final String prefix;
	private final String separator;
	private final String[] values;
	private final String[] suffixedValues;
	private OnChangeListener onChangeListener;

	private int currentSelection;
	private boolean sliding;
	private int slideStartX;
	private int slideStartIndex;

	private AlertDialog dlg;

	public SliderButton(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		prefix = getText().toString();
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SliderButton);
		final String valueSuffix = ta.getString(R.styleable.SliderButton_valueSuffix);
		final String vals = ta.getString(R.styleable.SliderButton_valuesList);
		values = vals.split(",");
		final String sep = ta.getString(R.styleable.SliderButton_separator);
		separator = sep == null ? "" : sep;

		if (valueSuffix == null)
			suffixedValues = values;
		else {
			suffixedValues = new String[values.length];
			for (int i = 0; i < values.length; i++)
				suffixedValues[i] = values[i] + valueSuffix;
		}

		setSelection(ta.getInt(R.styleable.SliderButton_defaultValueIndex, 0));

		setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if (sliding) return;
				final AlertDialog.Builder b = new AlertDialog.Builder(context);
				b.setTitle(prefix);
				b.setItems(suffixedValues, new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int which) {
						setSelection(which);
						notifyOnChangeListener();
					}
				});
				b.show();
			}
		});
	}

	public void setOnChangeListener(final OnChangeListener onChangeListener) {
		this.onChangeListener = onChangeListener;
	}

	public void setSelection(final int index) {
		currentSelection = index;
		final StringBuilder sb = new StringBuilder(prefix);
		sb.append(separator);
		sb.append(suffixedValues[index]);
		setText(sb.toString());
		invalidate();
	}

	private void notifyOnChangeListener() {
		if (onChangeListener != null)
			onChangeListener.valueChanged(values[currentSelection]);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				slideStartX = (int) event.getX();
				slideStartIndex = currentSelection;
				sliding = false;
				break;
			case MotionEvent.ACTION_MOVE:
				final int unit = getWidth() / (values.length + 2);
				final int delta = (int) (event.getX() - slideStartX);
				final int selDelta = delta / unit;
				int newSelection = slideStartIndex + selDelta;
				if (newSelection < 0) newSelection = 0;
				if (newSelection >= values.length) newSelection = values.length - 1;
				if (newSelection != currentSelection) {
					sliding = true;
					setSelection(newSelection);
					notifyOnChangeListener();
					if (dlg == null) {
						final AlertDialog.Builder b = new AlertDialog.Builder(getContext());
						b.setTitle(prefix);
						b.setMessage(suffixedValues[newSelection]);
						dlg = b.create();
						dlg.show();
					} else {
						dlg.setMessage(suffixedValues[newSelection]);
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				if (dlg != null) {
					dlg.dismiss();
					dlg = null;
				}
				break;
		}
		return super.onTouchEvent(event);
	}

	public interface OnChangeListener {
		void valueChanged(final String newValue);
	}
}
