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
package bdv.viewer.render;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovingAverageTest
{
	@Test
	public void testAverage()
	{
		final double[] values = new double[ 1000 ];
		Arrays.setAll( values, i -> Math.random() );

		final int width = 3;
		final MovingAverage avg = new MovingAverage( width );
		avg.init( 0 );

		for ( int i = 0; i < values.length; ++i )
		{
			avg.add( values[ i ] );
			double expected = average( values, i - width + 1, i + 1 );
			assertEquals( expected, avg.getAverage(), 1e-6 );
		}
	}

	private static double average( final double[] values, final int fromIndex, final int toIndex )
	{
		final int numValues = toIndex - fromIndex;
		double sum = 0;
		for ( int i = fromIndex; i < toIndex; i++ )
			sum += i < 0 ? 0 : values[ i ];
		return sum / numValues;
	}
}
