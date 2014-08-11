package bdv.ij;

import fiji.plugin.Bead_Registration;
import fiji.plugin.Multi_View_Fusion;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.io.TextFileAccess;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Pair;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.ValuePair;
import spimopener.SPIMExperiment;
import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5;
import bdv.ij.export.FusionResult;
import bdv.ij.export.SpimRegistrationSequence;
import bdv.ij.export.ViewSetupWrapper;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.Partition;
import bdv.img.hdf5.Util;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

public class ExportSpimFusionPlugIn implements PlugIn
{
	static double minValueStatic = 0;

	static double maxValueStatic = 65535;

	static boolean lastSetMipmapManual = false;

	static String lastSubsampling = "{1,1,1}, {2,2,2}, {4,4,4}";

	static String lastChunkSizes = "{32,32,32}, {16,16,16}, {8,8,8}";

	static boolean lastSplit = false;

	static int lastTimepointsPerPartition = 0;

	static int lastSetupsPerPartition = 0;

	static boolean lastDeflate = true;

	static String autoSubsampling = "{1,1,1}";

	static String autoChunkSizes = "{16,16,16}";

	@Override
	public void run( final String arg0 )
	{
		final Parameters params = getParameters();

		// cancelled
		if ( params == null )
			return;

		try
		{
			IJ.log( "starting export..." );
			if ( params.appendToExistingFile && params.seqFile.exists() )
				appendToExistingFile( params );
			else
				saveAsNewFile( params );
			IJ.showProgress( 1 );
			IJ.log( "done" );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

	public static void appendToExistingFile( final Parameters params ) throws SpimDataException, IOException
	{
		final ProgressWriter progress = new ProgressWriterIJ();
		final XmlIoSpimDataMinimal spimDataIo = new XmlIoSpimDataMinimal();

		final SpimRegistrationSequence spimseq = new SpimRegistrationSequence( params.conf );
		final Map< Integer, AffineTransform3D > fusionTransforms = spimseq.getFusionTransforms( params.cropOffsetX, params.cropOffsetY, params.cropOffsetZ, params.scale );
		final FusionResult fusionResult = FusionResult.create( spimseq, params.fusionDirectory, params.filenamePattern, params.numSlices, params.sliceValueMin, params.sliceValueMax, fusionTransforms );
		SequenceDescriptionMinimal fusionSeq = fusionResult.getSequenceDescription();
		ViewRegistrations fusionReg = fusionResult.getViewRegistrations();



		// open existing dataset
		final File existingDatasetXmlFile = params.seqFile;
		final File baseDirectory = existingDatasetXmlFile.getParentFile();
		final SpimDataMinimal existingSpimData = spimDataIo.load( existingDatasetXmlFile.getAbsolutePath() );
		final SequenceDescriptionMinimal existingSequence = existingSpimData.getSequenceDescription();
		final ViewRegistrations existingRegistrations = existingSpimData.getViewRegistrations();
		final Hdf5ImageLoader existingHdf5Loader = ( Hdf5ImageLoader ) existingSequence.getImgLoader();

		// maps every existing timepoint id to itself, needed for partitions
		final Map< Integer, Integer > timepointIdentityMap = new HashMap< Integer, Integer >();
		for ( final TimePoint tp : existingSequence.getTimePoints().getTimePointsOrdered() )
			timepointIdentityMap.put( tp.getId(), tp.getId() );

		// maps every existing setup id to itself, needed for partitions
		final Map< Integer, Integer > setupIdentityMap = new HashMap< Integer, Integer >();
		for ( final int s : existingSequence.getViewSetups().keySet() )
			setupIdentityMap.put( s, s );

		// create partition list for existing dataset
		final ArrayList< Partition > partitions = new ArrayList< Partition >( existingHdf5Loader.getPartitions() );
		final boolean notYetPartitioned = partitions.isEmpty();
		if ( notYetPartitioned )
			// add one partition for the unpartitioned existing dataset
			partitions.add( new Partition( existingHdf5Loader.getHdf5File().getAbsolutePath(), timepointIdentityMap, setupIdentityMap ) );



		// wrap fused data setups with unused setup ids
		final HashSet< Integer > usedSetupIds = new HashSet< Integer >( existingSequence.getViewSetups().keySet() );
		final HashMap< Integer, ViewSetupWrapper > fusionSetups = new HashMap< Integer, ViewSetupWrapper >();
		final ArrayList< ViewRegistration > fusionRegistrations = new ArrayList< ViewRegistration >();
		for ( final BasicViewSetup s : fusionSeq.getViewSetupsOrdered() )
		{
			int fusionSetupId = 0;
			while ( usedSetupIds.contains( fusionSetupId ) )
				++fusionSetupId;
			fusionSetups.put( fusionSetupId, new ViewSetupWrapper( fusionSetupId, fusionSeq, s ) );
			usedSetupIds.add( fusionSetupId );

			final int sourceSetupId = s.getId();
			for ( final TimePoint timepoint : fusionSeq.getTimePoints().getTimePointsOrdered() )
			{
				final int timepointId = timepoint.getId();
				final ViewRegistration r = fusionReg.getViewRegistrations().get( new ViewId( timepointId, sourceSetupId ) );
				if ( r == null )
					throw new RuntimeException( "could not find ViewRegistration for timepoint " + timepointId + " in the fused sequence." );
				fusionRegistrations.add( new ViewRegistration( timepointId, fusionSetupId, r.getModel() ) );
			}

		}
		final BasicImgLoader< UnsignedShortType > wrappedFusionImgLoader = new BasicImgLoader< UnsignedShortType >()
		{
			@Override
			public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view )
			{
				final ViewSetupWrapper w = fusionSetups.get( view.getViewSetupId() );
				@SuppressWarnings( "unchecked" )
				final BasicImgLoader< UnsignedShortType > il = ( BasicImgLoader< UnsignedShortType > ) w.getSourceSequence().getImgLoader();
				return il.getImage( new ViewId( view.getTimePointId(), w.getSourceSetupId() ) );
			}

			@Override
			public UnsignedShortType getImageType()
			{
				return new UnsignedShortType();
			}
		};
		fusionSeq = new SequenceDescriptionMinimal( fusionSeq.getTimePoints(), fusionSetups, wrappedFusionImgLoader, fusionSeq.getMissingViews() );
		fusionReg = new ViewRegistrations( fusionRegistrations );

		// add partitions for the fused data and split if desired
		final ArrayList< Partition > newPartitions = new ArrayList< Partition >();
		final String xmlFilename = params.seqFile.getAbsolutePath();
		final String basename = xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename;
		if ( params.split )
		{
			final List< TimePoint > timepoints = fusionSeq.getTimePoints().getTimePointsOrdered();
			final List< BasicViewSetup > setups = fusionSeq.getViewSetupsOrdered();
			for ( final Partition p : Partition.split( timepoints, setups, params.timepointsPerPartition, params.setupsPerPartition, basename ) )
			{
				final String baseFilename = p.getPath().substring( 0, p.getPath().length() - 3 ); // strip ".h5" extension
				final String path = PluginHelper.createNewPartitionFile( baseFilename ).getAbsolutePath();
				final Partition partition = new Partition( path, p.getTimepointIdSequenceToPartition(), p.getSetupIdSequenceToPartition() );
				newPartitions.add( partition );
				partitions.add( partition );
			}
		}
		else
		{
			final String path = PluginHelper.createNewPartitionFile( basename ).getAbsolutePath();
			final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap< Integer, Integer >();
			for ( final BasicViewSetup s : fusionSeq.getViewSetupsOrdered() )
				setupIdSequenceToPartition.put( s.getId(), s.getId() );
			final Partition partition = new Partition( path, timepointIdentityMap, setupIdSequenceToPartition );
			newPartitions.add( partition );
			partitions.add( partition );
		}

		// create ExportMipmapInfos for the fused data setups
		final Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
		final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo( params.resolutions, params.subdivisions );
		for ( final BasicViewSetup setup : fusionSeq.getViewSetupsOrdered() )
			perSetupExportMipmapInfo.put( setup.getId(), mipmapInfo );




		// determine filename for hdf5 link file
		final File newHdf5PartitionLinkFile;
		if ( notYetPartitioned )
			newHdf5PartitionLinkFile = PluginHelper.createNewPartitionFile( basename );
		else
			// if the existing dataset is already partitioned, override the old link file
			newHdf5PartitionLinkFile = params.hdf5File;



		// create aggregate SequenceDescription
		// TODO: For now the timepoints are just taken from the fusionSeq.
		// To do it properly, timepoints from existing dataset and fusionSeq should be combined.
		final TimePoints aggregateTimePoints = fusionSeq.getTimePoints();
		final HashMap< Integer, BasicViewSetup > aggregateSetups = new HashMap< Integer, BasicViewSetup >();
		for ( final BasicViewSetup s : existingSequence.getViewSetupsOrdered() )
			aggregateSetups.put( s.getId(), s );
		for ( final BasicViewSetup s : fusionSeq.getViewSetupsOrdered() )
			aggregateSetups.put( s.getId(), s );
		// TODO: For now the missingviews are just taken from the existingSequence.
		// To do it properly, missingviews from existing dataset and fusionSeq should be combined,
		// and new missingviews added for timepoints that are not present in one of existingSequence or fusionSeq.
		final MissingViews aggregateMissingViews = existingSequence.getMissingViews();
		final SequenceDescriptionMinimal aggregateSeq = new SequenceDescriptionMinimal(
				aggregateTimePoints,
				aggregateSetups,
				new Hdf5ImageLoader( newHdf5PartitionLinkFile, partitions, null, false ),
				aggregateMissingViews );

		// create aggregate ExportMipmapInfos
		final HashMap< Integer, ExportMipmapInfo > aggregateMipmapInfos = new HashMap< Integer, ExportMipmapInfo >( perSetupExportMipmapInfo );
		for ( final BasicViewSetup s : existingSequence.getViewSetupsOrdered() )
		{
			final MipmapInfo info = existingHdf5Loader.getMipmapInfo( s.getId() );
			aggregateMipmapInfos.put(
					s.getId(),
					new ExportMipmapInfo( Util.castToInts( info.getResolutions() ), info.getSubdivisions() ) );
		}

		// create aggregate ViewRegistrations
		final ArrayList< ViewRegistration > regs = new ArrayList< ViewRegistration >();
		regs.addAll( existingSpimData.getViewRegistrations().getViewRegistrationsOrdered() );
		regs.addAll( fusionReg.getViewRegistrationsOrdered() );
		final ViewRegistrations aggregateViewRegistrstions = new ViewRegistrations( regs );

		// create aggregate SpimData
		final SpimDataMinimal aggregateSpimData = new SpimDataMinimal( baseDirectory, aggregateSeq, aggregateViewRegistrstions );



		double complete = 0.05;
		progress.setProgress( complete );

		// write new data partitions
		final double completionStep = ( 0.95 - complete ) / newPartitions.size();
		for ( final Partition partition : newPartitions )
		{
			final SubTaskProgressWriter subtaskProgress = new SubTaskProgressWriter( progress, complete, complete + completionStep );
			WriteSequenceToHdf5.writeHdf5PartitionFile( fusionSeq, perSetupExportMipmapInfo, params.deflate, partition, subtaskProgress );
			complete += completionStep;
		}

		// (re-)write hdf5 link file
		WriteSequenceToHdf5.writeHdf5PartitionLinkFile( aggregateSeq, aggregateMipmapInfos, partitions, newHdf5PartitionLinkFile );
		progress.setProgress( 1 );

		// re-write xml file
		spimDataIo.save(
				new SpimDataMinimal( aggregateSpimData, new Hdf5ImageLoader( newHdf5PartitionLinkFile, partitions, null, false ) ),
				params.seqFile.getAbsolutePath() );
	}

	public static void saveAsNewFile( final Parameters params ) throws SpimDataException
	{
		final ProgressWriter progress = new ProgressWriterIJ();
		final XmlIoSpimDataMinimal spimDataIo = new XmlIoSpimDataMinimal();

		final SpimRegistrationSequence spimseq = new SpimRegistrationSequence( params.conf );
		final Map< Integer, AffineTransform3D > fusionTransforms = spimseq.getFusionTransforms( params.cropOffsetX, params.cropOffsetY, params.cropOffsetZ, params.scale );
		final FusionResult fusionResult = FusionResult.create( spimseq, params.fusionDirectory, params.filenamePattern, params.numSlices, params.sliceValueMin, params.sliceValueMax, fusionTransforms );

		// sequence description (no ImgLoader yet)
		final SequenceDescriptionMinimal desc = fusionResult.getSequenceDescription();

		// create ExportMipmapInfos
		final Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
		final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo( params.resolutions, params.subdivisions );
		for ( final BasicViewSetup setup : desc.getViewSetupsOrdered() )
			perSetupExportMipmapInfo.put( setup.getId(), mipmapInfo );

		// create partitions if desired
		final ArrayList< Partition > partitions;
		if ( params.split )
		{
			final String xmlFilename = params.seqFile.getAbsolutePath();
			final String basename = xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename;
			final List< TimePoint > timepoints = desc.getTimePoints().getTimePointsOrdered();
			final List< BasicViewSetup > setups = desc.getViewSetupsOrdered();
			partitions = Partition.split( timepoints, setups, params.timepointsPerPartition, params.setupsPerPartition, basename );
		}
		else
			partitions = null;

		// write to hdf5
		if ( params.split )
		{
			for ( int i = 0; i < partitions.size(); ++i )
			{
				final Partition partition = partitions.get( i );
				final ProgressWriter p = new SubTaskProgressWriter( progress, 0, 0.95 * i / partitions.size() );
				WriteSequenceToHdf5.writeHdf5PartitionFile( desc, perSetupExportMipmapInfo, params.deflate, partition, p );
			}
			WriteSequenceToHdf5.writeHdf5PartitionLinkFile( desc, perSetupExportMipmapInfo, partitions, params.hdf5File );
		}
		else
		{
			WriteSequenceToHdf5.writeHdf5File( desc, perSetupExportMipmapInfo, params.deflate, params.hdf5File, new SubTaskProgressWriter( progress, 0, 0.95 ) );
		}

		// write xml file
		final Hdf5ImageLoader loader = new Hdf5ImageLoader( params.hdf5File, partitions, null, false );
		final SequenceDescriptionMinimal sequenceDescription = new SequenceDescriptionMinimal( desc, loader );

		final File basePath = params.seqFile.getParentFile();
		final SpimDataMinimal spimData = new SpimDataMinimal( basePath, sequenceDescription, fusionResult.getViewRegistrations() );
		try
		{
			spimDataIo.save( spimData, params.seqFile.getAbsolutePath() );
			progress.setProgress( 1.0 );
		}
		catch ( final Exception e )
		{
			progress.err().println( "Failed to write xml file " + params.seqFile );
			e.printStackTrace( progress.err() );
		}
	}

	public static String allChannels = "0, 1";

	public static class Parameters
	{
		final SPIMConfiguration conf;
		final int[][] resolutions;
		final int[][] subdivisions;
		final int cropOffsetX;
		final int cropOffsetY;
		final int cropOffsetZ;
		final int scale;
		final String fusionDirectory;
		final String filenamePattern;
		final int numSlices;
		final double sliceValueMin;
		final double sliceValueMax;
		final File seqFile;
		final File hdf5File;
		final boolean appendToExistingFile;
		final boolean deflate;
		final boolean split;
		final int timepointsPerPartition;
		final int setupsPerPartition;

		public Parameters( final SPIMConfiguration conf, final int[][] resolutions, final int[][] subdivisions,
				final int cropOffsetX, final int cropOffsetY, final int cropOffsetZ, final int scale,
				final String fusionDirectory, final String filenamePattern, final int numSlices,
				final double sliceValueMin, final double sliceValueMax,
				final File seqFile, final File hdf5File, final boolean appendToExistingFile, final boolean deflate,
				final boolean split, final int timepointsPerPartition, final int setupsPerPartition )
		{
			this.conf = conf;
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.cropOffsetX = cropOffsetX;
			this.cropOffsetY = cropOffsetY;
			this.cropOffsetZ = cropOffsetZ;
			this.scale = scale;
			this.fusionDirectory = fusionDirectory;
			this.filenamePattern = filenamePattern;
			this.numSlices = numSlices;
			this.sliceValueMin = sliceValueMin;
			this.sliceValueMax = sliceValueMax;
			this.seqFile = seqFile;
			this.hdf5File = hdf5File;
			this.appendToExistingFile = appendToExistingFile;
			this.deflate = deflate;
			this.split = split;
			this.timepointsPerPartition = timepointsPerPartition;
			this.setupsPerPartition = setupsPerPartition;
		}
	}

	protected Parameters getParameters()
	{
		final GenericDialog gd0 = new GenericDialogPlus( "Export for BigDataViewer" );
		gd0.addChoice( "Select_channel type", ExportSpimSequencePlugIn.fusionType, ExportSpimSequencePlugIn.fusionType[ Multi_View_Fusion.defaultFusionType ] );
		gd0.showDialog();
		if ( gd0.wasCanceled() )
			return null;
		final int channelChoice = gd0.getNextChoiceIndex();
		Multi_View_Fusion.defaultFusionType = channelChoice;
		final boolean multichannel = channelChoice == 1;

		final GenericDialogPlus gd = new GenericDialogPlus( "Export for BigDataViewer" );

		gd.addDirectoryOrFileField( "SPIM_data_directory", Bead_Registration.spimDataDirectory );
		final TextField tfSpimDataDirectory = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Pattern_of_SPIM files", Bead_Registration.fileNamePattern, 25 );
		final TextField tfFilePattern = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Timepoints_to_process", Bead_Registration.timepoints );
		final TextField tfTimepoints = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Angles to process", Bead_Registration.angles );
		final TextField tfAngles = (TextField) gd.getStringFields().lastElement();

		final TextField tfChannels;
		if ( multichannel )
		{
			gd.addStringField( "Channels to process", allChannels );
			tfChannels = (TextField) gd.getStringFields().lastElement();
		}
		else
			tfChannels = null;

//		gd.addMessage("");
//		gd.addMessage("This Plugin is developed by Tobias Pietzsch (pietzsch@mpi-cbg.de)\n");
//		Bead_Registration.addHyperLinkListener( (MultiLineLabel) gd.getMessage(), "mailto:pietzsch@mpi-cbg.de");

		gd.addDialogListener( new DialogListener()
		{
			@Override
			public boolean dialogItemChanged( final GenericDialog dialog, final AWTEvent e )
			{
				if ( e instanceof TextEvent && e.getID() == TextEvent.TEXT_VALUE_CHANGED && e.getSource() == tfSpimDataDirectory )
				{
					final TextField tf = ( TextField ) e.getSource();
					final String spimDataDirectory = tf.getText();
					final File f = new File( spimDataDirectory );
					if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
					{
						final SPIMExperiment exp = new SPIMExperiment( f.getAbsolutePath() );

						// disable file pattern field
						tfFilePattern.setEnabled( false );

						// set timepoint string
						String expTimepoints = "";
						if ( exp.timepointStart == exp.timepointEnd )
							expTimepoints = "" + exp.timepointStart;
						else
							expTimepoints = "" + exp.timepointStart + "-" + exp.timepointEnd;
						tfTimepoints.setText( expTimepoints );

						// set angles string
						String expAngles = "";
						for ( final String angle : exp.angles )
						{
							final int a = Integer.parseInt( angle.substring( 1, angle.length() ) );
							if ( !expAngles.equals( "" ) )
								expAngles += ",";
							expAngles += a;
						}
						tfAngles.setText( expAngles );

						if ( multichannel )
						{
							// set channels string
							String expChannels = "";
							for ( final String channel : exp.channels )
							{
								final int c = Integer.parseInt( channel.substring( 1, channel.length() ) );
								if ( !expChannels.equals( "" ) )
									expChannels += ",";
								expChannels += c;
							}
							tfChannels.setText( expChannels );
						}
					}
					else
					{
						// enable file pattern field
						tfFilePattern.setEnabled( true );
					}
				}
				return true;
			}
		} );

		File f = new File( tfSpimDataDirectory.getText() );
		if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
		{
			// disable file pattern field
			tfFilePattern.setEnabled( false );
			if ( multichannel )
			{
				// set channels string
				final SPIMExperiment exp = new SPIMExperiment( f.getAbsolutePath() );
				String expChannels = "";
				for ( final String channel : exp.channels )
				{
					final int c = Integer.parseInt( channel.substring( 1, channel.length() ) );
					if ( !expChannels.equals( "" ) )
						expChannels += ",";
					expChannels += c;
				}
				tfChannels.setText( expChannels );
			}
		}
		gd.showDialog();

		if ( gd.wasCanceled() )
			return null;

		Bead_Registration.spimDataDirectory = gd.getNextString();
		Bead_Registration.fileNamePattern = gd.getNextString();
		Bead_Registration.timepoints = gd.getNextString();
		Bead_Registration.angles = gd.getNextString();

		int numChannels = 0;
		ArrayList<Integer> channels;

		// verify this part
		if ( multichannel )
		{
			allChannels = gd.getNextString();

			try
			{
				channels = SPIMConfiguration.parseIntegerString( allChannels );
				numChannels = channels.size();
			}
			catch (final ConfigurationParserException e)
			{
				IOFunctions.printErr( "Cannot understand/parse the channels: " + allChannels );
				return null;
			}

			if ( numChannels < 1 )
			{
				IOFunctions.printErr( "There are no channels given: " + allChannels );
				return null;
			}
		}
		else
		{
			numChannels = 1;
			channels = new ArrayList<Integer>();
			channels.add( 0 );
		}

		// create the configuration object
		final SPIMConfiguration conf = new SPIMConfiguration();

		conf.timepointPattern = Bead_Registration.timepoints;
		conf.anglePattern = Bead_Registration.angles;
		if ( multichannel )
		{
			conf.channelPattern = allChannels;
			conf.channelsToRegister = allChannels;
			conf.channelsToFuse = allChannels;
		}
		else
		{
			conf.channelPattern = "";
			conf.channelsToRegister = "";
			conf.channelsToFuse = "";
		}
		conf.inputFilePattern = Bead_Registration.fileNamePattern;

		f = new File( Bead_Registration.spimDataDirectory );
		if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
		{
			conf.spimExperiment = new SPIMExperiment( f.getAbsolutePath() );
			conf.inputdirectory = f.getAbsolutePath().substring( 0, f.getAbsolutePath().length() - 4 );
			System.out.println( "inputdirectory : " + conf.inputdirectory );
		}
		else
		{
			conf.inputdirectory = Bead_Registration.spimDataDirectory;
		}

		conf.fuseOnly = true; // this is to avoid an exception in the multi-channel case

		// get filenames and so on...
		if ( ! ExportSpimSequencePlugIn.init( conf ) )
			return null;

		// test which registration files are there for each channel
		// file = new File[ timepoints.length ][ channels.length ][ angles.length ];
		final ArrayList<ArrayList<Integer>> timepoints = new ArrayList<ArrayList<Integer>>();
		int numChoices = 0;
		conf.zStretching = -1;

		for ( int c = 0; c < channels.size(); ++c )
		{
			timepoints.add( new ArrayList<Integer>() );

			final String name = conf.file[ 0 ][ c ][ 0 ][ 0 ].getName();
			final File regDir = new File( conf.registrationFiledirectory );

			IOFunctions.println( "name: " + name );
			IOFunctions.println( "dir: " + regDir.getAbsolutePath() );

			if ( !regDir.isDirectory() )
			{
				IOFunctions.println( conf.registrationFiledirectory + " is not a directory. " );
				return null;
			}

			final String entries[] = regDir.list( new FilenameFilter() {
				@Override
				public boolean accept(final File directory, final String filename)
				{
					if ( filename.contains( name ) && filename.contains( ".registration" ) )
						return true;
					else
						return false;
				}
			});

			for ( final String e : entries )
				IOFunctions.println( e );

			for ( final String s : entries )
			{
				if ( s.endsWith( ".registration" ) )
				{
					if ( !timepoints.get( c ).contains( -1 ) )
					{
						timepoints.get( c ).add( -1 );
						numChoices++;
					}
				}
				else
				{
					final int timepoint = Integer.parseInt( s.substring( s.indexOf( ".registration.to_" ) + 17, s.length() ) );

					if ( !timepoints.get( c ).contains( timepoint ) )
					{
						timepoints.get( c ).add( timepoint );
						numChoices++;
					}
				}

				if ( conf.zStretching < 0 )
				{
					conf.zStretching = loadZStretching( conf.registrationFiledirectory + s );
					conf.overrideImageZStretching = true;
					IOFunctions.println( "Z-stretching = " + conf.zStretching );
				}
			}
		}

		if ( numChoices == 0 )
		{
			IOFunctions.println( "No registration files available." );
			return null;
		}

		for ( int c = 0; c < channels.size(); ++c )
			for ( final int i : timepoints.get( c ) )
				IOFunctions.println( c + ": " + i );

		final GenericDialogPlus gd2 = new GenericDialogPlus( "Export for BigDataViewer" );

		// build up choices
		final String[] choices = new String[ numChoices ];
		final int[] suggest = new int[ channels.size() ];

		int firstSuggestion = -1;
		int index = 0;
		for ( int c = 0; c < channels.size(); ++c )
		{
			final ArrayList<Integer> tps = timepoints.get( c );

			// no suggestion yet
			suggest[ c ] = -1;
			for ( int i = 0; i < tps.size(); ++i )
			{
				if ( tps.get( i ) == -1 )
					choices[ index ] = "Individual registration of channel " + channels.get( c );
				else
					choices[ index ] = "Time-point registration (reference=" + tps.get( i ) + ") of channel " + channels.get( c );

				if ( suggest[ c ] < 1 )
				{
					suggest[ c ] = index;
					if ( firstSuggestion < 1 )
						firstSuggestion = index;
				}

				index++;
			}
		}

		for ( int c = 0; c < channels.size(); ++c )
			if ( suggest[ c ] == -1 )
				suggest[ c ] = firstSuggestion;

		for ( int c = 0; c < channels.size(); ++c )
			gd2.addChoice( "Registration for channel " + channels.get( c ), choices, choices[ suggest[ c ] ]);

		gd2.addMessage( "" );
		gd2.addDirectoryField( "Fusion Output Directory", conf.outputdirectory, 25 );
		final TextField tfDirectory = (TextField) gd2.getStringFields().lastElement();

		gd2.addMessage( "" );
		gd2.addMessage( "Enter the crop values you used to create the fused data:" );
		gd2.addNumericField( "Downsample_output image n-times", Multi_View_Fusion.outputImageScalingStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_x", Multi_View_Fusion.cropOffsetXStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_y", Multi_View_Fusion.cropOffsetYStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_z", Multi_View_Fusion.cropOffsetZStatic, 0 );

		gd2.addMessage( "" );
		gd2.addNumericField( "min brightness value", minValueStatic, 0 );
		gd2.addNumericField( "max brightness value", maxValueStatic, 0 );

		gd2.addMessage( "" );
		gd2.addCheckbox( "manual mipmap setup", lastSetMipmapManual );
		final Checkbox cManualMipmap = ( Checkbox ) gd2.getCheckboxes().lastElement();
		gd2.addStringField( "Subsampling factors", lastSubsampling, 25 );
		final TextField tfSubsampling = ( TextField ) gd2.getStringFields().lastElement();
		gd2.addStringField( "Hdf5 chunk sizes", lastChunkSizes, 25 );
		final TextField tfChunkSizes = ( TextField ) gd2.getStringFields().lastElement();

		gd2.addMessage( "" );
		gd2.addCheckbox( "split hdf5", lastSplit );
		final Checkbox cSplit = ( Checkbox ) gd2.getCheckboxes().lastElement();
		gd2.addNumericField( "timepoints per partition", lastTimepointsPerPartition, 0, 25, "" );
		final TextField tfSplitTimepoints = ( TextField ) gd2.getNumericFields().lastElement();
		gd2.addNumericField( "setups per partition", lastSetupsPerPartition, 0, 25, "" );
		final TextField tfSplitSetups = ( TextField ) gd2.getNumericFields().lastElement();

		gd2.addMessage( "" );
		gd2.addCheckbox( "use deflate compression", lastDeflate );

		gd2.addMessage( "" );
		PluginHelper.addSaveAsFileField( gd2, "Export path", conf.inputdirectory + "export.xml", 25 );
		gd2.addCheckbox( "add to existing file", true );

//		gd.addMessage("");
//		gd.addMessage("This Plugin is developed by Tobias Pietzsch (pietzsch@mpi-cbg.de)\n");
//		Bead_Registration.addHyperLinkListener( (MultiLineLabel) gd.getMessage(), "mailto:pietzsch@mpi-cbg.de");

		tfDirectory.addTextListener( new TextListener()
		{
			@Override
			public void textValueChanged( final TextEvent t )
			{
				if ( t.getID() == TextEvent.TEXT_VALUE_CHANGED )
				{
					final String fusionDirectory = tfDirectory.getText();
					if ( updateProposedMipmaps( fusionDirectory, conf ) )
					{
						final boolean useManual = cManualMipmap.getState();
						tfSubsampling.setEnabled( useManual );
						tfChunkSizes.setEnabled( useManual );
						if ( !useManual )
						{
							tfSubsampling.setText( autoSubsampling );
							tfChunkSizes.setText( autoChunkSizes );
						}
					}
				}
			}
		});

		cManualMipmap.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( final ItemEvent arg0 )
			{
				final boolean useManual = cManualMipmap.getState();
				tfSubsampling.setEnabled( useManual );
				tfChunkSizes.setEnabled( useManual );
				if ( !useManual )
				{
					tfSubsampling.setText( autoSubsampling );
					tfChunkSizes.setText( autoChunkSizes );
				}
			}
		} );

		updateProposedMipmaps( tfDirectory.getText(), conf );
		tfSubsampling.setEnabled( lastSetMipmapManual );
		tfChunkSizes.setEnabled( lastSetMipmapManual );
		if ( !lastSetMipmapManual )
		{
			tfSubsampling.setText( autoSubsampling );
			tfChunkSizes.setText( autoChunkSizes );
		}

		cSplit.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( final ItemEvent arg0 )
			{
				final boolean split = cSplit.getState();
				tfSplitTimepoints.setEnabled( split );
				tfSplitSetups.setEnabled( split );
			}
		} );

		tfSplitTimepoints.setEnabled( lastSplit );
		tfSplitSetups.setEnabled( lastSplit );

		gd2.showDialog();

		if ( gd2.wasCanceled() )
			return null;

		// which channel uses which registration file from which channel to be fused
		final int[][] registrationAssignment = new int[ channels.size() ][ 2 ];

		for ( int c = 0; c < channels.size(); ++c )
		{
			final int choice = gd2.getNextChoiceIndex();

			index = 0;
			for ( int c2 = 0; c2 < channels.size(); ++c2 )
			{
				final ArrayList<Integer> tps = timepoints.get( c2 );
				for ( int i = 0; i < tps.size(); ++i )
				{
					if ( index == choice )
					{
						registrationAssignment[ c ][ 0 ] = tps.get( i );
						registrationAssignment[ c ][ 1 ] = c2;
					}
					index++;
				}
			}
		}

		// test consistency
		final int tp = registrationAssignment[ 0 ][ 0 ];

		for ( int c = 1; c < channels.size(); ++c )
		{
			if ( tp != registrationAssignment[ c ][ 0 ] )
			{
				IOFunctions.println( "Inconsistent choice of reference timeseries, only same reference timepoints or individual registration are allowed.");
				return null;
			}
		}

		// save from which channel to load registration
		conf.registrationAssignmentForFusion = new int[ channels.size() ];
		for ( int c = 0; c < channels.size(); ++c )
		{
			IOFunctions.println( "channel " + c + " takes it from channel " + registrationAssignment[ c ][ 1 ] );
			conf.registrationAssignmentForFusion[ c ] = registrationAssignment[ c ][ 1 ];
		}

		conf.timeLapseRegistration = ( tp >= 0 );
		conf.referenceTimePoint = tp;

		IOFunctions.println( "tp " + tp );

		// get fusion directory
		final String fusionDirectory = gd2.getNextString();

		// detect filename pattern
		final Pair< String, Integer > pair = detectPatternAndNumSlices( new File ( fusionDirectory ), conf.timepoints[0] );
		if ( pair == null )
		{
			IOFunctions.println( "Couldn't detect filename pattern" );
			return null;
		}
		final String filenamePattern = pair.getA();
		final int numSlices = pair.getB();

		// get crop settings
		Multi_View_Fusion.outputImageScalingStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropOffsetXStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropOffsetYStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropOffsetZStatic = (int)Math.round( gd2.getNextNumber() );

		// get min/max brightness
		minValueStatic = gd2.getNextNumber();
		maxValueStatic = gd2.getNextNumber();

		// parse mipmap resolutions and cell sizes
		lastSetMipmapManual = gd2.getNextBoolean();
		lastSubsampling = gd2.getNextString();
		lastChunkSizes = gd2.getNextString();
		final int[][] resolutions = PluginHelper.parseResolutionsString( lastSubsampling );
		final int[][] subdivisions = PluginHelper.parseResolutionsString( lastChunkSizes );
		if ( resolutions.length == 0 )
		{
			IOFunctions.println( "Cannot parse subsampling factors " + lastSubsampling );
			return null;
		}
		if ( subdivisions.length == 0 )
		{
			IOFunctions.println( "Cannot parse hdf5 chunk sizes " + lastChunkSizes );
			return null;
		}
		else if ( resolutions.length != subdivisions.length )
		{
			IOFunctions.println( "subsampling factors and hdf5 chunk sizes must have the same number of elements" );
			return null;
		}

		lastSplit = gd2.getNextBoolean();
		lastTimepointsPerPartition = ( int ) gd2.getNextNumber();
		lastSetupsPerPartition = ( int ) gd2.getNextNumber();

		lastDeflate = gd2.getNextBoolean();

		String seqFilename = gd2.getNextString();
		if ( ! seqFilename.endsWith( ".xml" ) )
			seqFilename += ".xml";
		final File seqFile = new File( seqFilename );
		final File parent = seqFile.getParentFile();
		if ( parent == null || !parent.exists() || !parent.isDirectory() )
		{
			IOFunctions.println( "Invalid export filename " + seqFilename );
			return null;
		}
		final String hdf5Filename = seqFilename.substring( 0, seqFilename.length() - 4 ) + ".h5";
		final File hdf5File = new File( hdf5Filename );
		final boolean appendToExistingFile = gd2.getNextBoolean();

		final int cropOffsetX = Multi_View_Fusion.cropOffsetXStatic;
		final int cropOffsetY = Multi_View_Fusion.cropOffsetYStatic;
		final int cropOffsetZ = Multi_View_Fusion.cropOffsetZStatic;
		final int scale = Multi_View_Fusion.outputImageScalingStatic;
		return new Parameters( conf, resolutions, subdivisions, cropOffsetX, cropOffsetY, cropOffsetZ, scale, fusionDirectory, filenamePattern, numSlices, minValueStatic, maxValueStatic, seqFile, hdf5File, appendToExistingFile, lastDeflate, lastSplit, lastTimepointsPerPartition, lastSetupsPerPartition );
	}

	protected boolean updateProposedMipmaps( final String fusionDirectory, final SPIMConfiguration conf )
	{
		final Pair< String, Integer > pair = detectPatternAndNumSlices( new File ( fusionDirectory ), conf.timepoints[0] );
		if ( pair != null )
		{
			final String filenamePattern = pair.getA();
			final int numSlices = pair.getB();
			final String fn = fusionDirectory + "/" + String.format( filenamePattern, conf.timepoints[0], conf.channels[0], 0 );
			final ImagePlus imp = new ImagePlus( fn );
			final int width = imp.getWidth();
			final int height = imp.getHeight();
			imp.close();
			final Dimensions size = new FinalDimensions( new int[] { width, height, numSlices } );
			final VoxelDimensions voxelSize = new FinalVoxelDimensions( "px", 1, 1, 1 );
			final ExportMipmapInfo info = ProposeMipmaps.proposeMipmaps( new BasicViewSetup( 0, "", size, voxelSize ) );
			autoSubsampling = ProposeMipmaps.getArrayString( info.getExportResolutions() );
			autoChunkSizes = ProposeMipmaps.getArrayString( info.getSubdivisions() );
			return true;
		}
		else
			return false;
	}

	public static Pair< String, Integer > detectPatternAndNumSlices( final File dir, final int someTimepoint )
	{
		final File subdir = new File( dir.getAbsolutePath() + "/" + someTimepoint );
		if ( subdir.isDirectory() )
		{
			final Pair< String, Integer > pair = detectPatternAndNumSlices( subdir, someTimepoint );
			if ( pair == null )
				return null;
			return new ValuePair< String, Integer >( "%1$d/" + pair.getA(), pair.getB() );
		}

		String zeros = "";
		for ( int digits = 1; digits < 5; ++digits )
		{
			zeros = zeros + "0";
			final String suffixFusion = "_" + zeros + ".tif";
			final String suffixDeconvolution = "_z" + zeros + ".tif";
			final File[] files = dir.listFiles( new FilenameFilter()
			{
				@Override
				public boolean accept( final File dir, final String name )
				{
					return name.endsWith( suffixFusion ) || name.endsWith( suffixDeconvolution );
				}
			} );
			if ( files != null && files.length > 0 )
			{
				final String name = files[ 0 ].getName();
				int tStart = name.indexOf( "_t" ) + 2;
				if ( name.substring( tStart, tStart + 1 ).equals("l") )
					tStart++;
				final int tEnd = name.indexOf( "_", tStart );
				final int cStart = name.indexOf( "_ch", tEnd ) + 3;
				final int cEnd = name.indexOf( "_", cStart );
				final String pattern = name.substring( 0, tStart ) + "%1$d" + name.substring( tEnd, cStart ) + "%2$d" + name.substring( cEnd, name.length() - 4 - digits ) + "%3$0" + digits + "d.tif";
				IOFunctions.println( "detected pattern = " + pattern );

				final String prefix = name.substring( 0, name.length() - 4 - digits );
				final File[] files2 = dir.listFiles( new FilenameFilter()
				{
					@Override
					public boolean accept( final File dir, final String name )
					{
						return name.startsWith( prefix );
					}
				} );
				final int numSlices = files2.length;
				IOFunctions.println( "detected numSlices = " + numSlices );

				return new ValuePair< String, Integer >( pattern, new Integer( numSlices ) );
			}
		}
		return null;
	}

	protected static double loadZStretching( final String file )
	{
		final BufferedReader in = TextFileAccess.openFileRead( file );
		double z = -1;
		try
		{
			while ( in.ready() )
			{
				final String line = in.readLine();

				if ( line.contains( "z-scaling:") )
					z = Double.parseDouble( line.substring( line.indexOf( "ing" ) + 4, line.length() ).trim() );
			}
		}
		catch (final IOException e)
		{
		}

		return z;
	}

	public static void main(final String[] args)
	{
		new ExportSpimFusionPlugIn().run( null );
	}
}
