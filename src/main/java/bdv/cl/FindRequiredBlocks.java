package bdv.cl;

import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.algorithm.kdtree.ConvexPolytope;
import net.imglib2.algorithm.kdtree.HyperPlane;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import bdv.util.Affine3DHelpers;

public class FindRequiredBlocks
{
	public static class RequiredBlocks
	{
		public final ArrayList< int[] > cellPositions;

		public final int[] minCell;

		public final int[] maxCell;

		public RequiredBlocks( final ArrayList< int[] > cellPositions, final int[] minCell, final int[] maxCell )
		{
			this.cellPositions = cellPositions;
			this.minCell = minCell;
			this.maxCell = maxCell;
		}
	}

	public static RequiredBlocks getRequiredBlocks(
			final AffineTransform3D sourceToScreen,
			final int w,
			final int h,
			final int dd,
			final int[] cellDimensions,
			final long[] imgDimensions )
	{
		final ArrayList< int[] > requiredCells = new ArrayList< int[] >();

		// bounding box in source coordinates...
		final RealPoint[] screenCorners = new RealPoint[ 8 ];
		screenCorners[ 0 ] = new RealPoint( 0.0, 0.0, 0.0 );
		screenCorners[ 1 ] = new RealPoint(   w, 0.0, 0.0 );
		screenCorners[ 2 ] = new RealPoint(   w,   h, 0.0 );
		screenCorners[ 3 ] = new RealPoint( 0.0,   h, 0.0 );
		screenCorners[ 4 ] = new RealPoint( 0.0, 0.0,  dd );
		screenCorners[ 5 ] = new RealPoint(   w, 0.0,  dd );
		screenCorners[ 6 ] = new RealPoint(   w,   h,  dd );
		screenCorners[ 7 ] = new RealPoint( 0.0,   h,  dd );
		final RealPoint sourceCorner = new RealPoint( 3 );
		final double[] bbMin = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		final double[] bbMax = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
		for ( int i = 0; i < screenCorners.length; ++i )
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

		// bounding box of potentially contained cells in cell grid coordinates
		final int[] minCell = new int[ 3 ];
		final int[] maxCell = new int[ 3 ];
		for ( int d = 0; d < 3; ++d )
			maxCell[ d ] = ( int ) ( ( imgDimensions[ d ] - 1 ) / cellDimensions[ d ] );
		for ( int d = 0; d < 3; ++d )
		{
			minCell[ d ] = Math.min( maxCell[ d ], Math.max( ( int ) bbMin[ d ] / cellDimensions[ d ] - 1, 0 ) );
			maxCell[ d ] = Math.max( 0, Math.min( ( int ) bbMax[ d ] / cellDimensions[ d ] + 1, maxCell[ d ] ) );
		}

		// planes bounding the volume, normals facing inwards...
		final ConvexPolytope sourceRegion = Affine3DHelpers.inverseTransform( new ConvexPolytope(
				new HyperPlane(  0,  0,  1,   0 ),
				new HyperPlane(  0,  0, -1, -dd ),
				new HyperPlane(  1,  0,  0,   0 ),
				new HyperPlane( -1,  0,  0,  -w ),
				new HyperPlane(  0,  1,  0,   0 ),
				new HyperPlane(  0, -1,  0,  -h ) ), sourceToScreen );
		final HyperPlane[] sourcePlanes = sourceRegion.getHyperplanes().toArray( new HyperPlane[0] );

		// shift planes such that we only have to check the (0,0,0) corner of cells
		final double[] cellBBMin = new double[ 3 ];
		final double[] cellBBMax = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			cellBBMin[ d ] = -0.5;
			cellBBMax[ d ] = cellDimensions[ d ] - 0.5;
		}
		final double[] offset = new double[ 3 ];
		for ( int i = 0; i < sourcePlanes.length; ++i )
		{
			final double[] nn = sourcePlanes[ i ].getNormal();
			final double m = sourcePlanes[ i ].getDistance();
			for ( int d = 0; d < 3; ++d )
				offset[ d ] = ( nn[ d ] < 0 ) ? cellBBMin[ d ] : cellBBMax[ d ];
			sourcePlanes[ i ] = new HyperPlane( nn, m - LinAlgHelpers.dot( nn, offset ) );
		}

		// stupid implementation for now: check all planes for all cells...
		// TODO: make this more clever
		final IntervalIterator cellsIter = new IntervalIterator( minCell, maxCell );
A:		while( cellsIter.hasNext() )
		{
			cellsIter.fwd();
			final double[] pos = new double[ 3 ];
			cellsIter.localize( pos );
			for ( int d = 0; d < 3; ++d )
				pos[ d ] *= cellDimensions[ d ];
			for ( int i = 0; i < sourcePlanes.length; ++i )
			{
				final HyperPlane plane = sourcePlanes[ i ];
				if ( LinAlgHelpers.dot( pos, plane.getNormal() ) < plane.getDistance() )
					continue A;
			}

			final int[] cellPos = new int[ 3 ];
			cellsIter.localize( cellPos );
			requiredCells.add( cellPos );
		}

		return new RequiredBlocks( requiredCells, minCell, maxCell );
	}
}
