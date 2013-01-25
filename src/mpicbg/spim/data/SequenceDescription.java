package mpicbg.spim.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A SPIM sequence consisting of a list of timepoints and a list of view setups.
 * Every (timepoint, setup) pair is a view (i.e., an image stack acquired at that time with that setup).
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SequenceDescription
{
	/**
	 * timepoint id for every timepoint index.
	 */
	final public int[] timepoints;
	// TODO: make protected and use getter
	// TODO: make ArrayList< Integer > ?

	/**
	 * angle and illumination setup for every view-setup index.
	 */
	final public ViewSetup[] setups;
	// TODO: make protected and use getter
	// TODO: make ArrayList< ViewSetup >

	/**
	 * Relative paths in the XML sequence description are interpreted with respect to this.
	 */
	final protected File basePath;

	/**
	 * load images for every view. might be null.
	 */
	final public ImgLoader imgLoader;
	// TODO: make protected and use getter

	public SequenceDescription( final ViewSetup[] setups, final int[] timepoints, final File basePath, final ImgLoader imgLoader )
	{
		this.timepoints = timepoints;
		this.setups = setups;
		this.basePath = basePath;
		this.imgLoader = imgLoader;
	}

	/**
	 * Load a SequenceDescription from an XML file.
	 *
	 * @param elem
	 *            The "SequenceDescription" DOM element.
	 * @param createImageLoader
	 *            Whether an {@link ImgLoader} should be created for the
	 *            sequence.
	 * @param xmlFileParentDirectory
	 *            Directory containing the XML file we are loading.
	 *            All relative paths in the XML file are relative to the {@link #basePath}.
	 *            The XML sequence description may contain a "BasePath" entry. If this is a relative path
	 *            itself, it is relative to the directory of the XML file.
	 */
	public SequenceDescription( final Element elem, final File xmlFileParentDirectory, final boolean createImageLoader ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		timepoints = createTimepointsFromXml( elem );
		setups = createViewSetupsFromXml( elem );
		basePath = XmlHelpers.loadPath( elem, "BasePath", ".", xmlFileParentDirectory );
		imgLoader = createImageLoader ? createImgLoaderFromXml( elem, basePath ) : null;
	}

	/**
	 * Load a SequenceDescription from an XML file. Do not create an {@link ImgLoader}.
	 */
	public SequenceDescription( final Element elem, final File xmlFileParentDirectory ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		this( elem, xmlFileParentDirectory, false );
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

	protected static ImgLoader createImgLoaderFromXml( final Element sequenceDescription, final File basePath ) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final Element elem = ( Element ) sequenceDescription.getElementsByTagName( "ImageLoader" ).item( 0 );
		final ImgLoader imgLoader = ( ImgLoader ) Class.forName( elem.getAttribute( "class" ) ).newInstance();
		imgLoader.init( elem, basePath );
		return imgLoader;
	}

	protected static int[] createTimepointsFromXml( final Element sequenceDescription )
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

	protected static ViewSetup[] createViewSetupsFromXml( final Element sequenceDescription )
	{
		final ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >();

		final NodeList nodes = sequenceDescription.getElementsByTagName( "ViewSetup" );
		for ( int i = 0; i < nodes.getLength(); ++i )
			setups.add( new ViewSetup( ( Element ) nodes.item( i ) ) );

		// sort by ViewSetup.id
		Collections.sort( setups );
		return setups.toArray( new ViewSetup[ 0 ] );
	}
}
