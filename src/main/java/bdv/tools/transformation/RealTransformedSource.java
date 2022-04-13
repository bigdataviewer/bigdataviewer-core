/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.tools.transformation;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.util.Intervals;
import net.imglib2.util.RealIntervals;
import net.imglib2.view.Views;

/**
 * A {@link Source} that wraps another {@link Source}, transforming it with a
 * {@link InvertibleRealTransform}.
 *
 * @author John Bogovic
 *
 * @param <T> the type
 */
public class RealTransformedSource < T > implements Source< T >, MipmapOrdering
{
	/**
	 * The wrapped {@link Source}.
	 */
	private final Source< T > source;

	private final String name;

	/**
	 * This is either the {@link #source} itself, if it implements
	 * {@link MipmapOrdering}, or a {@link DefaultMipmapOrdering}.
	 */
	private final MipmapOrdering sourceMipmapOrdering;

	private InvertibleRealTransform transform;

	private final Supplier< Boolean > boundingBoxCullingSupplier;

	private final BiFunction< RealTransform, Interval, Interval > boundingBoxEstimator;

	public RealTransformedSource( final Source< T > source, final String name,
			final InvertibleRealTransform transform )
	{
		this( source, name, transform,
				(t,i) -> { return Intervals.smallestContainingInterval( 
						RealIntervals.boundingIntervalFaces( i, t, 5 )); },
				null );
	}

	public RealTransformedSource( final Source< T > source, final String name,
		    final BiFunction< RealTransform, Interval, Interval > boundingBoxEstimator,
			final InvertibleRealTransform transform )
	{
		this( source, name, transform, boundingBoxEstimator, null );
	}

	public RealTransformedSource( final Source< T > source, final String name,
		    final InvertibleRealTransform transform,
		    final BiFunction< RealTransform, Interval, Interval > boundingBoxEstimator,
			final Supplier< Boolean > doBoundingBoxCulling )
	{
		this.source = source;
		this.name = name;
		this.boundingBoxEstimator = boundingBoxEstimator;
		this.boundingBoxCullingSupplier = doBoundingBoxCulling;
		setTransform( transform );

		sourceMipmapOrdering = MipmapOrdering.class.isInstance( source ) ?
				( MipmapOrdering ) source : new DefaultMipmapOrdering( source );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		if( boundingBoxCullingSupplier != null )
			return boundingBoxCullingSupplier.get();
		else
			return source.doBoundingBoxCulling();
	}

	public void setTransform( final InvertibleRealTransform transform )
	{
		this.transform = transform;
	}

	public Source< T > getWrappedSource()
	{
		return source;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		// TODO expose interp method - it probably matters
		@SuppressWarnings("unchecked")
		final RealTransformRealRandomAccessible<T,?> interpSrc = (RealTransformRealRandomAccessible<T,?>)getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );

		final AffineTransform3D transform = new AffineTransform3D();
		source.getSourceTransform( t, level, transform );
		final RealTransformSequence totalInverseTransform = new RealTransformSequence();
		totalInverseTransform.add( transform.inverse() );
		totalInverseTransform.add( transform.inverse() );
		totalInverseTransform.add( transform );
		final Interval boundingInterval = boundingBoxEstimator.apply( totalInverseTransform, source.getSource( t, level ) );

		return Views.interval( Views.raster(interpSrc), boundingInterval );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		final RealRandomAccessible<T> realSrc = source.getInterpolatedSource( t, level, method );
		final AffineTransform3D transform = new AffineTransform3D();
		source.getSourceTransform( t, level, transform );

		final RealTransformSequence totalTransform = new RealTransformSequence();
		totalTransform.add( transform );
		totalTransform.add( transform );
		totalTransform.add( transform.inverse() );

		return new RealTransformRealRandomAccessible< T, RealTransform >( realSrc, totalTransform );
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		source.getSourceTransform( t, level, transform );
	}

	public InvertibleRealTransform getTransform()
	{
		return transform;
	}

	@Override
	public T getType()
	{
		return source.getType();
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return source.getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return source.getNumMipmapLevels();
	}

	@Override
	public synchronized MipmapHints getMipmapHints( final AffineTransform3D screenTransform, final int timepoint, final int previousTimepoint )
	{
		return sourceMipmapOrdering.getMipmapHints( screenTransform, timepoint, previousTimepoint );
	}

}
