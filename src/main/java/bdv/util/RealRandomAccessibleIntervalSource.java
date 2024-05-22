/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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

import mpicbg.spim.data.sequence.DefaultVoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;

public class RealRandomAccessibleIntervalSource< T extends Type< T > > extends RealRandomAccessibleSource< T >
{
	private final Interval interval;

	private final AffineTransform3D sourceTransform;

	public RealRandomAccessibleIntervalSource(
			final RealRandomAccessible< T > accessible,
			final Interval interval,
			final T type,
			final String name )
	{
		this( accessible, interval, type, new AffineTransform3D(), name, false );
	}

	public RealRandomAccessibleIntervalSource(
			final RealRandomAccessible< T > accessible,
			final Interval interval,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name )
	{
		this( accessible, interval, type, sourceTransform, name, false );
	}

	public RealRandomAccessibleIntervalSource(
			final RealRandomAccessible< T > accessible,
			final Interval interval,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name,
			final boolean doBoundingBoxIntersectionCheck )
	{
		super( accessible, type, name, new DefaultVoxelDimensions( -1 ), doBoundingBoxIntersectionCheck );
		this.interval = interval;
		this.sourceTransform = sourceTransform;
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( sourceTransform );
	}

	@Override
	public Interval getInterval( final int t, final int level )
	{
		return interval;
	}
}
