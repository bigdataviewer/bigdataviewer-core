package mpicbg.tracking.data;

import static mpicbg.tracking.data.io.XmlHelpers.loadPath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.tracking.data.io.ImgLoader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SequenceDescription
{
	/**
	 * timepoint id for every timepoint index.
	 */
	final public int[] timepoints;

	/**
	 * angle and illumination setup for every view-setup index.
	 */
	final public ViewSetup[] setups;

	/**
	 * path where detection files are stored
	 */
	final public File detectionsPath;

	/**
	 * path where matches files are stored
	 */
	final public File matchesPath;

	/**
	 * load images for every view. might be null.
	 */
	final public ImgLoader imgLoader;

	public SequenceDescription( final ViewSetup[] setups, final int[] timepoints, final ImgLoader imgLoader, final File detectionsPath, final File matchesPath )
	{
		this.timepoints = timepoints;
		this.setups = setups;
		this.imgLoader = imgLoader;
		this.detectionsPath = detectionsPath;
		this.matchesPath = matchesPath;
	}

	/**
	 * Get number of timepoints in this sequence.
	 *
	 * @return number of timepoints
	 */
	final public int numTimepoints()
	{
		return timepoints.length;
	}

	/**
	 * Get number of view setups in this sequence.
	 *
	 * @return number of view setups
	 */
	final public int numViewSetups()
	{
		return setups.length;
	}

	/**
	 * Load a SequenceDescription from an XML file. Do not create an
	 * ImageLoader.
	 */
	public static SequenceDescription load( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException
	{
		try
		{
			return load( xmlFilename, false );
		}
		catch ( final InstantiationException e )
		{} // will not happen
		catch ( final IllegalAccessException e )
		{} // will not happen
		catch ( final ClassNotFoundException e )
		{} // will not happen
		return null;
	}

	/**
	 * Load a SequenceDescription from an XML file.
	 *
	 * @param createImageLoader
	 *            whether an ImageLoader should be created for the sequence.
	 */
	public static SequenceDescription load( final String xmlFilename, final boolean createImageLoader ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document dom = db.parse( xmlFilename );
		final Element root = dom.getDocumentElement();

		final ViewSetup[] setups = loadViewSetups( root );
		final int[] timepoints = loadTimepoints( root );
		final File basePath = loadPath( root, "BasePath", ".", new File( xmlFilename ).getParentFile() );
		final ImgLoader loader = createImageLoader ? loadImgLoader( root, basePath ) : null;
		final File detectionsDir = loadPath( root, "DetectionsPath", "detections", basePath );
		final File matchesDir = loadPath( root, "MatchesPath", "matches", basePath );

		return new SequenceDescription( setups, timepoints, loader, detectionsDir, matchesDir );
	}

	private static ImgLoader loadImgLoader( final Element sequenceDescription, final File basePath ) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final Element elem = ( Element ) sequenceDescription.getElementsByTagName( "ImageLoader" ).item( 0 );
		final ImgLoader imgLoader = ( ImgLoader ) Class.forName( elem.getAttribute( "class" ) ).newInstance();
		imgLoader.init( elem, basePath );
		return imgLoader;
	}

	private static int[] loadTimepoints( final Element sequenceDescription )
	{
		final Element timepoints = ( Element ) sequenceDescription.getElementsByTagName( "Timepoints" ).item( 0 );
		final String type = timepoints.getAttribute( "type" );
		if ( type.equals( "range" ) )
		{
			final int first = Integer.parseInt( timepoints.getElementsByTagName( "first" ).item( 0 ).getTextContent() );
			final int last = Integer.parseInt( timepoints.getElementsByTagName( "last" ).item( 0 ).getTextContent() );
			final int[] tp = new int[ last - first + 1 ];
			for ( int t = first, i = 0; t <= last; ++t, ++i )
				tp[ i ] = t;
			return tp;
		}
		else
		{
			System.err.println( "unknown <Timepoints> type" );
			return new int[ 0 ];
		}
	}

	private static ViewSetup[] loadViewSetups( final Element sequenceDescription )
	{
		final ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >();

		final NodeList nodes = sequenceDescription.getElementsByTagName( "ViewSetup" );
		for ( int i = 0; i < nodes.getLength(); ++i )
			setups.add( ViewSetup.fromXml( ( Element ) nodes.item( i ) ) );

		// sort by ViewSetup.id
		Collections.sort( setups );
		return setups.toArray( new ViewSetup[ 0 ] );
	}
}
