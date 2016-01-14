/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.viewer.render;

import java.util.Comparator;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.img.cache.CacheHints;

public interface MipmapOrdering
{
	/**
	 * Evaluate mipmap levels such that they can be ordered for rendering and/or
	 * prefetching.
	 *
	 * @param screenTransform
	 *            transforms screen coordinates to global coordinates.
	 * @param timepoint
	 *            current timepoint index
	 * @param previousTimepoint
	 *            previous timepoint index
	 */
	public MipmapHints getMipmapHints( AffineTransform3D screenTransform, int timepoint, int previousTimepoint );

	public static class Level
	{
		// level index in Source
		private final int mipmapLevel;

		// smaller value means render earlier (better quality)
		private final int renderOrder;

		// smaller value means prefetch earlier
		private final int prefetchOrder;

		// which CacheHints to use for rendering
		private final CacheHints renderCacheHints;

		// which CacheHints to use for prefetching
		private final CacheHints prefetchCacheHints;

		public Level(
				final int mipmapLevel,
				final int renderOrder,
				final int prefetchOrder,
				final CacheHints renderCacheHints,
				final CacheHints prefetchCacheHints )
		{
			this.mipmapLevel = mipmapLevel;
			this.renderOrder = renderOrder;
			this.prefetchOrder = prefetchOrder;
			this.renderCacheHints = renderCacheHints;
			this.prefetchCacheHints = prefetchCacheHints;
		}

		public Level( final int mipmapLevel, final int renderOrder, final int prefetchOrder )
		{
			this( mipmapLevel, renderOrder, prefetchOrder, null, null );
		}

		public int getMipmapLevel()
		{
			return mipmapLevel;
		}

		public int getRenderOrder()
		{
			return renderOrder;
		}

		public int getPrefetchOrder()
		{
			return prefetchOrder;
		}

		public CacheHints getRenderCacheHints()
		{
			return renderCacheHints;
		}

		public CacheHints getPrefetchCacheHints()
		{
			return prefetchCacheHints;
		}
	}

	public static class RenderOrderComparator implements Comparator< Level >
	{
		@Override
		public int compare( final Level o1, final Level o2 )
		{
			return o1.renderOrder - o2.renderOrder;
		}
	}

	public static class PrefetchOrderComparator implements Comparator< Level >
	{
		@Override
		public int compare( final Level o1, final Level o2 )
		{
			return o1.prefetchOrder - o2.prefetchOrder;
		}
	}

	public static RenderOrderComparator renderOrderComparator = new RenderOrderComparator();

	public static PrefetchOrderComparator prefetchOrderComparator = new PrefetchOrderComparator();

	public static class MipmapHints
	{
		private final List< Level > levels;

		private final boolean renewHintsAfterPaintingOnce;

		public MipmapHints( final List< Level > levels, final boolean renewHintsAfterPaintingOnce )
		{
			this.levels = levels;
			this.renewHintsAfterPaintingOnce = renewHintsAfterPaintingOnce;
		}

		public List< Level > getLevels()
		{
			return levels;
		}

		public boolean renewHintsAfterPaintingOnce()
		{
			return renewHintsAfterPaintingOnce;
		}
	}
}
