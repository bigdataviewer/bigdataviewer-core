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
