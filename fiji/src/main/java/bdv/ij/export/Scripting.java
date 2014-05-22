package bdv.ij.export;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.io.ConfigurationParserException;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.export.ExportMipmapInfo;
import bdv.export.WriteSequenceToHdf5;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

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
	public static SpimRegistrationSequence createSpimRegistrationSequence(
			final String huiskenExperimentXmlFile,
			final String channels,
			final String angles,
			final String timepoints,
			final int referenceTimePoint ) throws ConfigurationParserException
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
	public static SpimRegistrationSequence createSpimRegistrationSequence(
			final String inputDirectory,
			final String inputFilePattern,
			final String channels,
			final String angles,
			final String timepoints,
			final int referenceTimePoint,
			final boolean overrideImageZStretching,
			final double zStretching ) throws ConfigurationParserException
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
	public static Map< Integer, AffineTransform3D > getFusionTransforms(
			final SpimRegistrationSequence spimseq,
			final int scale,
			final int cropOffsetX,
			final int cropOffsetY,
			final int cropOffsetZ )
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
	public static FusionResult createFusionResult(
			final SpimRegistrationSequence spimseq,
			final String filepath,
			final String filepattern,
			final int numSlices,
			final double sliceValueMin,
			final double sliceValueMax,
			final Map< Integer, AffineTransform3D > fusionTransforms )
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
	public static ArrayList< Partition > split(
			final SetupAggregator aggregator,
			final int timepointsPerPartition,
			final int setupsPerPartition,
			final String xmlFilename )
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

		final ArrayList< HashMap< Integer, Integer > > timepointMaps = new ArrayList< HashMap< Integer, Integer > >();
		final List< TimePoint > timepoints = aggregator.timepoints.getTimePointsOrdered();
		for ( int i = 0; i < timepointSplits.size() - 1; ++i )
		{
			final HashMap< Integer, Integer > timepointIdSequenceToPartition = new HashMap< Integer, Integer >();
			for ( int t = timepointSplits.get( i ); t < timepointSplits.get( i + 1 ); ++t )
			{
				final int id = timepoints.get( t ).getId();
				timepointIdSequenceToPartition.put( id, id );
			}
			timepointMaps.add( timepointIdSequenceToPartition );
		}

		final ArrayList< Integer > setupSplits = new ArrayList< Integer >();
		setupSplits.add( 0 );
		if ( setupsPerPartition > 0 )
			for ( int s = setupsPerPartition; s < numSetups; s += setupsPerPartition )
				setupSplits.add( s );
		setupSplits.add( numSetups );

		final ArrayList< HashMap< Integer, Integer > > setupMaps = new ArrayList< HashMap< Integer, Integer > >();
		for ( int i = 0; i < timepointSplits.size() - 1; ++i )
		{
			final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap< Integer, Integer >();
			for ( int s = setupSplits.get( i ); s < setupSplits.get( i + 1 ); ++s )
			{
				final int id = aggregator.setups.get( s ).getId();
				setupIdSequenceToPartition.put( id, id );
			}
			setupMaps.add( setupIdSequenceToPartition );
		}

		final ArrayList< Partition > partitions = new ArrayList< Partition >();
		for ( int t = 0; t < timepointMaps.size(); ++t )
		{
			for ( int s = 0; s < setupMaps.size(); ++s )
			{
				final String path = String.format( partitionFilenameFormat, t, s );
				partitions.add( new Partition( path, timepointMaps.get( t ),setupMaps.get( s ) ) );
			}
		}

		return partitions;
	}

	public static class PartitionedSequenceWriter
	{
		protected final SpimDataMinimal spimData;

		protected final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo;

		protected final ArrayList< Partition > partitions;

		protected final File seqFile;

		protected final File hdf5File;

		public PartitionedSequenceWriter( final SetupAggregator aggregator, final String xmlFilename, final List< Partition > partitions )
		{
			seqFile = new File( xmlFilename );

			final String hdf5Filename = ( xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename ) + ".h5";
			hdf5File = new File( hdf5Filename );

			spimData = aggregator.createSpimData( seqFile );
			perSetupMipmapInfo = aggregator.getPerSetupMipmapInfo();
			this.partitions = new ArrayList< Partition >( partitions );
		}

		public int numPartitions()
		{
			return partitions.size();
		}

		public void writePartition( final int index )
		{
			if ( index >= 0 && index < partitions.size() )
				WriteSequenceToHdf5.writeHdf5PartitionFile( spimData.getSequenceDescription(), perSetupMipmapInfo, partitions.get( index ), null );
		}

		public void writeXmlAndLinks() throws SpimDataException
		{
			final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
			WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, perSetupMipmapInfo, partitions, hdf5File );
			final Hdf5ImageLoader loader = new Hdf5ImageLoader( hdf5File, partitions, null, false );

			new XmlIoSpimDataMinimal().save( new SpimDataMinimal( spimData, loader ), seqFile.getAbsolutePath() );
		}
	}

	public static void example_main( final String[] args ) throws Exception
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

		final Map< Integer, AffineTransform3D > fusionTransforms = getFusionTransforms( spimseq, scale, cropOffsetX, cropOffsetY, cropOffsetZ );
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
