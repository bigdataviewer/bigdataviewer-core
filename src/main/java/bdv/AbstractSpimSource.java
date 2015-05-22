package bdv;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;

public abstract class AbstractSpimSource< T extends NumericType< T > > implements Source< T >
{
	protected int currentTimePointIndex;

	protected RandomAccessibleInterval< T >[] currentSources;

	protected RealRandomAccessible< T >[][] currentInterpolatedSources;

	protected final AffineTransform3D[] currentSourceTransforms;

	protected final int setupId;

	protected final String name;

	protected final List< TimePoint > timePointsOrdered;

	protected final Map< ViewId, ViewRegistration > viewRegistrations;

	protected final Set< ViewId > missingViews;

	protected final VoxelDimensions voxelDimensions;

	protected final int numMipmapLevels;

	protected final static int numInterpolationMethods = 2;

	protected final static int iNearestNeighborMethod = 0;

	protected final static int iNLinearMethod = 1;

	protected final InterpolatorFactory< T, RandomAccessible< T > >[] interpolatorFactories;

	@SuppressWarnings( "unchecked" )
	public AbstractSpimSource( final AbstractSpimData< ? > spimData, final int setupId, final String name )
	{
		this.setupId = setupId;
		this.name = name;
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		timePointsOrdered = seq.getTimePoints().getTimePointsOrdered();
		viewRegistrations = spimData.getViewRegistrations().getViewRegistrations();
		missingViews = seq.getMissingViews() == null
				? new HashSet< ViewId >()
				: seq.getMissingViews().getMissingViews();
		voxelDimensions = seq.getViewSetups().get( setupId ).getVoxelSize();
		numMipmapLevels = ( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).numMipmapLevels( setupId );
		currentSources = new RandomAccessibleInterval[ numMipmapLevels ];
		currentInterpolatedSources = new RealRandomAccessible[ numMipmapLevels ][ numInterpolationMethods ];
		currentSourceTransforms = new AffineTransform3D[ numMipmapLevels ];
		for ( int level = 0; level < numMipmapLevels; level++ )
			currentSourceTransforms[ level ] = new AffineTransform3D();
		interpolatorFactories = new InterpolatorFactory[ numInterpolationMethods ];
		interpolatorFactories[ iNearestNeighborMethod ] = new NearestNeighborInterpolatorFactory< T >();
		interpolatorFactories[ iNLinearMethod ] = new ClampingNLinearInterpolatorFactory< T >();
	}

	protected void loadTimepoint( final int timepointIndex )
	{
		currentTimePointIndex = timepointIndex;
		if ( isPresent( timepointIndex ) )
		{
			final T zero = getType().createVariable();
			zero.setZero();
			final int timepointId = timePointsOrdered.get( timepointIndex ).getId();
			final ViewId viewId = new ViewId( timepointId, setupId );
			final AffineTransform3D reg = viewRegistrations.get( viewId ).getModel();
			for ( int level = 0; level < currentSources.length; level++ )
			{
				final AffineTransform3D mipmapTransform = getMipmapTransforms()[ level ];
				currentSourceTransforms[ level ].set( reg );
				currentSourceTransforms[ level ].concatenate( mipmapTransform );
				currentSources[ level ] = getImage( viewId, level );
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

	protected abstract AffineTransform3D[] getMipmapTransforms();

	protected abstract RandomAccessibleInterval< T > getImage( final ViewId viewId, final int level );

	@Override
	public boolean isPresent( final int t )
	{
		return t >= 0 && t < timePointsOrdered.size() && !missingViews.contains( new ViewId( timePointsOrdered.get( t ).getId(), setupId ) );
	}

	@Override
	public synchronized RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentSources[ level ];
	}

	@Override
	public synchronized RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentInterpolatedSources[ level ][ method == Interpolation.NLINEAR ? iNLinearMethod : iNearestNeighborMethod ];
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		transform.set( currentSourceTransforms[ level ] );
	}

	@Override
	@Deprecated
	public AffineTransform3D getSourceTransform( final int t, final int level )
	{
		final AffineTransform3D transform = new AffineTransform3D();
		getSourceTransform( t, level, transform );
		return transform;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return numMipmapLevels;
	}

	public int getSetupId()
	{
		return setupId;
	}
}
