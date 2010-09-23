package com.kos.ktodo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;

/**
 * Special list view and allows to drag items to the right and throw them off screen for removing
 * or drag them to the left for editing (if associated with a <code>SlidingView</code>).
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class MyListView extends ListView {
	private static final String TAG = "MyListView";

	private int maxThrowVelocity;
	private int vibrateOnTearOff;
	private ImageView dragView;
	private WindowManager windowManager;
	private WindowManager.LayoutParams windowParams;
	private Bitmap dragBitmap;
	private int dragItemNum;      // which item is being dragged
	private int lastDragX;
	private int dragStartX, dragStartY;
	private int dragPointX;    // at what offset inside the item did the user grab it
	private int /*coordOffsetY,*/ coordOffsetX;  // the difference between screen coordinates and coordinates in this view
	private final int scaledTouchSlop;
	private boolean scrolling;
	private final RawVelocityTracker dragVelocityTracker = new RawVelocityTracker();
	private MyScroller flightScroller;
	private State state = State.NORMAL;
	private DeleteItemListener deleteItemListener;
	private int dragItemY = -1;

	//slide left stuff
	private SlidingView slideLeftView;
	private SlideLeftListener slideLeftListener;

	private final ArrayList<MotionEvent> intercepted = new ArrayList<MotionEvent>();
	private boolean replaying;

	private Runnable itemFlinger = new Runnable() {
		public void run() {
			if (state != State.ITEM_FLYING) return;
			if (flightScroller.isFinished()) {
				final int distToEdge = getWidth() - flightScroller.getCurrX();
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
		}
	}

	public interface DeleteItemListener {
		void deleteItem(final long id);
	}

	public interface SlideLeftListener {
		void slideLeftStarted(final long id);

		void onSlideBack();
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
				dragItemY = -1;
				dragView();
				scrolling = scrollState != OnScrollListener.SCROLL_STATE_IDLE;
			}

			public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
				dragItemY = -1;
				dragView();
			}
		});

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MyListView);
		maxThrowVelocity = ta.getInt(R.styleable.MyListView_maxThrowVelocity, 1500);
		vibrateOnTearOff = ta.getInt(R.styleable.MyListView_vibrateOnTearOff, 20);
		ta.recycle();
	}

	public void setDeleteItemListener(final DeleteItemListener deleteItemListener) {
		this.deleteItemListener = deleteItemListener;
	}

	public void setSlideLeftInfo(final SlidingView sv, final SlideLeftListener listener) {
		slideLeftView = sv;
		slideLeftListener = listener;
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
				if (newState == State.DRAGGING_VIEW_LEFT) break;
				throw impossibleTransition;
			case ITEM_FLYING:
				if (newState == State.NORMAL) break;
				if (newState == State.DRAGGING_ITEM) break;
				throw impossibleTransition;
			case DRAGGING_VIEW_LEFT:
				if (newState == State.NORMAL) break;
				if (newState == State.PRESSED_ON_ITEM) break; //this is only a temporary change, either DRAGGING or NORMAL follow immediately
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
				dragItemY = -1;
				scrolling = false;
				dragVelocityTracker.clear();
				break;
			case DRAGGING_ITEM:
				superCancel();
				if (prevState == State.ITEM_FLYING) {
					flightScroller.forceFinished(true);
					dragVelocityTracker.clear();
				} else {
					if (!startDragging())
						state = prevState;
					else vibrateOnTearOff();
				}
				break;
			case DRAGGING_VIEW_LEFT:
				superCancel();
				dragVelocityTracker.clear();
				if (prevState == State.DRAGGING_ITEM)
					stopDragging();
		}
	}

	private void vibrateOnTearOff() {
		if (vibrateOnTearOff > 0) {
			final Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
			if (v != null)
				v.vibrate(vibrateOnTearOff);
		}
	}

	public boolean handleBack() {
		if (state != State.NORMAL) return false;
		if (slideLeftView == null) return false;
		if (slideLeftView.getScrollX() != slideLeftView.getWidth()) return false;
		slideLeftView.switchLeft();
		slideLeftView.setSlideListener(new SlidingView.SlideListener() {
			public void slidingFinished() {
				slideLeftListener.onSlideBack();
				slideLeftView.setSlideListener(null);
			}
		});
		return true;
	}

	@Override
	public boolean onTrackballEvent(final MotionEvent event) {
		if (event.getX() > 0 && slideLeftListener != null) {
			slideLeftListener.slideLeftStarted(getSelectedItemId());
			slideLeftView.switchRight();
			return true;
		} else
			return super.onTrackballEvent(event);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		if (replaying || deleteItemListener == null) return super.onTouchEvent(ev);
		boolean processed = false;
		switch (ev.getAction()) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				processed = processUpEvent();
				break;

			case MotionEvent.ACTION_DOWN:
				if (scrolling) break;
				processed = processDownEvent(ev);
				break;

			case MotionEvent.ACTION_MOVE:
				if (scrolling) break;
				processed = processMoveEvent(ev);
				break;
		}
		if (processed) {
			if (!(state == State.DRAGGING_ITEM))
				intercepted.add(ev);
			return true;
		} else if (intercepted.size() > 0) {
			replaying = true;
			for (final MotionEvent event : intercepted) {
				try {
					super.dispatchTouchEvent(event);
				} catch (ArrayIndexOutOfBoundsException e) {
					//failed attempt to replay events, abort
				}
			}
			replaying = false;
			intercepted.clear();
		}
		return super.onTouchEvent(ev);
	}

	private boolean processUpEvent() {
		boolean processed = false;
		if (state == State.DRAGGING_ITEM) {
			final int lastLeft = lastDragX - dragPointX + coordOffsetX;
			if (lastLeft <= scaledTouchSlop) {
				setState(State.NORMAL);
				return processed;
			}
			dragVelocityTracker.computeCurrentVelocity(1000, maxThrowVelocity);
			final float xVelocity = dragVelocityTracker.getXVelocity();
//					Log.i(TAG, "x velocity: " + xVelocity);
			if (flightScroller == null) flightScroller = new MyScroller(getContext());
			flightScroller.fling(lastLeft, 0, xVelocity == 0 ? -1 : (int) xVelocity, 0, 0, getWidth(), 0, 0, xVelocity < 0);
			setState(State.ITEM_FLYING);
			post(itemFlinger);
			processed = true;
		} else if (state == State.PRESSED_ON_ITEM)
			setState(State.NORMAL);
		else if (state == State.DRAGGING_VIEW_LEFT) {
			finishSlideLeft();
			processed = true;
		}
		scrolling = false;
		return processed;
	}

	private boolean processDownEvent(final MotionEvent ev) {
		boolean processed = false;
		if (state == State.ITEM_FLYING) {
			final int x = (int) ev.getX();
			final int y = (int) ev.getY();
			final int itemnum = pointToPositionWithInvisible(x, y);
			if (itemnum == AdapterView.INVALID_POSITION) return processed;
			final int lastLeft = lastDragX - dragPointX + coordOffsetX;
			if (lastLeft <= x && itemnum == dragItemNum && itemInBounds(dragItemNum)) {
				dragPointX = x - lastLeft;
				setState(State.DRAGGING_ITEM);
				processed = true;
			}
		} else startPreDragging(ev);
		return processed;
	}

	private boolean processMoveEvent(final MotionEvent ev) {
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		final int rawX = (int) ev.getRawX();
		final int off;
		boolean processed = false;

		switch (state) {
			case DRAGGING_ITEM:
				dragVelocityTracker.addMovement(ev, false);
				off = x - dragPointX + coordOffsetX;
				if (off < 0 && slideLeftListener != null) {
					setState(State.DRAGGING_VIEW_LEFT);
					slideLeftListener.slideLeftStarted(getItemIdAtPosition(dragItemNum));
				} else {
					dragView(x);
				}
				processed = true;
				break;
			case DRAGGING_VIEW_LEFT:
				final int correctedX = rawX - coordOffsetX; //don't know how to compute it for real, using x only. Scrolling affects it somehow
				dragVelocityTracker.addMovement(ev, true);
				off = dragPointX - correctedX - coordOffsetX;
				if (off < 0) {
					slideLeft(0);
					if (startPreDragging(ev))
						setState(State.DRAGGING_ITEM);
					else
						setState(State.NORMAL);
				} else {
					slideLeft(off);
				}
				processed = true;
				break;
			default:
				final int deltaXFromDown = x - dragStartX;
				final int deltaYFromDown = y - dragStartY;
				if (deltaYFromDown >= scaledTouchSlop)
					scrolling = true;
				final int itemnum = pointToPositionWithInvisible(x, y);
				if (!scrolling) {
					dragVelocityTracker.addMovement(ev, false);
					if (state == State.PRESSED_ON_ITEM && itemInBounds(dragItemNum) && deltaXFromDown > scaledTouchSlop) {
						setState(State.DRAGGING_ITEM);
						processed = true;
					} else if (state == State.PRESSED_ON_ITEM &&
					           deltaXFromDown < -scaledTouchSlop &&
					           slideLeftListener != null && itemnum != AdapterView.INVALID_POSITION) {
						setState(State.DRAGGING_VIEW_LEFT);
						dragPointX = x;
						slideLeftListener.slideLeftStarted(getItemIdAtPosition(itemnum));
						processed = true;
					}
				}
		}
		return processed;
	}

	private void finishSlideLeft() {
		dragVelocityTracker.computeCurrentVelocity(1000, maxThrowVelocity);
		final float xVelocity = dragVelocityTracker.getXVelocity();
		final boolean goRight;
//					Log.i(TAG, "xVelocity=" + xVelocity);
		if (xVelocity > -50 && xVelocity < 50)
			goRight = slideLeftView.getScrollX() > slideLeftView.getWidth() / 2;
		else
			goRight = xVelocity < 0;
		if (goRight)
			slideLeftView.switchRight();
		else
			slideLeftView.switchLeft();
		setState(State.NORMAL);
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
		if (state == State.ITEM_FLYING) return false;
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		dragStartX = x;
		dragStartY = y;
		final int itemnum = pointToPosition(x, y);
		if (itemnum == AdapterView.INVALID_POSITION) return false;
//		if (!itemInBounds(itemnum)) return false;
		dragItemNum = itemnum;
		final View item = getChildAt(itemnum - getFirstVisiblePosition());
		dragPointX = x - item.getLeft();
		//coordOffsetY = ((int) ev.getRawY()) - y;
		coordOffsetX = ((int) ev.getRawX()) - x;
		setState(State.PRESSED_ON_ITEM);
		return true;
	}

	private boolean startDragging() {
		final View item = getDragItem();
		if (item == null) return false;
		if (!itemInBounds(dragItemNum)) return false;
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

		if (dragItemY != -1)
			return dragItemY;

		//now some utterly ugly code.. alas view.getLocationInWindow gives absolute bullshit
		int t = 0;
		for (View v = view; v != null;) {
			boolean add = true;
			//there's some frame layout in my hierarchy that has getTop=50.. dunno what it is, there's
			//nothing like it in my layout definition.. it still spoils the offset on my nexus1
			if (v instanceof FrameLayout) {
				final FrameLayout frameLayout = (FrameLayout) v;
				if (frameLayout.getForeground() != null)
					add = false;

			}
			if (add)
				t += (v.getTop() + v.getPaddingTop());
			View p = null;
			final ViewParent viewParent = v.getParent();
			if (viewParent instanceof View) {
				p = (View) viewParent;
			}
			//Log.i(TAG, "v=" + v + ", vis: " + v.getVisibility() + ", top=" + v.getTop() + ", pt=" + v.getPaddingTop());
			v = p;
		}
		//Log.i(TAG, "t=" + t);
		return t;
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
				dragView.setVisibility(View.VISIBLE);
				if (item != null) item.setVisibility(View.INVISIBLE);
				windowParams.y = getDragItemY();
				windowManager.updateViewLayout(dragView, windowParams);
				lastDragX = x;
			}
		}
	}

	private void slideLeft(final int off) {
		//final int off = dragPointX - x - coordOffsetX;
//		Log.i(TAG, "off=" + off + " (" + dragPointX + " - " + x + " - " + coordOffsetX + ")");
		slideLeftView.scrollTo(off, 0);
		//slideLeftView.invalidate();
	}

	private boolean itemInBounds(final int itemPosition) {
		final View item = getChildAt(itemPosition - getFirstVisiblePosition());
		if (item == null) return false;
		return item.getTop() >= 0 && item.getBottom() <= getHeight();
	}

	private void stopDragging() {
		intercepted.clear();
		unExpandViews(true);
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
