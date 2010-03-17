package com.kos.ktodo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;

public class MyListView extends ListView {
	private static final String TAG = "MyListView";

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
	private boolean hasMoved = false;
	private boolean scrolling;
	private VelocityTracker dragVelocityTracker;
	private MyScroller flightScroller;
	private State state = State.NORMAL;
	private DeleteItemListener deleteItemListener;

	private final ArrayList<MotionEvent> intercepted = new ArrayList<MotionEvent>();
	private boolean replaying;

//	private Handler msgHandler = new Handler() {
//		@Override
//		public void handleMessage(final Message msg) {
	//			if (msg.what == MSG_LONG_PRESS) {
	//				handleLongPress();
//			} else throw new RuntimeException("unknown msg: " + msg);
//		}
//	};
	private Runnable itemFlinger = new Runnable() {
		public void run() {
			if (state != State.ITEM_FLYING) return;
			if (flightScroller.isFinished()) {
				final int distToEdge = getWidth() - flightScroller.getCurrX();
//				Log.i(TAG, "distToEdge: " + distToEdge);
				if (distToEdge == 0)
					deleteFlyingAndStop();
				else { //slide back
					final int lastLeft = lastDragX - dragPointX + coordOffsetX;
					flightScroller.fling(lastLeft, 0, -1, 0, 0, getWidth(), 0, 0, true);
					post(itemFlinger);
				}
			} else {
				flightScroller.computeScrollOffset();
				final int currLeft = flightScroller.getCurrX();
				if (currLeft == 0) {
					flightScroller.abortAnimation();
					setState(State.NORMAL);
				} else if (currLeft >= getWidth())
					deleteFlyingAndStop();
				else {
					dragView(currLeft + dragPointX - coordOffsetX);
					invalidate();
					post(itemFlinger);
				}
			}
		}

		private void deleteFlyingAndStop() {
			if (deleteItemListener != null) {
				final long id = getItemIdAtPosition(dragItemNum);
				deleteItemListener.deleteItem(id);
			}
			setState(State.NORMAL);
		}
	};

	@Override
	protected void onDetachedFromWindow() {
		setState(State.NORMAL);
		super.onDetachedFromWindow();
	}

	@Override
	public void onWindowFocusChanged(final boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		if (hasWindowFocus) {
			setState(State.NORMAL);
//			msgHandler.removeMessages(MSG_LONG_PRESS);
		}
	}

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
		setOnScrollListener(new OnScrollListener() {
			public void onScrollStateChanged(final AbsListView view, final int scrollState) {
				dragView();
				scrolling = scrollState == OnScrollListener.SCROLL_STATE_FLING;
			}

			public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
				dragView();
			}
		});
	}

	public void setDeleteItemListener(final DeleteItemListener deleteItemListener) {
		this.deleteItemListener = deleteItemListener;
	}

	private void setState(final State newState) {
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

	private void superCancel() {
		final long now = SystemClock.uptimeMillis();
		super.onTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0));
	}

	private void onStateChange(final State prevState) {
		switch (state) {
			case NORMAL:
				if (prevState == State.DRAGGING_ITEM || prevState == State.ITEM_FLYING)
					stopDragging();
				dragItemNum = -1;
				if (dragVelocityTracker != null)
					dragVelocityTracker.clear();
				break;
			case DRAGGING_ITEM:
				superCancel();
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
					dragVelocityTracker.computeCurrentVelocity(1000, 1500); //todo: make max speed configurable
					final float xVelocity = dragVelocityTracker.getXVelocity();
					Log.i(TAG, "x velocity: " + xVelocity);
					if (flightScroller == null) flightScroller = new MyScroller(getContext());
					flightScroller.fling(lastLeft, 0, xVelocity == 0 ? -1 : (int) xVelocity, 0, 0, getWidth(), 0, 0, xVelocity <= 0);
					setState(State.ITEM_FLYING);
					post(itemFlinger);
					processed = true;
				}
				if (state == State.PRESSED_ON_ITEM)
					setState(State.NORMAL);
				break;

			case MotionEvent.ACTION_DOWN:
				if (scrolling) break;
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
				} else startPreDragging(ev);
				break;

			case MotionEvent.ACTION_MOVE:
				final int x = (int) ev.getX();
				final int y = (int) ev.getY();

				if (state == State.DRAGGING_ITEM)
					dragVelocityTracker.addMovement(ev);

				if (!hasMoved) {
					final int deltaXFromDown = x - dragStartX;
					final int deltaYFromDown = y - dragStartY;
					if (deltaXFromDown > scaledTouchSlop && deltaYFromDown < scaledTouchSlop && state == State.PRESSED_ON_ITEM) {
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
		} else if (intercepted.size() > 0) {
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
		if (!itemInBounds(itemnum)) return false;
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
		final View item = getDragItem();
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
		dragBitmap = bm;

		windowManager = (WindowManager) mContext.getSystemService("window");
		windowManager.addView(v, windowParams);
		dragView = v;

		if (dragVelocityTracker == null)
			dragVelocityTracker = VelocityTracker.obtain();
		setState(State.DRAGGING_ITEM);
		return true;
	}

	private View getDragItem() {
		if (dragItemNum == -1) return null;
		return getChildAt(dragItemNum - getFirstVisiblePosition());
	}

	private int getDragItemY() {
		final View view = getDragItem();
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
			if (windowParams.x > getWidth()) {
				//setState(State.NORMAL);
				return;
			}
			if (windowParams.x < 0)
				windowParams.x = 0;
			if (!itemInBounds(dragItemNum))
				setState(State.NORMAL); //we're out of screen; todo: this could be a flight
			else {
				final View item = getDragItem();
				if (item != null) item.setVisibility(View.INVISIBLE);
				dragView.setVisibility(View.VISIBLE);
				windowParams.y = getDragItemY();
				windowManager.updateViewLayout(dragView, windowParams);
				lastDragX = x;
			}
		}
	}

	private boolean itemInBounds(final int itemPosition) {
		final View item = getChildAt(itemPosition - getFirstVisiblePosition());
		if (item == null) return false;
		return item.getTop() >= 0 && item.getBottom() <= getHeight();
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

	@Override
	public void createContextMenu(final ContextMenu menu) { //I don't know a better way to stop our animation when context menu is being shown...
		setState(State.NORMAL);
		super.createContextMenu(menu);
	}
}
