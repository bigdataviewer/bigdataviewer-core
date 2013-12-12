package viewer.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.ui.AbstractInterruptibleProjector;
import net.imglib2.ui.util.StopWatch;
import net.imglib2.view.Views;
import viewer.hdf5.img.CacheIoTiming;
import viewer.hdf5.img.CacheIoTiming.IoStatistics;

/**
 * {@link Projector} for a hierarchy of {@link Volatile} inputs.  After each
 * {@link #map()} call, the projector has a {@link #isValid() state} that
 * signalizes whether all projected pixels were perfect.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class VolatileHierarchyProjector< A extends Volatile< ? >, B extends NumericType< B > > extends AbstractInterruptibleProjector< A, B > implements VolatileProjector
{
	final protected ArrayList< RandomAccessible< A > > sources = new ArrayList< RandomAccessible< A > >();

	final private byte[] maskArray;

	final protected Img< ByteType > mask;

	protected volatile boolean valid = false;

	protected int numInvalidLevels;

	/**
	 * Extends of the source to be used for mapping.
	 */
	final protected FinalInterval sourceInterval;

	/**
	 * Target width
	 */
	final protected int width;

	/**
	 * Target height
	 */
	final protected int height;

	/**
	 * Steps for carriage return.  Typically -{@link #width}
	 */
	final protected int cr;

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

	/**
	 * TODO
	 */
	// TODO move to derived implementation for local sources only
	protected long lastFrameIoNanoTime;

    final protected AtomicBoolean interrupted = new AtomicBoolean();

	public VolatileHierarchyProjector(
			final List< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final int numThreads )
	{
		this( sources, converter, target, new byte[ ( int ) ( target.dimension( 0 ) * target.dimension( 1 ) ) ], numThreads );
	}

	public VolatileHierarchyProjector(
			final List< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final byte[] maskArray,
			final int numThreads )
	{
		super( Math.max( 2, sources.get( 0 ).numDimensions() ), converter, target );

		this.sources.addAll( sources );
		numInvalidLevels = sources.size();

		this.maskArray = maskArray;
		mask = ArrayImgs.bytes( maskArray, target.dimension( 0 ), target.dimension( 1 ) );

		iterableTarget = Views.iterable( target );

		for ( int d = 2; d < min.length; ++d )
			min[ d ] = max[ d ] = 0;

		max[ 0 ] = target.max( 0 );
		max[ 1 ] = target.max( 1 );
		sourceInterval = new FinalInterval( min, max );

		width = ( int )target.dimension( 0 );
		height = ( int )target.dimension( 1 );
		cr = -width;

		this.numThreads = numThreads;
		lastFrameRenderNanoTime = -1;

		clearMask();
	}

	@Override
	public void cancel()
	{
		interrupted.set( true );
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	public long getLastFrameIoNanoTime()
	{
		return lastFrameIoNanoTime;
	}

	@Override
	public boolean isValid()
	{
		return valid;
	}

	/**
	 * Set all pixels in target to 100% transparent zero, and mask to all
	 * Integer.MAX_VALUE.
	 */
	public void clearMask()
	{
		Arrays.fill( maskArray, 0, ( int ) mask.size(), Byte.MAX_VALUE );
		numInvalidLevels = sources.size();
	}

	/**
	 * Clear target pixels that were never written.
	 */
	protected void clearUntouchedTargetPixels()
	{
		final Cursor< ByteType > maskCursor = mask.cursor();
		for ( final B t : iterableTarget )
			if ( maskCursor.next().get() == Byte.MAX_VALUE )
				t.setZero();
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
		final IoStatistics iostat = CacheIoTiming.getThreadGroupIoStatistics();
		final long startTimeIo = iostat.getIoNanoTime();
		final long startTimeIoCumulative = iostat.getCumulativeIoNanoTime();
		final long startIoBytes = iostat.getIoBytes();

		final int numTasks;
		if ( numThreads > 1 )
		{
			numTasks = Math.max( numThreads * 10, height );
		}
		else
			numTasks = 1;
		final double taskHeight = ( double )height / numTasks;

		int i;

		valid = false;

		final ExecutorService ex = Executors.newFixedThreadPool( numThreads );
		for ( i = 0; i < numInvalidLevels && !valid; ++i )
		{
			final byte iFinal = ( byte ) i;

			valid = true;

			final ArrayList< Callable< Void > > tasks = new ArrayList< Callable< Void > >( numTasks );
			for ( int taskNum = 0; taskNum < numTasks; ++taskNum )
			{
				final int myOffset = width * ( int ) ( taskNum * taskHeight );
				final long myMinY = min[ 1 ] + ( int ) ( taskNum * taskHeight );
				final int myHeight = ( int ) ( ( (taskNum == numTasks - 1 ) ? height : ( int ) ( ( taskNum + 1 ) * taskHeight ) ) - myMinY - min[ 1 ] );

				final Callable< Void > r = new Callable< Void >()
				{
					@Override
					public Void call()
					{
						if ( interrupted.get() )
							return null;

						final RandomAccess< B > targetRandomAccess = target.randomAccess( target );
						final Cursor< ByteType > maskCursor = mask.cursor();
						final RandomAccess< A > sourceRandomAccess = sources.get( iFinal ).randomAccess( sourceInterval );
						boolean myValid = true;

						sourceRandomAccess.setPosition( min );
						sourceRandomAccess.setPosition( myMinY, 1 );

						targetRandomAccess.setPosition( min[ 0 ], 0 );
						targetRandomAccess.setPosition( myMinY, 1 );

						maskCursor.jumpFwd( myOffset );

						for ( int y = 0; y < myHeight; ++y )
						{
							if ( interrupted.get() )
								return null;

							for ( int x = 0; x < width; ++x )
							{
								final ByteType m = maskCursor.next();
								if ( m.get() > iFinal )
								{
									final A a = sourceRandomAccess.get();
									final boolean v = a.isValid();
									if ( v )
									{
										converter.convert( a, targetRandomAccess.get() );
										m.set( iFinal );
									}
									else
										myValid = false;
								}
								sourceRandomAccess.fwd( 0 );
								targetRandomAccess.fwd( 0 );
							}
							sourceRandomAccess.move( cr, 0 );
							targetRandomAccess.move( cr, 0 );
							sourceRandomAccess.fwd( 1 );
							targetRandomAccess.fwd( 1 );
						}
						if ( !myValid )
							valid = false;
						return null;
					}
				};
				tasks.add( r );
			}
			try
			{
				ex.invokeAll( tasks );
			}
			catch ( final InterruptedException e )
			{
				e.printStackTrace();
			}
			if ( interrupted.get() )
			{
//				System.out.println( "interrupted" );
				ex.shutdown();
				return false;
			}
		}
		ex.shutdown();

		if ( clearUntouchedTargetPixels && !interrupted.get() )
			clearUntouchedTargetPixels();

		final long lastFrameTime = stopWatch.nanoTime();
//		final long numIoBytes = iostat.getIoBytes() - startIoBytes;
		lastFrameIoNanoTime = iostat.getIoNanoTime() - startTimeIo;
		lastFrameRenderNanoTime = lastFrameTime - ( iostat.getCumulativeIoNanoTime() - startTimeIoCumulative ) / numThreads;

		System.out.println( "lastFrameTime = " + lastFrameTime / 1000000 );
		System.out.println( "lastFrameRenderNanoTime = " + lastFrameRenderNanoTime / 1000000 );

		if ( valid )
			numInvalidLevels = i - 1;
		valid = numInvalidLevels == 0;

//		System.out.println( "Mapping complete after " + ( s + 1 ) + " levels." );

		return !interrupted.get();
	}
}
