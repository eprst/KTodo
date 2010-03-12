package com.kos.ktodo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import com.sun.istack.internal.NotNull;

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
	private VelocityTracker dragVelocityTracker;
	private MyScroller flightScroller;
	private State state = State.NORMAL;
	private DeleteItemListener deleteItemListener;

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
	private Runnable itemFlinger = new Runnable() {
		public void run() {
			if (state != State.ITEM_FLYING) return;
			if (flightScroller.isFinished()) {
				final int distToEdge = getWidth() - flightScroller.getCurrX();
//				Log.i(TAG, "distToEdge: " + distToEdge);
				if (distToEdge == 0) {
					if (deleteItemListener != null) {
						final long id = getItemIdAtPosition(dragItemNum);
						deleteItemListener.deleteItem(id);
					}
					setState(State.NORMAL);
				} else { //slide back
					final int lastLeft = lastDragX - dragPointX + coordOffsetX;
					flightScroller.fling(lastLeft, 0, -1, 0, 0, getWidth(), 0, 0, true);
					post(itemFlinger);
				}
			} else {
				flightScroller.computeScrollOffset();
				final int currLeft = flightScroller.getCurrX();
				dragView(currLeft + dragPointX - coordOffsetX);
				invalidate();
				if (currLeft == 0) {
					flightScroller.abortAnimation();
					setState(State.NORMAL);
				} else
					post(itemFlinger);
			}
		}
	};

	public interface DeleteItemListener {
		void deleteItem(final long id);
	}

	private static enum State {
		NORMAL, PRESSED_ON_ITEM, DRAGGING_ITEM, ITEM_FLYING, DRAGGING_VIEW_LEFT
	}

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

	public void setDeleteItemListener(final DeleteItemListener deleteItemListener) {
		this.deleteItemListener = deleteItemListener;
	}

	private void setState(@NotNull final State newState) {
		if (state == newState) return;
		final IllegalStateException impossibleTransition = new IllegalStateException("impossible transition from " + state + " to " + newState);
		switch (state) {
			case NORMAL:
				if (newState == State.PRESSED_ON_ITEM) break;
				throw impossibleTransition;
			case PRESSED_ON_ITEM:
				break;
			case DRAGGING_ITEM:
				if (newState == State.NORMAL) break;
				if (newState == State.ITEM_FLYING) break;
				throw impossibleTransition;
			case ITEM_FLYING:
				if (newState == State.NORMAL) break;
				if (newState == State.DRAGGING_ITEM) break;
				throw impossibleTransition;
			default:
				throw impossibleTransition;
		}
		final State prevState = state;
		state = newState;
		onStateChange(prevState);
	}

	private void onStateChange(@NotNull final State prevState) {
		switch (state) {
			case NORMAL:
				if (prevState == State.DRAGGING_ITEM || prevState == State.ITEM_FLYING)
					stopDragging();
				dragItemNum = -1;
				if (dragVelocityTracker != null)
					dragVelocityTracker.clear();
				break;
			case DRAGGING_ITEM:
				if (prevState == State.ITEM_FLYING) {
					flightScroller.forceFinished(true);
					dragVelocityTracker.clear();
				} else if (!startDragging())
					state = prevState;
				break;
		}
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		if (replaying || deleteItemListener == null) return super.onTouchEvent(ev);
		boolean processed = false;
		final int action = ev.getAction();
		switch (action) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (state == State.DRAGGING_ITEM) {
					final int lastLeft = lastDragX - dragPointX + coordOffsetX;
					if (lastLeft <= scaledTouchSlop) {
						setState(State.NORMAL);
						break;
					}
					dragVelocityTracker.computeCurrentVelocity(1000, Float.MAX_VALUE); //todo: set good max value here
					final float xVelocity = dragVelocityTracker.getXVelocity();
					if (flightScroller == null) flightScroller = new MyScroller(getContext());
					flightScroller.fling(lastLeft, 0, (int) xVelocity, 0, 0, getWidth(), 0, 0, false);
					setState(State.ITEM_FLYING);
					post(itemFlinger);
					processed = true;
				}
				if (state == State.PRESSED_ON_ITEM)
					setState(State.NORMAL);
				break;

			case MotionEvent.ACTION_DOWN:
				if (state == State.ITEM_FLYING) {
					final int x = (int) ev.getX();
					final int y = (int) ev.getY();
					final int itemnum = pointToPositionWithInvisible(x, y);
					if (itemnum == AdapterView.INVALID_POSITION) break;
					final int lastLeft = lastDragX - dragPointX + coordOffsetX;
					if (lastLeft <= x && itemnum == dragItemNum) {
						dragPointX = x - lastLeft;
						setState(State.DRAGGING_ITEM);
						processed = true;
					}
				} else if (startPreDragging(ev)) {
					msgHandler.removeMessages(MSG_LONG_PRESS);
					msgHandler.sendEmptyMessageAtTime(MSG_LONG_PRESS,
							ev.getDownTime() + tapTime + longPressTime);
					isLongPress = false;
					processed = true;
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (isLongPress)
					break;
				final int x = (int) ev.getX();
				final int y = (int) ev.getY();

				if (state == State.DRAGGING_ITEM)
					dragVelocityTracker.addMovement(ev);

				if (!hasMoved) {
					final int deltaXFromDown = x - dragStartX;
					final int deltaYFromDown = y - dragStartY;
					final int distance = (deltaXFromDown * deltaXFromDown)
					                     + (deltaYFromDown * deltaYFromDown);
					if (distance > scaledTouchSlopSquared)
						msgHandler.removeMessages(MSG_LONG_PRESS);
					if (deltaXFromDown > scaledTouchSlop && state == State.PRESSED_ON_ITEM) {
						hasMoved = true;
						setState(State.DRAGGING_ITEM);
						processed = true;
					}
				} else if (state == State.DRAGGING_ITEM) {
					dragView(x);
					processed = true;
				}
				break;
		}
		if (processed) {
			if (!(state == State.DRAGGING_ITEM))
				intercepted.add(ev);
			return true;
		} else if (!isLongPress && intercepted.size() > 0) {
			replaying = true;
			for (final MotionEvent event : intercepted) {
				super.dispatchTouchEvent(event);
			}
			replaying = false;
			intercepted.clear();
		}
		return super.onTouchEvent(ev);
	}

	public int pointToPositionWithInvisible(final int x, final int y) {
		final Rect frame = new Rect();

		final int count = getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			final View child = getChildAt(i);
			child.getHitRect(frame);
			if (frame.contains(x, y)) {
				return getFirstVisiblePosition() + i;
			}
		}
		return INVALID_POSITION;
	}

	private boolean startPreDragging(final MotionEvent ev) {
		if (state == State.ITEM_FLYING) return false; //todo
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		final int itemnum = pointToPosition(x, y);
		if (itemnum == AdapterView.INVALID_POSITION) return false;
		dragItemNum = itemnum;
		dragStartX = x;
		dragStartY = y;
		final View item = getChildAt(itemnum - getFirstVisiblePosition());
		dragPointX = x - item.getLeft();
		coordOffsetY = ((int) ev.getRawY()) - y;
		coordOffsetX = ((int) ev.getRawX()) - x;
		setState(State.PRESSED_ON_ITEM);
		return true;
	}

	private boolean startDragging() {
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
		item.setVisibility(View.INVISIBLE);
		v.setVisibility(View.VISIBLE);
		dragBitmap = bm;

		windowManager = (WindowManager) mContext.getSystemService("window");
		windowManager.addView(v, windowParams);
		dragView = v;

		if (dragVelocityTracker == null)
			dragVelocityTracker = VelocityTracker.obtain();
		setState(State.DRAGGING_ITEM);
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
		if (state == State.DRAGGING_ITEM || state == State.ITEM_FLYING) {
			windowParams.x = x - dragPointX + coordOffsetX;
			if (windowParams.x < 0)
				windowParams.x = 0;
			final int dragItemY = getDragItemY();
			if (dragItemY < getTop() || dragItemY + dragView.getHeight() > getBottom())
				setState(State.NORMAL); //we're out of screen; todo: this could be a flight
			else {
				windowParams.y = dragItemY;
				windowManager.updateViewLayout(dragView, windowParams);
				lastDragX = x;
			}
		}
	}

	private void stopDragging() {
		intercepted.clear();
		if (dragView != null) {
			final Context mContext = getContext();
			final WindowManager wm = (WindowManager) mContext.getSystemService("window");
			dragView.setVisibility(View.INVISIBLE);
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

	private void unExpandViews(final boolean deletion) { //todo: remove unnecessary stuff
		for (int i = 0; ; i++) {
			View v = getChildAt(i);
			if (v == null) {
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
		if (state != State.PRESSED_ON_ITEM) return;
		final View view = getChildAt(dragItemNum - getFirstVisiblePosition());
		if (view == null) return;
		isLongPress = true;
		view.performLongClick();
	}
}
