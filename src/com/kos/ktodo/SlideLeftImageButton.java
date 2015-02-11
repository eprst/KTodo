package com.kos.ktodo;


import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;

import org.jetbrains.annotations.NotNull;


/**
 * A button that can be used slide the view left. Should be a View mixin actually..
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com" title="">Konstantin Sobolev</a>
 */
public class SlideLeftImageButton extends ImageButton {
	private static final String TAG = "SlideLeftButton";
	private final ArrayList<MotionEvent> intercepted = new ArrayList<>();
	private final RawVelocityTracker dragVelocityTracker = new RawVelocityTracker();
	private final Handler h = new Handler();
	private final Runnable slideExpiredHandler;
	private final int maxThrowVelocity;
	private final long waitForSlide;
	boolean replaying = false;

	private SlidingView slidingView;
	private SlideLeftListener slideLeftListener;
	private Long itemID; //ID of the added item

	private boolean sliding;
	private int dragPointX;

	public SlideLeftImageButton(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		final TypedArray ta_slide = context.obtainStyledAttributes(attrs, R.styleable.SlideLeftButton);
		final TypedArray ta_throw = context.obtainStyledAttributes(attrs, R.styleable.Throwable);
		maxThrowVelocity = ta_throw.getInt(R.styleable.Throwable_maxThrowVelocity, 1500);
		waitForSlide = ta_slide.getInt(R.styleable.SlideLeftButton_waitForSlide, 300);
		ta_throw.recycle();
		ta_slide.recycle();

		slideExpiredHandler = new Runnable() {
			public void run() {
				synchronized (SlideLeftImageButton.this) {
					if (itemID == null) {
						sliding = false;
						replayIntercepted();
					}
				}
			}
		};
	}

	public synchronized void setItemID(final long itemID) {
		this.itemID = itemID;
	}

	public synchronized void setSlideLeftInfo(final SlidingView slidingView, final SlideLeftListener slideLeftListener) {
		this.slidingView = slidingView;
		this.slideLeftListener = slideLeftListener;
	}

	@Override
	public synchronized boolean onTouchEvent(@NotNull final MotionEvent ev) {
		if (replaying || slidingView == null)
			return super.onTouchEvent(ev);

		boolean processed = false;
		switch (ev.getAction()) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				processed = processUpEvent();
				break;

			case MotionEvent.ACTION_DOWN:
				processed = processDownEvent(ev);
				break;

			case MotionEvent.ACTION_MOVE:
				processed = processMoveEvent(ev);
				break;
		}
		if (processed) {
			intercepted.add(MotionEvent.obtain(ev));
			return true;
		} else if (intercepted.size() > 0) {
			return !replayIntercepted() || super.onTouchEvent(ev);
		} else {
			try {
				return super.onTouchEvent(ev);
			} catch (final Exception e) {
				log("error forwarding: " + ev, e);
				return true;
			}
		}
	}

	private boolean replayIntercepted() {
		replaying = true;
		try {
			for (final MotionEvent event : intercepted) {
				super.dispatchTouchEvent(event);
			}
			return true;
		} catch (final Exception e) {
			log("oops on replaying", e);
			//failed attempt to replay events, abort
		} finally {
			replaying = false;
			intercepted.clear();
		}
		return false;
	}

	private boolean processMoveEvent(final MotionEvent ev) {
		final int rawX = (int) ev.getRawX();

		if (!sliding)
			return false;

		final int off = dragPointX - rawX;

		if (off < 0) {
			slidingView.scrollTo(0, 0);
			return true;
		}

		if (itemID == null) {
			performClick(); //this call can change itemID!
			if (itemID == null) {
				sliding = false;
				return false;
			}
			slideLeftListener.slideLeftStarted(itemID);
		}

		dragVelocityTracker.addMovement(ev, true);
		slidingView.scrollTo(off, 0);
		return true;
	}

	private boolean processDownEvent(final MotionEvent ev) {
		if (sliding || !slidingView.isOnLeft())
			return false;
		dragPointX = (int) ev.getRawX();
		sliding = true;
		dragVelocityTracker.clear();
		itemID = null;
		sliding = true;

		h.removeCallbacks(slideExpiredHandler);
		h.postDelayed(slideExpiredHandler, waitForSlide);
		return true;
	}

	private boolean processUpEvent() {
		if (!sliding) return false;
		h.removeCallbacks(slideExpiredHandler);
		sliding = false;
		if (itemID == null)
			return false;

		dragVelocityTracker.computeCurrentVelocity(1000, maxThrowVelocity);
		final float xVelocity = dragVelocityTracker.getXVelocity();
//		Log.i("ktodo", "xvel: " + xVelocity);
		final boolean goRight;
		if (xVelocity > -200 && xVelocity < 200)
			goRight = slidingView.getScrollX() > slidingView.getWidth() / 2;
		else
			goRight = xVelocity < 0;
		if (goRight)
			slidingView.switchRight();
		else {
			slidingView.switchLeft();
			slideLeftListener.onSlideBack();
		}
		itemID = null;
		sliding = false;
		return true;
	}

//	private void log(final String msg) {
//		Log.i(TAG, msg);
//	}

	@SuppressWarnings({"UnusedDeclaration"})
	private void log(final String msg, final Throwable t) {
//		Log.i(TAG, msg, t);
		Log.i(TAG, msg + ": " + t);
	}
}
