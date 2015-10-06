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
package bdv.img.catmaid;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import bdv.img.cache.CacheArrayLoader;

public class CatmaidVolatileIntArrayLoader implements CacheArrayLoader< VolatileIntArray >
{
	private VolatileIntArray theEmptyArray;

	private final String urlFormat;

	private final int tileWidth;

	private final int tileHeight;

	final private int[] zScales;

	/**
	 * <p>Create a {@link CacheArrayLoader} for a CATMAID source.  Tiles are
	 * addressed, in this order, by their</p>
	 * <ul>
	 * <li>scale level,</li>
	 * <li>scale,</li>
	 * <li>x,</li>
	 * <li>y,</li>
	 * <li>z,</li>
	 * <li>tile width,</li>
	 * <li>tile height,</li>
	 * <li>tile row, and</li>
	 * <li>tile column.</li>
	 * </ul>
	 * <p><code>urlFormat</code> specifies how these parameters are used
	 * to generate a URL referencing the tile.  Examples:</p>
	 *
	 * <dl>
	 * <dt>"http://catmaid.org/my-data/xy/%5$d/%8$d_%9$d_%1$d.jpg"</dt>
	 * <dd>CATMAID DefaultTileSource (type 1)</dd>
	 * <dt>"http://catmaid.org/my-data/xy/?x=%3$d&amp;y=%4$d&amp;width=%6d&amp;height=%7$d&amp;row=%8$d&amp;col=%9$d&amp;scale=%2$f&amp;z=%4$d"</dt>
     * <dd>CATMAID RequestTileSource (type 2)</dd>
	 * <dt>"http://catmaid.org/my-data/xy/%1$d/%5$d/%8$d/%9$d.jpg"</dt>
	 * <dd>CATMAID LargeDataTileSource (type 5)</dd>
	 * </dl>
	 *
	 * @param urlFormat
	 * @param tileWidth
	 * @param tileHeight
	 */
	public CatmaidVolatileIntArrayLoader( final String urlFormat, final int tileWidth, final int tileHeight, final int[] zScales )
	{
		theEmptyArray = new VolatileIntArray( tileWidth * tileHeight, false );
		this.urlFormat = urlFormat;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.zScales = zScales;
	}

	@Override
	public int getBytesPerElement()
	{
		return 4;
	}

	final private void loadSliceArray(
			final int[] slice,
			final int level,
			final double scale,
			final long c0,
			final long r0,
			final long x0,
			final long y0,
			final long z,
			final long xm,
			final long ym,
			final long[] min,
			final int w,
			final int h ) throws InterruptedException
	{
		final BufferedImage image = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
		final Graphics2D g2d = image.createGraphics();
		for (
				long c = c0, x = x0;
				x < xm;
				++c, x += tileWidth )
		{
			for (
					long r = r0, y = y0;
					y < ym;
					++r, y += tileHeight )
			{
				try
				{
					final String urlString = String.format( urlFormat, level, scale, x, y, z, tileWidth, tileHeight, r, c );
					final URL url = new URL( urlString );
					final BufferedImage tile = ImageIO.read( url );
					g2d.drawImage( tile, ( int )( x - min[ 0 ] ), ( int )( y - min[ 1 ] ), null );
				}
				catch (final IOException e)
				{
//					System.out.println( "failed loading r=" + r + " c=" + c );
				}
			}
		}

		final PixelGrabber pg = new PixelGrabber( image, 0, 0, w, h, slice, 0, w );
		pg.grabPixels();
	}


	final private void averageSlice(
			final int[] slice,
			final int level,
			final double scale,
			final long c0,
			final long r0,
			final long x0,
			final long y0,
			final long xm,
			final long ym,
			final long[] min,
			final int w,
			final int h ) throws InterruptedException
	{
		final long[] rs = new long[ slice.length ], gs = new long[ slice.length ], bs = new long[ slice.length ];
		for ( int z = ( int ) min[ 2 ] * zScales[ level ], dz = 0; dz < zScales[ level ]; ++dz )
		{
			loadSliceArray( slice, level, scale, c0, r0, x0, y0, z + dz, xm, ym, min, w, h );
			for ( int i = 0; i < slice.length; ++i )
			{
				rs[ i ] += ( slice[ i ] >> 16 ) & 0xff;
				gs[ i ] += ( slice[ i ] >> 8 ) & 0xff;
				bs[ i ] += slice[ i ] & 0xff;
			}
		}
		for ( int i = 0; i < slice.length; ++i )
		{
			final int red = ( int ) ( rs[ i ] / zScales[ level ] );
			final int green = ( int ) ( gs[ i ] / zScales[ level ] );
			final int blue = ( int ) ( bs[ i ] / zScales[ level ] );
			slice[ i ] = ( ( ( ( red << 8 ) | green ) << 8 ) | blue ) | 0xff000000;
		}
	}


	@Override
	public VolatileIntArray loadArray(
			 final int timepoint,
			 final int setup,
			 final int level,
			 final int[] dimensions,
			 final long[] min ) throws InterruptedException
	{
		final int w = dimensions[ 0 ];
		final int h = dimensions[ 1 ];
		final long xm = min[ 0 ] + w;
		final long ym = min[ 1 ] + h;
		final double scale = 1.0 / Math.pow(2.0, level);
		final int[] slice = new int[ w * h ];

		final long c0 = ( long )( min[ 0 ] / tileWidth );
		final long r0 = ( long )( min[ 1 ] / tileHeight );
		final long x0 = c0 * tileWidth;
		final long y0 = r0 * tileHeight;

		final int[] data;
		if ( dimensions[ 2 ] > 1 )
		{
			data = new int[ w * h * dimensions[ 2 ] ];
			final long[] zMin = min.clone();
			for ( int z = 0; z < dimensions[ 2 ]; ++z )
			{
				zMin[ 2 ] = min[ 2 ] + z;
				if ( zScales[ level ] > 1 )
					averageSlice( slice, level, scale, c0, r0, x0, y0, xm, ym, zMin, w, h );
				else
					loadSliceArray( slice, level, scale, c0, r0, x0, y0, zMin[ 2 ], xm, ym, zMin, w, h );

				System.arraycopy( slice, 0, data, z * slice.length, slice.length );
			}
		}
		else
		{
			data = slice;
			if ( zScales[ level ] > 1 )
				averageSlice( slice, level, scale, c0, r0, x0, y0, xm, ym, min, w, h );
			else
				loadSliceArray( slice, level, scale, c0, r0, x0, y0, min[ 2 ], xm, ym, min, w, h );
		}

		return new VolatileIntArray( data, true );
	}

	@Override
	public VolatileIntArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileIntArray( numEntities, false );
		return theEmptyArray;
	}
}
