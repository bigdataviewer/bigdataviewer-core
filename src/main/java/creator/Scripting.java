package creator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import mpicbg.spim.io.ConfigurationParserException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import viewer.hdf5.Hdf5ImageLoader;
import viewer.hdf5.Partition;
import creator.spim.SpimRegistrationSequence;
import creator.spim.imgloader.FusionImageLoader;

public class Scripting
{
	protected static class ViewSetupWrapper extends ViewSetup
	{
		protected final SequenceDescription sourceSequence;

		protected final int sourceSetupIndex;

		protected ViewSetupWrapper( final int id, final SequenceDescription sourceSequence, final ViewSetup sourceSetup )
		{
			super( id, sourceSetup.getAngle(), sourceSetup.getIllumination(), sourceSetup.getChannel(), sourceSetup.getWidth(), sourceSetup.getHeight(), sourceSetup.getDepth(), sourceSetup.getPixelWidth(), sourceSetup.getPixelHeight(), sourceSetup.getPixelDepth() );
			this.sourceSequence = sourceSequence;
			this.sourceSetupIndex = sourceSetup.getId();
		}
	}

	/**
	 * Aggregate {@link ViewSetup setups}, i.e., SPIM source angles and fused
	 * datasets from multiple {@link SequenceDescription}s.
	 *
	 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
	 */
	public static class SetupCollector
	{
		/**
		 * timepoint id for every timepoint index.
		 */
		final protected ArrayList< Integer > timepoints;

		/**
		 * the id (not index!) of the reference timepoint.
		 */
		final protected int referenceTimePoint;

		final protected ArrayList< ViewRegistration > registrations;

		/**
		 * Contains {@link ViewSetupWrapper wrappers} around setups in other sequences.
		 */
		final protected ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >();

		final protected ArrayList< int[][] > perSetupResolutions = new ArrayList< int[][] >();

		final protected ArrayList< int[][] > perSetupSubdivisions = new ArrayList< int[][] >();

		/**
		 * Forwards image loading to wrapped source sequences.
		 */
		final protected ImgLoader imgLoader = new ImgLoader()
		{
			@Override
			public void init( final Element elem, final File basePath )
			{
				throw new UnsupportedOperationException( "not implemented" );
			}

			@Override
			public Element toXml( final Document doc, final File basePath )
			{
				throw new UnsupportedOperationException( "not implemented" );
			}

			@Override
			public RandomAccessibleInterval< FloatType > getImage( final View view )
			{
				throw new UnsupportedOperationException( "not implemented" );
			}

			@Override
			public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view )
			{
				final ViewSetupWrapper w = ( ViewSetupWrapper ) view.getSetup();
				return w.sourceSequence.imgLoader.getUnsignedShortImage( new View( w.sourceSequence, view.getTimepointIndex(), w.sourceSetupIndex, view.getModel() ) );
			}
		};

		protected SetupCollector( final List< Integer > timepoints, final int referenceTimePoint )
		{
			this.timepoints = new ArrayList< Integer >( timepoints );
			this.referenceTimePoint = referenceTimePoint;
			registrations = new ArrayList< ViewRegistration >();
		}

		protected void add( final SequenceDescription sourceSequence, final ViewSetup sourceSetup, final ViewRegistrations sourceRegs, final int[][] resolutions, final int[][] subdivisions )
		{
			final int setupIdx = setups.size();
			setups.add( new ViewSetupWrapper( setupIdx, sourceSequence, sourceSetup ) );
			final int s = sourceSetup.getId();
			for ( int timepointIdx = 0; timepointIdx < timepoints.size(); ++timepointIdx )
			{
				final int tp = timepoints.get( timepointIdx );
				boolean found = false;
				for( final ViewRegistration r : sourceRegs.registrations )
					if ( s == r.getSetupIndex() && tp == sourceSequence.timepoints.get( r.getTimepointIndex() ) )
					{
						found = true;
						registrations.add( new ViewRegistration( timepointIdx, setupIdx, r.getModel() ) );
						break;
					}
				if ( !found )
					throw new RuntimeException( "could not find ViewRegistration for timepoint " + tp + " in the source sequence." );
			}
			perSetupResolutions.add( resolutions );
			perSetupSubdivisions.add( subdivisions );
		}

		protected SequenceDescription createSequenceDescription( final File basePath )
		{
			return new SequenceDescription( setups, timepoints, basePath, imgLoader );
		}

		protected ViewRegistrations createViewRegistrations()
		{
			return new ViewRegistrations( registrations, referenceTimePoint );
		}

		protected ArrayList< int[][] > getPerSetupResolutions()
		{
			return perSetupResolutions;
		}

		protected ArrayList< int[][] > getPerSetupSubdivisions()
		{
			return perSetupSubdivisions;
		}

		/**
		 * Add the setup (angle) of the given {@link SpimRegistrationSequence}
		 * to this collection. In the viewer format, every image is stored in
		 * multiple resolutions. The resolutions are described as int[] arrays
		 * defining multiple of original pixel size in every dimension. For
		 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
		 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image
		 * is stored as a chunked three-dimensional array (each chunk
		 * corresponds to one cell of a {@link CellImg} when the data is
		 * loaded). The chunk sizes are defined by the subdivisions parameter
		 * which is an array of int[], one per resolution. Each int[] array
		 * describes the X,Y,Z chunk size for one resolution.
		 *
		 * @param sequence
		 *            a registered spim sequence. see
		 *            {@link Scripting#createSpimRegistrationSequence(String, String, String, String, int, boolean, double)}
		 * @param setupIndex
		 *            which of the setups of the spim sequence to add.
		 * @param resolutionsString
		 *            the set of resolutions to store, formatted like
		 *            "{1,1,1}, {2,2,1}, {4,4,4}" where each "{...}" defines one
		 *            resolution.
		 * @param subdivisionsString
		 *            the set of subdivisions to store, formatted like
		 *            "{32,32,32}, {16,16,8}, {8,8,8}" where each "{...}"
		 *            defines one subdivision.
		 */
		public void addSetup( final SpimRegistrationSequence sequence, final int setupIndex, final String resolutionsString, final String subdivisionsString )
		{
			final SequenceDescription desc = sequence.getSequenceDescription();
			final ViewRegistrations regs = sequence.getViewRegistrations();
			final int[][] resolutions = PluginHelper.parseResolutionsString( resolutionsString );
			final int[][] subdivisions = PluginHelper.parseResolutionsString( subdivisionsString );
			if ( resolutions.length == 0 )
				throw new RuntimeException( "Cannot parse mipmap resolutions" + resolutionsString );
			if ( subdivisions.length == 0 )
				throw new RuntimeException( "Cannot parse subdivisions " + subdivisionsString );
			else if ( resolutions.length != subdivisions.length )
				throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
			final ViewSetup setup = desc.setups.get( setupIndex );
				add( desc, setup, regs, resolutions, subdivisions );
		}

		/**
		 * Add the setup (angle) of the given {@link SpimRegistrationSequence}
		 * to this collection. In the viewer format, every image is stored in
		 * multiple resolutions. The resolutions are described as int[] arrays
		 * defining multiple of original pixel size in every dimension. For
		 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
		 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image
		 * is stored as a chunked three-dimensional array (each chunk
		 * corresponds to one cell of a {@link CellImg} when the data is
		 * loaded). The chunk sizes are defined by the subdivisions parameter
		 * which is an array of int[], one per resolution. Each int[] array
		 * describes the X,Y,Z chunk size for one resolution.
		 *
		 * @param sequence
		 *            a registered spim sequence. see
		 *            {@link Scripting#createSpimRegistrationSequence(String, String, String, String, int, boolean, double)}
		 * @param setupIndex
		 *            which of the setups of the spim sequence to add.
		 * @param resolutions
		 *            the set of resolutions to store. each nested int[] array
		 *            defines one resolution.
		 * @param subdivisions
		 *            the set of subdivisions to store. each nested int[] array
		 *            defines one subdivision.
		 */
		public void addSetup( final SpimRegistrationSequence sequence, final int setupIndex, final int[][] resolutions, final int[][] subdivisions )
		{
			final SequenceDescription desc = sequence.getSequenceDescription();
			final ViewRegistrations regs = sequence.getViewRegistrations();
			if ( resolutions.length != subdivisions.length )
				throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
			final ViewSetup setup = desc.setups.get( setupIndex );
				add( desc, setup, regs, resolutions, subdivisions );
		}

		/**
		 * Add all setups (angles) of the given {@link SpimRegistrationSequence}
		 * to this collection. In the viewer format, every image is stored in
		 * multiple resolutions. The resolutions are described as int[] arrays
		 * defining multiple of original pixel size in every dimension. For
		 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
		 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image
		 * is stored as a chunked three-dimensional array (each chunk
		 * corresponds to one cell of a {@link CellImg} when the data is
		 * loaded). The chunk sizes are defined by the subdivisions parameter
		 * which is an array of int[], one per resolution. Each int[] array
		 * describes the X,Y,Z chunk size for one resolution.
		 *
		 * @param sequence
		 *            a registered spim sequence. see
		 *            {@link Scripting#createSpimRegistrationSequence(String, String, String, String, int, boolean, double)}
		 * @param resolutionsString
		 *            the set of resolutions to store, formatted like
		 *            "{1,1,1}, {2,2,1}, {4,4,4}" where each "{...}" defines one
		 *            resolution.
		 * @param subdivisionsString
		 *            the set of subdivisions to store, formatted like
		 *            "{32,32,32}, {16,16,8}, {8,8,8}" where each "{...}"
		 *            defines one subdivision.
		 */
		public void addSetups( final SpimRegistrationSequence sequence, final String resolutionsString, final String subdivisionsString )
		{
			for ( int s = 0; s < sequence.getSequenceDescription().setups.size(); ++s )
				addSetup( sequence, s, resolutionsString, subdivisionsString );
		}

		/**
		 * Add all setups (angles) of the given {@link SpimRegistrationSequence}
		 * to this collection. In the viewer format, every image is stored in
		 * multiple resolutions. The resolutions are described as int[] arrays
		 * defining multiple of original pixel size in every dimension. For
		 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
		 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image
		 * is stored as a chunked three-dimensional array (each chunk
		 * corresponds to one cell of a {@link CellImg} when the data is
		 * loaded). The chunk sizes are defined by the subdivisions parameter
		 * which is an array of int[], one per resolution. Each int[] array
		 * describes the X,Y,Z chunk size for one resolution.
		 *
		 * @param sequence
		 *            a registered spim sequence. see
		 *            {@link Scripting#createSpimRegistrationSequence(String, String, String, String, int, boolean, double)}
		 * @param resolutions
		 *            the set of resolutions to store. each nested int[] array
		 *            defines one resolution.
		 * @param subdivisions
		 *            the set of subdivisions to store. each nested int[] array
		 *            defines one subdivision.
		 */
		public void addSetups( final SpimRegistrationSequence sequence, final int[][] resolutions, final int[][] subdivisions )
		{
			for ( int s = 0; s < sequence.getSequenceDescription().setups.size(); ++s )
				addSetup( sequence, s, resolutions, subdivisions );
		}

		/**
		 * Add result of SPIM fusion or deconvolution as a setup to this
		 * collection. In the viewer format, every image is stored in multiple
		 * resolutions. The resolutions are described as int[] arrays defining
		 * multiple of original pixel size in every dimension. For example
		 * {1,1,1} is the original resolution, {4,4,2} is downsampled by factor
		 * 4 in X and Y and factor 2 in Z. Each resolution of the image is
		 * stored as a chunked three-dimensional array (each chunk corresponds
		 * to one cell of a {@link CellImg} when the data is loaded). The chunk
		 * sizes are defined by the subdivisions parameter which is an array of
		 * int[], one per resolution. Each int[] array describes the X,Y,Z chunk
		 * size for one resolution.
		 *
		 * @param fusionResult
		 *            a fused spim sequence.
		 *            {@link Scripting#createFusionResult(SpimRegistrationSequence, String, String, int, double, double, AffineTransform3D)}
		 * @param resolutionsString
		 *            the set of resolutions to store, formatted like
		 *            "{1,1,1}, {2,2,1}, {4,4,4}" where each "{...}" defines one
		 *            resolution.
		 * @param subdivisionsString
		 *            the set of subdivisions to store, formatted like
		 *            "{32,32,32}, {16,16,8}, {8,8,8}" where each "{...}"
		 *            defines one subdivision.
		 */
		public void addSetups( final FusionResult fusionResult, final String resolutionsString, final String subdivisionsString )
		{
			final int[][] resolutions = PluginHelper.parseResolutionsString( resolutionsString );
			final int[][] subdivisions = PluginHelper.parseResolutionsString( subdivisionsString );
			if ( resolutions.length == 0 )
				throw new RuntimeException( "Cannot parse mipmap resolutions" + resolutionsString );
			if ( subdivisions.length == 0 )
				throw new RuntimeException( "Cannot parse subdivisions " + subdivisionsString );
			else if ( resolutions.length != subdivisions.length )
				throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
			for ( final ViewSetup setup : fusionResult.desc.setups )
				add( fusionResult.desc, setup, fusionResult.regs, resolutions, subdivisions );
		}

		/**
		 * Add result of SPIM fusion or deconvolution as a setup to this
		 * collection. In the viewer format, every image is stored in multiple
		 * resolutions. The resolutions are described as int[] arrays defining
		 * multiple of original pixel size in every dimension. For example
		 * {1,1,1} is the original resolution, {4,4,2} is downsampled by factor
		 * 4 in X and Y and factor 2 in Z. Each resolution of the image is
		 * stored as a chunked three-dimensional array (final each chunk
		 * corresponds to one cell of a {@link CellImg} when the data is
		 * loaded). The chunk sizes are defined by the subdivisions parameter
		 * which is an array of int[], one per resolution. Each int[] array
		 * describes the X,Y,Z chunk size for one resolution.
		 *
		 * @param fusionResult
		 *            a fused spim sequence.
		 *            {@link Scripting#createFusionResult(SpimRegistrationSequence, String, String, int, double, double, AffineTransform3D)}
		 * @param resolutions
		 *            the set of resolutions to store. each nested int[] array
		 *            defines one resolution.
		 * @param subdivisions
		 *            the set of subdivisions to store. each nested int[] array
		 *            defines one subdivision.
		 */
		public void addSetups( final FusionResult fusionResult, final int[][] resolutions, final int[][] subdivisions )
		{
			if ( resolutions.length != subdivisions.length )
				throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
			for ( final ViewSetup setup : fusionResult.desc.setups )
				add( fusionResult.desc, setup, fusionResult.regs, resolutions, subdivisions );
		}
	}

	public static class FusionResult
	{
		final SequenceDescription desc;

		final ViewRegistrations regs;

		public FusionResult( final SpimRegistrationSequence spimseq, final String filepath, final String filepattern, final int numSlices, final double sliceValueMin, final double sliceValueMax, final AffineTransform3D fusionTransform )
		{
			final SequenceDescription spimdesc = spimseq.getSequenceDescription();
			final ImgLoader fusionLoader = new FusionImageLoader< FloatType >( filepath +"/" + filepattern, numSlices, new FusionImageLoader.Gray32ImagePlusLoader(), sliceValueMin, sliceValueMax );
			desc = new SequenceDescription( Arrays.asList( new ViewSetup( 0, 0, 0, 0, 0, 0, numSlices, 1, 1, 1 ) ), spimdesc.timepoints, null, fusionLoader );
			final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
			for ( int timepoint = 0; timepoint < spimdesc.numTimepoints(); ++timepoint )
				registrations.add( new ViewRegistration( timepoint, 0, fusionTransform ) );
			regs = new ViewRegistrations( registrations, spimseq.getViewRegistrations().referenceTimePoint );
		}
	}

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
	public static AffineTransform3D getFusionTransform( final SpimRegistrationSequence spimseq, final int scale, final int cropOffsetX, final int cropOffsetY, final int cropOffsetZ )
	{
		return spimseq.getFusionTransform( cropOffsetX, cropOffsetY, cropOffsetZ, scale );
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
	public static FusionResult createFusionResult( final SpimRegistrationSequence spimseq, final String filepath, final String filepattern, final int numSlices, final double sliceValueMin, final double sliceValueMax, final AffineTransform3D fusionTransform )
	{
		return new FusionResult( spimseq, filepath, filepattern, numSlices, sliceValueMin, sliceValueMax, fusionTransform );
	}

	/**
	 * TODO
	 * @param timepoints
	 * @param referenceTimePoint
	 * @return
	 */
	public static SetupCollector createEmptyCollector( final SpimRegistrationSequence sequence )
	{
		return new SetupCollector( sequence.getSequenceDescription().timepoints, sequence.getViewRegistrations().referenceTimePoint );
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

		public PartitionedSequenceWriter( final SetupCollector setupCollector, final ArrayList< Integer > timepointStarts, final ArrayList< Integer > setupStarts, final String xmlFilename, final String hdf5Filename, final String hdf5PartitionFilenameFormat )
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
			WriteSequenceToHdf5.writeHdf5PartitionFile( seq, perSetupResolutions, perSetupSubdivisions, partitions.get( index ), null );
		}

		public void writeXmlAndLinks() throws TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException
		{
			WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, perSetupResolutions, perSetupSubdivisions, partitions, hdf5File );
			final Hdf5ImageLoader loader = new Hdf5ImageLoader( hdf5File, partitions, false );
			final SequenceDescription sequenceDescription = new SequenceDescription( seq.setups, seq.timepoints, seqFile.getParentFile(), loader );
			WriteSequenceToXml.writeSequenceToXml( sequenceDescription, regs, seqFile.getAbsolutePath() );
		}
	}

	public static void main( final String[] args ) throws ConfigurationParserException, TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException
	{

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

		final AffineTransform3D fusionTransform = getFusionTransform( createSpimRegistrationSequence( huiskenExperimentXmlFile, angles, ""+referenceTimePoint, referenceTimePoint ), scale, cropOffsetX, cropOffsetY, cropOffsetZ );

		final FusionResult fusion = createFusionResult( spimseq, filepath, filepattern, numSlices, sliceValueMin, sliceValueMax, fusionTransform );

		final int[][] spimresolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 2 } };
		final int[][] spimsubdivisions = { { 32, 32, 4 }, { 16, 16, 8 }, { 8, 8, 8 } };

		final String fusionresolutions = "{ 1, 1, 1 }, { 2, 2, 2 }, { 4, 4, 4 }";
		final String fusionsubdivisions = "{ 16, 16, 16 }, { 16, 16, 16 }, { 8, 8, 8 }";

		final SetupCollector collector = createEmptyCollector( spimseq );
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
