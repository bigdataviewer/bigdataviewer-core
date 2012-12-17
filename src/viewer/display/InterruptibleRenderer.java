package viewer.display;

import java.util.concurrent.atomic.AtomicBoolean;

import net.imglib2.AbstractInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import viewer.hdf5.Util;
import viewer.hdf5.img.Hdf5GlobalCellCache;

public class InterruptibleRenderer< A, B > extends AbstractInterval
{
	final protected RandomAccessible< A > source;

	final protected Converter< A, B > converter;

	Hdf5GlobalCellCache< ? > cache;

	protected long lastFrameRenderTime;

	protected long lastFrameIoTime;

	public InterruptibleRenderer( final RandomAccessible< A > source, final Converter< A, B > converter, final Hdf5GlobalCellCache< ? > cache )
	{
		super( new long[ source.numDimensions() ] );
		this.source = source;
		this.converter = converter;
		this.cache = cache;
		lastFrameRenderTime = -1;
		lastFrameIoTime = -1;
	}

	public boolean map( final RandomAccessibleInterval< B > target )
	{
		interrupted.set( false );

		min[ 0 ] = target.min( 0 );
		min[ 1 ] = target.min( 1 );
		max[ 0 ] = target.max( 0 );
		max[ 1 ] = target.max( 1 );

		final long cr = -target.dimension( 0 );

		final RandomAccess< A > sourceRandomAccess = source.randomAccess( this );
		final RandomAccess< B > targetRandomAccess = target.randomAccess( target );

		final int width = ( int ) target.dimension( 0 );
		final int height = ( int ) target.dimension( 1 );

		sourceRandomAccess.setPosition( min );
		targetRandomAccess.setPosition( min[ 0 ], 0 );
		targetRandomAccess.setPosition( min[ 1 ], 1 );
		Util.timer = new Util.Timer();
		Util.timer.start();
		boolean wasInterrupted = false;
		for ( int y = 0; y < height; ++y )
		{
			wasInterrupted = interrupted.get();
			if ( wasInterrupted )
				break;
			for ( int x = 0; x < width; ++x )
			{
				converter.convert( sourceRandomAccess.get(), targetRandomAccess.get() );
				sourceRandomAccess.fwd( 0 );
				targetRandomAccess.fwd( 0 );
			}
			sourceRandomAccess.move( cr, 0 );
			targetRandomAccess.move( cr, 0 );
			sourceRandomAccess.fwd( 1 );
			targetRandomAccess.fwd( 1 );
		}
		Util.timer.stop();
		if ( wasInterrupted )
			System.out.println( "rendering was interrupted." );
		lastFrameIoTime = Util.timer.getIoTime();
		lastFrameRenderTime = Util.timer.getTotalTime() - lastFrameIoTime;
		System.out.println( Util.timer.getTotalTime()/1000000 + " ms  (io = " + lastFrameIoTime/1000000 + " ms,  render = " + lastFrameRenderTime/1000000 + " ms)" );
		final double bytesPerSecond = 1000.0 * 1000000.0 * ( ( double ) Util.timer.getIoBytes() / lastFrameIoTime ) / 1024.0;
		if ( ! Double.isNaN( bytesPerSecond ) )
			System.out.println( String.format( "%.0f kB/s", bytesPerSecond ) );

		return ! wasInterrupted;
	}

	protected AtomicBoolean interrupted = new AtomicBoolean();

	public void cancel()
	{
		interrupted.set( true );
	}

	public long getLastFrameRenderTime()
	{
		return lastFrameRenderTime;
	}

	public long getLastFrameIoTime()
	{
		return lastFrameIoTime;
	}
}
