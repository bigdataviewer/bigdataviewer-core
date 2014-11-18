package bdv.util;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;

/**
 * A {@link RandomAccessible} that has a given constant value everywhere.
 */
public class ConstantRandomAccessible< T > implements RandomAccessible< T >
{
	protected class ConstantRandomAccess extends Point implements RandomAccess< T >
	{
		public ConstantRandomAccess()
		{
			super( nDimensions );
		}

		@Override
		public T get()
		{
			return constant;
		}

		@Override
		public ConstantRandomAccess copy()
		{
			return new ConstantRandomAccess();
		}

		@Override
		public ConstantRandomAccess copyRandomAccess()
		{
			return new ConstantRandomAccess();
		}
	}

	private final int nDimensions;

	private final T constant;

	public ConstantRandomAccessible( final T constant, final int nDimensions )
	{
		this.nDimensions = nDimensions;
		this.constant = constant;
	}

	@Override
	public int numDimensions()
	{
		return nDimensions;
	}

	@Override
	public RandomAccess< T > randomAccess()
	{
		return new ConstantRandomAccess();
	}

	@Override
	public RandomAccess< T > randomAccess( final Interval interval )
	{
		return new ConstantRandomAccess();
	}
}
