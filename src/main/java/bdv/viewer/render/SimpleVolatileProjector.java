package bdv.viewer.render;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;

// TODO Fix naming. This is a VolatileProjector for a non-volatile source...
/**
 * An {@link VolatileProjector}, that renders a target 2D
 * {@code RandomAccessibleInterval} by copying values from a source
 * {@code RandomAccessible}. The source can have more dimensions than the
 * target. Target coordinate <em>(x,y)</em> is copied from source coordinate
 * <em>(x,y,0,...,0)</em>.
 * <p>
 * A specified number of threads is used for rendering.
 *
 * @param <A>
 *            pixel type of the source {@code RandomAccessible}.
 * @param <B>
 *            pixel type of the target {@code RandomAccessibleInterval}.
 *
 * @author Tobias Pietzsch
 * @author Stephan Saalfeld
 */
public class SimpleVolatileProjector< A, B > implements VolatileProjector
{
	/**
	 * A converter from the source pixel type to the target pixel type.
	 */
	private final Converter< ? super A, B > converter;

	/**
	 * The target interval. Pixels of the target interval should be set by
	 * {@link #map}
	 */
	private final RandomAccessibleInterval< B > target;

	private final RandomAccessible< A > source;

	/**
	 * Source interval which will be used for rendering. This is the 2D target
	 * interval expanded to source dimensionality (usually 3D) with
	 * {@code min=max=0} in the additional dimensions.
	 */
	private final FinalInterval sourceInterval;

	/**
	 * Number of threads to use for rendering
	 */
	private final int numThreads;

	private final ExecutorService executorService;

	/**
	 * Time needed for rendering the last frame, in nano-seconds.
	 */
	private long lastFrameRenderNanoTime;

	private AtomicBoolean canceled = new AtomicBoolean();

	private boolean valid = false;

	/**
	 * Create new projector with the given source and a converter from source to
	 * target pixel type.
	 *
	 * @param source
	 *            source pixels.
	 * @param converter
	 *            converts from the source pixel type to the target pixel type.
	 * @param target
	 *            the target interval that this projector maps to
	 * @param numThreads
	 *            how many threads to use for rendering.
	 * @param executorService
	 */
	public SimpleVolatileProjector(
			final RandomAccessible< A > source,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		this.converter = converter;
		this.target = target;
		this.source = source;

		final int n = Math.max( 2, source.numDimensions() );
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		min[ 0 ] = target.min( 0 );
		max[ 0 ] = target.max( 0 );
		min[ 1 ] = target.min( 1 );
		max[ 1 ] = target.max( 1 );
		sourceInterval = new FinalInterval( min, max );

		this.numThreads = numThreads;
		this.executorService = executorService;
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public void cancel()
	{
		canceled.set( true );
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	@Override
	public boolean isValid()
	{
		return valid;
	}

	/**
	 * Render the 2D target image by copying values from the source. Source can
	 * have more dimensions than the target. Target coordinate <em>(x,y)</em> is
	 * copied from source coordinate <em>(x,y,0,...,0)</em>.
	 *
	 * @return true if rendering was completed (all target pixels written).
	 *         false if rendering was interrupted.
	 */
	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		if ( canceled.get() )
			return false;

		final StopWatch stopWatch = StopWatch.createAndStart();

		final int targetHeight = ( int ) target.dimension( 1 );
		final int numTasks = numThreads <= 1 ? 1 : Math.min( numThreads * 10, targetHeight );
		final double taskHeight = ( double ) targetHeight / numTasks;
		final int[] taskStartHeights = new int[ numTasks + 1 ];
		for ( int i = 0; i < numTasks; ++i )
			taskStartHeights[ i ] = ( int ) ( i * taskHeight );
		taskStartHeights[ numTasks ] = targetHeight;

		final boolean createExecutor = ( executorService == null );
		final ExecutorService ex = createExecutor ? Executors.newFixedThreadPool( numThreads ) : executorService;

		final List< Callable< Void > > tasks = new ArrayList<>( numTasks );
		for( int i = 0; i < numTasks; ++i )
			tasks.add( createMapTask( taskStartHeights[ i ], taskStartHeights[ i + 1 ] ) );
		try
		{
			ex.invokeAll( tasks );
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
		}

		if ( createExecutor )
			ex.shutdown();

		lastFrameRenderNanoTime = stopWatch.nanoTime();

		final boolean success = !canceled.get();
		valid |= success;
		return success;
	}

	/**
	 * @return a {@code Callable} that runs {@code map(startHeight, endHeight)}
	 */
	private Callable< Void > createMapTask( final int startHeight, final int endHeight )
	{
		return Executors.callable( () -> map( startHeight, endHeight ), null );
	}

	/**
	 * Copy lines from {@code y = startHeight} up to {@code endHeight}
	 * (exclusive) from source to target. Check after
	 * each line whether rendering was {@link #cancel() canceled}.
	 *
	 * @param startHeight
	 *     start of line range to copy (relative to target min coordinate)
	 * @param endHeight
	 *     end (exclusive) of line range to copy (relative to target min
	 *     coordinate)
	 */
	private void map( final int startHeight, final int endHeight )
	{
		if ( canceled.get() )
			return;

		final RandomAccess< B > targetRandomAccess = target.randomAccess( target );
		final RandomAccess< A > sourceRandomAccess = source.randomAccess( sourceInterval );
		final int width = ( int ) target.dimension( 0 );
		final long[] smin = Intervals.minAsLongArray( sourceInterval );

		for ( int y = startHeight; y < endHeight; ++y )
		{
			if ( canceled.get() )
				return;
			smin[ 1 ] = y;
			sourceRandomAccess.setPosition( smin );
			targetRandomAccess.setPosition( smin );
			for ( int x = 0; x < width; ++x )
			{
				converter.convert( sourceRandomAccess.get(), targetRandomAccess.get() );
				sourceRandomAccess.fwd( 0 );
				targetRandomAccess.fwd( 0 );
			}
		}
	}
}
