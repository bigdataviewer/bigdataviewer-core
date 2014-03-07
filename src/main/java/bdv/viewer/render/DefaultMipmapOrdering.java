package bdv.viewer.render;

import java.util.ArrayList;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.util.MipmapTransforms;
import bdv.viewer.Source;

/**
 * The standard mipmap ordering strategy for local hdf5 data. Assumes that
 * mipmap indices in the source are ordered by decreasing resolution. Finds the
 * mipmap level that best matches the given screen scale for the given source.
 * Then, starting from this best level render all levels down to the lowest
 * resolution. For prefetching reverse that order (lowest resolution is
 * prefetched first).
 *
 * Additionally, when moving between time-points the following hack is used:
 * When scrolling through time, we often get frames for which no data was loaded
 * yet. To speed up rendering in these cases, use only two mipmap levels: the
 * optimal and the coarsest. By doing this, we require at most two passes over
 * the image at the expense of ignoring data present in intermediate mipmap
 * levels. The assumption is, that we will either be moving back and forth
 * between images that have all data present already or that we move to a new
 * image with no data present at all.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class DefaultMipmapOrdering implements MipmapOrdering
{
	private final Source< ? > source;

	private final LoadingStrategy renderStrategy;

	private final LoadingStrategy prefetchStrategy;

	public DefaultMipmapOrdering( final Source< ? > source, final LoadingStrategy renderStrategy, final LoadingStrategy prefetchStrategy )
	{
		this.source = source;
		this.renderStrategy = renderStrategy;
		this.prefetchStrategy = prefetchStrategy;
	}

	public DefaultMipmapOrdering( final Source< ? > source )
	{
		this( source, null, null );
	}

	@Override
	public MipmapHints getMipmapHints( final AffineTransform3D screenTransform, final int timepoint, final int previousTimepoint )
	{
		final int bestLevel = MipmapTransforms.getBestMipMapLevel( screenTransform, source, timepoint );
		final int numMipmapLevels = source.getNumMipmapLevels();
		final int maxLevel = numMipmapLevels - 1;
		boolean renewHintsAfterPaintingOnce = false;
		final ArrayList< Level > levels = new ArrayList< Level >();
		if ( timepoint != previousTimepoint )
		{
			// When scrolling through time, we often get frames for which no
			// data was loaded yet. To speed up rendering in these cases, use
			// only two mipmap levels: the optimal and the coarsest. By doing
			// this, we require at most two passes over the image at the expense
			// of ignoring data present in intermediate mipmap levels. The
			// assumption is, that we will either be moving back and forth
			// between images that have all data present already or that we move
			// to a new image with no data present at all.
			levels.add( new Level( bestLevel, 0, 1, renderStrategy, prefetchStrategy ) );
			if ( maxLevel != bestLevel )
				levels.add( new Level( maxLevel, 1, 0, renderStrategy, prefetchStrategy ) );

			// slight abuse of newFrameRequest: we only want this two-pass
			// rendering to happen once then switch to normal multi-pass
			// rendering if we remain longer on this frame.
			renewHintsAfterPaintingOnce = true;
		}
		else
			for ( int i = bestLevel; i < numMipmapLevels; ++i )
				levels.add( new Level( i, i, -i, renderStrategy, prefetchStrategy ) );
		return new MipmapHints( levels, renewHintsAfterPaintingOnce );
	}
}
