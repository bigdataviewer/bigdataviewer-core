package viewer.render;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.ui.util.StopWatch;
import net.imglib2.view.Views;

public class EmptyProjector< T extends NumericType< T> > implements VolatileProjector
{
	private final RandomAccessibleInterval< T > target;

	private volatile boolean valid = false;

    protected long lastFrameRenderNanoTime;

    EmptyProjector( final RandomAccessibleInterval< T > screenImage )
	{
		this.target = screenImage;
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public boolean map()
	{
		return map( false );
	}

	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for ( final T t : Views.iterable( target ) )
			t.setZero();
		lastFrameRenderNanoTime = stopWatch.nanoTime();
		return true;
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	@Override
	public void cancel()
	{}

	@Override
	public boolean isValid()
	{
		return valid;
	}
}