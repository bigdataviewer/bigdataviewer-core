/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
