package creator;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleNeighborhoodFactory;
import net.imglib2.algorithm.region.localneighborhood.RectangleNeighborhoodUnsafe;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class Downsample
{
	public static < T extends RealType< T > > void downsample( final RandomAccessible< T > input, final RandomAccessibleInterval< T > output, final int[] factor )
	{
		assert input.numDimensions() == output.numDimensions();
		assert input.numDimensions() == factor.length;

		final int n = input.numDimensions();
		final RectangleNeighborhoodFactory< T > f = RectangleNeighborhoodUnsafe.< T >factory();
		final long[] dim = new long[ n ];
		for ( int d = 0; d < n; ++d )
			dim[ d ] = factor[ d ];
		final Interval spanInterval = new FinalInterval( dim );

		final long[] minRequiredInput = new long[ n ];
		final long[] maxRequiredInput = new long[ n ];
		output.min( minRequiredInput );
		output.max( maxRequiredInput );
		for ( int d = 0; d < n; ++d )
			maxRequiredInput[ d ] += dim[ d ];
		final RandomAccessibleInterval< T > requiredInput = Views.interval(  input, new FinalInterval( minRequiredInput, maxRequiredInput ) );

		final NeighborhoodsAccessible< T > neighborhoods = new NeighborhoodsAccessible< T >( requiredInput, spanInterval, f );
		final RandomAccess< Neighborhood< T > > block = neighborhoods.randomAccess();

		long size = 1;
		for ( int d = 0; d < n; ++d )
			size *= factor[ d ];
		final double scale = 1.0 / size;

		final Cursor< T > out = Views.iterable( output ).localizingCursor();
		while( out.hasNext() )
		{
			final T o = out.next();
			for ( int d = 0; d < n; ++d )
				block.setPosition( out.getLongPosition( d ) * factor[ d ], d );
			double sum = 0;
			for ( final T i : block.get() )
				sum += i.getRealDouble();
			o.setReal( sum * scale );
		}
	}
}
