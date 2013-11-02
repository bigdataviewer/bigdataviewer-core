package viewer.display;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.ui.InterruptibleProjector;
import net.imglib2.ui.util.StopWatch;
import net.imglib2.view.Views;
import viewer.render.VolatileProjector;

public abstract class AccumulateProjector< A, B > implements VolatileProjector
{
	final protected ArrayList< VolatileProjector > sourceProjectors;

	final protected ArrayList< IterableInterval< A > > sources;

	/**
	 * A converter from the source pixel type to the target pixel type.
	 */
	final protected Converter< ? super A, B > converter;

	/**
	 * The target interval. Pixels of the target interval should be set by
	 * {@link InterruptibleProjector#map()}
	 */
	final protected RandomAccessibleInterval< B > target;

	/**
	 * A reference to the target image as an iterable.  Used for source-less
	 * operations such as clearing its content.
	 */
	final protected IterableInterval< B > iterableTarget;

	/**
     * Number of threads to use for rendering
     */
    final protected int numThreads;

    /**
     * Time needed for rendering the last frame, in nano-seconds.
     */
    protected long lastFrameRenderNanoTime;

	final protected AtomicBoolean interrupted = new AtomicBoolean();

	protected volatile boolean valid = false;

	public AccumulateProjector(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final int numThreads )
	{
		this.sourceProjectors = sourceProjectors;
		this.sources = new ArrayList< IterableInterval< A > >();
		for ( final RandomAccessible< A > source : sources )
			this.sources.add( Views.flatIterable( Views.interval( source, target ) ) );
		this.converter = converter;
		this.target = target;
		this.iterableTarget = Views.flatIterable( target );
		this.numThreads = numThreads;
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public boolean map()
	{
		return map( true );
	}

	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		interrupted.set( false );

		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		valid = true;
		for ( final VolatileProjector p : sourceProjectors )
			if ( !p.isValid() )
				if ( !p.map( clearUntouchedTargetPixels ) )
					return false;
				else
					valid &= p.isValid();

		final int width = ( int ) target.dimension( 0 );
		final int height = ( int ) target.dimension( 1 );
		final int length = width * height;

		final ExecutorService ex = Executors.newFixedThreadPool( numThreads );
		final int numTasks = Math.max( numThreads * 10, height );
		final double taskLength = ( double ) length / numTasks;
		final int numSources = sources.size();
		for ( int taskNum = 0; taskNum < numTasks; ++taskNum )
		{
			final int myOffset = ( int ) ( taskNum * taskLength );
			final int myLength = ( (taskNum == numTasks - 1 ) ? length : ( int ) ( ( taskNum + 1 ) * taskLength ) ) - myOffset;

			final Runnable r = new Runnable()
			{
				@SuppressWarnings( "unchecked" )
				@Override
				public void run()
				{
					if ( interrupted.get() )
						return;

					final Cursor< A >[] sourceCursors = new Cursor[ numSources ];
					for ( int s = 0; s < numSources; ++s )
					{
						final Cursor< A > c = sources.get( s ).cursor();
						c.jumpFwd( myOffset );
						sourceCursors[ s ] = c;
					}
					final Cursor< B > targetCursor = iterableTarget.cursor();
					targetCursor.jumpFwd( myOffset );

					for ( int i = 0; i < myLength; ++i )
					{
						for ( int s = 0; s < numSources; ++s )
							sourceCursors[ s ].fwd();
						accumulate( sourceCursors, targetCursor.next() );
					}
				}
			};
			ex.execute( r );
		}
		ex.shutdown();
		try
		{
			ex.awaitTermination( 1, TimeUnit.HOURS );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}

		lastFrameRenderNanoTime = stopWatch.nanoTime();

		return !interrupted.get();
	}

	protected abstract void accumulate( final Cursor< A >[] accesses, final B target );

	@Override
	public void cancel()
	{
		interrupted.set( true );
		for ( final VolatileProjector p : sourceProjectors )
			p.cancel();
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
}
