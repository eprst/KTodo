package com.kos.ktodo;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * A button for choosing a value from a list. Uses sliding motion to change current selection.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class SliderButton extends android.support.v7.widget.AppCompatButton {
	private final String prefix;
	private final String separator;
	private final String[] values;
	private final String[] suffixedValues;
	private Callback1<String, Unit> onChangeListener;
	private Callback1<MotionEvent, Boolean> onTrackballListener;

	private final int scaledTouchSlop;
	private int currentSelection;
	private boolean sliding;
	private int slideStartX;
	private int slideStartIndex;

	private AlertDialog dlg;

	public SliderButton(final Context context, final AttributeSet attrs) {
		super(context, attrs);
//		final ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		scaledTouchSlop = 2;//viewConfiguration.getScaledTouchSlop();
		prefix = getText().toString();
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SliderButton);
		final String valueSuffix = ta.getString(R.styleable.SliderButton_valueSuffix);
		final String vals = ta.getString(R.styleable.SliderButton_valuesList);
		assert vals != null;
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

		ta.recycle();
	}

	public void setOnChangeListener(final Callback1<String, Unit> onChangeListener) {
		this.onChangeListener = onChangeListener;
	}

	public void setOnTrackballListener(final Callback1<MotionEvent, Boolean> onTrackballListener) {
		this.onTrackballListener = onTrackballListener;
	}

	public void setSelection(final int index) {
		currentSelection = index;
		setText(prefix + separator + suffixedValues[index]);
		invalidate();
	}

	private void notifyOnChangeListener() {
		if (onChangeListener != null)
			onChangeListener.call(values[currentSelection]);
	}

	@Override
	public boolean onTrackballEvent(final MotionEvent event) {
//		if (event.getX() > 0) {
//			updateSelection(Math.min(currentSelection + 1, values.length - 1));
//			return true;
//		} else if (event.getX() < 0) {
//			updateSelection(Math.max(currentSelection - 1, 0));
//			return true;
//		}
		if (onTrackballListener != null) {
			return onTrackballListener.call(event);
		}
		return super.onTrackballEvent(event);
	}

	@Override
	public boolean onTouchEvent(@NotNull final MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				slideStartX = (int) event.getX();
				slideStartIndex = currentSelection;
				sliding = false;
				break;
			case MotionEvent.ACTION_MOVE:
				final int unit = getWidth() / (values.length + 2);
				final int delta = (int) (event.getX() - slideStartX);
				if (!sliding && (delta > scaledTouchSlop || delta < -scaledTouchSlop))
					sliding = true;
				final int selDelta = delta / unit;
				int newSelection = slideStartIndex + selDelta;
				if (newSelection < 0) newSelection = 0;
				if (newSelection >= values.length) newSelection = values.length - 1;
				if (sliding) {
					if (dlg == null) {
						final AlertDialog.Builder b = new AlertDialog.Builder(getContext());
						b.setTitle(prefix);
						b.setMessage(suffixedValues[newSelection]);
						dlg = b.create();
						dlg.show();
						final Window dwin = dlg.getWindow();
						assert dwin != null;
						dwin.setGravity(Gravity.TOP);
						final WindowManager.LayoutParams lp = dwin.getAttributes();
						lp.dimAmount = 0f;
						lp.windowAnimations = 0;
						dwin.setAttributes(lp);
					} else {
						dlg.setMessage(suffixedValues[newSelection]);
					}
				}
				updateSelection(newSelection);
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

	private void updateSelection(final int newSelection) {
		if (newSelection != currentSelection) {
			setSelection(newSelection);
			notifyOnChangeListener();
		}
	}
}
