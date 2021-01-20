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
package bdv.tools.boundingbox;

import net.imglib2.RealInterval;

/**
 * Utilities for computing the corners of a RealInterval.
 *
 * The index of a corner is interpreted as a binary number where bit 0
 * corresponds to X, bit 1 corresponds to Y, etc. A zero bit means min in the
 * corresponding dimension, a one bit means max in the corresponding dimension.
 *
 * @author Tobias Pietzsch
 */
public class IntervalCorners
{
	public static int numCorners( final RealInterval interval )
	{
		return 1 << interval.numDimensions();
	}

	public static double[] corner( final RealInterval interval, final int index )
	{
		final int n = interval.numDimensions();
		final double[] corner = new double[ n ];
		for ( int  d = 0, mask = 1; d < n; ++d, mask = mask << 1 )
			corner[ d ] = ( index & mask ) == 0 ? interval.realMin( d ) : interval.realMax( d );
		return corner;
	}

	public static void corner( final RealInterval interval, final int index, final double[] corner )
	{
		assert corner.length == interval.numDimensions();
		for ( int  d = 0, mask = 1; d < corner.length; ++d, mask = mask << 1 )
			corner[ d ] = ( index & mask ) == 0 ? interval.realMin( d ) : interval.realMax( d );
	}

	public static double[][] corners( final RealInterval interval )
	{
		final int n = interval.numDimensions();
		final int numCorners = 1 << n;
		final double[][] corners = new double[ numCorners ][ n ];
		for ( int index = 0; index < numCorners; index++ )
			for ( int d = 0, mask = 1; d < n; ++d, mask <<= 1 )
				corners[ index ][ d ] = ( index & mask ) == 0 ? interval.realMin( d ) : interval.realMax( d );
		return corners;
	}

	private IntervalCorners()
	{}
}
