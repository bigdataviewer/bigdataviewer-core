package creator.spim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import mpicbg.spim.io.ConfigurationParserException;
import net.imglib2.realtransform.AffineTransform3D;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import viewer.hdf5.Hdf5ImageLoader;
import viewer.hdf5.Partition;
import viewer.hdf5.Util;
import creator.SetupAggregator;
import creator.WriteSequenceToHdf5;
import creator.WriteSequenceToXml;

public class AddFusionExperiment
{
	public static File createNewPartitionFile( final File xmlSequenceFile ) throws IOException
	{
		final String seqFilename = xmlSequenceFile.getAbsolutePath();
		if ( !seqFilename.endsWith( ".xml" ) )
			throw new IllegalArgumentException();
		final String baseFilename = seqFilename.substring( 0, seqFilename.length() - 4 );
		for ( int i = 0; i < Integer.MAX_VALUE; ++i )
		{
			final File hdf5File = new File( String.format( "%s-%d.h5", baseFilename, i ) );
			if ( ! hdf5File.exists() )
				if ( hdf5File.createNewFile() )
					return hdf5File;
		}
		throw new RuntimeException( "could not generate new partition filename" );
	}

	public static void main( final String[] args ) throws ConfigurationParserException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, TransformerFactoryConfigurationError, TransformerException
	{
		// get these from the dialogs:
		final String existingDatasetPath = "/Users/pietzsch/Desktop/addfusionhere.xml";

		final String huiskenExperimentXmlFile = "/Users/pietzsch/workspace/data/fast fly/111010_weber/e012.xml";
		final String angles = "0,120,240";
		final String timepoints = "1-5";
		final int referenceTimePoint = 50;

		final String fusionDirectory = "/Users/pietzsch/workspace/data/fast fly/111010_weber/e012/output/";

		final int cropOffsetX = 58;
		final int cropOffsetY = 74;
		final int cropOffsetZ = 6;
		final int scale = 1;

		final double sliceValueMin = 0;
		final double sliceValueMax = 8000;

		final int[][] fusionresolutions = { { 1, 1, 1 }, { 2, 2, 2 }, { 4, 4, 2 } };
		final int[][] fusionsubdivisions = { { 16, 16, 16 }, { 16, 16, 16 }, { 8, 8, 8 } };

		// try to figure these out automatically:
		final String filepattern = "img_tl%d_ch1_z%03d.tif";
		final int numSlices = 387;


		// then do this:
		final SpimRegistrationSequence spimseq = new SpimRegistrationSequence( huiskenExperimentXmlFile, angles, timepoints, referenceTimePoint );
		final List< AffineTransform3D > fusionTransforms = spimseq.getFusionTransforms( cropOffsetX, cropOffsetY, cropOffsetZ, scale );
		final FusionResult fusionResult = new FusionResult( spimseq, fusionDirectory, filepattern, numSlices, sliceValueMin, sliceValueMax, fusionTransforms );


		// aggregate the ViewSetups
		final SetupAggregator aggregator = new SetupAggregator();

		// first add the setups from the existing dataset
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document dom = db.parse( existingDatasetPath );
		final Element root = dom.getDocumentElement();

		final File existingDatasetXmlFile = new File( existingDatasetPath );
		final File baseDirectory = existingDatasetXmlFile.getParentFile();
		final SequenceDescription existingSequence = new SequenceDescription( root, baseDirectory != null ? baseDirectory : new File("."), true );
		final ViewRegistrations existingRegistrations = new ViewRegistrations( root );
		final Hdf5ImageLoader hdf5Loader = ( Hdf5ImageLoader ) existingSequence.imgLoader;

		for ( int setup = 0; setup < existingSequence.numViewSetups(); ++setup )
			aggregator.add( existingSequence.setups.get( setup ), existingSequence, existingRegistrations, Util.castToInts( hdf5Loader.getMipmapResolutions( setup ) ), hdf5Loader.getSubdivisions( setup ) );

		// now add a new setup from the fusion result
		final SequenceDescription fusionSeq = fusionResult.getSequenceDescription();
		final ViewRegistrations fusionReg = fusionResult.getViewRegistrations();
		final ViewSetup fusionSetup = fusionSeq.setups.get( 0 );
		final int fusionSetupWrapperId = aggregator.add( fusionSetup, fusionSeq, fusionReg, fusionresolutions, fusionsubdivisions );

		// setup the partitions
		final ArrayList< Partition > partitions = new ArrayList< Partition >( hdf5Loader.getPartitions() );
		final boolean notYetPartitioned = partitions.isEmpty();
		if ( notYetPartitioned )
			// add a new partition for the existing stuff
			partitions.add( new Partition( hdf5Loader.getHdf5File().getAbsolutePath(), 0, 0, existingSequence.numTimepoints(), 0, 0, existingSequence.numViewSetups() ) );
		// add partition for the fused data
		final ArrayList< Partition > newPartitions = new ArrayList< Partition >();
		final String newPartitionPath = createNewPartitionFile( existingDatasetXmlFile ).getAbsolutePath();
		newPartitions.add( new Partition( newPartitionPath, 0, 0, fusionSeq.numTimepoints(), 0, fusionSetupWrapperId, 1 ) );
		partitions.addAll( newPartitions );

		final SequenceDescription aggregateSeq = aggregator.createSequenceDescription( baseDirectory );
		final ViewRegistrations aggregateRegs = aggregator.createViewRegistrations();
		final ArrayList< int[][] > perSetupResolutions = aggregator.getPerSetupResolutions();
		final ArrayList< int[][] > perSetupSubdivisions = aggregator.getPerSetupSubdivisions();

		// write new data partitions
		for ( final Partition partition : newPartitions )
			WriteSequenceToHdf5.writeHdf5PartitionFile( aggregateSeq, perSetupResolutions, perSetupSubdivisions, partition, null );

		// (re-)write hdf5 link file
		final File newHdf5PartitionLinkFile = createNewPartitionFile( existingDatasetXmlFile );
		WriteSequenceToHdf5.writeHdf5PartitionLinkFile( aggregateSeq, perSetupResolutions, perSetupSubdivisions, partitions, newHdf5PartitionLinkFile );

		// re-write xml file
		final Hdf5ImageLoader loader = new Hdf5ImageLoader( newHdf5PartitionLinkFile, partitions, false );
		final SequenceDescription sequenceDescription = new SequenceDescription( aggregateSeq.setups, aggregateSeq.timepoints, baseDirectory, loader );
		WriteSequenceToXml.writeSequenceToXml( sequenceDescription, aggregateRegs, existingDatasetPath );
	}
}
