/*
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

import java.util.function.Supplier;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.DefaultVoxelDimensions;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public abstract class AbstractSource< T extends NumericType< T > > implements Source< T >
{
	protected final T type;

	protected final String name;

	protected final VoxelDimensions voxelDimensions;

	protected final DefaultInterpolators< T > interpolators;

	private final boolean doBoundingBoxCulling;

	public AbstractSource( final T type, final String name, final VoxelDimensions voxelDimensions, final boolean doBoundingBoxCulling )
	{
		this.type = tryCreateVariable( type );
		this.name = name;
		this.voxelDimensions = voxelDimensions;
		interpolators = new DefaultInterpolators<>();
		this.doBoundingBoxCulling = doBoundingBoxCulling;
	}

	public AbstractSource( final T type, final String name, final VoxelDimensions voxelDimensions )
	{
		/*
		 * Do bounding box culling by default
		 */
		this( type, name, voxelDimensions, true );
	}

	public AbstractSource( final T type, final String name )
	{
		/*
		 * We don't know the dimensionality of the source here, but the
		 * DefaultVoxelDimensionsimplementation will return the same result
		 * for spacing and units regardless of the number of dimensions passed.
		 */
		this( type, name, new DefaultVoxelDimensions( -1 ), true );
	}

	public AbstractSource( final T type, final String name, final boolean doBoundingBoxCulling )
	{
		this( type, name, new DefaultVoxelDimensions( -1 ), doBoundingBoxCulling );
	}

	public AbstractSource( final Supplier< T > typeSupplier, final String name )
	{
		this( typeSupplier.get(), name );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return true;
	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return Views.interpolate( Views.extendZero( getSource( t, level ) ), interpolators.get( method ) );
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 1;
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		return doBoundingBoxCulling;
	}

	static < T > T tryCreateVariable( final T t )
	{
		if ( t instanceof Type< ? > )
		{
			final Type< ? > type = ( Type< ? > ) t;
			@SuppressWarnings( "unchecked" )
			final T copy = ( T ) type.createVariable();
			return copy;
		}
		return t;
	}
}
