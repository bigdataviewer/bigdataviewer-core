package viewer.hdf5;

import mpicbg.spim.data.View;

public class Util
{
	final static private String groupFormatString = "t%05d/s%02d/%d";

	final static private String cellsFormatString = "%s/cells";

	final static private String resolutionsFormatString = "s%02d/resolutions";

	final static private String subdivisionsFormatString = "s%02d/subdivisions";

	public static String getGroupPath( final int timepoint, final int setup, final int level )
	{
		return String.format( groupFormatString, timepoint, setup, level );
	}

	public static String getGroupPath( final View view, final int level )
	{
		return String.format( groupFormatString, view.getTimepointIndex(), view.getSetupIndex(), level );
	}

	public static String getCellsPath( final int timepoint, final int setup, final int level )
	{
		return String.format( cellsFormatString, getGroupPath( timepoint, setup, level ) );
	}

	public static String getCellsPath( final View view, final int level )
	{
		return String.format( cellsFormatString, getGroupPath( view, level ) );
	}

	public static String getResolutionsPath( final int setup )
	{
		return String.format( resolutionsFormatString, setup );
	}

	public static String getSubdivisionsPath( final int setup )
	{
		return String.format( subdivisionsFormatString, setup );
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
}
