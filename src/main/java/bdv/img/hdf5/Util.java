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
package bdv.img.hdf5;

import mpicbg.spim.data.sequence.ViewId;

public class Util
{
	final static private String groupFormatString = "t%05d/s%02d/%d";

	final static private String cellsFormatString = "%s/cells";

	final static private String resolutionsFormatString = "s%02d/resolutions";

	final static private String subdivisionsFormatString = "s%02d/subdivisions";

	public static String getGroupPath( final int timepointId, final int setupId, final int level )
	{
		return String.format( groupFormatString, timepointId, setupId, level );
	}

	public static String getGroupPath( final ViewId viewId, final int level )
	{
		return String.format( groupFormatString, viewId.getTimePointId(), viewId.getViewSetupId(), level );
	}

	public static String getGroupPath( final ViewLevelId viewLevelId )
	{
		return String.format( groupFormatString, viewLevelId.getTimePointId(), viewLevelId.getViewSetupId(), viewLevelId.getLevel() );
	}

	public static String getCellsPath( final int timepoint, final int setup, final int level )
	{
		return String.format( cellsFormatString, getGroupPath( timepoint, setup, level ) );
	}

	public static String getCellsPath( final ViewId viewId, final int level )
	{
		return String.format( cellsFormatString, getGroupPath( viewId, level ) );
	}

	public static String getCellsPath( final ViewLevelId viewLevelId )
	{
		return String.format( cellsFormatString, getGroupPath( viewLevelId ) );
	}

	public static String getResolutionsPath( final int setupId )
	{
		return String.format( resolutionsFormatString, setupId );
	}

	public static String getSubdivisionsPath( final int setupId )
	{
		return String.format( subdivisionsFormatString, setupId );
	}

	/**
	 * Reorder long array representing column-major coordinate (imglib2) to
	 * row-major (hdf5). Permuted in is stored in out and out is returned.
	 *
	 * @param in
	 *            column major coordinates
	 * @param out
	 *            row major coordinates
	 * @return out
	 */
	public static long[] reorder( final long[] in, final long[] out )
	{
		assert in.length == out.length;
		for ( int i = 0, o = in.length - 1; i < in.length; ++i, --o )
			out[ o ] = in[ i ];
		return out;
	}

	/**
	 * Reorder long array representing column-major coordinate (imglib2) to
	 * row-major (hdf5).
	 *
	 * @param in
	 *            column major coordinates
	 * @return row major coordinates (new array).
	 */
	public static long[] reorder( final long[] in )
	{
		return reorder( in, new long[ in.length ] );
	}

	/**
	 * Reorder long array representing column-major coordinate (imglib2) to
	 * row-major (hdf5). Permuted in is stored in out and out is returned.
	 *
	 * @param in
	 *            column major coordinates
	 * @param out
	 *            row major coordinates
	 * @return out
	 */
	public static int[] reorder( final int[] in, final int[] out )
	{
		assert in.length == out.length;
		for ( int i = 0, o = in.length - 1; i < in.length; ++i, --o )
			out[ o ] = in[ i ];
		return out;
	}

	/**
	 * Reorder long array representing column-major coordinate (imglib2) to
	 * row-major (hdf5).
	 *
	 * @param in
	 *            column major coordinates
	 * @return row major coordinates (new array).
	 */
	public static int[] reorder( final int[] in )
	{
		return reorder( in, new int[ in.length ] );
	}

	/**
	 * Reorder long array representing column-major coordinate (imglib2) to
	 * row-major (hdf5). Permuted in is stored in out and out is returned.
	 *
	 * @param in
	 *            column major coordinates
	 * @param out
	 *            row major coordinates
	 * @return out
	 */
	public static long[] reorder( final int[] in, final long[] out )
	{
		assert in.length == out.length;
		for ( int i = 0, o = in.length - 1; i < in.length; ++i, --o )
			out[ o ] = in[ i ];
		return out;
	}

	public static int[] castToInts( final double[] doubles )
	{
		final int[] ints = new int[ doubles.length ];
		for ( int i = 0; i < doubles.length; ++i )
			ints[ i ] = ( int ) doubles[ i ];
		return ints;
	}

	public static int[][] castToInts( final double[][] doubles )
	{
		final int[][] ints = new int[ doubles.length ][];
		for ( int l = 0; l < doubles.length; ++l )
			ints[ l ] = castToInts( doubles[ l ] );
		return ints;
	}

	public static double[] castToDoubles( final int[] ints )
	{
		final double[] doubles = new double[ ints.length ];
		for ( int i = 0; i < ints.length; ++i )
			doubles[ i ] = ints[ i ];
		return doubles;
	}

	public static double[][] castToDoubles( final int[][] ints )
	{
		final double[][] doubles = new double[ ints.length ][];
		for ( int l = 0; l < ints.length; ++l )
			doubles[ l ] = castToDoubles( ints[ l ] );
		return doubles;
	}
}
