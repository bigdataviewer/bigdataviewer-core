package bdv.img.catmaid;

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import bdv.img.cache.CacheArrayLoader;

public class CatmaidVolatileShortArrayLoader implements CacheArrayLoader< VolatileShortArray >
{
	private VolatileShortArray theEmptyArray;

	private final String baseUrl;

	private final int tileWidth;

	private final int tileHeight;

	public CatmaidVolatileShortArrayLoader( final String baseUrl, final int tileWidth, final int tileHeight )
	{
		theEmptyArray = new VolatileShortArray( 256 * 256, false );
		this.baseUrl = baseUrl;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	@Override
	public int getBytesPerElement()
	{
		return 2;
	}

	@Override
	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final int c = ( int ) min[ 0 ] / tileWidth;
		final int r = ( int ) min[ 1 ] / tileHeight;
		final int z = ( int ) min[ 2 ];
		final int s = level;
		final String urlString =
				new
					StringBuffer( baseUrl ).
					append( z ).
					append( "/" ).
					append( r ).
					append( "_" ).
					append( c ).
					append( "_" ).
					append( s ).
					append( ".jpg" ).
					toString();
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

		final short[] sdata = new short[ data.length ];
		for ( int i = 0; i < data.length; ++i )
			sdata[ i ] = ( short ) ( data[ i ] & 0x000000ff );
		return new VolatileShortArray( sdata, true );
	}

	@Override
	public VolatileShortArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileShortArray( numEntities, false );
		return theEmptyArray;
	}

}
