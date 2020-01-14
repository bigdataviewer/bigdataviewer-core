package bdv.util;

import org.junit.Assert;
import org.junit.Test;

public class BoundedRangeTest
{
	@Test
	public void testConstructor()
	{
		new BoundedRange( 0, 1, 0, 1 );
	}

	@Test( expected = IllegalArgumentException.class )
	public void testConstructorFails1()
	{
		new BoundedRange( 0, 1, -1, 1 );
	}

	@Test( expected = IllegalArgumentException.class )
	public void testConstructorFails2()
	{
		new BoundedRange( 0, -1, 0, 0 );
	}

	@Test( expected = IllegalArgumentException.class )
	public void testConstructorFails3()
	{
		new BoundedRange( 0, 11, 1, 0 );
	}

	@Test
	public void testWithMin()
	{
		Assert.assertEquals(
				new BoundedRange( 0, 1, 0, 1 ).withMin( -1 ),
				new BoundedRange( -1, 1, -1, 1 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 1, 0, 1 ).withMin( 0.5 ),
				new BoundedRange( 0, 1, 0.5, 1 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 1, 0, 1 ).withMin( 2 ),
				new BoundedRange( 0, 2, 2, 2 )
		);
	}

	@Test
	public void testWithMax()
	{
		Assert.assertEquals(
				new BoundedRange( 0, 1, 0, 1 ).withMax( 2 ),
				new BoundedRange( 0, 2, 0, 2 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 1, 0, 1 ).withMax( 0.5 ),
				new BoundedRange( 0, 1, 0, 0.5 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 1, 0, 1 ).withMax( -1 ),
				new BoundedRange( -1, 1, -1, -1 )
		);
	}

	@Test
	public void testWithMinBound()
	{
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).withMinBound( -1 ),
				new BoundedRange( -1, 3, 1, 2 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).withMinBound( 0.5 ),
				new BoundedRange( 0.5, 3, 1, 2 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).withMinBound( 1.5 ),
				new BoundedRange( 1.5, 3, 1.5, 2 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).withMinBound( 4 ),
				new BoundedRange( 4, 4, 4, 4 )
		);
	}

	@Test
	public void testWithMaxBound()
	{
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).withMaxBound( 4 ),
				new BoundedRange( 0, 4, 1, 2 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).withMaxBound( 2.5 ),
				new BoundedRange( 0, 2.5, 1, 2 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).withMaxBound( 1.5 ),
				new BoundedRange( 0, 1.5, 1, 1.5 )
		);
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).withMaxBound( -1 ),
				new BoundedRange( -1, -1, -1, -1 )
		);
	}

	@Test
	public void testJoin()
	{
		Assert.assertEquals(
				new BoundedRange( 0, 3, 1, 2 ).join( new BoundedRange( 5, 8, 6, 7 ) ),
				new BoundedRange( 0, 8, 1, 7 )
		);
	}
}
