package com.kos.ktodo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

public class MyListView extends ListView {
	private static final String TAG = "MyListView";
	private static final int MSG_LONG_PRESS = 1;

	private static final long tapTime = ViewConfiguration.getTapTimeout();
	private static final long longPressTime = ViewConfiguration.getLongPressTimeout();

	private ImageView mDragView;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWindowParams;
	private int mDragPos;      // which item is being dragged
	private int mDragItemY;
	private int mDragStartX, mDragStartY;
	private int mDragPointY, mDragPointX;    // at what offset inside the item did the user grab it
	private int mCoordOffsetY, mCoordOffsetX;  // the difference between screen coordinates and coordinates in this view
//	private Rect mTempRect = new Rect();
	private Bitmap mDragBitmap;
	private int scaledTouchSlopSquared;
	private boolean isLongPress = false;
	private boolean hasNotMoved = true;

	private Handler msgHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (msg.what == MSG_LONG_PRESS) {
				handleLongPress();
			} else throw new RuntimeException("unknown msg: " + msg);
		}
	};

	public MyListView(final Context context) {
		super(context);
		final ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		scaledTouchSlopSquared = viewConfiguration.getScaledTouchSlop()
		                         * viewConfiguration.getScaledTouchSlop();
	}

	public MyListView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		scaledTouchSlopSquared = viewConfiguration.getScaledTouchSlop()
		                         * viewConfiguration.getScaledTouchSlop();
	}

	@Override
	public boolean onInterceptTouchEvent(final MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			final int x = (int) ev.getX();
			final int y = (int) ev.getY();
			mDragStartX = x;
			mDragStartY = y;
			final int itemnum = pointToPosition(x, y);
			if (itemnum != AdapterView.INVALID_POSITION) {
				final View item = getChildAt(itemnum - getFirstVisiblePosition());
				mDragPointY = y - item.getTop();
				mDragPointX = x - item.getLeft();
				mCoordOffsetY = ((int) ev.getRawY()) - y;
				mCoordOffsetX = ((int) ev.getRawX()) - x;

				final int[] xy = new int[2];
				item.getLocationOnScreen(xy);
				mDragItemY = xy[1] - mCoordOffsetY / 2;
				item.setDrawingCacheEnabled(true);
				// Create a copy of the drawing cache so that it does not get recycled
				// by the framework when the list tries to clean up memory
				final Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
				startDragging(bitmap, x, y);
				mDragPos = itemnum;
			}
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		boolean processed = false;
		final int action = ev.getAction();
		switch (action) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
//				final Rect r = mTempRect;
//				mDragView.getDrawingRect(r);
				stopDragging();
				processed = true;
				break;

			case MotionEvent.ACTION_DOWN:
				// by moving or by canceling the gesture.
				msgHandler.removeMessages(MSG_LONG_PRESS);
				msgHandler.sendEmptyMessageAtTime(MSG_LONG_PRESS,
						ev.getDownTime() + tapTime + longPressTime);
				processed = true;
				break;

			case MotionEvent.ACTION_MOVE:
				if (isLongPress)
					break;
				final int x = (int) ev.getX();
				final int y = (int) ev.getY();
				getChildAt(mDragPos - getFirstVisiblePosition()).setVisibility(View.INVISIBLE);
				dragView(x, y);

				if (hasNotMoved) {
					final int deltaXFromDown = (int) (x - mDragStartX);
					final int deltaYFromDown = (int) (y - mDragStartY);
					final int distance = (deltaXFromDown * deltaXFromDown)
					                     + (deltaYFromDown * deltaYFromDown);
					if (distance > scaledTouchSlopSquared) {
						hasNotMoved = false;
						msgHandler.removeMessages(MSG_LONG_PRESS);
					}
				}
//				if (action == MotionEvent.ACTION_MOVE) {
//				final View first = getChildAt(mDragPos - getFirstVisiblePosition());
//				first.setVisibility(View.INVISIBLE);
//				}
				processed = true;
				break;
		}
		if (processed) {
//			Log.i(TAG, "ev: " + ev);
			return true;
		}
		return super.onTouchEvent(ev);
	}

	private void startDragging(final Bitmap bm, final int x, final int y) {
		stopDragging();

		mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP;
		//mWindowParams.x = 0;
		mWindowParams.x = x - mDragPointX + mCoordOffsetX;
		//mWindowParams.y = y - mDragPointY + mCoordOffsetY;
		mWindowParams.y = mDragItemY;

		mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
		                      | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
		                      | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
		                      | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		mWindowParams.format = PixelFormat.TRANSLUCENT;
		mWindowParams.windowAnimations = 0;

		final Context mContext = getContext();
		final ImageView v = new ImageView(mContext);
		final int backGroundColor = mContext.getResources().getColor(R.color.dragndrop_background);
		v.setBackgroundColor(backGroundColor);
		v.setImageBitmap(bm);
		v.setVisibility(View.VISIBLE);
		mDragBitmap = bm;

		mWindowManager = (WindowManager) mContext.getSystemService("window");
		mWindowManager.addView(v, mWindowParams);
		mDragView = v;
	}

	private void dragView(final int x, final int y) {
		mWindowParams.x = x - mDragPointX + mCoordOffsetX;
		if (mWindowParams.x < 0)
			mWindowParams.x = 0;
		mWindowParams.y = mDragItemY;
		mWindowManager.updateViewLayout(mDragView, mWindowParams);
	}

	private void stopDragging() {
		if (mDragView != null) {
			final Context mContext = getContext();
			final WindowManager wm = (WindowManager) mContext.getSystemService("window");
			wm.removeView(mDragView);
			mDragView.setImageDrawable(null);
			mDragView = null;
		}
		if (mDragBitmap != null) {
			mDragBitmap.recycle();
			mDragBitmap = null;
		}
		hasNotMoved = true;
		isLongPress = false;
		msgHandler.removeMessages(MSG_LONG_PRESS);
		unExpandViews(true);
	}

	private void unExpandViews(final boolean deletion) {
		for (int i = 0; ; i++) {
			View v = getChildAt(i);
			if (v == null) {
//				Log.i(TAG, "v=null for " + i);
				if (deletion) {
					// HACK force update of mItemCount
					final int position = getFirstVisiblePosition();
					final int y = getChildAt(0).getTop();
					setAdapter(getAdapter());
					setSelectionFromTop(position, y);
					// end hack
				}
				layoutChildren(); // force children to be recreated where needed
				v = getChildAt(i);
				if (v == null) {
					break;
				}
			}
			v.setVisibility(View.VISIBLE);
		}
	}

	private void handleLongPress() {
		isLongPress = true;
		stopDragging();
		getChildAt(mDragPos - getFirstVisiblePosition()).performLongClick();
	}
}
