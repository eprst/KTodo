package com.kos.ktodo;

import android.util.Log;
import android.view.MotionEvent;

/**
 * Helper for tracking the velocity of touch events. Uses raw coordinates.
 */
public final class RawVelocityTracker {
	static final String TAG = "VelocityTracker";
	static final boolean DEBUG = false;
	static final boolean localLOGV = DEBUG;

	static final int NUM_PAST = 10;
	static final int LONGEST_PAST_TIME = 200;

	final float mPastX[] = new float[NUM_PAST];
	final float mPastY[] = new float[NUM_PAST];
	final long mPastTime[] = new long[NUM_PAST];

	float mYVelocity;
	float mXVelocity;

	public RawVelocityTracker() {
	}

	/**
	 * Reset the velocity tracker back to its initial state.
	 */
	public void clear() {
		mPastTime[0] = 0;
	}

	public void addMovement(final MotionEvent ev, final boolean respectHistory) {
		final long time = ev.getEventTime();
		if (respectHistory) {
			final int N = ev.getHistorySize();
			for (int i = 0; i < N; i++) {
				addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i),
						ev.getHistoricalEventTime(i));
			}
		}
		addPoint(ev.getRawX(), ev.getRawY(), time);
	}

	private void addPoint(final float x, final float y, final long time) {
		int drop = -1;
		int i;
		if (localLOGV) Log.v(TAG, "Adding past y=" + y + " time=" + time);
		final long[] pastTime = mPastTime;
		for (i = 0; i < NUM_PAST; i++) {
			if (pastTime[i] == 0) {
				break;
			} else if (pastTime[i] < time - LONGEST_PAST_TIME) {
				if (localLOGV) Log.v(TAG, "Dropping past too old at "
				                          + i + " time=" + pastTime[i]);
				drop = i;
			}
		}
		if (localLOGV) Log.v(TAG, "Add index: " + i);
		if (i == NUM_PAST && drop < 0) {
			drop = 0;
		}
		if (drop == i) drop--;
		final float[] pastX = mPastX;
		final float[] pastY = mPastY;
		if (drop >= 0) {
			if (localLOGV) Log.v(TAG, "Dropping up to #" + drop);
			final int start = drop + 1;
			final int count = NUM_PAST - drop - 1;
			System.arraycopy(pastX, start, pastX, 0, count);
			System.arraycopy(pastY, start, pastY, 0, count);
			System.arraycopy(pastTime, start, pastTime, 0, count);
			i -= (drop + 1);
		}
		pastX[i] = x;
		pastY[i] = y;
		pastTime[i] = time;
		i++;
		if (i < NUM_PAST) {
			pastTime[i] = 0;
		}
	}

	/**
	 * Equivalent to invoking {@link #computeCurrentVelocity(int, float)} with a maximum
	 * velocity of Float.MAX_VALUE.
	 *
	 * @see #computeCurrentVelocity(int, float)
	 */
	public void computeCurrentVelocity(final int units) {
		computeCurrentVelocity(units, Float.MAX_VALUE);
	}

	/**
	 * Compute the current velocity based on the points that have been
	 * collected.  Only call this when you actually want to retrieve velocity
	 * information, as it is relatively expensive.  You can then retrieve
	 * the velocity with {@link #getXVelocity()} and
	 * {@link #getYVelocity()}.
	 *
	 * @param units       The units you would like the velocity in.  A value of 1
	 *                    provides pixels per millisecond, 1000 provides pixels per second, etc.
	 * @param maxVelocity The maximum velocity that can be computed by this method.
	 *                    This value must be declared in the same unit as the units parameter. This value
	 *                    must be positive.
	 */
	public void computeCurrentVelocity(final int units, final float maxVelocity) {
		final float[] pastX = mPastX;
		final float[] pastY = mPastY;
		final long[] pastTime = mPastTime;

		// Kind-of stupid.
		final float oldestX = pastX[0];
		final float oldestY = pastY[0];
		final long oldestTime = pastTime[0];
		float accumX = 0;
		float accumY = 0;
		int N = 0;
		while (N < NUM_PAST) {
			if (pastTime[N] == 0) {
				break;
			}
			N++;
		}
		// Skip the last received event, since it is probably pretty noisy.
		if (N > 3) N--;

		for (int i = 1; i < N; i++) {
			final int dur = (int) (pastTime[i] - oldestTime);
			if (dur == 0) continue;
			float dist = pastX[i] - oldestX;
			float vel = (dist / dur) * units;   // pixels/frame.
			if (accumX == 0) accumX = vel;
			else accumX = (accumX + vel) * .5f;

			dist = pastY[i] - oldestY;
			vel = (dist / dur) * units;   // pixels/frame.
			if (accumY == 0) accumY = vel;
			else accumY = (accumY + vel) * .5f;
		}
		mXVelocity = accumX < 0.0f ? Math.max(accumX, -maxVelocity) : Math.min(accumX, maxVelocity);
		mYVelocity = accumY < 0.0f ? Math.max(accumY, -maxVelocity) : Math.min(accumY, maxVelocity);

		if (localLOGV) Log.v(TAG, "Y velocity=" + mYVelocity + " X velocity="
		                          + mXVelocity + " N=" + N);
	}

	/**
	 * Retrieve the last computed X velocity.  You must first call
	 * {@link #computeCurrentVelocity(int)} before calling this function.
	 *
	 * @return The previously computed X velocity.
	 */
	public float getXVelocity() {
		return mXVelocity;
	}

	/**
	 * Retrieve the last computed Y velocity.  You must first call
	 * {@link #computeCurrentVelocity(int)} before calling this function.
	 *
	 * @return The previously computed Y velocity.
	 */
	public float getYVelocity() {
		return mYVelocity;
	}
}
