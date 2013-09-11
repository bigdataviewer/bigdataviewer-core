package creator.tiles;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import creator.tiles.CellVoyagerDataExporter.ChannelInfo;

public class TileImgLoader implements ImgLoader
{

	private static final Namespace NAMESPACE = Namespace.getNamespace( "bts", "http://www.yokogawa.co.jp/BTS/BTSSchema/1.0" );

	private final File imageIndexFile;

	private Document document;

	private final List< ChannelInfo > channelInfos;

	public TileImgLoader( final File imageIndexFile, final List< ChannelInfo > channelInfos )
	{

		if ( !imageIndexFile.exists() ) { throw new IllegalArgumentException( "The target file " + imageIndexFile + " does not exist." ); }
		if ( !imageIndexFile.isFile() ) { throw new IllegalArgumentException( "The target file " + imageIndexFile + " is not a file." ); }

		this.imageIndexFile = imageIndexFile;
		this.channelInfos = channelInfos;
		final SAXBuilder builder = new SAXBuilder();
		try
		{
			document = builder.build( imageIndexFile );
		}
		catch ( final JDOMException e )
		{
			throw new IllegalArgumentException( "The target file " + imageIndexFile + " is malformed:\n" + e.getMessage() );
		}
		catch ( final IOException e )
		{
			throw new IllegalArgumentException( "Trouble reading " + imageIndexFile + ":\n" + e.getMessage() );
		}

		if ( !document.getRootElement().getName().equals( "ImageIndex" ) ) { throw new IllegalArgumentException( "The target file " + imageIndexFile + " is not a CellVoyager Image Index file." ); }

	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view )
	{

		final int setupIndex = view.getSetupIndex();
		final int viewTimePoint = view.getTimepointIndex() + 1; // FIXME
		final ChannelInfo channelInfo = channelInfos.get( setupIndex );

		final int viewChannel = channelInfo.channelNumber;

		/*
		 * Collect file names
		 */

		// Map of z -> all the tiles. The tiles are a map of field index ->
		// filename
		final TreeMap< Double, Map< Integer, String > > filenames = new TreeMap< Double, Map< Integer, String > >();
		final Element root = document.getRootElement();

		for ( final Element element : root.getChildren( "MeasurementRecord", NAMESPACE ) )
		{
			int field;
			int timepoint;
			int channel;
			double z;
			boolean isMIP;
			try
			{
				field = element.getAttribute( "FieldIndex", NAMESPACE ).getIntValue();
				timepoint = element.getAttribute( "TimePoint", NAMESPACE ).getIntValue();
				z = element.getAttribute( "Z", NAMESPACE ).getDoubleValue();
				channel = element.getAttribute( "Ch", NAMESPACE ).getIntValue();
				isMIP = element.getAttribute( "Mip", NAMESPACE ).getBooleanValue();
			}
			catch ( final DataConversionException e )
			{
				System.err.println( "Incorrect attribute formatting for " + element );
				continue;
			}

			if ( isMIP || timepoint != viewTimePoint || channel != viewChannel )
			{
				continue;
			}

			final String filename = element.getText();
			final Double dz = Double.valueOf( z );
			Map< Integer, String > tilesAtZ = filenames.get( dz );
			if ( null == tilesAtZ )
			{
				tilesAtZ = new HashMap< Integer, String >();
				filenames.put( dz, tilesAtZ );
			}
			tilesAtZ.put( field, filename );

		}

		/*
		 * Build stack
		 */

		final long[] dimensions = new long[] { view.getSetup().getWidth(), view.getSetup().getHeight(), view.getSetup().getDepth() };
		final ArrayImg< UnsignedShortType, ShortArray > stack = ArrayImgs.unsignedShorts( dimensions );
		final ArrayRandomAccess< UnsignedShortType > randomAccess = stack.randomAccess();

		final Iterator< Map< Integer, String >> iterator = filenames.values().iterator();

		for ( int zindex = 0; zindex < filenames.size(); zindex++ )
		{
			final Map< Integer, String > tilesFilenames = iterator.next();

			for ( final Integer fieldNumber : tilesFilenames.keySet() )
			{
				final int fieldIndex = fieldNumber - 1;

				// Filename for this Z, this field
				String filename = tilesFilenames.get( fieldNumber );

				// Comply to local path separator
				filename = filename.replace( '\\', File.separatorChar );

				// Offset for this field index
				final long[] offset = channelInfo.offsets.get( fieldIndex );

				// Open and copy this slice on the stack image
				randomAccess.setPosition( zindex, 2 );
				final File filePath = new File( imageIndexFile.getParentFile(), filename );
				final ImagePlus imp = new ImagePlus( filePath.getAbsolutePath() );
				final Img< UnsignedShortType > sliceImg = ImageJFunctions.wrapShort( imp );

				final Cursor< UnsignedShortType > cursor = sliceImg.cursor();
				while ( cursor.hasNext() )
				{
					cursor.fwd();
					randomAccess.setPosition( offset[ 0 ] + cursor.getLongPosition( 0 ), 0 );
					randomAccess.setPosition( offset[ 1 ] + cursor.getLongPosition( 1 ), 1 );
					randomAccess.get().set( cursor.get() );

				}

			}

		}

		return stack;
	}

	/*
	 * UNUSED METHODS
	 */

	@Override
	public Element toXml( final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public RandomAccessibleInterval< FloatType > getImage( final View view )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

}
