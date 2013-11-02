package viewer.display;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import viewer.render.VolatileProjector;

public class AccumulateProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{

	public AccumulateProjectorARGB(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< ? extends RandomAccessible< ARGBType > > sources,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads )
	{
		super( sourceProjectors, sources, null, target, numThreads );
	}

	@Override
	protected void accumulate( final Cursor< ARGBType >[] accesses, final ARGBType target )
	{
		int aSum = 0, rSum = 0, gSum = 0, bSum = 0;
		for ( final Cursor< ARGBType > access : accesses )
		{
			final int value = access.get().get();
			final int a = ARGBType.alpha( value );
			final int r = ARGBType.red( value );
			final int g = ARGBType.green( value );
			final int b = ARGBType.blue( value );
			aSum += a;
			rSum += r;
			gSum += g;
			bSum += b;
		}
		if ( aSum > 255 )
			aSum = 255;
		if ( rSum > 255 )
			rSum = 255;
		if ( gSum > 255 )
			gSum = 255;
		if ( bSum > 255 )
			bSum = 255;
		target.set( ARGBType.rgba( rSum, gSum, bSum, aSum ) );
	}
}
