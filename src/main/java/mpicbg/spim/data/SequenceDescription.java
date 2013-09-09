package mpicbg.spim.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.Element;
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
	final public ArrayList< Integer > timepoints;
	// TODO: make protected and use getter

	/**
	 * angle and illumination setup for every view-setup index.
	 */
	final public ArrayList< ViewSetup > setups;
	// TODO: make protected and use getter

	/**
	 * Relative paths in the XML sequence description are interpreted with respect to this.
	 */
	final protected File basePath;

	/**
	 * load images for every view. might be null.
	 */
	final public ImgLoader imgLoader;
	// TODO: make protected and use getter

	public SequenceDescription( final List< ? extends ViewSetup > setups, final List< Integer > timepoints, final File basePath, final ImgLoader imgLoader )
	{
		this.timepoints = new ArrayList< Integer >( timepoints );
		this.setups = new ArrayList< ViewSetup >( setups );
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
	public SequenceDescription( final Element elem, final File xmlFileParentDirectory, final boolean createImageLoader ) throws InstantiationException, IllegalAccessException, ClassNotFoundException
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
		return timepoints.size();
	}

	/**
	 * Get number of view setups in this sequence.
	 *
	 * @return number of view setups
	 */
	final public int numViewSetups()
	{
		return setups.size();
	}

	/**
	 * Get the base path of the sequence. Relative paths in the XML sequence
	 * description are interpreted with respect to this.
	 *
	 * @return the base path of the sequence
	 */
	public synchronized File getBasePath()
	{
		return basePath;
	}

	protected static ImgLoader createImgLoaderFromXml( final Element sequenceDescription, final File basePath ) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final Element elem = sequenceDescription.getChild( "ImageLoader" );
		final ImgLoader imgLoader = ( ImgLoader ) Class.forName( elem.getAttributeValue( "class" ) ).newInstance();
		imgLoader.init( elem, basePath );
		return imgLoader;
	}

	protected static ArrayList< Integer > createTimepointsFromXml( final Element sequenceDescription )
	{
		final Element timepoints = sequenceDescription.getChild( "Timepoints" );
		final String type = timepoints.getAttributeValue( "type" );
		if ( type.equals( "range" ) )
		{
			final int first = Integer.parseInt( timepoints.getChildText( "first" ) );
			final int last = Integer.parseInt( timepoints.getChildText( "last" ) );
			final ArrayList< Integer > tp = new ArrayList< Integer >();
			for ( int t = first; t <= last; ++t )
				tp.add( t );
			return tp;
		}
		else
		{
			throw new RuntimeException( "unknown <Timepoints> type: " + type );
		}
	}

	protected static ArrayList< ViewSetup > createViewSetupsFromXml( final Element sequenceDescription )
	{
		final ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >();

		for ( final Element elem : sequenceDescription.getChildren( "ViewSetup" ) )
			setups.add( new ViewSetup( elem ) );

		// sort by ViewSetup.id
		Collections.sort( setups );
		return setups;
	}
}
