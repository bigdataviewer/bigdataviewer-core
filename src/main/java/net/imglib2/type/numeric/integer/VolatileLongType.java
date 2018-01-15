/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package net.imglib2.type.numeric.integer;

import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.VolatileLongAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.type.PrimitiveTypeInfo;
import net.imglib2.type.numeric.AbstractVolatileNativeRealType;

/**
 * A {@link Volatile} variant of {@link LongType}. It uses an
 * underlying {@link LongType} that maps into a
 * {@link VolatileLongAccess}.
 *
 * @author Stephan Saalfeld
 */
public class VolatileLongType extends AbstractVolatileNativeRealType< LongType, VolatileLongType >
{
	final protected NativeImg< ?, ? extends VolatileLongAccess > img;

	// this is the constructor if you want it to read from an array
	public VolatileLongType( final NativeImg< ?, ? extends VolatileLongAccess > img )
	{
		super( new LongType( img ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileLongType( final VolatileLongAccess access )
	{
		super( new LongType( access ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileLongType( final long value )
	{
		this( new VolatileLongArray( 1, true ) );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public VolatileLongType()
	{
		this( 0 );
	}

	public void set( final long value )
	{
		get().set( value );
	}

	@Override
	public void updateContainer( final Object c )
	{
		final VolatileLongAccess a = img.update( c );
		t.dataAccess = a;
		setValid( a.isValid() );
	}

	@Override
	public VolatileLongType duplicateTypeOnSameNativeImg()
	{
		return new VolatileLongType( img );
	}

	@Override
	public VolatileLongType createVariable()
	{
		return new VolatileLongType();
	}

	@Override
	public VolatileLongType copy()
	{
		final VolatileLongType v = createVariable();
		v.set( this );
		return v;
	}

	private static final PrimitiveTypeInfo< VolatileLongType, VolatileLongAccess > info = PrimitiveTypeInfo.LONG( img -> new VolatileLongType( img ) );

	@Override
	public PrimitiveTypeInfo< VolatileLongType, ? > getPrimitiveTypeInfo()
	{
		return info;
	}
}
