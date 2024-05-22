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

import java.util.function.Function;

import bdv.viewer.Interpolation;
import net.imglib2.RandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.NumericType;

public class DefaultInterpolators< T extends NumericType< T > > implements Function< Interpolation, InterpolatorFactory< T, RandomAccessible< T > > >
{
	private final InterpolatorFactory< T, RandomAccessible< T > >[] factories;

	@SuppressWarnings( "unchecked" )
	public DefaultInterpolators()
	{
		factories = new InterpolatorFactory[ Interpolation.values().length  ];
		factories[ Interpolation.NEARESTNEIGHBOR.ordinal() ] = new NearestNeighborInterpolatorFactory<>();
		factories[ Interpolation.NLINEAR.ordinal() ] = new ClampingNLinearInterpolatorFactory<>();
	}

	public InterpolatorFactory< T, RandomAccessible< T > > get( final Interpolation method )
	{
		return factories[ method.ordinal() ];
	}

	public int size()
	{
		return factories.length;
	}

	@Override
	public InterpolatorFactory< T, RandomAccessible< T > > apply( final Interpolation t )
	{
		return get( t );
	}
}
