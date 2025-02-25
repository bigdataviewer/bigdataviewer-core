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
package bdv.img.cache;

import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileCharArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileCharArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.type.PrimitiveType;

/**
 * {@link EmptyArrayCreator} implementation for standard access types. Maintains
 * one invalid array that grows to the biggest size requested so far. The same
 * array is reused and returned for all
 * {@link EmptyArrayCreator#getEmptyArray(long) requests}.
 *
 * <p>
 * Access types provided through {@link #get(PrimitiveType, boolean)} are
 * </p>
 * <ul>
 * <li>{@link DirtyVolatileByteArray}</li>
 * <li>{@link VolatileByteArray}</li>
 * <li>{@link DirtyVolatileCharArray}</li>
 * <li>{@link VolatileCharArray}</li>
 * <li>{@link DirtyVolatileDoubleArray}</li>
 * <li>{@link VolatileDoubleArray}</li>
 * <li>{@link DirtyVolatileFloatArray}</li>
 * <li>{@link VolatileFloatArray}</li>
 * <li>{@link DirtyVolatileIntArray}</li>
 * <li>{@link VolatileIntArray}</li>
 * <li>{@link DirtyVolatileLongArray}</li>
 * <li>{@link VolatileLongArray}</li>
 * <li>{@link DirtyVolatileShortArray}</li>
 * <li>{@link VolatileShortArray}</li>
 * </ul>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class DefaultEmptyArrayCreator< A extends VolatileArrayDataAccess< A > > implements EmptyArrayCreator< A >
{
	public static final int INITIAL_EMPTY_SIZE = 32 * 32 * 32;

	private final A creator;

	private EmptyArray< A > theEmptyArray;

	static class EmptyArray< A extends VolatileArrayDataAccess< A > >
	{
		A data;

		int numEntities;

		EmptyArray( final int numEntities, final A creator )
		{
			this.data = creator.createArray( numEntities, false );
			this.numEntities = numEntities;
		}
	}

	public DefaultEmptyArrayCreator( final A creator )
	{
		this.creator = creator;
		this.theEmptyArray = new EmptyArray<>( INITIAL_EMPTY_SIZE, creator );
	}

	@Override
	public A getEmptyArray( final long numEntities )
	{
		EmptyArray< A > empty = theEmptyArray;
		if ( empty.numEntities < numEntities )
		{
			empty = new EmptyArray<>( ( int ) numEntities, creator );
			theEmptyArray = empty;
		}
		return empty.data;
	}

	private static DefaultEmptyArrayCreator< DirtyVolatileByteArray > dirtyBytes;

	private static DefaultEmptyArrayCreator< VolatileByteArray > bytes;

	private static DefaultEmptyArrayCreator< DirtyVolatileCharArray > dirtyChars;

	private static DefaultEmptyArrayCreator< VolatileCharArray > chars;

	private static DefaultEmptyArrayCreator< DirtyVolatileDoubleArray > dirtyDoubles;

	private static DefaultEmptyArrayCreator< VolatileDoubleArray > doubles;

	private static DefaultEmptyArrayCreator< DirtyVolatileFloatArray > dirtyFloats;

	private static DefaultEmptyArrayCreator< VolatileFloatArray > floats;

	private static DefaultEmptyArrayCreator< DirtyVolatileIntArray > dirtyInts;

	private static DefaultEmptyArrayCreator< VolatileIntArray > ints;

	private static DefaultEmptyArrayCreator< DirtyVolatileLongArray > dirtyLongs;

	private static DefaultEmptyArrayCreator< VolatileLongArray > longs;

	private static DefaultEmptyArrayCreator< DirtyVolatileShortArray > dirtyShorts;

	private static DefaultEmptyArrayCreator< VolatileShortArray > shorts;

	@SuppressWarnings( "unchecked" )
	public static < A extends VolatileArrayDataAccess< A > > DefaultEmptyArrayCreator< A > get(
			final PrimitiveType primitiveType,
			final boolean dirty )
	{
		switch ( primitiveType )
		{
		case BYTE:
			if ( dirty )
			{
				if ( dirtyBytes == null )
					dirtyBytes = new DefaultEmptyArrayCreator<>( new DirtyVolatileByteArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) dirtyBytes;
			}
			else
			{
				if ( bytes == null )
					bytes = new DefaultEmptyArrayCreator<>( new VolatileByteArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) bytes;
			}
		case CHAR:
			if ( dirty )
			{
				if ( dirtyChars == null )
					dirtyChars = new DefaultEmptyArrayCreator<>( new DirtyVolatileCharArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) dirtyChars;
			}
			else
			{
				if ( chars == null )
					chars = new DefaultEmptyArrayCreator<>( new VolatileCharArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) chars;
			}
		case DOUBLE:
			if ( dirty )
			{
				if ( dirtyDoubles == null )
					dirtyDoubles = new DefaultEmptyArrayCreator<>( new DirtyVolatileDoubleArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) dirtyDoubles;
			}
			else
			{
				if ( doubles == null )
					doubles = new DefaultEmptyArrayCreator<>( new VolatileDoubleArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) doubles;
			}
		case FLOAT:
			if ( dirty )
			{
				if ( dirtyFloats == null )
					dirtyFloats = new DefaultEmptyArrayCreator<>( new DirtyVolatileFloatArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) dirtyFloats;
			}
			else
			{
				if ( floats == null )
					floats = new DefaultEmptyArrayCreator<>( new VolatileFloatArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) floats;
			}
		case INT:
			if ( dirty )
			{
				if ( dirtyInts == null )
					dirtyInts = new DefaultEmptyArrayCreator<>( new DirtyVolatileIntArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) dirtyInts;
			}
			else
			{
				if ( ints == null )
					ints = new DefaultEmptyArrayCreator<>( new VolatileIntArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) ints;
			}
		case LONG:
			if ( dirty )
			{
				if ( dirtyLongs == null )
					dirtyLongs = new DefaultEmptyArrayCreator<>( new DirtyVolatileLongArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) dirtyLongs;
			}
			else
			{
				if ( longs == null )
					longs = new DefaultEmptyArrayCreator<>( new VolatileLongArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) longs;
			}
		case SHORT:
			if ( dirty )
			{
				if ( dirtyShorts == null )
					dirtyShorts = new DefaultEmptyArrayCreator<>( new DirtyVolatileShortArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) dirtyShorts;
			}
			else
			{
				if ( shorts == null )
					shorts = new DefaultEmptyArrayCreator<>( new VolatileShortArray( 0, false ) );
				return ( DefaultEmptyArrayCreator< A > ) shorts;
			}
		case UNDEFINED:
		default:
			return null;
		}
	}
}
