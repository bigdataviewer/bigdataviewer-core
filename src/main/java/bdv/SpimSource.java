package bdv;

import mpicbg.spim.data.View;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.integer.VolatileUnsignedShortType;
import net.imglib2.view.Views;

public class SpimSource extends AbstractSpimSource< UnsignedShortType >
{
	public SpimSource( final SequenceViewsLoader loader, final int setup, final String name )
	{
		super( loader, setup, name );
	}

	@Override
	protected void loadTimepoint( final int timepoint )
	{
		currentTimepoint = timepoint;
		if ( isPresent( timepoint ) )
		{
			final UnsignedShortType zero = new VolatileUnsignedShortType( 0 ).get();
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
				currentSources[ level ] = imgLoader.getUnsignedShortImage( view, level );
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
	public UnsignedShortType getType()
	{
		return new UnsignedShortType();
	}
}
