package viewer.hdf5;

import java.io.File;

import mpicbg.spim.data.XmlHelpers;

import org.jdom2.Element;

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
		timepointOffset = Integer.parseInt( elem.getChildText( "timepointOffset" ) );
		timepointStart = Integer.parseInt( elem.getChildText( "timepointStart" ) );
		timepointLength = Integer.parseInt( elem.getChildText( "timepointLength" ) );
		setupOffset = Integer.parseInt( elem.getChildText( "setupOffset" ) );
		setupStart = Integer.parseInt( elem.getChildText( "setupStart" ) );
		setupLength = Integer.parseInt( elem.getChildText( "setupLength" ) );
	}

	public Element toXml( final File basePath )
	{
		final Element elem = new Element( "partition" );
		elem.addContent( XmlHelpers.pathElement( "path", new File( path ), basePath ) );
		elem.addContent( XmlHelpers.intElement( "timepointOffset", getTimepointOffset() ) );
		elem.addContent( XmlHelpers.intElement( "timepointStart", getTimepointStart() ) );
		elem.addContent( XmlHelpers.intElement( "timepointLength", getTimepointLength() ) );
		elem.addContent( XmlHelpers.intElement( "setupOffset", getSetupOffset() ) );
		elem.addContent( XmlHelpers.intElement( "setupStart", getSetupStart() ) );
		elem.addContent( XmlHelpers.intElement( "setupLength", getSetupLength() ) );
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

	/**
	 * Does this partition contain the given timepoint and setup?
	 *
	 * @param timepoint
	 *            timepoint index (in the full sequence)
	 * @param setup
	 *            setup index (in the full sequence)
	 * @return
	 */
	public boolean contains( final int timepoint, final int setup )
	{
		final int t0 = timepointOffset + timepointStart;
		if ( timepoint < t0 )
			return false;
		final int t1 = t0 + timepointLength;
		if ( timepoint >= t0 )
			return false;
		final int s0 = setupOffset + setupStart;
		if ( setup < s0 )
			return false;
		final int s1 = s0 + setupLength;
		if ( setup >= s0 )
			return false;
		return true;
	}
}
