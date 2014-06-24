package bdv.viewer.box;

import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.Type;
import net.imglib2.util.Intervals;
import bdv.util.ModifiableInterval;

// a simple imglib2 RealRandomAccessible that has one value inside a box and another value outside
public class BoxRealRandomAccessible< T extends Type< T > > implements RealRandomAccessible< T >
{
	private final int n;

	private final ModifiableInterval interval;

	private final T insideValue;

	private final T outsideValue;

	public BoxRealRandomAccessible( final Interval interval, final T insideValue, final T outsideValue )
	{
		n = interval.numDimensions();
		this.interval = new ModifiableInterval( interval );
		this.insideValue = insideValue.copy();
		this.outsideValue = outsideValue.copy();
	}

	@Override
	public int numDimensions()
	{
		return n;
	}

	public class Access extends RealPoint implements RealRandomAccess< T >
	{
		public Access()
		{
			super( BoxRealRandomAccessible.this.n );
		}

		protected Access( final Access a )
		{
			super( a );
		}

		@Override
		public T get()
		{
			return Intervals.contains( interval, this ) ? insideValue : outsideValue;
		}

		@Override
		public Access copy()
		{
			return new Access( this );
		}

		@Override
		public Access copyRealRandomAccess()
		{
			return copy();
		}
	}

	@Override
	public RealRandomAccess< T > realRandomAccess()
	{
		return new Access();
	}

	@Override
	public RealRandomAccess< T > realRandomAccess( final RealInterval interval )
	{
		return new Access();
	}

	public ModifiableInterval getInterval()
	{
		return interval;
	}
}
