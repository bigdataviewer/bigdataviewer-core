package viewer.hdf5;

import java.io.File;

import mpicbg.spim.data.XmlHelpers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Partition describes one partition file of a dataset that is split across
 * multiple hdf5 files. An aggregating hdf5 linking the partition files can be
 * created using the Partition information (without looking at the constituent
 * files).
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class Partition
{
	protected final String path;
	protected final int timepointOffset;
	protected final int timepointStart;
	protected final int timepointLength;
	protected final int setupOffset;
	protected final int setupStart;
	protected final int setupLength;

	/**
	 * Create a new Partition description.
	 *
	 * @param path
	 *            path to the hdf5 file for this partition
	 * @param timepointOffset
	 *            The timepoint <em>t</em> in the partition corresponds to
	 *            timepoint <em>t + <code>timepointOffset</code></em> in the
	 *            full sequence.
	 * @param timepointStart
	 *            The first timepoint <em>t</em> contained in this partition
	 *            (relative to the offset, not the full sequence).
	 * @param timepointLength
	 *            How many timepoints are contained in this partition.
	 * @param setupOffset
	 *            The setup <em>s</em> in the partition corresponds to
	 *            setup <em>s + <code>setupOffset</code></em> in the
	 *            full sequence.
	 * @param setupStart
	 *            The first setup <em>s</em> contained in this partition
	 *            (relative to the offset, not the full sequence).
	 * @param setupLength
	 *            How many setups are contained in this partition.
	 */
	public Partition( final String path, final int timepointOffset, final int timepointStart, final int timepointLength, final int setupOffset, final int setupStart, final int setupLength )
	{
		this.path = path;
		this.timepointOffset = timepointOffset;
		this.timepointStart = timepointStart;
		this.timepointLength = timepointLength;
		this.setupOffset = setupOffset;
		this.setupStart = setupStart;
		this.setupLength = setupLength;
	}

	/**
	 * Load a Partition description from an XML file.
	 *
	 * @param elem
	 *            The "partition" DOM element.
	 */
	public Partition( final Element elem, final File basePath )
	{
		try
		{
			path = XmlHelpers.loadPath( elem, "path", basePath ).toString();
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}
		timepointOffset = Integer.parseInt( elem.getElementsByTagName( "timepointOffset" ).item( 0 ).getTextContent() );
		timepointStart = Integer.parseInt( elem.getElementsByTagName( "timepointStart" ).item( 0 ).getTextContent() );
		timepointLength = Integer.parseInt( elem.getElementsByTagName( "timepointLength" ).item( 0 ).getTextContent() );
		setupOffset = Integer.parseInt( elem.getElementsByTagName( "setupOffset" ).item( 0 ).getTextContent() );
		setupStart = Integer.parseInt( elem.getElementsByTagName( "setupStart" ).item( 0 ).getTextContent() );
		setupLength = Integer.parseInt( elem.getElementsByTagName( "setupLength" ).item( 0 ).getTextContent() );
	}

	public Element toXml( final Document doc, final File basePath )
	{
		final Element elem = doc.createElement( "partition" );
		elem.appendChild( XmlHelpers.pathElement( doc, "path", new File( path ), basePath ) );
		elem.appendChild( XmlHelpers.intElement( doc, "timepointOffset", getTimepointOffset() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "timepointStart", getTimepointStart() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "timepointLength", getTimepointLength() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "setupOffset", getSetupOffset() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "setupStart", getSetupStart() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "setupLength", getSetupLength() ) );
		return elem;
	}

	/**
	 * Get the  path to the hdf5 file for this partition.
	 *
	 * @return path to the hdf5 file for this partition.
	 */
	public String getPath()
	{
		return path;
	}

	/**
	 * Get the timepoint offset. The timepoint <em>t</em> in the partition
	 * corresponds to timepoint <em>t + <code>offset</code></em> in the full
	 * sequence.
	 *
	 * @return the timepoint offset.
	 */
	public int getTimepointOffset()
	{
		return timepointOffset;
	}

	/**
	 * Get the index of the first timepoint <em>t</em> contained in this
	 * partition (relative to the offset, not the full sequence).
	 *
	 * @return index of the first timepoint in this partition.
	 */
	public int getTimepointStart()
	{
		return timepointStart;
	}

	/**
	 * Get the number of timepoints in this partition.
	 * @return how many timepoints are contained in this partition.
	 */
	public int getTimepointLength()
	{
		return timepointLength;
	}

	/**
	 * Get the setup offset. The setup <em>t</em> in the partition
	 * corresponds to setup <em>t + <code>offset</code></em> in the full
	 * sequence.
	 *
	 * @return the setup offset.
	 */
	public int getSetupOffset()
	{
		return setupOffset;
	}

	/**
	 * Get the index of the first setup <em>t</em> contained in this
	 * partition (relative to the offset, not the full sequence).
	 *
	 * @return index of the first setup in this partition.
	 */
	public int getSetupStart()
	{
		return setupStart;
	}

	/**
	 * Get the number of setups in this partition.
	 * @return how many setups are contained in this partition.
	 */
	public int getSetupLength()
	{
		return setupLength;
	}
}
