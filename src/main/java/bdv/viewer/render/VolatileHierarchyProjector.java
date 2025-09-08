/*
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
package bdv.viewer.render;

import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.operators.SetZero;

/**
 * {@link VolatileProjector} for a hierarchy of {@link Volatile} inputs.
 * After each {@link #map()} call, the projector has a {@link #isValid() state}
 * that signalizes whether all projected pixels were perfect.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch
 */
public class VolatileHierarchyProjector< A extends Volatile< ? >, B extends SetZero > extends AbstractVolatileHierarchyProjector< A, B >
{
	public VolatileHierarchyProjector(
			final List< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target )
	{
		this( sources, converter, target, new byte[ ( int ) ( target.dimension( 0 ) * target.dimension( 1 ) ) ] );
	}

	public VolatileHierarchyProjector(
			final List< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final byte[] maskArray )
	{
		super( sources, converter, target, maskArray );
	}

	@Override
	int processLine(final RandomAccess< A > sourceRandomAccess, final RandomAccess< B > targetRandomAccess, final byte resolutionIndex, final int width, final int y )
	{
		int numInvalidPixels = 0;
		final int mi = y * width;
		for ( int x = 0; x < width; ++x )
		{
			if ( mask[ mi + x ] > resolutionIndex )
			{
				final A a = sourceRandomAccess.get();
				final boolean v = a.isValid();
				if ( v )
				{
					converter.convert( a, targetRandomAccess.get() );
					mask[ mi + x ] = resolutionIndex;
				}
				else
					++numInvalidPixels;
			}
			sourceRandomAccess.fwd( 0 );
			targetRandomAccess.fwd( 0 );
		}
		return numInvalidPixels;
	}
}
