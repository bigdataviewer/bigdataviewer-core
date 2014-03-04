package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.SequenceDescription;
import net.imglib2.img.basictypeaccess.array.ShortArray;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.xml.sax.SAXException;

import viewer.SequenceViewsLoader;
import viewer.hdf5.Hdf5ImageLoader;
import viewer.hdf5.Hdf5ImageLoader.DimensionsInfo;
import viewer.hdf5.img.Hdf5Cell;
import viewer.hdf5.img.Hdf5GlobalCellCache;

public class CellServer
{
	public static void main( final String[] args ) throws Exception
	{
		final String fn = args.length > 0 ? args[ 0 ] : "/Users/pietzsch/Desktop/Valia/valia.xml";
		final Server server = new Server( 8080 );
		server.setHandler( new CellHandler( fn ) );
		server.start();
		server.join();
	}

	static class CellHandler extends AbstractHandler
	{
		final Hdf5ImageLoader imgLoader;

		final Hdf5GlobalCellCache< ShortArray > cache;

		final byte[] initObjects;

		public CellHandler( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
		{
			final SequenceViewsLoader loader = new SequenceViewsLoader( xmlFilename );
			final SequenceDescription seq = loader.getSequenceDescription();
			imgLoader = ( Hdf5ImageLoader ) seq.imgLoader;
			cache = imgLoader.getCache();

			final ArrayList< double[][] > perSetupResolutions = new ArrayList< double[][] >();
			for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
				perSetupResolutions.add( imgLoader.getMipmapResolutions( setup ) );
			final ByteArrayOutputStream bs = new ByteArrayOutputStream();
			final ObjectOutputStream os = new ObjectOutputStream( bs );
			os.writeInt( seq.numTimepoints() );
			os.writeObject( perSetupResolutions );
			os.close();
			initObjects = bs.toByteArray();
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
				Hdf5Cell< ShortArray > cell = cache.getGlobalIfCached( timepoint, setup, level, index );
				if ( cell == null )
				{
					final int[] cellDims = new int[] { Integer.parseInt( parts[ 5 ] ), Integer.parseInt( parts[ 6 ] ), Integer.parseInt( parts[ 7 ] ) };
					final long[] cellMin = new long[] { Long.parseLong( parts[ 8 ] ), Long.parseLong( parts[ 9 ] ), Long.parseLong( parts[ 10 ] ) };
					cell = cache.loadGlobal( cellDims, cellMin, timepoint, setup, level, index );
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

//				final byte[] buf = new byte[ 4096 ];
//				for ( int i = 0, l = Math.min( buf.length / 2, data.length - i ); i < data.length; i += l )
//				{
//					ByteBuffer.wrap( buf ).asShortBuffer().put( data, i, l );
//					os.write( buf, 0, 2 * l );
//				}
//				os.close();
			}
			else if ( parts[ 0 ].equals( "dim" ) )
			{
				final int timepoint = Integer.parseInt( parts[ 1 ] );
				final int setup = Integer.parseInt( parts[ 2 ] );
				final int level = Integer.parseInt( parts[ 3 ] );
				final DimensionsInfo info = imgLoader.getDimensionsInfo( timepoint, setup, level );

				response.setContentType( "application/octet-stream" );
				response.setStatus( HttpServletResponse.SC_OK );
				baseRequest.setHandled( true );
				final ObjectOutputStream os = new ObjectOutputStream( response.getOutputStream() );
				os.writeObject( info );
				os.close();
			}
			else if ( parts[ 0 ].equals( "init" ) )
			{
				response.setContentType( "application/octet-stream" );
				response.setStatus( HttpServletResponse.SC_OK );
				baseRequest.setHandled( true );
				final OutputStream os = response.getOutputStream();
				os.write( initObjects );
				os.close();
			}
		}
	}
}
