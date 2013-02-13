package viewer;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import viewer.hdf5.Hdf5ImageLoader;
import viewer.refactor.Interpolation;

class Source implements SpimSource< UnsignedShortType >
{
	int currentTimepoint;

	RandomAccessibleInterval< UnsignedShortType >[] currentSources;

	RealRandomAccessible< UnsignedShortType >[][] currentInterpolatedSources;

	final AffineTransform3D[] currentSourceTransforms;

	final int setup;

	final String name;

	final SequenceViewsLoader sequenceViews;

	final Hdf5ImageLoader imgLoader;

	final int numTimepoints;

	final protected static int numInterpolationMethods = 2;

	final protected static int iNearestNeighborMethod = 0;

	final protected static int iNLinearMethod = 1;

	final protected InterpolatorFactory< UnsignedShortType, RandomAccessible< UnsignedShortType > >[] interpolatorFactories;

	@SuppressWarnings( "unchecked" )
	Source( final SequenceViewsLoader loader, final int setup, final String name )
	{
		this.setup = setup;
		this.name = name;
		this.sequenceViews = loader;
		final SequenceDescription seq = loader.getSequenceDescription();
		imgLoader = ( Hdf5ImageLoader ) seq.imgLoader;
		numTimepoints = seq.numTimepoints();
		final int levels = imgLoader.numMipmapLevels();
		currentSources = new RandomAccessibleInterval[ levels ];
		currentInterpolatedSources = new RealRandomAccessible[ levels ][ 2 ];
		currentSourceTransforms = new AffineTransform3D[ levels ];
		for ( int level = 0; level < levels; level++ )
			currentSourceTransforms[ level ] = new AffineTransform3D();
		interpolatorFactories = new InterpolatorFactory[ numInterpolationMethods ];
		interpolatorFactories[ iNearestNeighborMethod ] = new NearestNeighborInterpolatorFactory< UnsignedShortType >();
		interpolatorFactories[ iNLinearMethod ] = new NLinearInterpolatorFactory< UnsignedShortType >();
		loadTimepoint( 0 );
	}

	void loadTimepoint( final int timepoint )
	{
		currentTimepoint = timepoint;
		if ( isPresent( timepoint ) )
		{
			final UnsignedShortType zero = new UnsignedShortType();
			zero.setZero();
			final View view = sequenceViews.getView( timepoint, setup );
			final AffineTransform3D reg = view.getModel();
			final AffineTransform3D mipmapTransform = new AffineTransform3D();
			for ( int level = 0; level < currentSources.length; level++ )
			{
				final double[] resolution = imgLoader.getMipmapResolutions()[ level ];
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
	public boolean isPresent( final int t )
	{
		return t >= 0 && t < numTimepoints;
	}

	@Override
	public synchronized RandomAccessibleInterval< UnsignedShortType > getSource( final int t, final int level )
	{
		if ( t != currentTimepoint )
			loadTimepoint( t );
		return currentSources[ level ];
	}

	@Override
	public RealRandomAccessible< UnsignedShortType > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		if ( t != currentTimepoint )
			loadTimepoint( t );
		return currentInterpolatedSources[ level ][ method == Interpolation.NLINEAR ? iNLinearMethod : iNearestNeighborMethod ];
	}

	@Override
	public synchronized AffineTransform3D getSourceTransform( final int t, final int level )
	{
		if ( t != currentTimepoint )
			loadTimepoint( t );
		return currentSourceTransforms[ level ];
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public UnsignedShortType getType()
	{
		return new UnsignedShortType();
	}
}