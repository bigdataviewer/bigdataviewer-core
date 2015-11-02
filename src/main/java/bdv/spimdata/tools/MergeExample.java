package bdv.spimdata.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import bdv.export.ExportMipmapInfo;
import bdv.export.WriteSequenceToHdf5;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

/**
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class MergeExample
{
	/**
	 * Merge multiple HDF5 datasets, where each dataset contains the same
	 * timepoints but different views.
	 *
	 * @param inputFilenames
	 * 	xml file names for input datasets
	 * @param transforms
	 *  transforms to apply to each input dataset
	 * @param outputXmlFilename
	 * 	xml filename into which to store the merged dataset. An HDF5 link master file with the same basename and extension ".h5" will be created that links into the source hdf5s.
	 * @throws SpimDataException
	 */
	public static void mergeHdf5Views(
			final List< String > inputFilenames,
			final List< ViewTransform > transforms,
			final String outputXmlFilename )
					throws SpimDataException
	{
		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();

		final HashMap< String, Set< Integer > > attributeIdsInUse = new HashMap< String, Set< Integer > >();
		final HashSet< Integer > setupIdsInUse = new HashSet< Integer >();
		final ArrayList< Partition > newPartitions = new ArrayList< Partition >();
		final Map< Integer, ExportMipmapInfo > newMipmapInfos = new HashMap< Integer, ExportMipmapInfo >();
		final ArrayList< SpimDataMinimal > spimDatas = new ArrayList< SpimDataMinimal >();
		for ( int i = 0; i < inputFilenames.size(); ++i )
		{
			final String fn = inputFilenames.get( i );
			final ViewTransform transform = transforms.get( i );

			final SpimDataMinimal spimData = io.load( fn );
			final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
			final ArrayList< Partition > partitions = MergePartitionList.getPartitions( seq );
			final Map< Integer, ExportMipmapInfo > mipmapInfos = MergePartitionList.getHdf5PerSetupExportMipmapInfos( seq );

			final Map< String, Map< Integer, Integer > > attributesReassigned = ChangeAttributeId.assignNewAttributeIds( spimData, attributeIdsInUse );
			final Map< Integer, Integer > setupsReassigned = ChangeViewSetupId.assignNewViewSetupIds( spimData, setupIdsInUse );

			for ( final Partition partition : partitions )
			{
				final Map< Integer, Integer > seqToPart = partition.getSetupIdSequenceToPartition();
				final Map< Integer, Integer > newSeqToPart = new HashMap< Integer, Integer >();
				for ( final Entry< Integer, Integer > entry : seqToPart.entrySet() )
				{
					final int oldSeq = entry.getKey();
					final int newSeq = setupsReassigned.containsKey( oldSeq ) ? setupsReassigned.get( oldSeq ) : oldSeq;
					newSeqToPart.put( newSeq, entry.getValue() );
				}
				newPartitions.add( new Partition( partition.getPath(), partition.getTimepointIdSequenceToPartition(), newSeqToPart ) );
			}

			for ( final Entry< Integer, ExportMipmapInfo > entry : mipmapInfos.entrySet() )
			{
				final int oldSeq = entry.getKey();
				final int newSeq = setupsReassigned.containsKey( oldSeq ) ? setupsReassigned.get( oldSeq ) : oldSeq;
				newMipmapInfos.put( newSeq, entry.getValue() );
			}

			final ViewRegistrations regs = spimData.getViewRegistrations();
			for ( final ViewRegistration reg : regs.getViewRegistrationsOrdered() )
				reg.concatenateTransform( transform );

			spimDatas.add( spimData );
		}

		final File xmlFile = new File( outputXmlFilename );
		final File path = xmlFile.getParentFile();
		final String xmlFilename = xmlFile.getAbsolutePath();
		final String basename = xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename;
		final File h5File = new File( basename + ".h5" );
		if ( h5File.exists() )
			h5File.delete();

		final SpimDataMinimal spimData = MergeTools.merge( path, spimDatas );
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		final Hdf5ImageLoader imgLoader = new Hdf5ImageLoader( h5File, newPartitions, seq, false );
		seq.setImgLoader( imgLoader );

		WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, newMipmapInfos );
		io.save( spimData, xmlFilename );
	}
}
