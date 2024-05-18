/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
