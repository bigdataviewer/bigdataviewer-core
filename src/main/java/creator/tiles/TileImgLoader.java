package creator.tiles;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
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

public class TileImgLoader implements ImgLoader
{

	private static final String IMAGE_INDEX_FILENAME = "ImageIndex.xml";

	private static final Namespace NAMESPACE = Namespace.getNamespace( "bts", "http://www.yokogawa.co.jp/BTS/BTSSchema/1.0" );

	private final File rootFolder;

	private Document document;

	private final int nChannels;

	public TileImgLoader( final File rootFolder, final int nChannels )
	{
		this.rootFolder = rootFolder;
		this.nChannels = nChannels;
		final SAXBuilder builder = new SAXBuilder();
		try
		{
			document = builder.build( new File( rootFolder, IMAGE_INDEX_FILENAME ) );
		}
		catch ( final JDOMException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		final int viewTimePoint = view.getTimepointIndex() + 1; // 1-based in
																// the file
																// FIXME

		final int viewFieldIndex = view.getSetupIndex() / nChannels;
		final int viewChannel = view.getSetup().getChannel();

		/*
		 * Collect file names
		 */

		final Element root = document.getRootElement();
		final TreeMap< Double, String > filenames = new TreeMap< Double, String >();

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

			if ( isMIP || field != viewFieldIndex || timepoint != viewTimePoint || channel != viewChannel )
			{
				continue;
			}

			final String filename = element.getText();
			final Double dz = Double.valueOf( z );
			filenames.put( dz, filename );

		}

		/*
		 * Build stack
		 */

		final long[] dimensions = new long[] { view.getSetup().getWidth(), view.getSetup().getHeight(), view.getSetup().getDepth() };
		final ArrayImg< UnsignedShortType, ShortArray > stack = ArrayImgs.unsignedShorts( dimensions );
		final ArrayRandomAccess< UnsignedShortType > randomAccess = stack.randomAccess();

		final Iterator< String > iterator = filenames.values().iterator();

		for ( int zindex = 0; zindex < filenames.size(); zindex++ )
		{
			String filename = iterator.next();

			// Comply to local path separator
			filename = filename.replace( '\\', File.separatorChar );

			// Open and copy this slice on the stack image
			randomAccess.setPosition( zindex, 2 );
			final ImagePlus imp = new ImagePlus( new File( rootFolder, filename ).getAbsolutePath() );
			final Img< UnsignedShortType > sliceImg = ImageJFunctions.wrapShort( imp );

			final Cursor< UnsignedShortType > cursor = sliceImg.cursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				randomAccess.setPosition( cursor.getLongPosition( 0 ), 0 );
				randomAccess.setPosition( cursor.getLongPosition( 1 ), 1 );
				randomAccess.get().set( cursor.get() );

			}

		}

		return stack;
	}

	/*
	 * UNUSED METHODS
	 */

	@Override
	public RandomAccessibleInterval< FloatType > getImage( final View view )
	{
		System.out.println( "[TileimgLoader.getImage is not implemented." );
		return null;
	}

	@Override
	public void init( final org.w3c.dom.Element elem, final File basePath )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public org.w3c.dom.Element toXml( final org.w3c.dom.Document doc, final File basePath )
	{
		// TODO Auto-generated method stub
		return null;
	}

}
