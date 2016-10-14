/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.cache;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * A cache associating keys {@code K} to values {@code V}. Key-value pairs are
 * added manually to the cache. When and which entries are evicted is
 * implementation-specific.
 * <p>
 * Values can be added to the cache with two different "priorities".
 * <ul>
 * <li>Values added with {@link #putWeak(Object, Object)} can be discarded
 * freely (roughly equivalent to maintaining a {@link WeakReference} to the
 * value). Use for values that are cheap to (re-)create.</li>
 * <li>Values added with {@link #putSoft(Object, Object)} are discarded only if
 * the need arises (roughly equivalent to maintaining a {@link SoftReference} to
 * the value). Use for values that are expensive to (re-)create.</li>
 * </ul>
 *
 * @param <K>
 *            key type.
 * @param <V>
 *            value type.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface WeakSoftCache< K, V >
{
	public void putWeak( final K key, final V value );

	public void putSoft( final K key, final V value );

	public V get( final Object key );

	/**
	 * Performs pending maintenance operations needed by the cache. Exactly
	 * which activities are performed is implementation-dependent. This should
	 * be called periodically
	 */
	public void cleanUp();

	/**
	 * Discards all entries in the cache.
	 */
	void invalidateAll();

	/**
	 * Create a new {@link WeakSoftCache}.
	 * <p>
	 * This is here so we can swap out implementations easily and will probably
	 * be replaced by a scijava service later.
	 */
	public static < K, V > WeakSoftCache< K, V > newInstance()
	{
		return new WeakSoftCacheImp<>();
	}
}
