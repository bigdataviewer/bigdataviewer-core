/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.util.volatiles;

import net.imglib2.AbstractWrappedInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;

public class VolatileRandomAccessibleIntervalView< T, V extends Volatile< T > >
	extends AbstractWrappedInterval< RandomAccessibleInterval< V > >
	implements VolatileView< T, V >, RandomAccessibleInterval< V >
{
	private final VolatileViewData< T, V > viewData;

	public VolatileRandomAccessibleIntervalView(
			final VolatileViewData< T, V > viewData )
	{
		super( ( RandomAccessibleInterval< V > ) viewData.getImg() );
		this.viewData = viewData;
	}

	@Override
	public VolatileViewData< T, V > getVolatileViewData()
	{
		return viewData;
	}

	@Override
	public RandomAccess< V > randomAccess()
	{
		return sourceInterval.randomAccess();
	}

	@Override
	public RandomAccess< V > randomAccess( final Interval interval )
	{
		return sourceInterval.randomAccess( interval );
	}

	@Override
	public V getType()
	{
		return viewData.getVolatileType();
	}
}
