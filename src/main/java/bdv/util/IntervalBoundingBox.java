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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;

public class IntervalBoundingBox
{
	/**
	 * get bounding box of a list of points.
	 */
	public static < P extends RealLocalizable > RealInterval getBoundingBox( final List< P > points )
	{
		assert !points.isEmpty();

		final P p0 = points.get( 0 );
		final int n = p0.numDimensions();
		final double[] min = new double[ n ];
		p0.localize( min );
		final double[] max = min.clone();
		for ( final P point : points )
		{
			for ( int d = 0; d < n; ++d )
			{
				final double p = point.getDoublePosition( d );
				if (p < min[ d ])
					min[ d ] = p;
				else if (p > max[ d ])
					max[ d ] = p;
			}
		}
		return new FinalRealInterval( min, max );
	}

	/**
	 * get "corners" of an interval as a list of points.
	 */
	public static List< RealLocalizable > getCorners( final RealInterval interval )
	{
		final ArrayList< RealLocalizable > corners = new ArrayList< RealLocalizable >();
		final int n = interval.numDimensions();
		final int[] tmp = new int[ n ];
		Arrays.fill( tmp, 2 );
		final LocalizingZeroMinIntervalIterator i = new LocalizingZeroMinIntervalIterator( tmp );
		while ( i.hasNext() )
		{
			i.fwd();
			final RealPoint p = new RealPoint( n );
			for ( int d = 0; d < n; ++d )
				p.setPosition( i.getIntPosition( d ) == 0 ? interval.realMin( d ) : interval.realMax( d ), d );
			corners.add( p );
		}
		return corners;
	}
}
