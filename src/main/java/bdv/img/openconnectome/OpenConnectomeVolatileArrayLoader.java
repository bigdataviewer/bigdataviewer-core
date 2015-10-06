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
package bdv.img.openconnectome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import bdv.img.cache.CacheArrayLoader;

public class OpenConnectomeVolatileArrayLoader implements CacheArrayLoader< VolatileByteArray >
{
	private VolatileByteArray theEmptyArray;

	final private String tokenUrl;

	final private String mode;

	final private long zMin;

	/**
	 * <p>Create a {@link CacheArrayLoader} for a source provided by the
	 * <a href="http://hssl.cs.jhu.edu/wiki/doku.php?id=randal:hssl:research:brain:data_set_description">Open
	 * Connectome Volume Cutout Service</a>.</p>
	 *
	 * <p>It is created with a base URL, e.g.
	 * <a href="http://openconnecto.me/ocp/ca/kasthuri11">http://openconnecto.me/ocp/ca/kasthuri11</a>
	 * the cell dimensions, and an offset in <em>z</em>.  This offset constitutes the
	 * 0-coordinate in <em>z</em> and should point to the first slice of the
	 * dataset.</p>
	 *
	 * @param baseUrl e.g.
	 * 		<a href="http://openconnecto.me/ocp/ca">http://openconnecto.me/ocp/ca</a>
	 * @param token e.g. "kasthuri11"
	 * @param mode z-scaling mode, either of [null, "", "neariso"]
	 * @param zMin first z-index
	 */
	public OpenConnectomeVolatileArrayLoader(
			final String baseUrl,
			final String token,
			final String mode,
			final long zMin )
	{
		theEmptyArray = new VolatileByteArray( 1, false );
		this.tokenUrl = baseUrl + "/" + token + "/zip/";
		this.mode = "/" + mode + ( mode == null || mode.equals( "" ) ? "" : "/" );
		this.zMin = zMin;
	}

	@Override
	public int getBytesPerElement()
	{
		return 1;
	}

	@Override
	public VolatileByteArray loadArray(
			final int timepoint,
			final int setup,
			final int level,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		try
		{
			return tryLoadArray( timepoint, setup, level, dimensions, min );
		}
		catch ( final OutOfMemoryError e )
		{
			System.gc();
			return tryLoadArray( timepoint, setup, level, dimensions, min );
		}
	}

	public VolatileByteArray tryLoadArray(
			final int timepoint,
			final int setup,
			final int level,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		final byte[] data = new byte[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];

		final StringBuffer url = new StringBuffer( tokenUrl );

		final long z = min[ 2 ] + zMin;

		url.append( level );
		url.append( "/" );
		url.append( min[ 0 ] );
		url.append( "," );
		url.append( min[ 0 ] + dimensions[ 0 ] );
		url.append( "/" );
		url.append( min[ 1 ] );
		url.append( "," );
		url.append( min[ 1 ] +  + dimensions[ 1 ] );
		url.append( "/" );
		url.append( z );
		url.append( "," );
		url.append( z + dimensions[ 2 ] );
		url.append( mode );

		try
		{
			final URL file = new URL( url.toString() );
			final InputStream in = file.openStream();
			final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			final byte[] chunk = new byte[ 4096 ];
			int l;
			for ( l = in.read( chunk ); l > 0; l = in.read( chunk ) )
			    byteStream.write( chunk, 0, l );

			final byte[] zippedData = byteStream.toByteArray();
			final Inflater inflater = new Inflater();
			inflater.setInput( zippedData );
			inflater.inflate( data );
			inflater.end();
			byteStream.close();
		}
		catch ( final IOException e )
		{
			System.out.println( "failed loading x=" + min[ 0 ] + " y=" + min[ 1 ] + " z=" + min[ 2 ] + " url(" + url.toString() + ")" );
		}
		catch( final DataFormatException e )
		{
			System.out.println( "failed unpacking x=" + min[ 0 ] + " y=" + min[ 1 ] + " z=" + min[ 2 ] + " url(" + url.toString() + ")" );
		}

		return new VolatileByteArray( data, true );
	}

	@Override
	public VolatileByteArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileByteArray( numEntities, false );
		return theEmptyArray;
	}
}
