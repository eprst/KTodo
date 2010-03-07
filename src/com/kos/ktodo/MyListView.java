package com.kos.ktodo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;

public class MyListView extends ListView {
	private static final String TAG = "MyListView";
	private static final int MSG_LONG_PRESS = 1;

	private static final long tapTime = ViewConfiguration.getTapTimeout();
	private static final long longPressTime = ViewConfiguration.getLongPressTimeout();

	private final int[] xy = new int[2];

	private ImageView dragView;
	private WindowManager windowManager;
	private WindowManager.LayoutParams windowParams;
	private Bitmap dragBitmap;
	private int dragItemNum;      // which item is being dragged
	private int lastDragX;
	private int dragStartX, dragStartY;
	private int dragPointX;    // at what offset inside the item did the user grab it
	private int coordOffsetY, coordOffsetX;  // the difference between screen coordinates and coordinates in this view
	private int scaledTouchSlop;
	private int scaledTouchSlopSquared;
	private boolean isLongPress = false;
	private boolean hasMoved = false;

	private final ArrayList<MotionEvent> intercepted = new ArrayList<MotionEvent>();
	private boolean replaying;

	private Handler msgHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (msg.what == MSG_LONG_PRESS) {
				handleLongPress();
			} else throw new RuntimeException("unknown msg: " + msg);
		}
	};

	public MyListView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		scaledTouchSlop = viewConfiguration.getScaledTouchSlop();
		scaledTouchSlopSquared = scaledTouchSlop * scaledTouchSlop;
		setOnScrollListener(new OnScrollListener() {
			public void onScrollStateChanged(final AbsListView view, final int scrollState) {
				dragView();
			}

			public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
				dragView();
			}
		});
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		if (replaying) return super.onTouchEvent(ev);
		boolean processed = false;
		final int action = ev.getAction();
		switch (action) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (isDragging()) {
					stopDragging();
					processed = true;
				}
				if (isPreDragging())
					stopPreDragging();
				break;

			case MotionEvent.ACTION_DOWN: //todo: highlight the item
				startPreDragging(ev);
				//intercepted.add(ev);
				msgHandler.removeMessages(MSG_LONG_PRESS);
				msgHandler.sendEmptyMessageAtTime(MSG_LONG_PRESS,
						ev.getDownTime() + tapTime + longPressTime);
				isLongPress = false;
				processed = true;
				break;

			case MotionEvent.ACTION_MOVE:
				if (isLongPress)
					break;
				final int x = (int) ev.getX();
				final int y = (int) ev.getY();

//				Log.i(TAG, "move: hasMoved=" + hasMoved + ", isDragging:" + isDragging());
				if (!hasMoved) {
					final int deltaXFromDown = x - dragStartX;
					final int deltaYFromDown = y - dragStartY;
					final int distance = (deltaXFromDown * deltaXFromDown)
					                     + (deltaYFromDown * deltaYFromDown);
					if (distance > scaledTouchSlopSquared)
						msgHandler.removeMessages(MSG_LONG_PRESS);
					if (deltaXFromDown > scaledTouchSlop && isPreDragging()) {
						hasMoved = true;
						startDragging();
						processed = true;
					}
				} else if (isDragging()) {
					dragView(x);
					processed = true;
				}
				break;
		}
		if (processed) {
//			Log.i(TAG, "[" + intercepted.size() + "] ev: " + ev);
			if (!isDragging())
				intercepted.add(ev);
			return true;
		} else if (intercepted.size() > 0) {
			replaying = true;
			for (final MotionEvent event : intercepted) {
				super.dispatchTouchEvent(event);
			}
			replaying = false;
			//stopPreDragging();
			intercepted.clear();
		}
		return super.onTouchEvent(ev);
	}

	private boolean isPreDragging() {
		return dragItemNum != -1;
	}

	private boolean isDragging() {
		return isPreDragging() && dragView != null;
	}

	private boolean startPreDragging(final MotionEvent ev) {
		stopDragging();

		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		final int itemnum = pointToPosition(x, y);
		if (itemnum == AdapterView.INVALID_POSITION) return false;
		dragItemNum = itemnum;
		dragStartX = x;
		dragStartY = y;
		final View item = getChildAt(itemnum - getFirstVisiblePosition());
		//mDragPointY = y - item.getTop();
		dragPointX = x - item.getLeft();
		coordOffsetY = ((int) ev.getRawY()) - y;
		coordOffsetX = ((int) ev.getRawX()) - x;

		return true;
	}

	private boolean startDragging() {
		if (!isPreDragging()) return false;

		final View item = getChildAt(dragItemNum - getFirstVisiblePosition());
		if (item == null) return false;
		item.setDrawingCacheEnabled(true);
		final Bitmap bm = Bitmap.createBitmap(item.getDrawingCache());
		windowParams = new WindowManager.LayoutParams();
		windowParams.gravity = Gravity.TOP;
		//mWindowParams.x = 0;
		windowParams.x = dragStartX - dragPointX + coordOffsetX;
		//mWindowParams.y = y - mDragPointY + mCoordOffsetY;
		windowParams.y = getDragItemY();

		windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
		                     | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
		                     | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
		                     | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		windowParams.format = PixelFormat.TRANSLUCENT;
		windowParams.windowAnimations = 0;

		final Context mContext = getContext();
		final ImageView v = new ImageView(mContext);
		final int backGroundColor = mContext.getResources().getColor(R.color.dragndrop_background);
		v.setBackgroundColor(backGroundColor);
		v.setImageBitmap(bm);
		v.setVisibility(View.VISIBLE);
		item.setVisibility(View.INVISIBLE);
		dragBitmap = bm;

		windowManager = (WindowManager) mContext.getSystemService("window");
		windowManager.addView(v, windowParams);
		dragView = v;
		return true;
	}

	private int getDragItemY() {
		final View view = getChildAt(dragItemNum - getFirstVisiblePosition());
		if (view == null)
			return -1;
		view.getLocationOnScreen(xy);
		return xy[1] - coordOffsetY / 2;
	}

	private void dragView() {
		dragView(lastDragX);
	}

	private void dragView(final int x) {
		if (isDragging()) {
			windowParams.x = x - dragPointX + coordOffsetX;
			if (windowParams.x < 0)
				windowParams.x = 0;
			final int dragItemY = getDragItemY();
			if (dragItemY < getTop() || dragItemY + dragView.getHeight() > getBottom())
				stopDragging(); //we're out of screen
			else {
				windowParams.y = dragItemY;
				windowManager.updateViewLayout(dragView, windowParams);
				lastDragX = x;
			}
		}
	}

	private void stopPreDragging() {
		dragItemNum = -1;
	}

	private void stopDragging() {
		stopPreDragging();
		intercepted.clear();
		if (dragView != null) {
			final Context mContext = getContext();
			final WindowManager wm = (WindowManager) mContext.getSystemService("window");
			wm.removeView(dragView);
			dragView.setImageDrawable(null);
			dragView = null;
		}
		if (dragBitmap != null) {
			dragBitmap.recycle();
			dragBitmap = null;
		}
		hasMoved = false;
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
		if (dragItemNum == -1) return;
		final View view = getChildAt(dragItemNum - getFirstVisiblePosition());
		if (view == null) return;
		isLongPress = true;
		view.performLongClick();
	}
}
