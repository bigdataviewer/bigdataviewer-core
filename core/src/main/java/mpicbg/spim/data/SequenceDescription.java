package mpicbg.spim.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;

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
	 * Contains all time-points.
	 */
	private final TimePoints timepoints;

	/**
	 * Maps setup id to setup.
	 */
	private final Map< Integer, ViewSetup > setups;

	/**
	 * Relative paths in the XML sequence description are interpreted with respect to this.
	 */
	private final File basePath;

	/**
	 * load images for every view. might be null.
	 */
	private final ImgLoader< ? > imgLoader;
	// TODO: make protected and use getter

	public SequenceDescription( final List< ViewSetup > setups, final List< Integer > timepoints, final File basePath, final ImgLoader< ? > imgLoader )
	{
		final ArrayList< TimePoint > tplist = new ArrayList< TimePoint >();
		for ( final int i : timepoints )
			tplist.add( new TimePoint( i ) );
		this.timepoints = new TimePoints( tplist );
		this.setups = new HashMap< Integer, ViewSetup >();
		for ( final ViewSetup setup : setups )
			this.setups.put( setup.getId(), setup );
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
		viewSetupsOrderedDirty = true;
	}

	/**
	 * Load a SequenceDescription from an XML file. Do not create an {@link ImgLoader}.
	 */
	public SequenceDescription( final Element elem, final File xmlFileParentDirectory ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		this( elem, xmlFileParentDirectory, false );
		viewSetupsOrderedDirty = true;
	}

	private boolean viewSetupsOrderedDirty;

	private List< ViewSetup > viewSetupsOrdered;

	public List< ViewSetup > getViewSetupsOrdered()
	{
		if ( viewSetupsOrderedDirty )
		{
			final ArrayList< ViewSetup > list = new ArrayList< ViewSetup >();
			for ( final ViewSetup setup : setups.values() )
				list.add( setup );
//			Entity.sortById( viewSetupsOrdered );
			Collections.sort( list, new Comparator< ViewSetup >()
					{
						@Override
						public int compare( final ViewSetup o1, final ViewSetup o2 )
						{
							return o1.getId() - o2.getId();
						}
					} );
			viewSetupsOrdered = list;
			viewSetupsOrderedDirty = false;
		}
		return viewSetupsOrdered;
	}

	/**
	 * Get number of timepoints in this sequence.
	 *
	 * @return number of timepoints
	 */
//	public final int numTimepoints()
//	{
//		return timepoints.size();
//	}

	/**
	 * Get number of view setups in this sequence.
	 *
	 * @return number of view setups
	 */
//	public final int numViewSetups()
//	{
//		return setups.size();
//	}

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

	protected static ImgLoader< ? > createImgLoaderFromXml( final Element sequenceDescription, final File basePath ) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final Element elem = sequenceDescription.getChild( "ImageLoader" );
		String classn = elem.getAttributeValue( "class" );
		if ( classn.equals( "viewer.hdf5.Hdf5ImageLoader" ) )
			classn = "bdv.img.hdf5.Hdf5ImageLoader";
		final ImgLoader< ? > imgLoader = ( ImgLoader< ? > ) Class.forName( classn ).newInstance();
		imgLoader.init( elem, basePath );
		return imgLoader;
	}

	protected static TimePoints createTimepointsFromXml( final Element sequenceDescription )
	{
		final Element timepoints = sequenceDescription.getChild( "Timepoints" );
		final String type = timepoints.getAttributeValue( "type" );
		if ( type.equals( "range" ) )
		{
			final int first = Integer.parseInt( timepoints.getChildText( "first" ) );
			final int last = Integer.parseInt( timepoints.getChildText( "last" ) );
			final ArrayList< TimePoint > tp = new ArrayList< TimePoint >();
			for ( int t = first; t <= last; ++t )
				tp.add( new TimePoint( t ) );
			return new TimePoints( tp );
		}
		else
		{
			throw new RuntimeException( "unknown <Timepoints> type: " + type );
		}
	}

	protected static HashMap< Integer, ViewSetup > createViewSetupsFromXml( final Element sequenceDescription )
	{
		final HashMap< Integer, ViewSetup > setups = new HashMap< Integer, ViewSetup >();

		for ( final Element elem : sequenceDescription.getChildren( "ViewSetup" ) )
		{
			final ViewSetup setup = new ViewSetup( elem );
			setups.put( setup.getId(), setup );
		}

		// sort by ViewSetup.id
//		Collections.sort( setups );
		return setups;
	}

	public TimePoints getTimePoints()
	{
		return timepoints;
	}

	public Map< Integer, ViewSetup > getViewSetups()
	{
		return setups;
	}

	public ImgLoader< ? > getImgLoader()
	{
		return imgLoader;
	}
}
