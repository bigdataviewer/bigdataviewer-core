package bdv.img.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import bdv.img.cache.CacheArrayLoader;

public class RemoteVolatileShortArrayLoader implements CacheArrayLoader< VolatileShortArray >
{
	private VolatileShortArray theEmptyArray;

	private final RemoteImageLoader imgLoader;

	public RemoteVolatileShortArrayLoader( final RemoteImageLoader imgLoader )
	{
		theEmptyArray = new VolatileShortArray( 32 * 32 * 32, false );
		this.imgLoader = imgLoader;
	}

	@Override
	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final int index = imgLoader.getCellIndex( timepoint, setup, level, min );
		final short[] data = new short[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
		try
		{
			final URL url = new URL( String.format( "%s?p=cell/%d/%d/%d/%d/%d/%d/%d/%d/%d/%d",
					imgLoader.baseUrl,
					index,
					timepoint,
					setup,
					level,
					dimensions[ 0 ],
					dimensions[ 1 ],
					dimensions[ 2 ],
					min[ 0 ],
					min[ 1 ],
					min[ 2 ] ) );
			final InputStream s = url.openStream();
			final byte[] buf = new byte[ data.length * 2 ];
			for ( int i = 0, l = s.read( buf, 0, buf.length ); l != -1; i += l, l = s.read( buf, i, buf.length - i ) );
			for ( int i = 0, j = 0; i < data.length; ++i, j += 2 )
				data[ i ] = ( short ) ( ( ( buf[ j ] & 0xff ) << 8 ) | ( buf[ j + 1 ] & 0xff ) );
			s.close();
		}
		catch ( final MalformedURLException e )
		{
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		return new VolatileShortArray( data, true );
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

	@Override
	public int getBytesPerElement() {
		return 2;
	}

}
