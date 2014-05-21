package bdv.img.hdf5;

import static mpicbg.spim.data.XmlHelpers.loadPath;
import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import net.imglib2.Pair;
import net.imglib2.util.ValuePair;

import org.jdom2.Element;

@ImgLoaderIo( format = "bdv.hdf5", type = Hdf5ImageLoader.class )
public class XmlIoHdf5ImageLoader implements XmlIoBasicImgLoader< Hdf5ImageLoader >
{
	@Override
	public Element toXml( final Hdf5ImageLoader imgLoader, final File basePath )
	{
		final Element elem = new Element( "ImageLoader" );
		elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "bdv.hdf5" );
		elem.addContent( XmlHelpers.pathElement( "hdf5", imgLoader.getHdf5File(), basePath ) );
		for ( final Partition partition : imgLoader.getPartitions() )
			elem.addContent( partitionToXml( partition, basePath ) );
		return elem;
	}

	@Override
	public Hdf5ImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		final String path = loadPath( elem, "hdf5", basePath ).toString();
		final ArrayList< Partition > partitions = new ArrayList< Partition >();
		for ( final Element p : elem.getChildren( "partition" ) )
			partitions.add( partitionFromXml( p, basePath ) );
//			partitions.add( legacy_partitionFromXml( p, basePath ) );
		return new Hdf5ImageLoader( new File( path ), partitions, sequenceDescription );
	}

	private Element partitionToXml( final Partition partition, final File basePath )
	{
		final Element elem = new Element( "partition" );
		elem.addContent( XmlHelpers.pathElement( "path", new File( partition.getPath() ), basePath ) );

		final Pair< int[], int[] > timepointIdVecs = idMapToVectors( partition.getTimepointIdSequenceToPartition() );
		final int[] timepointIds = timepointIdVecs.getA();
		final int[] timepointIdsPartition = timepointIdVecs.getB();
		elem.addContent( XmlHelpers.intArrayElement( "timepoints", timepointIds ) );
		if ( ! Arrays.equals( timepointIds, timepointIdsPartition ) )
			elem.addContent( XmlHelpers.intArrayElement( "timepointsMapped", timepointIdsPartition ) );

		final Pair< int[], int[] > setupIdVecs = idMapToVectors( partition.getSetupIdSequenceToPartition() );
		final int[] setupIds = setupIdVecs.getA();
		final int[] setupIdsPartition = setupIdVecs.getB();
		elem.addContent( XmlHelpers.intArrayElement( "setups", setupIds ) );
		if ( ! Arrays.equals( setupIds, setupIdsPartition ) )
			elem.addContent( XmlHelpers.intArrayElement( "setupsMapped", setupIdsPartition ) );

		return elem;
	}

	private static Pair< int[], int[] > idMapToVectors( final Map< Integer, Integer > setupIdMap )
	{
		final ArrayList< Integer > seqIdList = new ArrayList< Integer >( setupIdMap.keySet() );
		Collections.sort( seqIdList );
		final int[] seqIds = new int[ seqIdList.size() ];
		final int[] parIds = new int[ seqIdList.size() ];
		int i = 0;
		for ( final Integer seqId : seqIdList )
		{
			seqIds[ i ] = seqId;
			parIds[ i ] = setupIdMap.get( seqId );
			++i;
		}
		return new ValuePair< int[], int[] >( seqIds, parIds );
	}

	private Partition partitionFromXml( final Element elem, final File basePath )
	{
		String path;
		try
		{
			path = XmlHelpers.loadPath( elem, "path", basePath ).toString();
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		final int[] timepointIds = XmlHelpers.getIntArray( elem, "timepoints" );
		final int[] timepointIdsPartition = XmlHelpers.getIntArray( elem, "timepointsMapped", timepointIds );
		final int[] setupIds = XmlHelpers.getIntArray( elem, "setups" );
		final int[] setupIdsPartition = XmlHelpers.getIntArray( elem, "setupsMapped", setupIds );

		return new Partition( path, timepointIds, setupIds, timepointIdsPartition, setupIdsPartition );
	}

	private Partition legacy_partitionFromXml( final Element elem, final File basePath )
	{
		String path;
		try
		{
			path = XmlHelpers.loadPath( elem, "path", basePath ).toString();
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		/* @param timepointOffset
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
		final int timepointOffset = Integer.parseInt( elem.getChildText( "timepointOffset" ) );
		final int timepointStart = Integer.parseInt( elem.getChildText( "timepointStart" ) );
		final int timepointLength = Integer.parseInt( elem.getChildText( "timepointLength" ) );
		final int setupOffset = Integer.parseInt( elem.getChildText( "setupOffset" ) );
		final int setupStart = Integer.parseInt( elem.getChildText( "setupStart" ) );
		final int setupLength = Integer.parseInt( elem.getChildText( "setupLength" ) );

		final HashMap< Integer, Integer > timepointIdSequenceToPartition = new HashMap< Integer, Integer >();
		for ( int tPartition = timepointStart; tPartition < timepointStart + timepointLength; ++tPartition )
		{
			final int tSequence = tPartition + timepointOffset;
			timepointIdSequenceToPartition.put( tSequence, tPartition );
		}

		final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap< Integer, Integer >();
		for ( int sPartition = setupStart; sPartition < setupStart + setupLength; ++sPartition )
		{
			final int sSequence = sPartition + setupOffset;
			setupIdSequenceToPartition.put( sSequence, sPartition );
		}

		return new Partition( path, timepointIdSequenceToPartition, setupIdSequenceToPartition );
	}
}
