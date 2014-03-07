package bdv.viewer.render;

import java.util.Comparator;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;

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

		// which LoadingStrategy to use for rendering
		private final LoadingStrategy renderLoadingStrategy;

		// which LoadingStrategy to use for prefetching
		private final LoadingStrategy prefetchLoadingStrategy;

		public Level(
				final int mipmapLevel,
				final int renderOrder,
				final int prefetchOrder,
				final LoadingStrategy renderLoadingStrategy,
				final LoadingStrategy prefetchLoadingStrategy )
		{
			this.mipmapLevel = mipmapLevel;
			this.renderOrder = renderOrder;
			this.prefetchOrder = prefetchOrder;
			this.renderLoadingStrategy = renderLoadingStrategy;
			this.prefetchLoadingStrategy = prefetchLoadingStrategy;
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

		public LoadingStrategy getRenderLoadingStrategy()
		{
			return renderLoadingStrategy;
		}

		public LoadingStrategy getPrefetchLoadingStrategy()
		{
			return prefetchLoadingStrategy;
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
