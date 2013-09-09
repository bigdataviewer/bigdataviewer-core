package creator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	 * Create a {@link SpimRegistrationSequence} based on a Huisken experiment
	 * xml file.
	 *
	 * @param huiskenExperimentXmlFile
	 *            path of the experiment xml file.
	 * @param channels
	 *            String specifying the channels to include in the sequence. In
	 *            the format used by the SPIMRegistration plugin.
	 *            For single-channel sequence, set to null.
	 * @param angles
	 *            String specifying the angles to include in the sequence. In
	 *            the format used by the SPIMRegistration plugin.
	 * @param timepoints
	 *            String specifying the timepoints to include in the sequence.
	 *            In the format used by the SPIMRegistration plugin.
	 * @param referenceTimePoint
	 *            the reference timepoint (registrations relative to this
	 *            timepoint are laoded), or <em>-1</em>, indicating to use
	 *            individual (non-timelapse) view registrations.
	 * @return an initialized {@link SpimRegistrationSequence} sequence.
	 * @throws ConfigurationParserException
	 */
	public static SpimRegistrationSequence createSpimRegistrationSequence( final String huiskenExperimentXmlFile, final String channels, final String angles, final String timepoints, final int referenceTimePoint ) throws ConfigurationParserException
	{
		return new SpimRegistrationSequence( huiskenExperimentXmlFile, channels, angles, timepoints, referenceTimePoint );
	}

	/**
	 * Create a {@link SpimRegistrationSequence} based on a directory of image
	 * (.tif, .czi, etc...).
	 *
	 * @param inputDirectory
	 *            path of image directory.
	 * @param inputFilePattern
	 *            pattern of the image file names In the format used by the
	 *            SPIMRegistration plugin.
	 * @param channels
	 *            String specifying the channels to include in the sequence. In
	 *            the format used by the SPIMRegistration plugin.
	 *            For single-channel sequence, set to null.
	 * @param angles
	 *            String specifying the angles to include in the sequence. In
	 *            the format used by the SPIMRegistration plugin.
	 * @param timepoints
	 *            String specifying the timepoints to include in the sequence.
	 *            In the format used by the SPIMRegistration plugin.
	 * @param referenceTimePoint
	 *            the reference timepoint (registrations relative to this
	 *            timepoint are laoded), or <em>-1</em>, indicating to use
	 *            individual (non-timelapse) view registrations.
	 * @param overrideImageZStretching
	 *            whether the z-stretching of the images should be overridden.
	 *            If <code>false</code>, the z-stretching is read from the image
	 *            metadata.
	 * @param zStretching
	 *            z-stretching to use (if
	 *            <code>overrideImageZStretching == true</code>).
	 * @return an initialized {@link SpimRegistrationSequence} sequence.
	 * @throws ConfigurationParserException
	 */
	public static SpimRegistrationSequence createSpimRegistrationSequence( final String inputDirectory, final String inputFilePattern, final String channels, final String angles, final String timepoints, final int referenceTimePoint, final boolean overrideImageZStretching, final double zStretching ) throws ConfigurationParserException
	{
		return new SpimRegistrationSequence( inputDirectory, inputFilePattern, channels, angles, timepoints, referenceTimePoint, overrideImageZStretching, zStretching );
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
		return FusionResult.create( spimseq, filepath, filepattern, numSlices, sliceValueMin, sliceValueMax, fusionTransforms );
	}

	/**
	 * Split the sequence represented in <code>aggregator</code> into
	 * partitions.
	 *
	 * @param aggregator
	 *            represents the full dataset.
	 * @param timepointsPerPartition
	 *            how many timepoints should each partition contain (if this is
	 *            &leq;0, put do not split timepoints across partitions).
	 * @param setupsPerPartition
	 *            how many setups should each partition contain (if this is
	 *            &leq;0, put do not split setups across partitions).
	 * @param xmlFilename
	 *            path to the xml file to which the sequence will be saved. This
	 *            is used to generate paths for the partitions.
	 * @return list of partitions.
	 */
	public static ArrayList< Partition > split( final SetupAggregator aggregator, final int timepointsPerPartition, final int setupsPerPartition, final String xmlFilename )
	{
		final String basename = xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename;
		final String partitionFilenameFormat = basename + "-%02d-%02d.h5";
		final int numTimepoints = aggregator.timepoints.size();
		final int numSetups = aggregator.setups.size();

		final ArrayList< Integer > timepointSplits = new ArrayList< Integer >();
		timepointSplits.add( 0 );
		if ( timepointsPerPartition > 0 )
			for ( int t = timepointsPerPartition; t < numTimepoints; t += timepointsPerPartition )
				timepointSplits.add( t );
		timepointSplits.add( numTimepoints );

		final ArrayList< Integer > setupSplits = new ArrayList< Integer >();
		setupSplits.add( 0 );
		if ( setupsPerPartition > 0 )
			for ( int s = setupsPerPartition; s < numSetups; s += setupsPerPartition )
				setupSplits.add( s );
		setupSplits.add( numSetups );

		final ArrayList< Partition > partitions = new ArrayList< Partition >();
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
				final String path = String.format( partitionFilenameFormat, i, j );
				partitions.add( new Partition( path, timepointOffset, timepointStart, timepointLength, setupOffset, setupStart, setupLength ) );
			}
		}

		return partitions;
	}

	/**
	 * Get the partition from the given list that contains the given timepoint and setup.
	 *
	 * @return partition that contains given timepoint and setup or null if there is no such partition.
	 */
	Partition select( final List< Partition > partitions, final int timepoint, final int setup )
	{
		for ( final Partition p : partitions )
			if ( p.contains( timepoint, setup ) )
				return p;
		return null;
	}

	public static class PartitionedSequenceWriter
	{
		protected final SequenceDescription seq;

		protected final ViewRegistrations regs;

		protected final ArrayList< int[][] > perSetupResolutions;

		protected final ArrayList< int[][] > perSetupSubdivisions;

		protected final ArrayList< Partition > partitions;

		protected final File seqFile;

		protected final File hdf5File;

		public PartitionedSequenceWriter( final SetupAggregator setupCollector, final String xmlFilename, final List< Partition > partitions )
		{
			seq = setupCollector.createSequenceDescription( null );
			regs = setupCollector.createViewRegistrations();
			perSetupResolutions = setupCollector.getPerSetupResolutions();
			perSetupSubdivisions = setupCollector.getPerSetupSubdivisions();
			this.partitions = new ArrayList< Partition >( partitions );

			this.seqFile = new File( xmlFilename );

			final String hdf5Filename = ( xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename ) + ".h5";
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

		public void writeXmlAndLinks() throws IOException
		{
			WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, perSetupResolutions, perSetupSubdivisions, partitions, hdf5File );
			final Hdf5ImageLoader loader = new Hdf5ImageLoader( hdf5File, partitions, false );
			final SequenceDescription sequenceDescription = new SequenceDescription( seq.setups, seq.timepoints, seqFile.getParentFile(), loader );
			WriteSequenceToXml.writeSequenceToXml( sequenceDescription, regs, seqFile.getAbsolutePath() );
		}
	}

	public static void main( final String[] args ) throws IOException, ConfigurationParserException
	{
		final SetupAggregator aggregator = new SetupAggregator();

		final String huiskenExperimentXmlFile = "/Users/pietzsch/workspace/data/fast fly/111010_weber/e012.xml";
		final String angles = "0,120,240";
		final String timepoints = "1-5";
		final int referenceTimePoint = 50;

		final SpimRegistrationSequence spimseq = createSpimRegistrationSequence( huiskenExperimentXmlFile, null, angles, timepoints, referenceTimePoint );

		final String filepath = "/Users/pietzsch/workspace/data/fast fly/111010_weber/e012/output/";
		final String filepattern = "img_tl%1$d_ch%2$d_z%3$03d.tif";
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

		aggregator.addSetups( spimseq, spimresolutions, spimsubdivisions );
		aggregator.addSetups( fusion, fusionresolutions, fusionsubdivisions );

		final String xmlFilename = "/Users/pietzsch/Desktop/everything.xml";

		// splitting ...
		final int timepointsPerPartition = 3;
		final int setupsPerPartition = 2;
		final ArrayList< Partition > partitions = split( aggregator, timepointsPerPartition, setupsPerPartition, xmlFilename );

		final PartitionedSequenceWriter writer = new PartitionedSequenceWriter( aggregator,xmlFilename, partitions );
		System.out.println( writer.numPartitions() );
		for ( int i = 0; i < writer.numPartitions(); ++i )
			writer.writePartition( i );
		writer.writeXmlAndLinks();
	}
}
