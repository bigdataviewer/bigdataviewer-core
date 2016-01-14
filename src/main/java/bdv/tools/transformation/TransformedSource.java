/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.img.cache.CacheHints;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import bdv.viewer.render.SetCacheHints;

/**
 * A {@link Source} that wraps another {@link Source} and allows to decorate it
 * with an extra {@link AffineTransform3D}.
 * <p>
 * This extra transformation is made to capture manual editing of the actual
 * transform in the SpimViewer.
 *
 * @author Jean-Yves Tinevez - Sept 2013
 *
 * @param <T>
 *            the type of the original source.
 */
public class TransformedSource< T > implements Source< T >, MipmapOrdering, SetCacheHints
{
	protected final Source< T > source;

	/**
	 * This is either the {@link #source} itself, if it implements
	 * {@link MipmapOrdering}, or a {@link DefaultMipmapOrdering}.
	 */
	protected final MipmapOrdering sourceMipmapOrdering;

	/**
	 * This is either the {@link #source} itself, if it implements
	 * {@link SetCacheHints}, or a {@link SetCacheHints} doing
	 * nothing.
	 */
	protected final SetCacheHints sourceSetCacheHints;

	/**
	 * Incremental part of the extra transformation.
	 */
	protected final AffineTransform3D incrementalTransform;

	/**
	 * Fixed part of the extra transformation.
	 */
	protected final AffineTransform3D fixedTransform;

	/**
	 * Extra transformation. Concatenation of {@link #incrementalTransform} *
	 * {@link #fixedTransform}.
	 */
	protected final AffineTransform3D sourceTransform;

	/**
	 * temporary. concatenation of {@link #sourceTransform} and the transform
	 * obtained from the decorated source.
	 */
	protected final AffineTransform3D composed;

	/**
	 * Instantiates a new {@link TransformedSource} wrapping the specified
	 * source with the identity transform.
	 *
	 * @param source
	 *            the source to wrap.
	 */
	public TransformedSource( final Source< T > source )
	{
		this( source,
				new AffineTransform3D(),
				new AffineTransform3D(),
				new AffineTransform3D() );
	}

	public TransformedSource( final Source< T > source, final TransformedSource< ? > shareTransform )
	{
		this( source,
				shareTransform.incrementalTransform,
				shareTransform.fixedTransform,
				shareTransform.sourceTransform );
	}

	private TransformedSource(
			final Source< T > source,
			final AffineTransform3D incrementalTransform,
			final AffineTransform3D fixedTransform,
			final AffineTransform3D sourceTransform )
	{
		this.source = source;

		sourceMipmapOrdering = MipmapOrdering.class.isInstance( source ) ?
				( MipmapOrdering ) source : new DefaultMipmapOrdering( source );

		sourceSetCacheHints = SetCacheHints.class.isInstance( source ) ?
				( SetCacheHints ) source : SetCacheHints.empty;

		this.incrementalTransform = incrementalTransform;
		this.fixedTransform = fixedTransform;
		this.sourceTransform = sourceTransform;
		this.composed = new AffineTransform3D();
	}

	/*
	 * EXTRA TRANSFORMATION methods
	 */

	/**
	 * Sets the fixed part of the extra transformation to the specified
	 * transform.
	 * <p>
	 * The extra transformation applied by the {@link TransformedSource} is a
	 * concatenation of an {@link #getIncrementalTransform(AffineTransform3D)
	 * incremental} and a {@link #getFixedTransform(AffineTransform3D) fixed}
	 * transform.
	 *
	 * @param transform
	 *            is copied to the {@link #getFixedTransform(AffineTransform3D)
	 *            fixed} transform.
	 */
	public synchronized void setFixedTransform( final AffineTransform3D transform )
	{
		fixedTransform.set( transform );
		sourceTransform.set( incrementalTransform );
		sourceTransform.concatenate( fixedTransform );
	}

	/**
	 * Get the fixed part of the extra transformation.
	 * <p>
	 * The extra transformation applied by the {@link TransformedSource} is a
	 * concatenation of an {@link #getIncrementalTransform(AffineTransform3D)
	 * incremental} and this fixed transform.
	 *
	 * @param transform
	 *            is set to the fixed transform.
	 */
	public synchronized void getFixedTransform( final AffineTransform3D transform )
	{
		transform.set( fixedTransform );
	}

	/**
	 * Sets the incremental part of the extra transformation to the specified
	 * transform.
	 * <p>
	 * The extra transformation applied by the {@link TransformedSource} is a
	 * concatenation of an {@link #getIncrementalTransform(AffineTransform3D)
	 * incremental} and a {@link #getFixedTransform(AffineTransform3D) fixed}
	 * transform.
	 *
	 * @param transform
	 *            is copied to the {@link #getIncrementalTransform(AffineTransform3D)
	 *            incremental} transform.
	 */
	public synchronized void setIncrementalTransform( final AffineTransform3D transform )
	{
		incrementalTransform.set( transform );
		sourceTransform.set( incrementalTransform );
		sourceTransform.concatenate( fixedTransform );
	}

	/**
	 * Get the incremental part of the extra transformation.
	 * <p>
	 * The extra transformation applied by the {@link TransformedSource} is a
	 * concatenation of this incremental transform and a
	 * {@link #getFixedTransform(AffineTransform3D) fixed} transform.
	 *
	 * @param transform
	 *            is set to the incremental transform.
	 */
	public synchronized void getIncrementalTransform( final AffineTransform3D transform )
	{
		transform.set( incrementalTransform );
	}

	/*
	 * SOURCE methods
	 */

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		source.getSourceTransform( t, level, transform );
		transform.preConcatenate( sourceTransform );
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
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return source.getSource( t, level );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return source.getInterpolatedSource( t, level, method );
	}

	@Override
	public T getType()
	{
		return source.getType();
	}

	@Override
	public String getName()
	{
		return source.getName();
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
	public void setCacheHints( final int level, final CacheHints cacheHints )
	{
		sourceSetCacheHints.setCacheHints( level, cacheHints );
	}

	@Override
	public synchronized MipmapHints getMipmapHints( final AffineTransform3D screenTransform, final int timepoint, final int previousTimepoint )
	{
		composed.set( screenTransform );
		composed.concatenate( sourceTransform );
		return sourceMipmapOrdering.getMipmapHints( composed, timepoint, previousTimepoint );
	}

	public Source< T > getWrappedSource()
	{
		return source;
	}
}
