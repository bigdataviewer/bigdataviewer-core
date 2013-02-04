package viewer.display;

import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.ARGBType;

public class AccumulateARGB extends Accumulate< ARGBType >
{
	public AccumulateARGB( final List< RandomAccessible< ARGBType > > sources )
	{
		super( sources, new ARGBType() );
	}

	@Override
	protected void accumulate( final RandomAccess< ARGBType >[] accesses, final ARGBType target )
	{
		int aSum = 0, rSum = 0, gSum = 0, bSum = 0;
		for ( final RandomAccess< ARGBType > access : accesses )
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
