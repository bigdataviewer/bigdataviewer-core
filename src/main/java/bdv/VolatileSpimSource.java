package bdv;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class VolatileSpimSource< T extends NumericType< T >, V extends Volatile< T > & NumericType< V >  > extends AbstractSpimSource< V >
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
	protected void loadTimepoint( final int timepoint )
	{
		currentTimepoint = timepoint;
		if ( isPresent( timepoint ) )
		{
			final V zero = imgLoader.getVolatileImageType().createVariable();
			// TODO: change to zero.setZero(); the following is just for debugging
			( ( RealType< ? > ) zero ).setReal( 128 );
			final View view = sequenceViews.getView( timepoint, setup );
			final AffineTransform3D reg = view.getModel();
			final AffineTransform3D mipmapTransform = new AffineTransform3D();
			for ( int level = 0; level < currentSources.length; level++ )
			{
				final double[] resolution = imgLoader.getMipmapResolutions( setup )[ level ];
				for ( int d = 0; d < 3; ++d )
				{
					mipmapTransform.set( resolution[ d ], d, d );
					mipmapTransform.set( 0.5 * ( resolution[ d ] - 1 ), d, 3 );
				}
				currentSourceTransforms[ level ].set( reg );
				currentSourceTransforms[ level ].concatenate( mipmapTransform );
				currentSources[ level ] = imgLoader.getVolatileImage( view, level );
				for ( int method = 0; method < numInterpolationMethods; ++method )
					currentInterpolatedSources[ level ][ method ] = Views.interpolate( Views.extendValue( currentSources[ level ], zero ), interpolatorFactories[ method ] );
			}
		}
		else
		{
			for ( int level = 0; level < currentSources.length; level++ )
			{
				currentSourceTransforms[ level ].identity();
				currentSources[ level ] = null;
				for ( int method = 0; method < numInterpolationMethods; ++method )
					currentInterpolatedSources[ level ][ method ] = null;
			}
		}
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
}
