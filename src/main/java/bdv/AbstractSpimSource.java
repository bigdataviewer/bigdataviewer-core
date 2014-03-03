package bdv;

import mpicbg.spim.data.SequenceDescription;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;

public abstract class AbstractSpimSource< T extends NumericType< T > > implements Source< T >
{
	protected int currentTimepoint;

	protected RandomAccessibleInterval< T >[] currentSources;

	protected RealRandomAccessible< T >[][] currentInterpolatedSources;

	protected final AffineTransform3D[] currentSourceTransforms;

	protected final int setup;

	protected final String name;

	protected final SequenceViewsLoader sequenceViews;

	protected final int numTimepoints;

	protected final int numMipmapLevels;

	protected final static int numInterpolationMethods = 2;

	protected final static int iNearestNeighborMethod = 0;

	protected final static int iNLinearMethod = 1;

	protected final InterpolatorFactory< T, RandomAccessible< T > >[] interpolatorFactories;

	@SuppressWarnings( "unchecked" )
	public AbstractSpimSource( final SequenceViewsLoader loader, final int setup, final String name )
	{
		this.setup = setup;
		this.name = name;
		this.sequenceViews = loader;
		final SequenceDescription seq = loader.getSequenceDescription();
		numTimepoints = seq.numTimepoints();
		numMipmapLevels =  ( ( ViewerImgLoader< ?, ? > ) seq.imgLoader ).numMipmapLevels( setup );
		currentSources = new RandomAccessibleInterval[ numMipmapLevels ];
		currentInterpolatedSources = new RealRandomAccessible[ numMipmapLevels ][ numInterpolationMethods ];
		currentSourceTransforms = new AffineTransform3D[ numMipmapLevels ];
		for ( int level = 0; level < numMipmapLevels; level++ )
			currentSourceTransforms[ level ] = new AffineTransform3D();
		interpolatorFactories = new InterpolatorFactory[ numInterpolationMethods ];
		interpolatorFactories[ iNearestNeighborMethod ] = new NearestNeighborInterpolatorFactory< T >();
		interpolatorFactories[ iNLinearMethod ] = new NLinearInterpolatorFactory< T >();
	}

	protected abstract void loadTimepoint( final int timepoint );

	@Override
	public boolean isPresent( final int t )
	{
		return t >= 0 && t < numTimepoints;
	}

	@Override
	public synchronized RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		if ( t != currentTimepoint )
			loadTimepoint( t );
		return currentSources[ level ];
	}

	@Override
	public synchronized RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
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
	public int getNumMipmapLevels()
	{
		return numMipmapLevels;
	}
}
