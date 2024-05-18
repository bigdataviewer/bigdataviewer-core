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
package bdv.export;

import java.util.ArrayList;
import java.util.Arrays;
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
 * currently. Chunk sizes are proposed such that chunks have power-of-two side
 * lengths, are roughly square in world space, and contain close to (but not
 * more than) a specified number of elements (4096 by default). It is very
 * likely that more efficient choices can be found by manual tuning, depending
 * on hardware and use case.
 *
 * @author Tobias Pietzsch
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
		final HashMap< Integer, ExportMipmapInfo > perSetupExportMipmapInfo = new HashMap<>();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			perSetupExportMipmapInfo.put( setup.getId(), proposeMipmaps( setup ) );
		return perSetupExportMipmapInfo;
	}

	/**
	 * Propose number of mipmap levels as well subsampling factors and chunk
	 * size for each level, based on the image and voxel size of the given
	 * setup. Chunks contain close to (but not more than) 4096 elements.
	 *
	 * @param setup
	 * @return proposed mipmap settings
	 */
	public static ExportMipmapInfo proposeMipmaps( final BasicViewSetup setup )
	{
		return proposeMipmaps( setup, 4096 );
	}

	/**
	 * Propose number of mipmap levels as well subsampling factors and chunk
	 * size for each level, based on the image and voxel size of the given
	 * setup. Chunk sizes are proposed such that chunks have power-of-two side
	 * lengths, are roughly square in world space, and contain close to (but not
	 * more than) {@code maxNumElements}.
	 *
	 * @param setup
	 * @param maxNumElements
	 * @return proposed mipmap settings
	 */
	public static ExportMipmapInfo proposeMipmaps( final BasicViewSetup setup, final int maxNumElements )
	{
		final VoxelDimensions voxelSize = setup.getVoxelSize();
		final double[] voxelScale = new double[ 3 ];
		voxelSize.dimensions( voxelScale );
		normalizeVoxelSize( voxelScale );

		final int[] res = new int[] { 1, 1, 1 };
		final long[] size = new long[ 3 ];

		final ArrayList< int[] > resolutions = new ArrayList<>();
		final ArrayList< int[] > subdivisions = new ArrayList<>();

//		for ( int level = 0;; ++level )
		while ( true )
		{
			resolutions.add( res.clone() );

			setup.getSize().dimensions( size );
			long maxSize = 0;
			for ( int d = 0; d < 3; ++d )
			{
				size[ d ] = Math.max( 1, size[ d ] / res[ d ] );
				maxSize = Math.max( maxSize, size[ d ] );
			}

			subdivisions.add( suggestPoTBlockSize( voxelScale, size, maxNumElements ) );

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

			boolean anyDimensionChanged = false;
			for ( int d = 0; d < 3; ++d )
			{
				if ( voxelScale[ d ] <= 2.0 && size[ d ] > 1 )
				{
					res[ d ] *= 2;
					voxelScale[ d ] *= 2;
					anyDimensionChanged = true;
				}
			}
			if ( !anyDimensionChanged )
			{
				for ( int d = 0; d < 3; ++d )
					res[ d ] *= 2;
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
	 * Adjust voxelSize such that the smallest dimension is 1.0
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

	/**
	 * Propose block size such that
	 * <ol>
	 * <li>each dimension is power-of-two,</li>
	 * <li>number of elements is as big as possible, but not larger than
	 * {@code maxNumElements}</li>
	 * <li>and the block (scaled by the {@code voxelSize}) is as close to square
	 * as possible given constraints 1 and 2.</li>
	 * </ol>
	 */
	public static int[] suggestPoTBlockSize( final double[] voxelSize, final int maxNumElements )
	{
		return suggestPoTBlockSize( voxelSize, null, maxNumElements );
	}

	/**
	 * Propose block size such that
	 * <ol>
	 * <li>each dimension is power-of-two,</li>
	 * <li>number of elements is as big as possible, but not larger than
	 * {@code maxNumElements}</li>
	 * <li>and the block (scaled by the {@code voxelSize}) is as close to square
	 * as possible given constraints 1 and 2.</li>
	 * </ol>
	 * Additionally, the full {@code size[]} of the image limits the number of bits
	 * spent in a particular dimension. For example, if {@code size[2] = 3},
	 * then resulting block size will not exceed 4 in the Z dimension.
	 */
	public static int[] suggestPoTBlockSize( final double[] voxelSize, final long[] size, final int maxNumElements )
	{
		final int n = voxelSize.length;
		final double[] bias = new double[ n ];
		Arrays.setAll( bias, d -> 0.01 * ( n - d ) );
		return suggestPoTBlockSize( voxelSize, size, maxNumElements, bias );
	}

	/**
	 * Propose block size such that
	 * <ol>
	 * <li>each dimension is power-of-two,</li>
	 * <li>number of elements is as big as possible, but not larger than
	 * {@code maxNumElements}</li>
	 * <li>and the block (scaled by the {@code voxelSize}) is as close to square
	 * as possible given constraints 1 and 2.</li>
	 * </ol>
	 *
	 * Determination works by finding real PoT for each dimension, then rounding
	 * down, and increasing one by one the PoT for dimensions until going over
	 * maxNumElements. Dimensions are ordered by decreasing fractional remainder
	 * of real PoT plus some per-dimension bias (usually set such that X is
	 * enlarged before Y before Z...)
	 *
	 * Optionally, if the full {@code size[]} of the image can be provided (
	 * {@code size != null}), this will limit the number of bits spent in a particular
	 * dimension. For example, if {@code size[2] = 3}, then resulting block size will
	 * not exceed 4 in the Z dimension.
	 */
	private static int[] suggestPoTBlockSize( final double[] voxelSize, final long[] size, final int maxNumElements, final double[] bias )
	{
		final int n = voxelSize.length;

		final double[] shape = new double[ n ];
		double shapeVol = 1;
		for ( int d = 0; d < n; ++d )
		{
			shape[ d ] = 1 / voxelSize[ d ];
			shapeVol *= shape[ d ];
		}
		final double m = Math.pow( maxNumElements / shapeVol, 1. /  n  );
		final double sumNumBits = Math.log( maxNumElements ) / Math.log( 2 );
		final double[] numBits = new double[ n ];
		Arrays.setAll( numBits, d -> Math.log( m * shape[ d ] ) / Math.log( 2 ) );
		final int[] intNumBits = new int[ n ];
		Arrays.setAll( intNumBits, d -> Math.max( 0, ( int ) numBits[ d ] ) );

		final int[] fullSizeBits = new int[ n ];
		if ( size != null )
		{
			// fullSizeBits[d] contains the number of bits needed to fit the full size in dimension d
			// (rounded up to next full bit)
			Arrays.setAll( fullSizeBits, d -> Math.max( 0, ( int ) ( Math.log( size[ d ] - 1 ) / Math.log( 2 ) ) + 1 ) );

			// if this is more than the (rounded down) number of bits allocated to block size in d,
			// use the fullSizeBits[d] instead.
			Arrays.setAll( intNumBits, d -> Math.min( intNumBits[ d ], fullSizeBits[ d ] ) );
		}

		for ( int sumIntNumBits = Arrays.stream( intNumBits ).sum(); sumIntNumBits + 1 <= sumNumBits; ++sumIntNumBits )
		{
			double maxDiff = Double.NEGATIVE_INFINITY;
			int maxDiffDim = 0;
			for ( int d = 0; d < n; ++d )
			{
				// if full image size was specified and we already fit the full image
				// in dimension d, skip to the next dimension.
				// Note that if we skip all dimensions in this way (because the block already fits the full image),
				// we have maxDiffDim==0 and just keep increasing the X block size until we hit the limit.
				if ( size != null && intNumBits[ d ] >= fullSizeBits[ d ] )
					continue;
				final double diff = numBits[ d ] - intNumBits[ d ] + bias[ d ];
				if ( diff > maxDiff )
				{
					maxDiff = diff;
					maxDiffDim = d;
				}
			}
			++intNumBits[ maxDiffDim ];
		}

		final int[] blockSize = new int[ n ];
		for ( int d = 0; d < n; ++d )
			blockSize[ d ] = 1 << intNumBits[ d ];
		return blockSize;
	}
}
