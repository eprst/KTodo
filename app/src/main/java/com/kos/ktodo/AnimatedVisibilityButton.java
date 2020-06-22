package com.kos.ktodo;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/**
 * A button that plays animations when shown/hidden.
 */
public class AnimatedVisibilityButton extends androidx.appcompat.widget.AppCompatButton {
	private final Handler h = new Handler();
	private final Animation inAnimation;
	private final Animation outAnimation;

	private final Runnable hider = new Runnable() {
		public void run() {
			setVisibility(View.GONE);
		}
	};

	public AnimatedVisibilityButton(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AnimatedVisibilityButton);
		final int inAnimId = ta.getResourceId(R.styleable.AnimatedVisibilityButton_inAnim, R.anim.fade_in);
		final int outAnimId = ta.getResourceId(R.styleable.AnimatedVisibilityButton_inAnim, R.anim.fade_out);
		inAnimation = AnimationUtils.loadAnimation(context, inAnimId);
		outAnimation = AnimationUtils.loadAnimation(context, outAnimId);
		ta.recycle();
	}

	@Override
	public void setVisibility(final int visibility) {
		if (visibility != getVisibility()) {
			switch (visibility) {
				case View.VISIBLE:
					startAnimation(inAnimation);
					break;
				case View.GONE:
				case View.INVISIBLE:
					startAnimation(outAnimation);
					break;
			}
			super.setVisibility(visibility);
		}
	}

	public void hideAfter(final long millis) {
		h.removeCallbacks(hider);
		h.postDelayed(hider, millis);
	}

	public void hideNoAnimation() {
		super.setVisibility(View.GONE);
	}
}
