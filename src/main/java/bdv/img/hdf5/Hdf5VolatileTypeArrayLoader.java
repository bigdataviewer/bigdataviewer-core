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
package bdv.img.hdf5;

import bdv.img.cache.CacheArrayLoader;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
import static bdv.export.Hdf5BlockWriterPixelTypes.PixelTypeMaintainer;

public class Hdf5VolatileTypeArrayLoader <VT extends VolatileArrayDataAccess<VT>>
implements CacheArrayLoader< VT >
{
	private final IHDF5Access hdf5Access;
	private final PixelTypeMaintainer px;

	public Hdf5VolatileTypeArrayLoader( final IHDF5Access hdf5Access, final PixelTypeMaintainer px )
	{
		this.hdf5Access = hdf5Access;
		this.px = px;
		//NB: we need a reference 'px' on the (type-specific) object that had created this instance,
		//    so that we can keep using its type-specific methods (in our methods)
	}

	@SuppressWarnings("unchecked")
	@Override
	public VT loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		//NB: we cast back to the VT interface because we know that loadArray() (PixelTypeMaintainer)
		//    always instantiates implementation of this interface
		return (VT) px.loadArray(hdf5Access, timepoint, setup, level, dimensions, min );
	}

	@Override
	public int getBytesPerElement()
	{
		return px.getBytesPerElement();
	}

//	PrintStream log = System.out;
//	public static volatile long pStart = System.currentTimeMillis();
//	public static volatile long pEnd = System.currentTimeMillis();
//	public static volatile long tLoad = 0;
//	public static volatile long sLoad = 0;
//
//	@Override
//	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
//	{
//		final short[] array;
//
//		pStart = System.currentTimeMillis();
//		final long msBetweenLoads = pStart - pEnd;
//		if ( msBetweenLoads > 2 )
//		{
//			log.println( msBetweenLoads + " ms pause before this load." );
//			final StringWriter sw = new StringWriter();
//			final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
//			for ( final StackTraceElement elem : trace )
//				sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
//			log.println( sw.toString() );
//		}
//		final long t0 = System.currentTimeMillis();
//		array = hdf5Access.readShortMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
//		pEnd = System.currentTimeMillis();
//		final long t = System.currentTimeMillis() - t0;
//		final long size = array.length;
//		tLoad += t;
//		sLoad += size;
//		if ( sLoad > 10000000 )
//		{
//			final double megPerSec = sLoad * 2000.0 / ( 1024.0 * 1024.0 * tLoad ); // megabytes read per second
//			log.println( String.format( "%.0f mb/sec ", megPerSec ) );
//			tLoad = 1;
//			sLoad = 1;
//		}
//
//		return new VolatileShortArray( array, true );
//	}
}
