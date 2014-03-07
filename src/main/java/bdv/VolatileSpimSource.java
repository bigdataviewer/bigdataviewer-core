package bdv;

import java.util.ArrayList;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.viewer.render.MipmapOrdering;
import bdv.viewer.render.SetLoadingStrategy;
import bdv.viewer.state.ViewerState;

public class VolatileSpimSource< T extends NumericType< T >, V extends Volatile< T > & NumericType< V > >
		extends AbstractSpimSource< V >
		implements MipmapOrdering, SetLoadingStrategy
{
	protected final SpimSource< T > nonVolatileSource;

	protected final ViewerImgLoader< ?, V > imgLoader;

	@SuppressWarnings( "unchecked" )
	public VolatileSpimSource( final SequenceViewsLoader loader, final int setup, final String name )
	{
		super( loader, setup, name );
		nonVolatileSource = new SpimSource< T >( loader, setup, name );
		final SequenceDescription seq = loader.getSequenceDescription();
		imgLoader = ( ViewerImgLoader< ?, V > ) seq.imgLoader;
		loadTimepoint( 0 );
	}

	@Override
	public V getType()
	{
		return imgLoader.getVolatileImageType();
	}

	public SpimSource< T > nonVolatile()
	{
		return nonVolatileSource;
	}

	@Override
	protected RandomAccessibleInterval< V > getImage( final View view, final int level )
	{
		return imgLoader.getVolatileImage( view, level );
	}

	@Override
	protected AffineTransform3D[] getMipmapTransforms( final int setup )
	{
		return imgLoader.getMipmapTransforms( setup );
	}

	@Override
	public MipmapHints getMipmapHints( final AffineTransform3D screenTransform, final int timepoint, final int previousTimepoint )
	{
		if ( MipmapOrdering.class.isInstance( imgLoader ) )
			return ( ( MipmapOrdering ) imgLoader ).getMipmapHints( screenTransform, timepoint, previousTimepoint );

		final int bestLevel = ViewerState.getBestMipMapLevel( screenTransform, this, timepoint );
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
			levels.add( new Level( bestLevel, 0, 1, LoadingStrategy.BUDGETED, LoadingStrategy.VOLATILE ) );
			if ( maxLevel != bestLevel )
				levels.add( new Level( maxLevel, 1, 0, LoadingStrategy.BUDGETED, LoadingStrategy.VOLATILE ) );

			// slight abuse of newFrameRequest: we only want this two-pass
			// rendering to happen once then switch to normal multi-pass
			// rendering if we remain longer on this frame.
			renewHintsAfterPaintingOnce = true;
		}
		else
			for ( int i = bestLevel; i < numMipmapLevels; ++i )
				levels.add( new Level( i, i, -i, LoadingStrategy.BUDGETED, LoadingStrategy.VOLATILE ) );
		return new MipmapHints( levels, renewHintsAfterPaintingOnce );
	}

	@Override
	public void setLoadingStrategy( final int level, final LoadingStrategy strategy )
	{
		final RandomAccessibleInterval< V > source = currentSources[ level ];
		// The type check is currently necessary because it might be a
		// constant RandomAccessibleInterval (for missing images, see
		// Hdf5ImageLoader#getMissingDataImage)
		if ( CachedCellImg.class.isInstance( source ) )
			( ( CachedCellImg< ?, ? > ) source ).setLoadingStrategy( strategy );
	}
}
