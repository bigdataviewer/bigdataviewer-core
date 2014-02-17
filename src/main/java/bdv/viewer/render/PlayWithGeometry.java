package bdv.viewer.render;

import java.util.ArrayList;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class PlayWithGeometry
{
	public static void scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final RandomAccess< ? > cellsRandomAccess )
	{
//		final ArrayList< long[] > l1 = new Prefetcher6().scan( sourceToScreen, cellDimensions, dimensions, screenInterval, cellsRandomAccess );
		final ArrayList< long[] > l2 = new Prefetcher7().scan( sourceToScreen, cellDimensions, dimensions, screenInterval, cellsRandomAccess );
//		System.out.println( compare( l1, l2 ) ? "equal" : "!!!!!!!!!!!!!!!!!!!!!!!! not equal" );
	}

	private static boolean compare( final ArrayList< long[] > l1, final ArrayList< long[] > l2 )
	{
		if ( l1.size() != l2.size() )
			return false;
		for ( int i = 0; i < l1.size(); ++i )
		{
			final long[] p1 = l1.get( i );
			final long[] p2 = l2.get( i );
			for ( int d = 0; d < 3; ++d )
				if ( p1[ d ] != p2[ d ] )
					return false;
		}
		return true;
	}

	static class Prefetcher7
	{
		private final double[] xStep = new double[ 3 ];

		private final double[] offsetNeg = new double[ 3 ];

		private final double[] offsetPos = new double[ 3 ];

		private static final double eps = 0.0000001;

		public ArrayList< long[] > scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final RandomAccess< ? > cellsRandomAccess )
		{
			final RealPoint pSource = new RealPoint( 3 );
			final RealPoint pScreen = new RealPoint( 3 );
			final ArrayList< long[] > arrayList = new ArrayList< long[] >();
			final int[] minCell = new int[ 3 ];
			final int[] maxCell = new int[ 3 ];
			final int w = ( int ) screenInterval.dimension( 0 );
			final int h = ( int ) screenInterval.dimension( 1 );

			for ( int d = 0; d < 3; ++d )
				maxCell[ d ] = ( int ) ( ( dimensions[ d ] - 1 ) / cellDimensions[ d ] );

			// compute bounding box
			final RealPoint[] screenCorners = new RealPoint[ 4 ];
			screenCorners[ 0 ] = new RealPoint( 0, 0, 0 );
			screenCorners[ 1 ] = new RealPoint( w, 0, 0 );
			screenCorners[ 2 ] = new RealPoint( w, h, 0 );
			screenCorners[ 3 ] = new RealPoint( 0, h, 0 );
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

//			System.out.println( net.imglib2.util.Util.printCoordinates( numCells ) );
//			System.out.println( net.imglib2.util.Util.printCoordinates( borderSize ) );

			checkProtoCell( cellDimensions, sourceToScreen );
			getStep( cellDimensions, sourceToScreen );

//			final long[] cellGridPosition = new long[ 3 ];
//			final long[] cellMin = new long[ 3 ];

			pSource.setPosition( ( minCell[ 2 ] - 1 ) * cellDimensions[ 2 ], 2 );
			for ( cellsRandomAccess.setPosition( minCell[ 2 ], 2 ); cellsRandomAccess.getIntPosition( 2 ) <= maxCell[ 2 ]; cellsRandomAccess.fwd( 2 ) )
//			for ( cellGridPosition[ 2 ] = minCell[ 2 ]; cellGridPosition[ 2 ] <= maxCell[ 2 ]; ++cellGridPosition[ 2 ] )
			{
//				cellMin[ 2 ] = cellGridPosition[ 2 ] * cellDimensions[ 2 ];
				pSource.move( cellDimensions[ 2 ], 2 );
				pSource.setPosition( ( minCell[ 1 ] - 1 ) * cellDimensions[ 1 ], 1 );
//				cellsRandomAccess.fwd( 2 );
//				for ( cellGridPosition[ 1 ] = minCell[ 1 ]; cellGridPosition[ 1 ] <= maxCell[ 1 ]; ++cellGridPosition[ 1 ] )
				for ( cellsRandomAccess.setPosition( minCell[ 1 ], 1 ); cellsRandomAccess.getIntPosition( 1 ) <= maxCell[ 1 ]; cellsRandomAccess.fwd( 1 ) )
				{
					pSource.move( cellDimensions[ 1 ], 1 );
//					cellsRandomAccess.fwd( 1 );
//					cellMin[ 1 ] = cellGridPosition[ 1 ] * cellDimensions[ 1 ];

					// find first cell that hits z
//					cellMin[ 0 ] = minCell[ 0 ] * cellDimensions[ 0 ];
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
//						cellMin[ 0 ] = cellGridPosition[ 0 ] * cellDimensions[ 0 ];
//						pSource.setPosition( cellMin );
						sourceToScreen.apply( pSource, pScreen );
						final double x = pScreen.getDoublePosition( 0 );
						final double y = pScreen.getDoublePosition( 1 );
						if (    ( x + offsetPos[ 0 ] >= 0 ) &&
								( x + offsetNeg[ 0 ] < w ) &&
								( y + offsetPos[ 1 ] >= 0 ) &&
								( y + offsetNeg[ 1 ] < h ) )
						{
//							final long[] c = new long[] { cellsRandomAccess.getLongPosition( 0 ), cellsRandomAccess.getLongPosition( 1 ), cellsRandomAccess.getLongPosition( 2 ) };
//							arrayList.add( c );
							cellsRandomAccess.get();
						}
						pSource.move( cellDimensions[ 0 ], 0 );
					}
				}
			}
			return arrayList;
		}

		void getStep( final int[] cellStep, final AffineTransform3D sourceToScreen )
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

		void checkProtoCell( final int[] cellDims, final AffineTransform3D sourceToScreen )
		{
			final RealPoint pSource = new RealPoint( 3 );
			final RealPoint[] pScreen = new RealPoint[ 8 ];
			final long[] cellMin = new long[ 3 ];
			int i = 0;
			for ( int z = 0; z < 2; ++z )
			{
				pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellMin[ 2 ] + cellDims[ 2 ], 2 );
				for ( int y = 0; y < 2; ++y )
				{
					pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellMin[ 1 ] + cellDims[ 1 ], 1 );
					for ( int x = 0; x < 2; ++x )
					{
						pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellMin[ 0 ] + cellDims[ 0 ], 0 );
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
				offsetNeg[ d ] = min - pScreen[ 0 ].getDoublePosition( d );
				offsetPos[ d ] = max - pScreen[ 0 ].getDoublePosition( d );
			}
		}
	}



	static class Prefetcher6
	{
		final RealPoint pSource;
		final RealPoint pScreen;

		public Prefetcher6()
		{
			pSource = new RealPoint( 3 );
			pScreen = new RealPoint( 3 );
		}

		final double[] xStep = new double[ 3 ];

		final double[] offsetNeg = new double[ 3 ];

		final double[] offsetPos = new double[ 3 ];

		private static final double eps = 0.0000001;

		public ArrayList< long[] > scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final RandomAccess< ? > cellsRandomAccess )
		{
			final ArrayList< long[] > arrayList = new ArrayList< long[] >();
			final int n = dimensions.length;
			final long[] minCell = new long[ n ];
			final long[] maxCell = new long[ n ];
			final int w = ( int ) screenInterval.dimension( 0 );
			final int h = ( int ) screenInterval.dimension( 1 );

			for ( int d = 0; d < n; ++d )
				maxCell[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ];

			// compute bounding box
			final RealPoint[] screenCorners = new RealPoint[ 4 ];
			screenCorners[ 0 ] = new RealPoint( 0, 0, 0 );
			screenCorners[ 1 ] = new RealPoint( w, 0, 0 );
			screenCorners[ 2 ] = new RealPoint( w, h, 0 );
			screenCorners[ 3 ] = new RealPoint( 0, h, 0 );
			final RealPoint sourceCorner = new RealPoint( 3 );
			final double[] bbMin = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
			final double[] bbMax = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
			for ( int i = 0; i < 4; ++i )
			{
				sourceToScreen.applyInverse( sourceCorner, screenCorners[ i ] );
				for ( int d = 0; d < n; ++d )
				{
					final double p = sourceCorner.getDoublePosition( d );
					if ( p < bbMin[ d ] )
						bbMin[ d ] = p;
					if ( p > bbMax[ d ] )
						bbMax[ d ] = p;
				}
			}
			for ( int d = 0; d < n; ++d )
			{
				minCell[ d ] = Math.min( maxCell[ d ], Math.max( ( long ) bbMin[ d ] / cellDimensions[ d ] - 1, 0 ) );
				maxCell[ d ] = Math.max( 0, Math.min( ( long ) bbMax[ d ] / cellDimensions[ d ] + 1, maxCell[ d ] ) );
			}

//			System.out.println( net.imglib2.util.Util.printCoordinates( numCells ) );
//			System.out.println( net.imglib2.util.Util.printCoordinates( borderSize ) );

			checkProtoCell( cellDimensions, sourceToScreen );
			getStep( cellDimensions, sourceToScreen );

			final long[] cellGridPosition = new long[ 3 ];
			final long[] cellMin = new long[ 3 ];
			for ( cellGridPosition[ 2 ] = minCell[ 2 ]; cellGridPosition[ 2 ] <= maxCell[ 2 ]; ++cellGridPosition[ 2 ] )
			{
				cellMin[ 2 ] = cellGridPosition[ 2 ] * cellDimensions[ 2 ];
				for ( cellGridPosition[ 1 ] = minCell[ 1 ]; cellGridPosition[ 1 ] <= maxCell[ 1 ]; ++cellGridPosition[ 1 ] )
				{
					cellMin[ 1 ] = cellGridPosition[ 1 ] * cellDimensions[ 1 ];

					// find first cell that hits z
					cellMin[ 0 ] = minCell[ 0 ] * cellDimensions[ 0 ];
					pSource.setPosition( cellMin );
					sourceToScreen.apply( pSource, pScreen );
					final double z0 = pScreen.getDoublePosition( 2 );
					long nStart = 0;
					long nStop = 0;
					if ( xStep[ 2 ] > eps )
					{
						nStart = minCell[ 0 ] + Math.max( 0, ( long ) Math.ceil( - ( z0 + offsetPos[ 2 ] ) / xStep[ 2 ] ) );
						if ( nStart > maxCell[ 0 ] )
							continue;
						nStop = Math.min( maxCell[ 0 ], minCell[ 0 ] + ( long ) Math.floor( - ( z0 + offsetNeg[ 2 ] ) / xStep[ 2 ] ) );
						if ( nStop < minCell[ 0 ] )
							continue;
					}
					else if ( xStep[ 2 ] < - eps )
					{
						nStart = minCell[ 0 ] + Math.max( 0, ( long ) Math.ceil( - ( z0 + offsetNeg[ 2 ] ) / xStep[ 2 ] ) );
						if ( nStart > maxCell[ 0 ] )
							continue;
						nStop = Math.min( maxCell[ 0 ], minCell[ 0 ] + ( long ) Math.floor( - ( z0 + offsetPos[ 2 ] ) / xStep[ 2 ] ) );
						if ( nStop < minCell[ 0 ] )
							continue;
					}
					else
					{
						if ( z0 + offsetNeg[ 2 ] > 0 || z0 + offsetPos[ 2 ] < 0 )
							continue;
						nStart = 0;
						nStop = maxCell[ 0 ] - minCell[ 0 ];
					}

					for ( cellGridPosition[ 0 ] = nStart; cellGridPosition[ 0 ] <= nStop; ++cellGridPosition[ 0 ] )
					{
						cellMin[ 0 ] = cellGridPosition[ 0 ] * cellDimensions[ 0 ];
						pSource.setPosition( cellMin );
						sourceToScreen.apply( pSource, pScreen );
						final double x = pScreen.getDoublePosition( 0 );
						final double y = pScreen.getDoublePosition( 1 );
						if (    ( x + offsetPos[ 0 ] >= 0 ) &&
								( x + offsetNeg[ 0 ] < w ) &&
								( y + offsetPos[ 1 ] >= 0 ) &&
								( y + offsetNeg[ 1 ] < h ) )
						{
							final long[] c = new long[] { cellGridPosition[ 0 ], cellGridPosition[ 1 ], cellGridPosition[ 2 ] };
							arrayList.add( c );
							cellsRandomAccess.setPosition( cellGridPosition );
							cellsRandomAccess.get();
						}
					}
				}
			}
			return arrayList;
		}

		void getStep( final int[] cellStep, final AffineTransform3D sourceToScreen )
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

		void checkProtoCell( final int[] cellDims, final AffineTransform3D sourceToScreen )
		{
			final RealPoint[] pScreen = new RealPoint[ 8 ];
			final long[] cellMin = new long[ 3 ];
			int i = 0;
			for ( int z = 0; z < 2; ++z )
			{
				pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellMin[ 2 ] + cellDims[ 2 ], 2 );
				for ( int y = 0; y < 2; ++y )
				{
					pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellMin[ 1 ] + cellDims[ 1 ], 1 );
					for ( int x = 0; x < 2; ++x )
					{
						pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellMin[ 0 ] + cellDims[ 0 ], 0 );
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
				offsetNeg[ d ] = min - pScreen[ 0 ].getDoublePosition( d );
				offsetPos[ d ] = max - pScreen[ 0 ].getDoublePosition( d );
			}
		}
	}

	static class Prefetcher5
	{
		final RealPoint pSource;
		final RealPoint pScreen;

		public Prefetcher5()
		{
			pSource = new RealPoint( 3 );
			pScreen = new RealPoint( 3 );
		}

		final double[] xStep = new double[ 3 ];

		final double[] offsetNeg = new double[ 3 ];

		final double[] offsetPos = new double[ 3 ];

		private static final double eps = 0.000001;

		public ArrayList< long[] > scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final RandomAccess< ? > cellsRandomAccess )
		{
			final ArrayList< long[] > arrayList = new ArrayList< long[] >();
			final int n = dimensions.length;
			final long[] minCell = new long[ n ];
			final long[] numCells = new long[ n ];
			final int w = ( int ) screenInterval.dimension( 0 );
			final int h = ( int ) screenInterval.dimension( 1 );

			for ( int d = 0; d < n; ++d )
				numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;

			// compute bounding box
			final RealPoint[] screenCorners = new RealPoint[ 4 ];
			screenCorners[ 0 ] = new RealPoint( 0, 0, 0 );
			screenCorners[ 1 ] = new RealPoint( w, 0, 0 );
			screenCorners[ 2 ] = new RealPoint( w, h, 0 );
			screenCorners[ 3 ] = new RealPoint( 0, h, 0 );
			final RealPoint sourceCorner = new RealPoint( 3 );
			final double[] bbMin = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
			final double[] bbMax = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
			for ( int i = 0; i < 4; ++i )
			{
				sourceToScreen.applyInverse( sourceCorner, screenCorners[ i ] );
				for ( int d = 0; d < n; ++d )
				{
					final double p = sourceCorner.getDoublePosition( d );
					if ( p < bbMin[ d ] )
						bbMin[ d ] = p;
					if ( p > bbMax[ d ] )
						bbMax[ d ] = p;
				}
			}
			for ( int d = 0; d < n; ++d )
			{
				minCell[ d ] = Math.min( numCells[ d ] - 1, Math.max( ( long ) bbMin[ d ] / cellDimensions[ d ] - 1, 0 ) );
				numCells[ d ] = Math.max( 0, Math.min( ( long ) bbMax[ d ] / cellDimensions[ d ] + 2, numCells[ d ] ) );
			}

//			System.out.println( net.imglib2.util.Util.printCoordinates( numCells ) );
//			System.out.println( net.imglib2.util.Util.printCoordinates( borderSize ) );

			checkProtoCell( cellDimensions, sourceToScreen );
			getStep( cellDimensions, sourceToScreen );

			final long[] cellGridPosition = new long[ 3 ];
			final long[] cellMin = new long[ 3 ];
			for ( cellGridPosition[ 2 ] = minCell[ 2 ]; cellGridPosition[ 2 ] < numCells[ 2 ]; ++cellGridPosition[ 2 ] )
			{
				cellMin[ 2 ] = cellGridPosition[ 2 ] * cellDimensions[ 2 ];
				for ( cellGridPosition[ 1 ] = minCell[ 1 ]; cellGridPosition[ 1 ] < numCells[ 1 ]; ++cellGridPosition[ 1 ] )
				{
					cellMin[ 1 ] = cellGridPosition[ 1 ] * cellDimensions[ 1 ];

					// find first cell that hits z
					cellMin[ 0 ] = minCell[ 0 ] * cellDimensions[ 0 ];
					pSource.setPosition( cellMin );
					sourceToScreen.apply( pSource, pScreen );
					final double z0 = pScreen.getDoublePosition( 2 );
					long nStart = 0;
					long nStop = 0;
					boolean shortcut = false;
					if ( xStep[ 2 ] > eps )
					{
						nStart = ( long ) Math.ceil( - ( z0 + offsetPos[ 2 ] ) / xStep[ 2 ] );
						nStart = minCell[ 0 ] + Math.min( Math.max( 0, nStart ), numCells[ 0 ] - minCell[ 0 ] - 1 );
						nStop = ( long ) Math.floor( - ( z0 + offsetNeg[ 2 ] ) / xStep[ 2 ] );
						nStop = 1 + minCell[ 0 ] + Math.min( Math.max( 0, nStop ), numCells[ 0 ] - minCell[ 0 ] - 1 );
						shortcut = true;
					}
					else if ( xStep[ 2 ] < - eps )
					{
						nStart = ( long ) Math.ceil( - ( z0 + offsetNeg[ 2 ] ) / xStep[ 2 ] );
						nStart = minCell[ 0 ] + Math.min( Math.max( 0, nStart ), numCells[ 0 ] - minCell[ 0 ] - 1 );
						nStop = ( long ) Math.floor( - ( z0 + offsetPos[ 2 ] ) / xStep[ 2 ] );
						nStop = 1 + minCell[ 0 ] + Math.min( Math.max( 0, nStop ), numCells[ 0 ] - minCell[ 0 ] - 1 );
						shortcut = true;
					}
					else
					{
						if ( z0 + offsetNeg[ 2 ] > 0 || z0 + offsetPos[ 2 ] < 0 )
							continue;
						nStart = minCell[ 0 ];
						nStop = numCells[ 0 ];
					}
					boolean first = true;
					long nnStart = Long.MAX_VALUE;
					long nnStop = Long.MAX_VALUE;

					for ( cellGridPosition[ 0 ] = minCell[ 0 ]; cellGridPosition[ 0 ] < numCells[ 0 ]; ++cellGridPosition[ 0 ] )
					{
						cellMin[ 0 ] = cellGridPosition[ 0 ] * cellDimensions[ 0 ];
						pSource.setPosition( cellMin );
						sourceToScreen.apply( pSource, pScreen );
						final double z = pScreen.getDoublePosition( 2 );
						if ( ! ( z + offsetNeg[ 2 ] > 0 || z + offsetPos[ 2 ] < 0 ) )
						{
							if ( first )
							{
								first = false;
								nnStart = cellGridPosition[ 0 ];
							}
							nnStop = cellGridPosition[ 0 ];
							final double x = pScreen.getDoublePosition( 0 );
							final double y = pScreen.getDoublePosition( 1 );
							if (    ( x + offsetPos[ 0 ] >= 0 ) &&
									( x + offsetNeg[ 0 ] < w ) &&
									( y + offsetPos[ 1 ] >= 0 ) &&
									( y + offsetNeg[ 1 ] < h ) )
							{
	//							System.out.println( "fetch " + net.imglib2.util.Util.printCoordinates( cellGridPosition ) );
								final long[] c = new long[] { cellGridPosition[ 0 ], cellGridPosition[ 1 ], cellGridPosition[ 2 ] };
								arrayList.add( c );
								cellsRandomAccess.setPosition( cellGridPosition );
								cellsRandomAccess.get();
							}
						}
					}
					if ( shortcut && !first )
					{
						nnStop += 1;
						if ( nStart != nnStart )
							System.out.println( "nStart = " + nStart + " vs " + nnStart );
						if ( nStop != nnStop )
							System.out.println( "nStop = " + nStop + " vs " + nnStop );
						if ( nStart != nnStart || nStop != nnStop )
							System.exit( 0 );
					}
				}
			}
//			System.out.println();
			return arrayList;
		}

		void getStep( final int[] cellStep, final AffineTransform3D sourceToScreen )
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

		void checkProtoCell( final int[] cellDims, final AffineTransform3D sourceToScreen )
		{
			final RealPoint[] pScreen = new RealPoint[ 8 ];
			final long[] cellMin = new long[ 3 ];
			int i = 0;
			for ( int z = 0; z < 2; ++z )
			{
				pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellMin[ 2 ] + cellDims[ 2 ], 2 );
				for ( int y = 0; y < 2; ++y )
				{
					pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellMin[ 1 ] + cellDims[ 1 ], 1 );
					for ( int x = 0; x < 2; ++x )
					{
						pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellMin[ 0 ] + cellDims[ 0 ], 0 );
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
				offsetNeg[ d ] = min - pScreen[ 0 ].getDoublePosition( d );
				offsetPos[ d ] = max - pScreen[ 0 ].getDoublePosition( d );
			}
		}
	}

	static class Prefetcher4
	{
		final RealPoint pSource;
		final RealPoint pScreen;

		public Prefetcher4()
		{
			pSource = new RealPoint( 3 );
			pScreen = new RealPoint( 3 );
		}

		final double[] offsetNeg = new double[ 3 ];

		final double[] offsetPos = new double[ 3 ];

		public ArrayList< long[] > scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final RandomAccess< ? > cellsRandomAccess )
		{
			final ArrayList< long[] > arrayList = new ArrayList< long[] >();
			final int n = dimensions.length;
			final long[] minCell = new long[ n ];
			final long[] numCells = new long[ n ];
			final int w = ( int ) screenInterval.dimension( 0 );
			final int h = ( int ) screenInterval.dimension( 1 );

			for ( int d = 0; d < n; ++d )
				numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;

			// compute bounding box
			final RealPoint[] screenCorners = new RealPoint[ 4 ];
			screenCorners[ 0 ] = new RealPoint( 0, 0, 0 );
			screenCorners[ 1 ] = new RealPoint( w, 0, 0 );
			screenCorners[ 2 ] = new RealPoint( w, h, 0 );
			screenCorners[ 3 ] = new RealPoint( 0, h, 0 );
			final RealPoint sourceCorner = new RealPoint( 3 );
			final double[] bbMin = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
			final double[] bbMax = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
			for ( int i = 0; i < 4; ++i )
			{
				sourceToScreen.applyInverse( sourceCorner, screenCorners[ i ] );
				for ( int d = 0; d < n; ++d )
				{
					final double p = sourceCorner.getDoublePosition( d );
					if ( p < bbMin[ d ] )
						bbMin[ d ] = p;
					if ( p > bbMax[ d ] )
						bbMax[ d ] = p;
				}
			}
			for ( int d = 0; d < n; ++d )
			{
				minCell[ d ] = Math.min( numCells[ d ] - 1, Math.max( ( long ) bbMin[ d ] / cellDimensions[ d ] - 1, 0 ) );
				numCells[ d ] = Math.max( 0, Math.min( ( long ) bbMax[ d ] / cellDimensions[ d ] + 2, numCells[ d ] ) );
			}


//			System.out.println( net.imglib2.util.Util.printCoordinates( numCells ) );
//			System.out.println( net.imglib2.util.Util.printCoordinates( borderSize ) );

			checkProtoCell( cellDimensions, sourceToScreen );

			final long[] cellGridPosition = new long[ 3 ];
			final long[] cellMin = new long[ 3 ];
			for ( cellGridPosition[ 2 ] = minCell[ 2 ]; cellGridPosition[ 2 ] < numCells[ 2 ]; ++cellGridPosition[ 2 ] )
			{
				cellMin[ 2 ] = cellGridPosition[ 2 ] * cellDimensions[ 2 ];
				for ( cellGridPosition[ 1 ] = minCell[ 1 ]; cellGridPosition[ 1 ] < numCells[ 1 ]; ++cellGridPosition[ 1 ] )
				{
					cellMin[ 1 ] = cellGridPosition[ 1 ] * cellDimensions[ 1 ];
					for ( cellGridPosition[ 0 ] = minCell[ 0 ]; cellGridPosition[ 0 ] < numCells[ 0 ]; ++cellGridPosition[ 0 ] )
					{
						cellMin[ 0 ] = cellGridPosition[ 0 ] * cellDimensions[ 0 ];

						if ( checkCell( cellDimensions, cellMin, sourceToScreen, w, h ) )
						{
//							System.out.println( "fetch " + net.imglib2.util.Util.printCoordinates( cellGridPosition ) );
							final long[] c = new long[] { cellGridPosition[ 0 ], cellGridPosition[ 1 ], cellGridPosition[ 2 ] };
							arrayList.add( c );
							cellsRandomAccess.setPosition( cellGridPosition );
							cellsRandomAccess.get();
						}
					}
				}
			}
//			System.out.println();
			return arrayList;
		}

		void checkProtoCell( final int[] cellDims, final AffineTransform3D sourceToScreen )
		{
			final RealPoint[] pScreen = new RealPoint[ 8 ];
			final long[] cellMin = new long[ 3 ];
			int i = 0;
			for ( int z = 0; z < 2; ++z )
			{
				pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellMin[ 2 ] + cellDims[ 2 ], 2 );
				for ( int y = 0; y < 2; ++y )
				{
					pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellMin[ 1 ] + cellDims[ 1 ], 1 );
					for ( int x = 0; x < 2; ++x )
					{
						pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellMin[ 0 ] + cellDims[ 0 ], 0 );
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
				offsetNeg[ d ] = min - pScreen[ 0 ].getDoublePosition( d );
				offsetPos[ d ] = max - pScreen[ 0 ].getDoublePosition( d );
			}
		}

		boolean checkCell( final int[] cellDims, final long[] cellMin, final AffineTransform3D sourceToScreen, final int wScreen, final int hScreen )
		{
			pSource.setPosition( cellMin );
			sourceToScreen.apply( pSource, pScreen );
			final double x = pScreen.getDoublePosition( 0 );
			final double y = pScreen.getDoublePosition( 1 );
			final double z = pScreen.getDoublePosition( 2 );
			return  ( z + offsetNeg[ 2 ] <= 0 ) &&
					( z + offsetPos[ 2 ] >= 0 ) &&
					( x + offsetPos[ 0 ] >= 0 ) &&
					( x + offsetNeg[ 0 ] < wScreen ) &&
					( y + offsetPos[ 1 ] >= 0 ) &&
					( y + offsetNeg[ 1 ] < hScreen );
//			if ( z + OffsetPos[ 2 ] > 0 || z + OffsetNeg[ 2 ] < 0 )
//				return false;
//
//			boolean x0 = false;
//			boolean x1 = false;
//			boolean y0 = false;
//			boolean y1 = false;
//			if ( x + OffsetNeg[ 0 ] >= 0 )
//				x0 = true;
//			if ( x + OffsetPos[ 0 ] < wScreen )
//				x1 = true;
//			if ( y + OffsetNeg[ 1 ] >= 0 )
//				y0 = true;
//			if ( y + OffsetPos[ 1 ] < hScreen )
//				y1 = true;
//			return x0 && x1 && y0 && y1;
		}
	}

	static class Prefetcher3
	{
		final RealPoint pSource;
		final RealPoint pScreen;

		public Prefetcher3()
		{
			pSource = new RealPoint( 3 );
			pScreen = new RealPoint( 3 );
		}

		final double[] offsetNeg = new double[ 3 ];

		final double[] offsetPos = new double[ 3 ];

		public ArrayList< long[] > scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final RandomAccess< ? > cellsRandomAccess )
		{
			final ArrayList< long[] > arrayList = new ArrayList< long[] >();
			final int n = dimensions.length;
			final long[] numCells = new long[ n ];
			final int[] borderSize = new int[ n ];
			final int wScreen = ( int ) screenInterval.dimension( 0 );
			final int hScreen = ( int ) screenInterval.dimension( 1 );

			for ( int d = 0; d < n; ++d )
			{
				numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
				borderSize[ d ] = ( int ) ( dimensions[ d ] - ( numCells[ d ] - 1 ) * cellDimensions[ d ] );
			}

//			System.out.println( net.imglib2.util.Util.printCoordinates( numCells ) );
//			System.out.println( net.imglib2.util.Util.printCoordinates( borderSize ) );

			checkProtoCell( cellDimensions, sourceToScreen );

			final long[] cellGridPosition = new long[ 3 ];
			final long[] cellMin = new long[ 3 ];
			for ( cellGridPosition[ 2 ] = 0; cellGridPosition[ 2 ] < numCells[ 2 ]; ++cellGridPosition[ 2 ] )
			{
				cellMin[ 2 ] = cellGridPosition[ 2 ] * cellDimensions[ 2 ];
				for ( cellGridPosition[ 1 ] = 0; cellGridPosition[ 1 ] < numCells[ 1 ]; ++cellGridPosition[ 1 ] )
				{
					cellMin[ 1 ] = cellGridPosition[ 1 ] * cellDimensions[ 1 ];
					for ( cellGridPosition[ 0 ] = 0; cellGridPosition[ 0 ] < numCells[ 0 ]; ++cellGridPosition[ 0 ] )
					{
						cellMin[ 0 ] = cellGridPosition[ 0 ] * cellDimensions[ 0 ];

						if ( checkCell( cellDimensions, cellMin, sourceToScreen, wScreen, hScreen ) )
						{
//							System.out.println( "fetch " + net.imglib2.util.Util.printCoordinates( cellGridPosition ) );
							final long[] c = new long[] { cellGridPosition[ 0 ], cellGridPosition[ 1 ], cellGridPosition[ 2 ] };
							arrayList.add( c );
							cellsRandomAccess.setPosition( cellGridPosition );
							cellsRandomAccess.get();
						}
					}
				}
			}
//			System.out.println();
			return arrayList;
		}

		void checkProtoCell( final int[] cellDims, final AffineTransform3D sourceToScreen )
		{
			final RealPoint[] pScreen = new RealPoint[ 8 ];
			final long[] cellMin = new long[ 3 ];
			int i = 0;
			for ( int z = 0; z < 2; ++z )
			{
				pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellMin[ 2 ] + cellDims[ 2 ], 2 );
				for ( int y = 0; y < 2; ++y )
				{
					pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellMin[ 1 ] + cellDims[ 1 ], 1 );
					for ( int x = 0; x < 2; ++x )
					{
						pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellMin[ 0 ] + cellDims[ 0 ], 0 );
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
				offsetNeg[ d ] = min - pScreen[ 0 ].getDoublePosition( d );
				offsetPos[ d ] = max - pScreen[ 0 ].getDoublePosition( d );
			}
		}

		boolean checkCell( final int[] cellDims, final long[] cellMin, final AffineTransform3D sourceToScreen, final int wScreen, final int hScreen )
		{
			pSource.setPosition( cellMin );
			sourceToScreen.apply( pSource, pScreen );
			final double x = pScreen.getDoublePosition( 0 );
			final double y = pScreen.getDoublePosition( 1 );
			final double z = pScreen.getDoublePosition( 2 );
			return  ( z + offsetNeg[ 2 ] <= 0 ) &&
					( z + offsetPos[ 2 ] >= 0 ) &&
					( x + offsetPos[ 0 ] >= 0 ) &&
					( x + offsetNeg[ 0 ] < wScreen ) &&
					( y + offsetPos[ 1 ] >= 0 ) &&
					( y + offsetNeg[ 1 ] < hScreen );
//			if ( z + OffsetPos[ 2 ] > 0 || z + OffsetNeg[ 2 ] < 0 )
//				return false;
//
//			boolean x0 = false;
//			boolean x1 = false;
//			boolean y0 = false;
//			boolean y1 = false;
//			if ( x + OffsetNeg[ 0 ] >= 0 )
//				x0 = true;
//			if ( x + OffsetPos[ 0 ] < wScreen )
//				x1 = true;
//			if ( y + OffsetNeg[ 1 ] >= 0 )
//				y0 = true;
//			if ( y + OffsetPos[ 1 ] < hScreen )
//				y1 = true;
//			return x0 && x1 && y0 && y1;
		}
	}

	static class Prefetcher2
	{
		final RealPoint pSource;
		final RealPoint[] pScreen;

		public Prefetcher2()
		{
			pSource = new RealPoint( 3 );
			pScreen = new RealPoint[ 8 ];
			for ( int i = 0; i < 8; ++i )
			{
				pScreen[ i ] = new RealPoint( 3 );
			}
		}

		public ArrayList< long[] > scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final RandomAccess< ? > cellsRandomAccess )
		{
			final ArrayList< long[] > arrayList = new ArrayList< long[] >();
			final int n = dimensions.length;
			final long[] numCells = new long[ n ];
			final int[] borderSize = new int[ n ];
			final int wScreen = ( int ) screenInterval.dimension( 0 );
			final int hScreen = ( int ) screenInterval.dimension( 1 );

			for ( int d = 0; d < n; ++d )
			{
				numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
				borderSize[ d ] = ( int ) ( dimensions[ d ] - ( numCells[ d ] - 1 ) * cellDimensions[ d ] );
			}

//			System.out.println( net.imglib2.util.Util.printCoordinates( numCells ) );
//			System.out.println( net.imglib2.util.Util.printCoordinates( borderSize ) );

			final long[] cellGridPosition = new long[ 3 ];
			final long[] cellMin = new long[ 3 ];
			for ( cellGridPosition[ 2 ] = 0; cellGridPosition[ 2 ] < numCells[ 2 ]; ++cellGridPosition[ 2 ] )
			{
				cellMin[ 2 ] = cellGridPosition[ 2 ] * cellDimensions[ 2 ];
				for ( cellGridPosition[ 1 ] = 0; cellGridPosition[ 1 ] < numCells[ 1 ]; ++cellGridPosition[ 1 ] )
				{
					cellMin[ 1 ] = cellGridPosition[ 1 ] * cellDimensions[ 1 ];
					for ( cellGridPosition[ 0 ] = 0; cellGridPosition[ 0 ] < numCells[ 0 ]; ++cellGridPosition[ 0 ] )
					{
						cellMin[ 0 ] = cellGridPosition[ 0 ] * cellDimensions[ 0 ];

						if ( checkCell( cellDimensions, cellMin, sourceToScreen, wScreen, hScreen ) )
						{
//							System.out.println( "fetch " + net.imglib2.util.Util.printCoordinates( cellGridPosition ) );
							final long[] c = new long[] { cellGridPosition[ 0 ], cellGridPosition[ 1 ], cellGridPosition[ 2 ] };
							arrayList.add( c );
							cellsRandomAccess.setPosition( cellGridPosition );
							cellsRandomAccess.get();
						}
					}
				}
			}
//			System.out.println();
			return arrayList;
		}

		boolean checkCell( final int[] cellDims, final long[] cellMin, final AffineTransform3D sourceToScreen, final int wScreen, final int hScreen )
		{
			int i = 0;
			for ( int z = 0; z < 2; ++z )
			{
				pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellMin[ 2 ] + cellDims[ 2 ], 2 );
				for ( int y = 0; y < 2; ++y )
				{
					pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellMin[ 1 ] + cellDims[ 1 ], 1 );
					for ( int x = 0; x < 2; ++x )
					{
						pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellMin[ 0 ] + cellDims[ 0 ], 0 );
						sourceToScreen.apply( pSource, pScreen[ i++ ] );
					}
				}
			}

			boolean screenPositive = false;
			boolean screenNegative = false;
			for ( i = 0; i < 8; ++i )
			{
				final double zScreen = pScreen[ i ].getDoublePosition( 2 );
				if ( zScreen >= 0 )
					screenPositive = true;
				if ( zScreen <= 0 )
					screenNegative = true;
			}
			if ( screenPositive && screenNegative )
			{
				boolean x0 = false;
				boolean x1 = false;
				boolean y0 = false;
				boolean y1 = false;
				for ( i = 0; i < 8; ++i )
				{
					final double x = pScreen[ i ].getDoublePosition( 0 );
					final double y = pScreen[ i ].getDoublePosition( 1 );
					if ( x >= 0 )
						x0 = true;
					if ( x < wScreen )
						x1 = true;
					if ( y >= 0 )
						y0 = true;
					if ( y < hScreen )
						y1 = true;
				}
				return x0 && x1 && y0 && y1;
			}
			else
				return false;
		}
	}

	static class Prefetcher1
	{
		final RealPoint pSource;
		final RealPoint[] pScreen;

		public Prefetcher1()
		{
			pSource = new RealPoint( 3 );
			pScreen = new RealPoint[ 8 ];
			for ( int i = 0; i < 8; ++i )
			{
				pScreen[ i ] = new RealPoint( 3 );
			}
		}

		public ArrayList< long[] > scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final RandomAccess< ? > cellsRandomAccess )
		{
			final ArrayList< long[] > arrayList = new ArrayList< long[] >();
			final int n = dimensions.length;
			final long[] numCells = new long[ n ];
			final int[] borderSize = new int[ n ];
			final int wScreen = ( int ) screenInterval.dimension( 0 );
			final int hScreen = ( int ) screenInterval.dimension( 1 );

			for ( int d = 0; d < n; ++d )
			{
				numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
				borderSize[ d ] = ( int ) ( dimensions[ d ] - ( numCells[ d ] - 1 ) * cellDimensions[ d ] );
			}

//			System.out.println( net.imglib2.util.Util.printCoordinates( numCells ) );
//			System.out.println( net.imglib2.util.Util.printCoordinates( borderSize ) );

			final long[] cellGridPosition = new long[ 3 ];
			final int[] cellDims = new int[ 3 ];
			final long[] cellMin = new long[ 3 ];
			for ( cellGridPosition[ 2 ] = 0; cellGridPosition[ 2 ] < numCells[ 2 ]; ++cellGridPosition[ 2 ] )
			{
				cellDims[ 2 ] = ( ( cellGridPosition[ 2 ] + 1 == numCells[ 2 ] ) ? borderSize[ 2 ] : cellDimensions[ 2 ] );
				cellMin[ 2 ] = cellGridPosition[ 2 ] * cellDimensions[ 2 ];
				for ( cellGridPosition[ 1 ] = 0; cellGridPosition[ 1 ] < numCells[ 1 ]; ++cellGridPosition[ 1 ] )
				{
					cellDims[ 1 ] = ( ( cellGridPosition[ 1 ] + 1 == numCells[ 1 ] ) ? borderSize[ 1 ] : cellDimensions[ 1 ] );
					cellMin[ 1 ] = cellGridPosition[ 1 ] * cellDimensions[ 1 ];
					for ( cellGridPosition[ 0 ] = 0; cellGridPosition[ 0 ] < numCells[ 0 ]; ++cellGridPosition[ 0 ] )
					{
						cellDims[ 0 ] = ( ( cellGridPosition[ 0 ] + 1 == numCells[ 0 ] ) ? borderSize[ 0 ] : cellDimensions[ 0 ] );
						cellMin[ 0 ] = cellGridPosition[ 0 ] * cellDimensions[ 0 ];

						if ( checkCell( cellDims, cellMin, sourceToScreen, wScreen, hScreen ) )
						{
//							System.out.println( "fetch " + net.imglib2.util.Util.printCoordinates( cellGridPosition ) );
							final long[] c = new long[] { cellGridPosition[ 0 ], cellGridPosition[ 1 ], cellGridPosition[ 2 ] };
							arrayList.add( c );
							cellsRandomAccess.setPosition( cellGridPosition );
							cellsRandomAccess.get();
						}
					}
				}
			}
//			System.out.println();
			return arrayList;
		}

		boolean checkCell( final int[] cellDims, final long[] cellMin, final AffineTransform3D sourceToScreen, final int wScreen, final int hScreen )
		{
			int i = 0;
			for ( int z = 0; z < 2; ++z )
			{
				pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellMin[ 2 ] + cellDims[ 2 ], 2 );
				for ( int y = 0; y < 2; ++y )
				{
					pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellMin[ 1 ] + cellDims[ 1 ], 1 );
					for ( int x = 0; x < 2; ++x )
					{
						pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellMin[ 0 ] + cellDims[ 0 ], 0 );
						sourceToScreen.apply( pSource, pScreen[ i++ ] );
					}
				}
			}

			boolean screenPositive = false;
			boolean screenNegative = false;
			for ( i = 0; i < 8; ++i )
			{
				final double zScreen = pScreen[ i ].getDoublePosition( 2 );
				if ( zScreen >= 0 )
					screenPositive = true;
				if ( zScreen <= 0 )
					screenNegative = true;
			}
			if ( screenPositive && screenNegative )
			{
				boolean x0 = false;
				boolean x1 = false;
				boolean y0 = false;
				boolean y1 = false;
				for ( i = 0; i < 8; ++i )
				{
					final double x = pScreen[ i ].getDoublePosition( 0 );
					final double y = pScreen[ i ].getDoublePosition( 1 );
					if ( x >= 0 )
						x0 = true;
					if ( x < wScreen )
						x1 = true;
					if ( y >= 0 )
						y0 = true;
					if ( y < hScreen )
						y1 = true;
				}
				return x0 && x1 && y0 && y1;
			}
			else
				return false;
		}
	}
}
