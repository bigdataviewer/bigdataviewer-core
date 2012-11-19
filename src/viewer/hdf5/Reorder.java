package viewer.hdf5;

public class Reorder
{
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
