package bdv.img.catmaid;

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
	 * <dt>"http://catmaid.org/my-data/xy/?x=%3$d&y=%4$d&width=%6d&height=%7$d&row=%8$d&col=%9$d&scale=%2$f&z=%4$d"</dt>
     * <dd>CATMAID RequestTileSource (type 2)</dd>
	 * <dt>"http://catmaid.org/my-data/xy/%1$d/%5$d/%8$d/%9$d.jpg"</dt>
	 * <dd>CATMAID LargeDataTileSource (type 5)</dd>
	 * </dl>
	 * 
	 * @param urlFormat
	 * @param tileWidth
	 * @param tileHeight
	 */
	public CatmaidVolatileIntArrayLoader( final String urlFormat, final int tileWidth, final int tileHeight )
	{
		theEmptyArray = new VolatileIntArray( tileWidth * tileHeight, false );
		this.urlFormat = urlFormat;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}
	
	@Override
	public int getBytesPerElement()
	{
		return 1;
	}

	@Override
	public VolatileIntArray loadArray(
			final int timepoint,
			final int setup,
			final int level,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		final int c = ( int ) min[ 0 ] / tileWidth;
		final int r = ( int ) min[ 1 ] / tileHeight;
		final double scale = 1.0 / Math.pow(2.0, level);
		
		final String urlString = String.format( urlFormat, level, scale, min[ 0 ], min[ 1 ], min[ 2 ], tileWidth, tileHeight, r, c );
		
		final int w = dimensions[ 0 ];
		final int h = dimensions[ 1 ];
		final int[] data = new int[ w * h ];
		try
		{
			final URL url = new URL( urlString );
//			final Image image = toolkit.createImage( url );
		    final BufferedImage jpg = ImageIO.read( url );

			/* This gymnastic is necessary to get reproducible gray
			 * values, just opening a JPG or PNG, even when saved by
			 * ImageIO, and grabbing its pixels results in gray values
			 * with a non-matching gamma transfer function, I cannot tell
			 * why... */
		    final BufferedImage image = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
			image.createGraphics().drawImage( jpg, 0, 0, null );
			final PixelGrabber pg = new PixelGrabber( image, 0, 0, w, h, data, 0, w );
			pg.grabPixels();

//			System.out.println( "success loading r=" + entry.key.r + " c=" + entry.key.c + " url(" + urlString + ")" );

		}
		catch (final IOException e)
		{
			System.out.println( "failed loading r=" + r + " c=" + c + " url(" + urlString + ")" );
		}
		catch (final InterruptedException e)
		{
			e.printStackTrace();
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
