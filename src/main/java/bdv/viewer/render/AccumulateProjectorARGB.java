package bdv.viewer.render;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

public class AccumulateProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	final static private ARGBCompositeAlphaAdd composite = new ARGBCompositeAlphaAdd();
//	final static private ARGBCompositeAlphaYCbCr composite = new ARGBCompositeAlphaYCbCr();

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
		// TODO This is bad, we want to be able to paint on top of arbitrary background, e.g.
		// the sky line of NY or some sort of eye pleasing gradient, the outside code should
		// take care for clearing/ preparing the background and not leave it as is.
		target.set( 0xff000000 );

		for ( final Cursor< ARGBType > access : accesses )
			composite.compose( target, access.get(), target );

//		target.set( accesses[ 0 ].get() );
//		composite.compose( target, accesses[ 1 ].get(), target );
	}
}
