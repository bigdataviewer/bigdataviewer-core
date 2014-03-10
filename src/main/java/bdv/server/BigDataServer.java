package bdv.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mpicbg.spim.data.SequenceDescription;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jdom2.JDOMException;

import bdv.SequenceViewsLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.VolatileCell;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.remote.RemoteImageLoaderMetaData;

import com.google.gson.Gson;

public class BigDataServer
{
	public static void main( final String[] args ) throws Exception
	{
		final String fn = args.length > 0 ? args[ 0 ] : "/Users/pietzsch/Desktop/data/fibsem.xml";
		final Server server = new Server( 8080 );
		server.setHandler( new CellHandler( fn ) );
		server.start();
		server.join();
	}

	static class CellHandler extends AbstractHandler
	{
		private final VolatileGlobalCellCache< VolatileShortArray > cache;

		private final String metadataJson;

		private final RemoteImageLoaderMetaData metadata;

		private final CacheHints cacheHints;

		public CellHandler( final String xmlFilename ) throws JDOMException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
		{
			final SequenceViewsLoader loader = new SequenceViewsLoader( xmlFilename );
			final SequenceDescription seq = loader.getSequenceDescription();
			final Hdf5ImageLoader imgLoader = ( Hdf5ImageLoader ) seq.imgLoader;
			cache = imgLoader.getCache();
			metadata = new RemoteImageLoaderMetaData( imgLoader, seq.numTimepoints(), seq.numViewSetups() );
			metadataJson = new Gson().toJson( metadata );
			cacheHints = new CacheHints( LoadingStrategy.BLOCKING, 0, false );
		}

		@Override
		public void handle( final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response ) throws IOException, ServletException
		{
			final String cellString = request.getParameter( "p" );
			if ( cellString == null )
				return;
			final String[] parts = cellString.split( "/" );
			if ( parts[ 0 ].equals( "cell" ) )
			{
				final int index = Integer.parseInt( parts[ 1 ] );
				final int timepoint = Integer.parseInt( parts[ 2 ] );
				final int setup = Integer.parseInt( parts[ 3 ] );
				final int level = Integer.parseInt( parts[ 4 ] );
				VolatileCell< VolatileShortArray > cell = cache.getGlobalIfCached( timepoint, setup, level, index, cacheHints );
				if ( cell == null )
				{
					final int[] cellDims = new int[] {
							Integer.parseInt( parts[ 5 ] ),
							Integer.parseInt( parts[ 6 ] ),
							Integer.parseInt( parts[ 7 ] ) };
					final long[] cellMin = new long[] {
							Long.parseLong( parts[ 8 ] ),
							Long.parseLong( parts[ 9 ] ),
							Long.parseLong( parts[ 10 ] ) };
					cell = cache.createGlobal( cellDims, cellMin, timepoint, setup, level, index, cacheHints );
				}

				final short[] data = cell.getData().getCurrentStorageArray();
				final byte[] buf = new byte[ 2 * data.length ];
				for ( int i = 0, j = 0; i < data.length; i++ )
				{
					final short s = data[ i ];
					buf[ j++ ] = ( byte ) ( ( s >> 8 ) & 0xff );
					buf[ j++ ] = ( byte ) ( s & 0xff );
				}

				response.setContentType( "application/octet-stream" );
				response.setContentLength( buf.length );
				response.setStatus( HttpServletResponse.SC_OK );
				baseRequest.setHandled( true );
				final OutputStream os = response.getOutputStream();
				os.write( buf );
				os.close();
			}
			else if ( parts[ 0 ].equals( "init" ) )
			{
				response.setContentType( "application/octet-stream" );
				response.setStatus( HttpServletResponse.SC_OK );
				baseRequest.setHandled( true );
				final PrintWriter ow = response.getWriter();
				ow.write( metadataJson );
				ow.close();
			}
		}
	}
}
