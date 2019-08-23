/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package bdv.export;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhoodFactory;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhoodUnsafe;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class Downsample
{
	/**
	 * TODO: Revise. This is probably not very efficient
	 */
	public static < T extends RealType< T > > void downsample( final RandomAccessible< T > input, final RandomAccessibleInterval< T > output, final int[] factor )
	{
		assert input.numDimensions() == output.numDimensions();
		assert input.numDimensions() == factor.length;

		final int n = input.numDimensions();
		final RectangleNeighborhoodFactory< T > f = RectangleNeighborhoodUnsafe.< T >factory();
		final long[] dim = new long[ n ];
		for ( int d = 0; d < n; ++d )
			dim[ d ] = factor[ d ];
		final Interval spanInterval = new FinalInterval( dim );

		final long[] minRequiredInput = new long[ n ];
		final long[] maxRequiredInput = new long[ n ];
		output.min( minRequiredInput );
		output.max( maxRequiredInput );
		for ( int d = 0; d < n; ++d )
		{
			minRequiredInput[ d ] *= factor[ d ];
			maxRequiredInput[ d ] *= factor[ d ];
			maxRequiredInput[ d ] += factor[ d ] - 1;
		}
		final RandomAccessibleInterval< T > requiredInput = Views.interval( input, new FinalInterval( minRequiredInput, maxRequiredInput ) );

		final RectangleShape.NeighborhoodsAccessible< T > neighborhoods = new RectangleShape.NeighborhoodsAccessible<>( requiredInput, spanInterval, f );
		final RandomAccess< Neighborhood< T > > block = neighborhoods.randomAccess();

		long size = 1;
		for ( int d = 0; d < n; ++d )
			size *= factor[ d ];
		final double scale = 1.0 / size;

		final Cursor< T > out = Views.iterable( output ).localizingCursor();
		while( out.hasNext() )
		{
			final T o = out.next();
			for ( int d = 0; d < n; ++d )
				block.setPosition( out.getLongPosition( d ) * factor[ d ], d );
			double sum = 0;
			for ( final T i : block.get() )
				sum += i.getRealDouble();
			o.setReal( sum * scale );
		}
	}
}
