package com.kos.ktodo;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class ToggleButton extends Button {
	private boolean pushed;

	public ToggleButton(final Context context) {
		super(context);
	}

	public ToggleButton(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public ToggleButton(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean isPressed() {
		if (!pushed)
			return super.isPressed();
		return true;
	}

	public void setPushed(final boolean pushed) {
		this.pushed = pushed;
		setClickable(!pushed);
		super.setPressed(pushed);
	}

	@Override
	public void setPressed(final boolean pressed) {
		if (pushed)
			super.setPressed(true);
		else
			super.setPressed(pressed);
	}
}
