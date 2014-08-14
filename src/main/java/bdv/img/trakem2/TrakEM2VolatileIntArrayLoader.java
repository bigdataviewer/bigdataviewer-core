package bdv.img.trakem2;

import ij.ImagePlus;
import ini.trakem2.display.Displayable;
import ini.trakem2.display.LayerSet;
import ini.trakem2.persistence.Loader;

import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.PixelGrabber;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import bdv.img.cache.CacheArrayLoader;

public class TrakEM2VolatileIntArrayLoader implements CacheArrayLoader< VolatileIntArray >
{
	private VolatileIntArray theEmptyArray;

	final private Loader loader;
	
	final private LayerSet layerset;
	
	final private int[] zScales;
	
	/**
	 * <p>Create a {@link CacheArrayLoader} for a TrakEM2 source.  Tiles are
	 * addressed by their pixel position and dimension.</p>
	 *  
	 * @param 
	 * @param tileWidth
	 * @param tileHeight
	 */
	public TrakEM2VolatileIntArrayLoader( final Loader loader, final LayerSet layerset, final int[] zScales )
	{
		theEmptyArray = new VolatileIntArray(1, false );
		this.loader = loader;
		this.layerset = layerset;
		this.zScales = zScales;
	}
	
	@Override
	public int getBytesPerElement()
	{
		return 4;
	}

	@Override
	public VolatileIntArray loadArray(
			final int timepoint,
			final int setup,
			final int level,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		final int iScale = 1 << level;
		final double scale = 1.0 / iScale;
		final int w = dimensions[ 0 ] * iScale;
		final int h = dimensions[ 1 ] * iScale;
		final Rectangle box = new Rectangle( ( int )( min[ 0 ] * iScale ), ( int )( min[ 1 ] * iScale ), w, h );
		
		final int[] data = new int[ dimensions[ 0 ] * dimensions[ 1 ] ];
		try
		{
			if ( zScales[ level ] > 1 )
			{
				final long[] rs = new long[ data.length ], gs = new long[ data.length ], bs = new long[ data.length ];
				
				for ( int z = ( int )min[ 2 ] * zScales[ level ], dz = 0; dz < zScales[ level ]; ++dz )
				{
					final Image image = loader.getFlatAWTImage(
							layerset.getLayer( z + dz ),
							box,
							scale,
							0xffffffff,
							ImagePlus.COLOR_RGB,
							Displayable.class,
							null,
							true,
							new Color( 0x00000000, true ) );
					final PixelGrabber pg = new PixelGrabber( image, 0, 0, dimensions[ 0 ], dimensions[ 1 ], data, 0, dimensions[ 0 ] );
					pg.grabPixels();
					for ( int i = 0; i < data.length; ++i )
					{
						rs[ i ] += ( data[ i ] >> 16 ) & 0xff;
						gs[ i ] += ( data[ i ] >> 8 ) & 0xff;
						bs[ i ] += data[ i ] & 0xff;
					}
				}
				for ( int i = 0; i < data.length; ++i )
				{
					final int r = ( int )( rs[ i ] / zScales[ level ] );
					final int g = ( int )( gs[ i ] / zScales[ level ] );
					final int b = ( int )( bs[ i ] / zScales[ level ] );
					
					data[ i ] = ( ( ( ( r << 8 ) | g ) << 8 ) | b ) | 0xff000000;
				}
			}
			else
			{
				final Image image = loader.getFlatAWTImage(
						layerset.getLayer( ( int )min[ 2 ] ),
						box,
						scale,
						0xffffffff,
						ImagePlus.COLOR_RGB,
						Displayable.class,
						null,
						true,
						new Color( 0x00000000, true ) );
				final PixelGrabber pg = new PixelGrabber( image, 0, 0, dimensions[ 0 ], dimensions[ 1 ], data, 0, dimensions[ 0 ] );
				pg.grabPixels();
			}

//			System.out.println( "success loading r=" + entry.key.r + " c=" + entry.key.c + " url(" + urlString + ")" );

		}
		catch ( final Exception e )
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
