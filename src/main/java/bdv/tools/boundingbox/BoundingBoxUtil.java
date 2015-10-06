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
package bdv.tools.boundingbox;

import java.util.ArrayList;
import java.util.Collection;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;

public class BoundingBoxUtil
{
	public static Interval getSourcesBoundingBox( final ViewerState state )
	{
		return getSourcesBoundingBox( state, 0, state.getNumTimePoints() - 1 );
	}

	public static Interval getSourcesBoundingBox( final ViewerState state, final int minTimepointIndex, final int maxTimepointIndex )
	{
		final ArrayList< Source< ? > > sources = new ArrayList< Source< ? > >();
		for ( final SourceState< ? > source : state.getSources() )
			sources.add( source.getSpimSource() );
		return getSourcesBoundingBox( sources, minTimepointIndex, maxTimepointIndex );
	}

	public static Interval getSourcesBoundingBox( final Collection< ? extends Source< ? > > sources, final int minTimepointIndex, final int maxTimepointIndex )
	{
		final long[] bbMin = new long[] { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
		final long[] bbMax = new long[] { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
		final long[] min = new long[ 3 ];
		final long[] max = new long[ 3 ];
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		final RealPoint sourceCorner = new RealPoint( 3 );
		final RealPoint globalCorner = new RealPoint( 3 );
		for ( final Source< ? > source : sources )
		{
			for ( int t = minTimepointIndex; t <= maxTimepointIndex; ++t )
			{
				if ( !source.isPresent( t ) )
					continue;
				final Interval sourceInterval = source.getSource( t, 0 );
				sourceInterval.min( min );
				sourceInterval.max( max );
				source.getSourceTransform( 0, 0, sourceTransform );

				for ( int i = 0; i < 8; ++i )
				{
					for ( int d = 0; d < 3; ++d )
					{
						final double p = ( i & ( 1 << d ) ) == 0 ? min[ d ] : max[ d ];
						sourceCorner.setPosition( p, d );
					}
					sourceTransform.apply( sourceCorner, globalCorner );
					for ( int d = 0; d < 3; ++d )
					{
						final long p = ( long ) globalCorner.getDoublePosition( d );
						if ( p < bbMin[ d ] )
							bbMin[ d ] = p;
						if ( p > bbMax[ d ] )
							bbMax[ d ] = p;
					}
				}
			}
		}
		return new FinalInterval( bbMin, bbMax );
	}
}
