/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
package bdv;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.UncheckedCache;
import net.imglib2.cache.ref.BoundedSoftRefLoaderCache;
import net.imglib2.cache.util.Caches;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public abstract class AbstractSpimSource< T extends NumericType< T > > implements Source< T >
{
	protected static class ImgKey
	{
		protected final int timepoint;

		protected final int level;

		protected final Interpolation method;

		protected final ThreadGroup threadGroup;

		private final int hashcode;

		public ImgKey(
				final int timepoint,
				final int level,
				final Interpolation method,
				final ThreadGroup threadGroup )
		{
			this.timepoint = timepoint;
			this.level = level;
			this.method = method;
			this.threadGroup = threadGroup;

			int hash = 31 * timepoint + level;
			if ( method != null )
				hash = 31 * hash + method.hashCode();
			if ( threadGroup != null )
				hash = 31 * hash + threadGroup.hashCode();
			hashcode = hash;
		}

		@Override
		public int hashCode()
		{
			return hashcode;
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj instanceof ImgKey )
			{
				final ImgKey other = ( ImgKey ) obj;
				return this.timepoint == other.timepoint
						&& this.level == other.level
						&& this.method == other.method
						&& this.threadGroup == other.threadGroup;
			}
			return false;
		}
	}

	protected int currentTimePointIndex;

	protected boolean currentTimePointIsPresent;

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

	protected final UncheckedCache< ImgKey, RandomAccessibleInterval< T > > cachedSources;

	protected final UncheckedCache< ImgKey, RealRandomAccessible< T > > cachedInterpolatedSources;

	@SuppressWarnings( "unchecked" )
	public AbstractSpimSource( final AbstractSpimData< ? > spimData, final int setupId, final String name )
	{
		this.setupId = setupId;
		this.name = name;
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		timePointsOrdered = seq.getTimePoints().getTimePointsOrdered();
		viewRegistrations = spimData.getViewRegistrations().getViewRegistrations();
		missingViews = seq.getMissingViews() == null
				? new HashSet<>()
				: seq.getMissingViews().getMissingViews();
		voxelDimensions = seq.getViewSetups().get( setupId ).getVoxelSize();
		numMipmapLevels = ( ( ViewerImgLoader ) seq.getImgLoader() ).getSetupImgLoader( setupId ).numMipmapLevels();

		interpolatorFactories = new InterpolatorFactory[ numInterpolationMethods ];
		interpolatorFactories[ iNearestNeighborMethod ] = new NearestNeighborInterpolatorFactory<>();
		interpolatorFactories[ iNLinearMethod ] = new ClampingNLinearInterpolatorFactory<>();

		final CacheLoader< ImgKey, RandomAccessibleInterval< T > > loader = key -> {
			return getImage( timePointsOrdered.get( key.timepoint ).getId(), key.level );
		};
		cachedSources = Caches.unchecked( Caches.withLoader(
				new BoundedSoftRefLoaderCache<>( 3 * numMipmapLevels ),
				loader ) );

		final CacheLoader< ImgKey, RealRandomAccessible< T > > interpolLoader = key -> {
			final T zero = getType().createVariable();
			zero.setZero();
			final int i = key.method == Interpolation.NLINEAR ? iNLinearMethod : iNearestNeighborMethod;
			final InterpolatorFactory< T, RandomAccessible< T > > factory = interpolatorFactories[ i ];
			return Views.interpolate( Views.extendValue( getSource( key.timepoint, key.level ), zero ), factory );
		};
		cachedInterpolatedSources = Caches.unchecked( Caches.withLoader(
				new BoundedSoftRefLoaderCache<>( 3 * numMipmapLevels * numInterpolationMethods ),
				interpolLoader ) );

		currentSourceTransforms = new AffineTransform3D[ numMipmapLevels ];
		for ( int level = 0; level < numMipmapLevels; level++ )
			currentSourceTransforms[ level ] = new AffineTransform3D();

	}

	protected void loadTimepoint( final int timepointIndex )
	{
		currentTimePointIndex = timepointIndex;
		currentTimePointIsPresent = isPresent( timepointIndex );
		if ( currentTimePointIsPresent )
		{
			final int timepointId = timePointsOrdered.get( timepointIndex ).getId();
			final ViewId viewId = new ViewId( timepointId, setupId );
			final AffineTransform3D reg = viewRegistrations.get( viewId ).getModel();
			for ( int level = 0; level < currentSourceTransforms.length; level++ )
			{
				final AffineTransform3D mipmapTransform = getMipmapTransforms()[ level ];
				currentSourceTransforms[ level ].set( reg );
				currentSourceTransforms[ level ].concatenate( mipmapTransform );
			}
		}
		else
		{
			for ( final AffineTransform3D t : currentSourceTransforms )
				t.identity();
		}
	}

	protected abstract AffineTransform3D[] getMipmapTransforms();

	protected abstract RandomAccessibleInterval< T > getImage( final int timepointId, final int level );

	@Override
	public boolean isPresent( final int t )
	{
		return t >= 0 && t < timePointsOrdered.size() && !missingViews.contains( new ViewId( timePointsOrdered.get( t ).getId(), setupId ) );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return getSource( t, level, Thread.currentThread().getThreadGroup() );
	}

	public synchronized RandomAccessibleInterval< T > getSource( final int t, final int level, final ThreadGroup threadGroup )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentTimePointIsPresent
				? cachedSources.get( new ImgKey( t, level, null, threadGroup ) )
				: null;
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return getInterpolatedSource( t, level, method, Thread.currentThread().getThreadGroup() );
	}

	public synchronized RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method, final ThreadGroup threadGroup )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentTimePointIsPresent
				? cachedInterpolatedSources.get( new ImgKey( t, level, method, threadGroup ) )
				: null;
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		transform.set( currentSourceTransforms[ level ] );
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
