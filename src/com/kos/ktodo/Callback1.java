package com.kos.ktodo;

/**
 * 1-argument callback.
 */
public interface Callback1<T,K> {
	K call(final T arg);
}
