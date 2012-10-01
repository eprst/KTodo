package com.kos.ktodo;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * A drawable delegating to another drawable.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com" title="">Konstantin Sobolev</a>
 * @version $Revision$
 */
public class DelegatingDrawable extends Drawable {
	private final Drawable d;

	public DelegatingDrawable(Drawable d) {this.d = d;}

	@Override
	public void draw(Canvas canvas) {
		d.draw(canvas);
	}

	@Override
	public void setAlpha(int alpha) {
		d.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		d.setColorFilter(cf);
	}

	@Override
	public int getOpacity() {
		return d.getOpacity();
	}

	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		d.setBounds(left, top, right, bottom);
	}

	@Override
	public void setBounds(Rect bounds) {
		d.setBounds(bounds);
	}

	@Override
	public void setChangingConfigurations(int configs) {
		d.setChangingConfigurations(configs);
	}

	@Override
	public int getChangingConfigurations() {
		return d.getChangingConfigurations();
	}

	@Override
	public void setDither(boolean dither) {
		d.setDither(dither);
	}

	@Override
	public void setFilterBitmap(boolean filter) {
		d.setFilterBitmap(filter);
	}

	@Override
	public void invalidateSelf() {
		d.invalidateSelf();
	}

	@Override
	public void scheduleSelf(Runnable what, long when) {
		d.scheduleSelf(what, when);
	}

	@Override
	public void unscheduleSelf(Runnable what) {
		d.unscheduleSelf(what);
	}

	@Override
	public void setColorFilter(int color, PorterDuff.Mode mode) {
		d.setColorFilter(color, mode);
	}

	@Override
	public void clearColorFilter() {
		d.clearColorFilter();
	}

	@Override
	public boolean isStateful() {
		return d.isStateful();
	}

	@Override
	public boolean setState(int[] stateSet) {
		return d.setState(stateSet);
	}

	@Override
	public int[] getState() {
		return d.getState();
	}

	@Override
	public Drawable getCurrent() {
		return d.getCurrent();
	}

	@Override
	public boolean setVisible(boolean visible, boolean restart) {
		return d.setVisible(visible, restart);
	}

	@Override
	public Region getTransparentRegion() {
		return d.getTransparentRegion();
	}

//	@Override
//	protected boolean onStateChange(int[] state) {
//		return d.onStateChange(state);
//	}
//
//	@Override
//	protected boolean onLevelChange(int level) {
//		return d.onLevelChange(level);
//	}
//
//	@Override
//	protected void onBoundsChange(Rect bounds) {
//		super.onBoundsChange(bounds);
//	}

	@Override
	public int getIntrinsicWidth() {
		return d.getIntrinsicWidth();
	}

	@Override
	public int getIntrinsicHeight() {
		return d.getIntrinsicHeight();
	}

	@Override
	public int getMinimumWidth() {
		return d.getMinimumWidth();
	}

	@Override
	public int getMinimumHeight() {
		return d.getMinimumHeight();
	}

	@Override
	public boolean getPadding(Rect padding) {
		return d.getPadding(padding);
	}

	@Override
	public Drawable mutate() {
		return d.mutate();
	}

	@Override
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
		d.inflate(r, parser, attrs);
	}

	@Override
	public ConstantState getConstantState() {
		return d.getConstantState();
	}
}
