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

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileByteType;
import net.imglib2.type.volatiles.VolatileDoubleType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileIntType;
import net.imglib2.type.volatiles.VolatileLongType;
import net.imglib2.type.volatiles.VolatileShortType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedIntType;
import net.imglib2.type.volatiles.VolatileUnsignedLongType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

public class VolatileTypeMatcher
{
	public static < T extends NativeType< T > > NativeType< ? > getVolatileTypeForType( final T type )
	{
		if ( type instanceof ARGBType )
			return new VolatileARGBType();
		else if ( type instanceof FloatType )
			return new VolatileFloatType();
		else if ( type instanceof DoubleType )
			return new VolatileDoubleType();
		else if ( type instanceof ByteType )
			return new VolatileByteType();
		else if ( type instanceof ShortType )
			return new VolatileShortType();
		else if ( type instanceof IntType )
			return new VolatileIntType();
		else if ( type instanceof LongType )
			return new VolatileLongType();
		else if ( type instanceof UnsignedByteType )
			return new VolatileUnsignedByteType();
		else if ( type instanceof UnsignedShortType )
			return new VolatileUnsignedShortType();
		else if ( type instanceof UnsignedIntType )
			return new VolatileUnsignedIntType();
		else if ( type instanceof UnsignedLongType )
			return new VolatileUnsignedLongType();
		else
			return null;
	}
}
