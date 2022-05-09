package bdv.viewer.render;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.imglib2.parallel.Parallelization;
import net.imglib2.util.StopWatch;

/**
 * A {@code VolatileProjector} that iterates over a list of other {@code
 * VolatileProjector}s, one for each rendered tile.
 */
class TiledProjector implements VolatileProjector
{
	/**
	 * Projectors for individual tiles.
	 */
	private List< VolatileProjector > tileProjectors;

	/**
	 * Time needed for rendering the last frame, in nano-seconds.
	 */
	private long lastFrameRenderNanoTime;

	private volatile boolean canceled = false;

	private boolean valid = false;

	TiledProjector( final List< VolatileProjector > tileProjectors )
	{
		this.tileProjectors = tileProjectors;
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public void cancel()
	{
		canceled = true;
		for ( VolatileProjector tileProjector : tileProjectors )
			tileProjector.cancel();
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	/**
	 * Returns {@code true}, if all tile projectors are {@link VolatileProjector#isValid() valid}.
	 *
	 * @return {@code true}, if all tile projectors are valid.
	 */
	@Override
	public boolean isValid()
	{
		return valid;
	}

	/**
	 * Call {@code map()} for each tile projector that is not {@link VolatileProjector#isValid() valid} yet.
	 * Returns {@code true}, if all tile {@code map()}s returned {@code true}.
	 *
	 * @param clearUntouchedTargetPixels
	 * @return true if rendering was completed (all target pixels written).
	 *         false if rendering was interrupted.
	 */
	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		if ( canceled )
			return false;

		if ( isValid() )
			return true;

		final StopWatch stopWatch = StopWatch.createAndStart();
		ForkJoinTask.invokeAll(
				tileProjectors.stream()
						.map( p -> ForkJoinTask.adapt( () -> p.map( clearUntouchedTargetPixels ) ) )
						.collect( Collectors.toList() ) );
		if ( canceled )
			return false;
		tileProjectors = tileProjectors.stream()
				.filter( p -> !p.isValid() )
				.collect( Collectors.toList() );
		lastFrameRenderNanoTime = stopWatch.nanoTime();
		valid = tileProjectors.isEmpty();
		return !canceled;
	}
}
