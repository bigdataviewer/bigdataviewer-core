package bdv;

import mpicbg.spim.data.View;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.view.Views;

public class VolatileSpimSource extends AbstractSpimSource< VolatileUnsignedShortType >
{
	protected final SpimSource nonVolatileSource;

	public VolatileSpimSource( final SequenceViewsLoader loader, final int setup, final String name )
	{
		super( loader, setup, name );
		nonVolatileSource = new SpimSource( loader, setup, name );
	}

	@Override
	protected void loadTimepoint( final int timepoint )
	{
		currentTimepoint = timepoint;
		if ( isPresent( timepoint ) )
		{
			final VolatileUnsignedShortType zero = new VolatileUnsignedShortType( 128 );
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
				currentSources[ level ] = imgLoader.getVolatileUnsignedShortImage( view, level );
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
	public VolatileUnsignedShortType getType()
	{
		return new VolatileUnsignedShortType();
	}

	public SpimSource nonVolatile()
	{
		return nonVolatileSource;
	}
}
