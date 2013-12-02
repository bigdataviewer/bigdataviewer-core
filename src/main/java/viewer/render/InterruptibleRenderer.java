package viewer.render;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.imglib2.AbstractInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.ui.util.StopWatch;

// TODO: should this extend net.imglib2.ui.InterruptibleProjector?
// rename to XInterruptibleProjector
public class InterruptibleRenderer< A extends Volatile< ? >, B > extends AbstractInterval
{
	final protected RandomAccessible< A > source;

	final protected Converter< ? super A, B > converter;

	protected long lastFrameRenderNanoTime;

	protected volatile boolean valid = false;

	public InterruptibleRenderer( final RandomAccessible< A > source, final Converter< ? super A, B > converter )
	{
		super( new long[ source.numDimensions() ] );
		this.source = source;
		this.converter = converter;
		lastFrameRenderNanoTime = -1;
	}

	public boolean map( final RandomAccessibleInterval< B > target, final int numThreads )
	{
		interrupted.set( false );

		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		final int size = ( int ) ( target.dimension( 0 ) * target.dimension( 1 ) );
		final int[] maskArray = new int[ size ];
		Arrays.fill( maskArray, Integer.MAX_VALUE );
		final Img< IntType > mask = ArrayImgs.ints( maskArray, target.dimension( 0 ), target.dimension( 1 ) );

		min[ 0 ] = target.min( 0 );
		min[ 1 ] = target.min( 1 );
		max[ 0 ] = target.max( 0 );
		max[ 1 ] = target.max( 1 );

		final long cr = -target.dimension( 0 );

		final int width = ( int ) target.dimension( 0 );
		final int height = ( int ) target.dimension( 1 );

		final int iFinal = 0;
		valid = true;

		final ExecutorService ex = Executors.newFixedThreadPool( numThreads );
		final int numTasks;
		if ( numThreads > 1 )
		{
			numTasks = Math.max( numThreads * 10, height );
		}
		else
			numTasks = 1;
		final double taskHeight = ( double ) height / numTasks;
		for ( int taskNum = 0; taskNum < numTasks; ++taskNum )
		{
			final long myMinY = min[ 1 ] + ( int ) ( taskNum * taskHeight );
			final long myHeight = ( (taskNum == numTasks - 1 ) ? height : ( int ) ( ( taskNum + 1 ) * taskHeight ) ) - myMinY - min[ 1 ];

			final Runnable r = new Runnable()
			{
				@Override
				public void run()
				{
					if ( interrupted.get() )
						return;

					final RandomAccess< A > sourceRandomAccess = source.randomAccess( InterruptibleRenderer.this );
					final RandomAccess< B > targetRandomAccess = target.randomAccess( target );
					final RandomAccess< IntType > maskRandomAccess = mask.randomAccess( target );
					boolean myValid = true;

					sourceRandomAccess.setPosition( min );
					sourceRandomAccess.setPosition( myMinY, 1 );
					targetRandomAccess.setPosition( min[ 0 ], 0 );
					targetRandomAccess.setPosition( myMinY, 1 );
					maskRandomAccess.setPosition( min[ 0 ], 0 );
					maskRandomAccess.setPosition( myMinY, 1 );
					for ( int y = 0; y < myHeight; ++y )
					{
						if ( interrupted.get() )
							break;
						for ( int x = 0; x < width; ++x )
						{
							final IntType m = maskRandomAccess.get();
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
							maskRandomAccess.fwd( 0 );
						}
						sourceRandomAccess.move( cr, 0 );
						targetRandomAccess.move( cr, 0 );
						maskRandomAccess.move( cr, 0 );
						sourceRandomAccess.fwd( 1 );
						targetRandomAccess.fwd( 1 );
						maskRandomAccess.fwd( 1 );
					}
					if ( !myValid )
						valid = false;
				}
			};
			ex.execute( r );
		}
		ex.shutdown();
		try
		{
			ex.awaitTermination( 1000, TimeUnit.DAYS );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}

		lastFrameRenderNanoTime = stopWatch.nanoTime();

//		if ( interrupted.get() )
//			System.out.println( "rendering was interrupted." );
//		System.out.println( String.format( "rendering:%4d ms", lastFrameRenderNanoTime / 1000000 ) );

		return ! interrupted.get();
	}

	protected AtomicBoolean interrupted = new AtomicBoolean();

	public void cancel()
	{
//		System.out.println( "interrupting..." );
		interrupted.set( true );
	}

	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}
}
