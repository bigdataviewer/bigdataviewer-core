package bdv.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;

/**
 * Propose number of mipmap levels, as well subsampling factors and chunk size
 * for each level.
 *
 * <p>
 * Choice of proposed chunksize is not based on any hard benchmark data
 * currently. Chunksize is set as either 16x16x16 or 32x32x4 depending on which
 * one is closer to isotropic. It is very likely that more efficient choices can
 * be found by manual tuning, depending on hardware and use case.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class ProposeMipmaps
{
	/**
	 * Propose number of mipmap levels as well subsampling factors and chunk
	 * size for each level, for each setup of the given sequence.
	 *
	 * @param seq
	 * @return map from setup id to proposed mipmap settings
	 */
	public static Map< Integer, ExportMipmapInfo > proposeMipmaps( final AbstractSequenceDescription< ?, ?, ? > seq )
	{
		final HashMap< Integer, ExportMipmapInfo > perSetupExportMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			perSetupExportMipmapInfo.put( setup.getId(), proposeMipmaps( setup ) );
		return perSetupExportMipmapInfo;
	}

	/**
	 * Propose number of mipmap levels as well subsampling factors and chunk
	 * size for each level, based on the image and voxel size of the given
	 * setup.
	 *
	 * @param setup
	 * @return proposed mipmap settings
	 */
	public static ExportMipmapInfo proposeMipmaps( final BasicViewSetup setup )
	{
		final VoxelDimensions voxelSize = setup.getVoxelSize();
		final double[] voxelScale = new double[ 3 ];
		voxelSize.dimensions( voxelScale );
		normalizeVoxelSize( voxelScale );

		final int[] res = new int[] { 1, 1, 1 };
		final long[] size = new long[ 3 ];

		final ArrayList< int[] > resolutions = new ArrayList< int[] >();
		final ArrayList< int[] > subdivisions = new ArrayList< int[] >();

//		for ( int level = 0;; ++level )
		while( true )
		{
			resolutions.add( res.clone() );

			double vmax = 0;
			int dmax = 0;
			for ( int d = 0; d < 3; ++d )
			{
				if ( voxelScale[ d ] > vmax )
				{
					vmax = voxelScale[ d ];
					dmax = d;
				}
			}
			if ( ( 4 * vmax / 32 ) > ( 1 / vmax ) )
				subdivisions.add( subdiv_32_32_4[ dmax ] );
			else
				subdivisions.add( subdiv_16_16_16 );

			setup.getSize().dimensions( size );
			long maxSize = 0;
			for ( int d = 0; d < 3; ++d )
			{
				size[ d ] /= res[ d ];
				maxSize = Math.max( maxSize, size[ d ] );
			}

//			System.out.println( "  level " + level );
//			System.out.println( "    res:        " + net.imglib2.util.Util.printCoordinates( res ) );
//			System.out.println( "    subdiv:     " + net.imglib2.util.Util.printCoordinates( subdivisions.get( level ) ) );
//			System.out.println( "        size:       " + net.imglib2.util.Util.printCoordinates( size ) );
//			System.out.println( "        voxelScale: " + net.imglib2.util.Util.printCoordinates( voxelScale ) );
//			System.out.println( "        vmax = " + vmax );
//			System.out.println( "        4 vmax / 32 = " + ( 4 * vmax / 32 ) );
//			System.out.println( "        1 / vmax = " + ( 1 / vmax ) );

			if ( maxSize <= 256 )
				break;

			for ( int d = 0; d < 3; ++d )
			{
				if ( voxelScale[ d ] <= 2.0 )
				{
					res[ d ] *= 2;
					voxelScale[ d ] *= 2;
				}
			}
			normalizeVoxelSize( voxelScale );
		}
		return new ExportMipmapInfo( resolutions.toArray( new int[ 0 ][ 0 ] ), subdivisions.toArray( new int[ 0 ][ 0 ] ) );
	}

	/**
	 * Format {@code in[][]} array, such as resolutions or chunksizes
	 * definition, as a String (to be used in export dialog textfields).
	 */
	public static String getArrayString( final int[][] resolutions )
	{
		final StringBuilder sb = new StringBuilder();
		sb.append( "{ " );
		boolean first = true;
		for ( final int[] res : resolutions )
		{
			if ( first )
				first = false;
			else
				sb.append( ", " );
			sb.append( String.format( "{%d,%d,%d}", res[ 0 ], res[ 1 ], res[ 2 ] ) );
		}
		sb.append( " }" );
		return sb.toString();
	}

	/**
	 * Adjust voxelSize such that the largest dimension is 1.0
	 *
	 * @param size
	 */
	public static void normalizeVoxelSize( final double[] size )
	{
		double minVoxelDim = Double.POSITIVE_INFINITY;
		for ( int d = 0; d < 3; ++d )
			minVoxelDim = Math.min( minVoxelDim, size[ d ] );
		for ( int d = 0; d < 3; ++d )
			size[ d ] /= minVoxelDim;
	}

	private static int[] subdiv_16_16_16 = new int[] { 16, 16, 16 };

	private static int[][] subdiv_32_32_4 = new int[][] { { 4, 32, 32 }, { 32, 4, 32 }, { 32, 32, 4 } };
}
