package bdv.viewer.render;

import java.util.concurrent.ExecutorService;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.ui.SimpleInterruptibleProjector;

// TODO Fix naming. This is a VolatileProjector for a non-volatile source...
public class SimpleVolatileProjector< A, B > extends SimpleInterruptibleProjector< A, B > implements VolatileProjector
{
	private boolean valid = false;

	public SimpleVolatileProjector(
			final RandomAccessible< A > source,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( source, converter, target, numThreads, executorService );
	}

	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		final boolean success = super.map();
		valid |= success;
		return success;
	}

	@Override
	public boolean isValid()
	{
		return valid;
	}
}
