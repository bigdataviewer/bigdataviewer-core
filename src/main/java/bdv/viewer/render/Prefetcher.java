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
package bdv.viewer.render;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.Interpolation;

public class Prefetcher
{
	/**
	 * Access cells that will be needed for rendering to the screen.
	 *
	 * @param sourceToScreen
	 *            source-to-screen transform
	 * @param cellDimensions
	 *            standard size of a source cell
	 * @param dimensions
	 *            dimensions of the source {@link CellImg}
	 * @param screenInterval
	 *            the interval of the screen that will be rendered
	 * @param interpolation
	 *            the interpolation method
	 * @param cellsRandomAccess
	 *            access to the source cells
	 */
	public static void fetchCells( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval,  final Interpolation interpolation, final RandomAccess< ? > cellsRandomAccess )
	{
		new Prefetcher().scan( sourceToScreen, cellDimensions, dimensions, screenInterval, interpolation, cellsRandomAccess );
	}

	private Prefetcher()
	{}

	/**
	 * The transformed vector in screen coordinate when moving by by one cell in
	 * X direction.
	 */
	private final double[] xStep = new double[ 3 ];

	/**
	 * The min of the bounding box of a cell under the current transform and
	 * interpolation method, as seen from the min corner of the cell.
	 */
	private final double[] offsetNeg = new double[ 3 ];

	/**
	 * The max of the bounding box of a cell under the current transform and
	 * interpolation method, as seen from the min corner of the cell.
	 */
	private final double[] offsetPos = new double[ 3 ];

	private static final double eps = 0.0000001;

	/**
	 * Access cells that will be needed for rendering to the screen.
	 *
	 * @param sourceToScreen
	 *            source-to-screen transform
	 * @param cellDimensions
	 *            standard size of a source cell
	 * @param dimensions
	 *            dimensions of the source {@link CellImg}
	 * @param screenInterval
	 *            the interval of the screen that will be rendered
	 * @param interpolation
	 *            the interpolation method
	 * @param cellsRandomAccess
	 *            access to the source cells
	 */
	private void scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final Interpolation interpolation, final RandomAccess< ? > cellsRandomAccess )
	{
		final RealPoint pSource = new RealPoint( 3 );
		final RealPoint pScreen = new RealPoint( 3 );
		final int[] minCell = new int[ 3 ];
		final int[] maxCell = new int[ 3 ];
		final int w = ( int ) screenInterval.dimension( 0 );
		final int h = ( int ) screenInterval.dimension( 1 );

		for ( int d = 0; d < 3; ++d )
			maxCell[ d ] = ( int ) ( ( dimensions[ d ] - 1 ) / cellDimensions[ d ] );

		// compute bounding box
		final RealPoint[] screenCorners = new RealPoint[ 4 ];
		screenCorners[ 0 ] = new RealPoint( 0d, 0d, 0d );
		screenCorners[ 1 ] = new RealPoint( w, 0d, 0d );
		screenCorners[ 2 ] = new RealPoint( w, h, 0d );
		screenCorners[ 3 ] = new RealPoint( 0d, h, 0d );
		final RealPoint sourceCorner = new RealPoint( 3 );
		final double[] bbMin = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		final double[] bbMax = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
		for ( int i = 0; i < 4; ++i )
		{
			sourceToScreen.applyInverse( sourceCorner, screenCorners[ i ] );
			for ( int d = 0; d < 3; ++d )
			{
				final double p = sourceCorner.getDoublePosition( d );
				if ( p < bbMin[ d ] )
					bbMin[ d ] = p;
				if ( p > bbMax[ d ] )
					bbMax[ d ] = p;
			}
		}
		for ( int d = 0; d < 3; ++d )
		{
			minCell[ d ] = Math.min( maxCell[ d ], Math.max( ( int ) bbMin[ d ] / cellDimensions[ d ] - 1, 0 ) );
			maxCell[ d ] = Math.max( 0, Math.min( ( int ) bbMax[ d ] / cellDimensions[ d ] + 1, maxCell[ d ] ) );
		}

		checkProtoCell( cellDimensions, sourceToScreen, interpolation );
		getXStep( cellDimensions, sourceToScreen );

		pSource.setPosition( ( minCell[ 2 ] - 1 ) * cellDimensions[ 2 ], 2 );
		for ( cellsRandomAccess.setPosition( minCell[ 2 ], 2 ); cellsRandomAccess.getIntPosition( 2 ) <= maxCell[ 2 ]; cellsRandomAccess.fwd( 2 ) )
		{
			pSource.move( cellDimensions[ 2 ], 2 );
			pSource.setPosition( ( minCell[ 1 ] - 1 ) * cellDimensions[ 1 ], 1 );
			for ( cellsRandomAccess.setPosition( minCell[ 1 ], 1 ); cellsRandomAccess.getIntPosition( 1 ) <= maxCell[ 1 ]; cellsRandomAccess.fwd( 1 ) )
			{
				pSource.move( cellDimensions[ 1 ], 1 );

				// find first and last cell that hits z
				pSource.setPosition( minCell[ 0 ] * cellDimensions[ 0 ], 0 );
				sourceToScreen.apply( pSource, pScreen );
				final double z0 = pScreen.getDoublePosition( 2 );
				int nStart = 0;
				int nStop = 0;
				if ( xStep[ 2 ] > eps )
				{
					nStart = minCell[ 0 ] + Math.max( 0, ( int ) Math.ceil( - ( z0 + offsetPos[ 2 ] ) / xStep[ 2 ] ) );
					if ( nStart > maxCell[ 0 ] )
						continue;
					nStop = Math.min( maxCell[ 0 ], minCell[ 0 ] + ( int ) Math.floor( - ( z0 + offsetNeg[ 2 ] ) / xStep[ 2 ] ) );
					if ( nStop < minCell[ 0 ] )
						continue;
				}
				else if ( xStep[ 2 ] < - eps )
				{
					nStart = minCell[ 0 ] + Math.max( 0, ( int ) Math.ceil( - ( z0 + offsetNeg[ 2 ] ) / xStep[ 2 ] ) );
					if ( nStart > maxCell[ 0 ] )
						continue;
					nStop = Math.min( maxCell[ 0 ], minCell[ 0 ] + ( int ) Math.floor( - ( z0 + offsetPos[ 2 ] ) / xStep[ 2 ] ) );
					if ( nStop < minCell[ 0 ] )
						continue;
				}
				else
				{
					if ( z0 + offsetNeg[ 2 ] > 0 || z0 + offsetPos[ 2 ] < 0 )
						continue;
					nStart = minCell[ 0 ];
					nStop = maxCell[ 0 ];
				}

				pSource.setPosition( nStart * cellDimensions[ 0 ], 0 );
				for ( cellsRandomAccess.setPosition( nStart, 0 ); cellsRandomAccess.getIntPosition( 0 ) <= nStop; cellsRandomAccess.fwd( 0 ) )
				{
					sourceToScreen.apply( pSource, pScreen );
					final double x = pScreen.getDoublePosition( 0 );
					final double y = pScreen.getDoublePosition( 1 );
					if (    ( x + offsetPos[ 0 ] >= 0 ) &&
							( x + offsetNeg[ 0 ] < w ) &&
							( y + offsetPos[ 1 ] >= 0 ) &&
							( y + offsetNeg[ 1 ] < h ) )
					{
						cellsRandomAccess.get();
					}
					pSource.move( cellDimensions[ 0 ], 0 );
				}
			}
		}
	}

	/**
	 * Get the transformed vector in screen coordinate when moving by
	 * cellStep[0] in X direction.
	 */
	private void getXStep( final int[] cellStep, final AffineTransform3D sourceToScreen )
	{
		final RealPoint p0 = new RealPoint( 3 );
		final RealPoint p1 = new RealPoint( 3 );
		p1.setPosition( cellStep[ 0 ], 0 );
		final RealPoint s0 = new RealPoint( 3 );
		final RealPoint s1 = new RealPoint( 3 );
		sourceToScreen.apply( p0, s0 );
		sourceToScreen.apply( p1, s1 );
		for ( int d = 0; d < 3; ++d )
			xStep[ d ] = s1.getDoublePosition( d ) - s0.getDoublePosition( d );
	}

	/**
	 * Get the bounding box of a cell under the current transform and
	 * interpolation method. Set {@link #offsetNeg} and {@link #offsetPos} as
	 * seen from the min corner of the cell.
	 *
	 * <p>
	 * The box <em>(0,0,0)-cellDims</em> is projected to screen coordinates
	 * (padded for interpolation). The bounding box in screen coordinates with
	 * respect to the projected cell origin <em>(0,0,0)</em> is computed and
	 * stored in {@link #offsetNeg} and {@link #offsetPos}.
	 */
	private void checkProtoCell( final int[] cellDims, final AffineTransform3D sourceToScreen, final Interpolation interpolation )
	{
		final RealPoint pSource = new RealPoint( 3 );
		final RealPoint pScreenAnchor = new RealPoint( 3 );
		sourceToScreen.apply( pSource, pScreenAnchor );

		final RealPoint[] pScreen = new RealPoint[ 8 ];
		final double[] cellMin = new double[ 3 ];
		final double[] cellSize = new double[] { cellDims[ 0 ], cellDims[ 1 ], cellDims[ 2 ] };
		if ( interpolation == Interpolation.NEARESTNEIGHBOR )
		{
			for ( int d = 0; d < 3; ++d )
			{
				cellMin[ d ] -= 0.5;
				cellSize[ d ] -= 0.5;
			}
		}
		else // Interpolation.NLINEAR
		{
			for ( int d = 0; d < 3; ++d )
				cellMin[ d ] -= 1;
		}
		int i = 0;
		for ( int z = 0; z < 2; ++z )
		{
			pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellSize[ 2 ], 2 );
			for ( int y = 0; y < 2; ++y )
			{
				pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellSize[ 1 ], 1 );
				for ( int x = 0; x < 2; ++x )
				{
					pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellSize[ 0 ], 0 );
					pScreen[ i ] = new RealPoint( 3 );
					sourceToScreen.apply( pSource, pScreen[ i++ ] );
				}
			}
		}

		for ( int d = 0; d < 3; ++d )
		{
			double min = pScreen[ 0 ].getDoublePosition( d );
			double max = pScreen[ 0 ].getDoublePosition( d );
			for ( i = 1; i < 8; ++i )
			{
				final double p = pScreen[ i ].getDoublePosition( d );
				if ( p < min )
					min = p;
				if ( p > max )
					max = p;
			}
			offsetNeg[ d ] = min - pScreenAnchor.getDoublePosition( d );
			offsetPos[ d ] = max - pScreenAnchor.getDoublePosition( d );
		}
	}
}
