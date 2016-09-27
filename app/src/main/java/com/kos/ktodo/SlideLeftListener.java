package com.kos.ktodo;

/**
 * Listener interested in 'main pane is sliding to the left' event.
 *
 * @author <a href="mailto:konstantin.sobolev@gmail.com" title="">Konstantin Sobolev</a>
 */
public interface SlideLeftListener {
	boolean canSlideLeft();
	/**
	 * Sliding has started.
	 *
	 * @param id ID of the item being edited in the right pane.
	 */
	void slideLeftStarted(final long id);

	void onSlideBack();
}
