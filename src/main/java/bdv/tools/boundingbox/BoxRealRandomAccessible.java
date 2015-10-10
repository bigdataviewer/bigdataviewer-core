/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.tools.boundingbox;

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
