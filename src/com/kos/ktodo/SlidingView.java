package com.kos.ktodo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class SlidingView extends FrameLayout {
	private static final String TAG = "SlidingView";
	private SlideListener slideListener;
	final Scroller scroller;

	public SlidingView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		scroller = new Scroller(context, new LinearInterpolator());
	}

	public void setSlideListener(final SlideListener slideListener) {
		this.slideListener = slideListener;
	}

	public void slideLeft() {
		scrollBy(1, 0);
	}

	public void slideRight() {
		scrollBy(-1, 0);
	}

	public void switchRight() {
		scroller.startScroll(getScrollX(), 0, getWidth() - getScrollX(), 0, 200);
		invalidate();
		getChildAt(1).requestFocus();
//		postInvalidate();
	}

	public void switchLeft() {
		scroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 200);
		invalidate();
		getChildAt(0).requestFocus();
//		postInvalidate();
	}

	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			scrollTo(scroller.getCurrX(), 0);
//			postInvalidate();
		} else if (slideListener != null)
			slideListener.slidingFinished();
	}

	@Override
	protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
		super.onLayout(changed, l, t, r, b);
		final int w = r - l;
		getChildAt(1).layout(l + w, t, r + w, b);
	}

	public interface SlideListener {
		void slidingFinished();
	}
}
