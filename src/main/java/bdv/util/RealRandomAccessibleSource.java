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
package bdv.util;

import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.view.Views;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;

/**
 * A {@link Source} wrapping some {@link RealRandomAccessible}. Derived concrete
 * classes must implement {@link #getInterval(int, int)} to specify which
 * interval to use for {@link #getSource(int, int)}.
 *
 * @param <T>
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public abstract class RealRandomAccessibleSource< T extends Type< T > > implements Source< T >
{
	protected RealRandomAccessible< T > accessible;

	protected final T type;

	protected final String name;

	protected final VoxelDimensions voxelDimensions;

	public RealRandomAccessibleSource( final RealRandomAccessible< T > accessible, final T type, final String name )
	{
		this( accessible, type, name, null );
	}

	public RealRandomAccessibleSource( final RealRandomAccessible< T > accessible, final T type, final String name, final VoxelDimensions voxelDimensions )
	{
		this.accessible = accessible;
		this.type = type.createVariable();
		this.name = name;
		this.voxelDimensions = voxelDimensions;
	}

	@Override
	public boolean isPresent( final int t )
	{
		return true;
	}

	/**
	 * Return the interval which should be used to bound the wrapped
	 * (unbounded) {@link RealRandomAccessible} for {@link #getSource(int, int)}.
	 *
	 * @return image interval
	 */
	public abstract Interval getInterval( final int t, final int level );

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return Views.interval( Views.raster( accessible ), getInterval( t, level ) );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return accessible;
	}

	@Override
	public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.identity();
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
	public T getType()
	{
		return type;
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
		return 1;
	}
}
