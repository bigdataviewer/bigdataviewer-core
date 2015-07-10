package bdv.viewer.render;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

public class AccumulateProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{

	public AccumulateProjectorARGB(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< ? extends RandomAccessible< ARGBType > > sources,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( sourceProjectors, sources, null, target, numThreads, executorService );
	}

	@Override
	protected void accumulate( final Cursor< ARGBType >[] accesses, final ARGBType target )
	{
		double aAcc = 0;
		int rSum = 0, gSum = 0, bSum = 0;
		for ( final Cursor< ARGBType > access : accesses )
		{
			final int value = access.get().get();
			final double a = ARGBType.alpha( value ) / 255.0;
			final int r = ARGBType.red( value );
			final int g = ARGBType.green( value );
			final int b = ARGBType.blue( value );

			/* try this for testing purposes */
//			if ( r != g && g != b ) a *= 0.125;

			aAcc += a - aAcc * a;
			rSum += a * r;
			gSum += a * g;
			bSum += a * b;
		}
		if ( aAcc > 1.0 )
			aAcc = 1.0;
		if ( rSum > 255 )
			rSum = 255;
		if ( gSum > 255 )
			gSum = 255;
		if ( bSum > 255 )
			bSum = 255;
		target.set( ARGBType.rgba( rSum, gSum, bSum, ( int )( aAcc * 255 ) ) );
	}
}
