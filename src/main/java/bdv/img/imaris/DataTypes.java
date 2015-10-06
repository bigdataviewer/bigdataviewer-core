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
package bdv.img.imaris;

import bdv.img.cache.CacheArrayLoader;
import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

class DataTypes
{
	static interface DataType<
			T extends NativeType< T >,
			V extends Volatile< T > & NativeType< V > ,
			A extends VolatileAccess >
	{
		public T getType();

		public V getVolatileType();

		public T createLinkedType( NativeImg< T, A > img );

		public V createLinkedVolatileType( NativeImg< V, A > img );

		public CacheArrayLoader< A > createArrayLoader( final IHDF5Access hdf5Access );
	}

	static DataType< UnsignedByteType, VolatileUnsignedByteType, VolatileByteArray > UnsignedByte =
			new DataType< UnsignedByteType, VolatileUnsignedByteType, VolatileByteArray >()
	{
		private final UnsignedByteType type = new UnsignedByteType();

		private final VolatileUnsignedByteType volatileType = new VolatileUnsignedByteType();

		@Override
		public UnsignedByteType getType()
		{
			return type;
		}

		@Override
		public VolatileUnsignedByteType getVolatileType()
		{
			return volatileType;
		}

		@Override
		public UnsignedByteType createLinkedType( final NativeImg< UnsignedByteType, VolatileByteArray > img )
		{
			return new UnsignedByteType( img );
		}

		@Override
		public VolatileUnsignedByteType createLinkedVolatileType( final NativeImg< VolatileUnsignedByteType, VolatileByteArray > img )
		{
			return new VolatileUnsignedByteType( img );
		}

		@Override
		public CacheArrayLoader< VolatileByteArray > createArrayLoader( final IHDF5Access hdf5Access )
		{
			return new ImarisVolatileByteArrayLoader( hdf5Access );
		}
	};

	static DataType< UnsignedShortType, VolatileUnsignedShortType, VolatileShortArray > UnsignedShort =
			new DataType< UnsignedShortType, VolatileUnsignedShortType, VolatileShortArray >()
	{
		private final UnsignedShortType type = new UnsignedShortType();

		private final VolatileUnsignedShortType volatileType = new VolatileUnsignedShortType();

		@Override
		public UnsignedShortType getType()
		{
			return type;
		}

		@Override
		public VolatileUnsignedShortType getVolatileType()
		{
			return volatileType;
		}

		@Override
		public UnsignedShortType createLinkedType( final NativeImg< UnsignedShortType, VolatileShortArray > img )
		{
			return new UnsignedShortType( img );
		}

		@Override
		public VolatileUnsignedShortType createLinkedVolatileType( final NativeImg< VolatileUnsignedShortType, VolatileShortArray > img )
		{
			return new VolatileUnsignedShortType( img );
		}

		@Override
		public CacheArrayLoader< VolatileShortArray > createArrayLoader( final IHDF5Access hdf5Access )
		{
			return new ImarisVolatileShortArrayLoader( hdf5Access );
		}
	};

	static DataType< FloatType, VolatileFloatType, VolatileFloatArray > Float =
			new DataType< FloatType, VolatileFloatType, VolatileFloatArray >()
	{
		private final FloatType type = new FloatType();

		private final VolatileFloatType volatileType = new VolatileFloatType();

		@Override
		public FloatType getType()
		{
			return type;
		}

		@Override
		public VolatileFloatType getVolatileType()
		{
			return volatileType;
		}

		@Override
		public FloatType createLinkedType( final NativeImg< FloatType, VolatileFloatArray > img )
		{
			return new FloatType( img );
		}

		@Override
		public VolatileFloatType createLinkedVolatileType( final NativeImg< VolatileFloatType, VolatileFloatArray > img )
		{
			return new VolatileFloatType( img );
		}

		@Override
		public CacheArrayLoader< VolatileFloatArray > createArrayLoader( final IHDF5Access hdf5Access )
		{
			return new ImarisVolatileFloatArrayLoader( hdf5Access );
		}
	};
}
