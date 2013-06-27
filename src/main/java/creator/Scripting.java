package creator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.io.ConfigurationParserException;
import net.imglib2.realtransform.AffineTransform3D;
import viewer.hdf5.Hdf5ImageLoader;
import viewer.hdf5.Partition;
import creator.spim.FusionResult;
import creator.spim.SpimRegistrationSequence;

public class Scripting
{
	/**
	 * TODO
	 *
	 * @param huiskenExperimentXmlFile
	 * @param angles
	 * @param timepoints
	 * @param referenceTimePoint
	 * @return
	 * @throws ConfigurationParserException
	 */
	public static SpimRegistrationSequence createSpimRegistrationSequence( final String huiskenExperimentXmlFile, final String angles, final String timepoints, final int referenceTimePoint ) throws ConfigurationParserException
	{
		return new SpimRegistrationSequence( huiskenExperimentXmlFile, angles, timepoints, referenceTimePoint );
	}

	/**
	 * TODO
	 *
	 * @param inputDirectory
	 * @param inputFilePattern
	 * @param angles
	 * @param timepoints
	 * @param referenceTimePoint
	 * @param overrideImageZStretching
	 * @param zStretching
	 * @return
	 * @throws ConfigurationParserException
	 */
	public static SpimRegistrationSequence createSpimRegistrationSequence( final String inputDirectory, final String inputFilePattern, final String angles, final String timepoints, final int referenceTimePoint, final boolean overrideImageZStretching, final double zStretching ) throws ConfigurationParserException
	{
		return new SpimRegistrationSequence( inputDirectory, inputFilePattern, angles, timepoints, referenceTimePoint, overrideImageZStretching, zStretching );
	}

	/**
	 * TODO
	 *
	 * @param spimseq
	 * @param scale
	 * @param cropOffsetX
	 * @param cropOffsetY
	 * @param cropOffsetZ
	 * @return
	 */
	public static List< AffineTransform3D > getFusionTransforms( final SpimRegistrationSequence spimseq, final int scale, final int cropOffsetX, final int cropOffsetY, final int cropOffsetZ )
	{
		return spimseq.getFusionTransforms( cropOffsetX, cropOffsetY, cropOffsetZ, scale );
	}

	/**
	 * TODO
	 *
	 * @param spimseq
	 * @param filepath
	 * @param filepattern
	 * @param numSlices
	 * @param sliceValueMin
	 * @param sliceValueMax
	 * @param fusionTransform
	 * @return
	 */
	public static FusionResult createFusionResult( final SpimRegistrationSequence spimseq, final String filepath, final String filepattern, final int numSlices, final double sliceValueMin, final double sliceValueMax, final List< AffineTransform3D > fusionTransforms )
	{
		return new FusionResult( spimseq, filepath, filepattern, numSlices, sliceValueMin, sliceValueMax, fusionTransforms );
	}

	/**
	 * TODO
	 * @param timepoints
	 * @param referenceTimePoint
	 * @return
	 */
	public static SetupAggregator createEmptyCollector()
	{
		return new SetupAggregator();
	}


	public static class PartitionedSequenceWriter
	{
		final SequenceDescription seq;

		final ViewRegistrations regs;

		final ArrayList< int[][] > perSetupResolutions;

		final ArrayList< int[][] > perSetupSubdivisions;

		final ArrayList< Partition > partitions;

		final File seqFile;

		final File hdf5File;

		public PartitionedSequenceWriter( final SetupAggregator setupCollector, final ArrayList< Integer > timepointStarts, final ArrayList< Integer > setupStarts, final String xmlFilename, final String hdf5Filename, final String hdf5PartitionFilenameFormat )
		{
			seq = setupCollector.createSequenceDescription( null );
			regs = setupCollector.createViewRegistrations();
			perSetupResolutions = setupCollector.getPerSetupResolutions();
			perSetupSubdivisions = setupCollector.getPerSetupSubdivisions();

			final ArrayList< Integer > timepointSplits = new ArrayList< Integer >();
			if ( timepointStarts != null )
			{
				for ( final int t : timepointStarts )
					if ( t >= 0 && t < seq.numTimepoints() )
						timepointSplits.add( t );
			}
			else
				timepointSplits.add( 0 );
			timepointSplits.add( seq.numTimepoints() );

			final ArrayList< Integer > setupSplits = new ArrayList< Integer >();
			if ( setupStarts != null )
			{
				for ( final int s : setupStarts )
					if ( s >= 0 && s < seq.numViewSetups() )
						setupSplits.add( s );
			}
			else
				setupSplits.add( 0 );
			setupSplits.add( seq.numViewSetups() );

			this.partitions = new ArrayList< Partition >();
			final int timepointOffset = 0;
			final int setupOffset = 0;
			for ( int i = 0; i < timepointSplits.size() - 1; ++i )
			{
				final int timepointStart = timepointSplits.get( i );
				final int timepointLength = timepointSplits.get( i + 1 ) - timepointStart;
				for ( int j = 0; j < setupSplits.size() - 1; ++j )
				{
					final int setupStart = setupSplits.get( j );
					final int setupLength = setupSplits.get( j + 1 ) - setupStart;
					final String path = String.format( hdf5PartitionFilenameFormat, i, j );
					this.partitions.add( new Partition( path, timepointOffset, timepointStart, timepointLength, setupOffset, setupStart, setupLength ) );
				}
			}

			this.seqFile = new File( xmlFilename );
			this.hdf5File = new File( hdf5Filename );
		}

		public int numPartitions()
		{
			return partitions.size();
		}

		public void writePartition( final int index )
		{
			if ( index >= 0 && index < partitions.size() )
				WriteSequenceToHdf5.writeHdf5PartitionFile( seq, perSetupResolutions, perSetupSubdivisions, partitions.get( index ), null );
		}

		public void writeXmlAndLinks() throws TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException, FileNotFoundException
		{
			WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, perSetupResolutions, perSetupSubdivisions, partitions, hdf5File );
			final Hdf5ImageLoader loader = new Hdf5ImageLoader( hdf5File, partitions, false );
			final SequenceDescription sequenceDescription = new SequenceDescription( seq.setups, seq.timepoints, seqFile.getParentFile(), loader );
			WriteSequenceToXml.writeSequenceToXml( sequenceDescription, regs, seqFile.getAbsolutePath() );
		}
	}

	public static void main( final String[] args ) throws ConfigurationParserException, TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException, FileNotFoundException
	{
		final SetupAggregator collector = createEmptyCollector();

		final String huiskenExperimentXmlFile = "/Users/pietzsch/workspace/data/fast fly/111010_weber/e012.xml";
		final String angles = "0,120,240";
		final String timepoints = "1-5";
		final int referenceTimePoint = 50;

		final SpimRegistrationSequence spimseq = createSpimRegistrationSequence( huiskenExperimentXmlFile, angles, timepoints, referenceTimePoint );

		final String filepath = "/Users/pietzsch/workspace/data/fast fly/111010_weber/e012/output/";
		final String filepattern = "img_tl%d_ch1_z%03d.tif";
		final int numSlices = 387;
		final double sliceValueMin = 0;
		final double sliceValueMax = 8000;

		final int cropOffsetX = 58;
		final int cropOffsetY = 74;
		final int cropOffsetZ = 6;
		final int scale = 1;

		final List< AffineTransform3D > fusionTransforms = getFusionTransforms( spimseq, scale, cropOffsetX, cropOffsetY, cropOffsetZ );
		final FusionResult fusion = createFusionResult( spimseq, filepath, filepattern, numSlices, sliceValueMin, sliceValueMax, fusionTransforms );

		final int[][] spimresolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 2 } };
		final int[][] spimsubdivisions = { { 32, 32, 4 }, { 16, 16, 8 }, { 8, 8, 8 } };

		final String fusionresolutions = "{ 1, 1, 1 }, { 2, 2, 2 }, { 4, 4, 4 }";
		final String fusionsubdivisions = "{ 16, 16, 16 }, { 16, 16, 16 }, { 8, 8, 8 }";

		collector.addSetups( spimseq, spimresolutions, spimsubdivisions );
		collector.addSetups( fusion, fusionresolutions, fusionsubdivisions );


		// splitting ...
		final int timepointsPerPartition = 3;
		final int numTimepoints = 10;
		final ArrayList< Integer > timepointStarts = new ArrayList< Integer >();
		for ( int timepoint = 0; timepoint < numTimepoints; timepoint += timepointsPerPartition )
			timepointStarts.add( timepoint );
		final ArrayList< Integer > setupStarts = new ArrayList< Integer >( Arrays.asList( 0 ) );

		final String xmlFilename = "/Users/pietzsch/Desktop/data/everything2.xml";
		final String hdf5Filename = "/Users/pietzsch/Desktop/data/everything2.h5";
		final String hdf5PartitionFilenameFormat = "/Users/pietzsch/Desktop/data/everything2-%02d-%02d.h5";
		final PartitionedSequenceWriter writer = new PartitionedSequenceWriter( collector, timepointStarts, setupStarts, xmlFilename, hdf5Filename, hdf5PartitionFilenameFormat );

		System.out.println( writer.numPartitions() );
		for ( int i = 0; i < writer.numPartitions(); ++i )
			writer.writePartition( i );
		writer.writeXmlAndLinks();
	}
}
