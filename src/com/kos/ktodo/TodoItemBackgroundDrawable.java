package com.kos.ktodo;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import org.jetbrains.annotations.Nullable;

/**
 * A drawable that fills backround using two colors, with a changeable proportion between them.
 * Used to draw kind of a progress bar in the item background.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com">Konstantin Sobolev</a>
 */
public class TodoItemBackgroundDrawable extends Drawable {
	private TodoItemBackgroundState s;
	private final Paint p = new Paint();
	private int percent;
	private int prioColor;

	public TodoItemBackgroundDrawable() {
		this(null);
	}

	public TodoItemBackgroundDrawable(@Nullable final TodoItemBackgroundState s) {
		this.s = new TodoItemBackgroundState(s);
		p.setStyle(Paint.Style.FILL);
	}

	public TodoItemBackgroundDrawable(final int c1, final int c2, final int prioStripeWidth) {
		this.s = new TodoItemBackgroundState(c1, c2, prioStripeWidth);
	}

	public void setPercent(final int percent) {
		this.percent = percent;
	}

	public void setPrioColor(final int prioColor) {
		this.prioColor = prioColor;
	}

	@Override
	public void draw(final Canvas canvas) {
		if (percent == 100) {
			canvas.drawColor(s.c2);
		} else if (percent == 0) {
			if (Color.alpha(s.c1) != 0)
				canvas.drawColor(s.c1);
		} else {
			final int height = canvas.getHeight();
			final int width = canvas.getWidth();
			final int d = (width * percent) / 100;
			p.setColor(s.c2);
			canvas.drawRect(0, 0, d, height, p);
			if (Color.alpha(s.c1) != 0) {
				p.setColor(s.c1);
				canvas.drawRect(d, 0, width, height, p);
			}
		}

		//draw prio stripe
		p.setColor(prioColor);
		canvas.drawRect(0, 0, s.prioStripeWidth, canvas.getHeight(), p);
	}

	@Override
	public void setAlpha(final int alpha) {
	}

	@Override
	public void setColorFilter(final ColorFilter cf) {
	}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	@Override
	public ConstantState getConstantState() {
		s.changingConf = super.getChangingConfigurations();
		return s;
	}

//	@Override
//	public void inflate(final Resources r, final XmlPullParser parser, final AttributeSet attrs) throws XmlPullParserException, IOException {
//		super.inflate(r, parser, attrs);
//
//		final TypedArray a = r.obtainAttributes(attrs, R.styleable.TodoItemBackgroundDrawable);
//		s.c1 = a.getColor(R.styleable.TwoColorDrawable_color1, Color.BLACK);
//		s.c2 = a.getColor(R.styleable.TwoColorDrawable_color1, Color.GRAY);
//
//		a.recycle();
//	}

	final class TodoItemBackgroundState extends ConstantState {
		int c1, c2;
		int prioStripeWidth;
		int changingConf;

		TodoItemBackgroundState(final int c1, final int c2, final int prioStripeWidth) {
			this.c1 = c1;
			this.c2 = c2;
			this.prioStripeWidth = prioStripeWidth;
		}

		TodoItemBackgroundState(final TodoItemBackgroundState s) {
			if (s != null) {
				c1 = s.c1;
				c2 = s.c2;
				prioStripeWidth = s.prioStripeWidth;
			}
		}

		@Override
		public int getChangingConfigurations() {
			return changingConf;
		}

		@Override
		public Drawable newDrawable() {
			return new TodoItemBackgroundDrawable(this);
		}
	}
}
