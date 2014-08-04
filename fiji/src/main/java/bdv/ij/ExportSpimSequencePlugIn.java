package bdv.ij;

import fiji.plugin.Bead_Registration;
import fiji.plugin.Multi_View_Fusion;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.io.TextFileAccess;
import spimopener.SPIMExperiment;
import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5;
import bdv.ij.export.SpimRegistrationSequence;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

public class ExportSpimSequencePlugIn implements PlugIn
{
	@Override
	public void run( final String arg0 )
	{
		final Parameters params = getParameters();

		// cancelled
		if ( params == null )
			return;

		final ProgressWriter progress = new ProgressWriterIJ();
		progress.out().println( "starting export..." );
		final SpimRegistrationSequence sequence = new SpimRegistrationSequence( params.conf );
		final SequenceDescriptionMinimal desc = sequence.getSequenceDescription();

		Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo;
		if ( params.setMipmapManual )
		{
			perSetupExportMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
			final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo( params.resolutions, params.subdivisions );
			for ( final BasicViewSetup setup : desc.getViewSetupsOrdered() )
				perSetupExportMipmapInfo.put( setup.getId(), mipmapInfo );
		}
		else
		{
			perSetupExportMipmapInfo = ProposeMipmaps.proposeMipmaps( desc );
		}

		final ArrayList< Partition > partitions;
		if ( params.split )
		{
			final String xmlFilename = params.seqFile.getAbsolutePath();
			final String basename = xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename;
			final List< TimePoint > timepoints = desc.getTimePoints().getTimePointsOrdered();
			final List< BasicViewSetup > setups = desc.getViewSetupsOrdered();
			partitions = Partition.split( timepoints, setups, params.timepointsPerPartition, params.setupsPerPartition, basename );

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
			partitions = null;
			WriteSequenceToHdf5.writeHdf5File( desc, perSetupExportMipmapInfo, params.deflate, params.hdf5File, new SubTaskProgressWriter( progress, 0, 0.95 ) );
		}

		final Hdf5ImageLoader loader = new Hdf5ImageLoader( params.hdf5File, partitions, null, false );
		final SequenceDescriptionMinimal sequenceDescription = new SequenceDescriptionMinimal( desc, loader );

		final File basePath = params.seqFile.getParentFile();
		final SpimDataMinimal spimData = new SpimDataMinimal( basePath, sequenceDescription, sequence.getViewRegistrations() );
		try
		{
			new XmlIoSpimDataMinimal().save( spimData, params.seqFile.getAbsolutePath() );
			progress.setProgress( 1.0 );
		}
		catch ( final Exception e )
		{
			progress.err().println( "Failed to write xml file " + params.seqFile );
			e.printStackTrace( progress.err() );
		}
		progress.out().println( "done" );
	}

	static boolean lastSetMipmapManual = false;

	static String lastSubsampling = "{1,1,1}, {2,2,1}, {4,4,2}";

	static String lastChunkSizes = "{16,16,16}, {16,16,16}, {16,16,16}";

	static boolean lastSplit = false;

	static int lastTimepointsPerPartition = 0;

	static int lastSetupsPerPartition = 0;

	static boolean lastDeflate = true;

	public static String fusionType[] = new String[] { "Single-channel", "Multi-channel" };

	public static String allChannels = "0, 1";

	protected static class Parameters
	{
		final SPIMConfiguration conf;

		final boolean setMipmapManual;

		final int[][] resolutions;

		final int[][] subdivisions;

		final File seqFile;

		final File hdf5File;

		final boolean deflate;

		final boolean split;

		final int timepointsPerPartition;

		final int setupsPerPartition;

		public Parameters(
				final SPIMConfiguration conf,
				final boolean setMipmapManual, final int[][] resolutions, final int[][] subdivisions,
				final File seqFile, final File hdf5File, final boolean deflate,
				final boolean split, final int timepointsPerPartition, final int setupsPerPartition )
		{
			this.conf = conf;
			this.setMipmapManual = setMipmapManual;
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.seqFile = seqFile;
			this.hdf5File = hdf5File;
			this.deflate = deflate;
			this.split = split;
			this.timepointsPerPartition = timepointsPerPartition;
			this.setupsPerPartition = setupsPerPartition;
		}
	}

	protected Parameters getParameters()
	{
		final GenericDialog gd0 = new GenericDialogPlus( "Export for BigDataViewer" );
		gd0.addChoice( "Select_channel type", fusionType, fusionType[ Multi_View_Fusion.defaultFusionType ] );
		gd0.showDialog();
		if ( gd0.wasCanceled() )
			return null;
		final int channelChoice = gd0.getNextChoiceIndex();
		Multi_View_Fusion.defaultFusionType = channelChoice;
		final boolean multichannel = channelChoice == 1;

		final GenericDialogPlus gd = new GenericDialogPlus( "Export for BigDataViewer" );

		gd.addDirectoryOrFileField( "SPIM_data_directory", Bead_Registration.spimDataDirectory );
		final TextField tfSpimDataDirectory = ( TextField ) gd.getStringFields().lastElement();
		gd.addStringField( "Pattern_of_SPIM files", Bead_Registration.fileNamePattern, 25 );
		final TextField tfFilePattern = ( TextField ) gd.getStringFields().lastElement();
		gd.addStringField( "Timepoints_to_process", Bead_Registration.timepoints );
		final TextField tfTimepoints = ( TextField ) gd.getStringFields().lastElement();
		gd.addStringField( "Angles to process", Bead_Registration.angles );
		final TextField tfAngles = ( TextField ) gd.getStringFields().lastElement();

		final TextField tfChannels;
		if ( multichannel )
		{
			gd.addStringField( "Channels to process", allChannels );
			tfChannels = ( TextField ) gd.getStringFields().lastElement();
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
		ArrayList< Integer > channels;

		// verify this part
		if ( multichannel )
		{
			allChannels = gd.getNextString();

			try
			{
				channels = SPIMConfiguration.parseIntegerString( allChannels );
				numChannels = channels.size();
			}
			catch ( final ConfigurationParserException e )
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
			channels = new ArrayList< Integer >();
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

		conf.fuseOnly = true; // this is to avoid an exception in the
								// multi-channel case

		// get filenames and so on...
		if ( !init( conf ) )
			return null;

		// test which registration files are there for each channel
		// file = new File[ timepoints.length ][ channels.length ][
		// angles.length ];
		final ArrayList< ArrayList< Integer >> timepoints = new ArrayList< ArrayList< Integer >>();
		int numChoices = 0;
		conf.zStretching = -1;

		for ( int c = 0; c < channels.size(); ++c )
		{
			timepoints.add( new ArrayList< Integer >() );

			final String name = conf.file[ 0 ][ c ][ 0 ][ 0 ].getName();
			final File regDir = new File( conf.registrationFiledirectory );

			IOFunctions.println( "name: " + name );
			IOFunctions.println( "dir: " + regDir.getAbsolutePath() );

			if ( !regDir.isDirectory() )
			{
				IOFunctions.println( conf.registrationFiledirectory + " is not a directory. " );
				return null;
			}

			final String entries[] = regDir.list( new FilenameFilter()
			{
				@Override
				public boolean accept( final File directory, final String filename )
				{
					if ( filename.contains( name ) && filename.contains( ".registration" ) )
						return true;
					else
						return false;
				}
			} );

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
			final ArrayList< Integer > tps = timepoints.get( c );

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
			gd2.addChoice( "Registration for channel " + channels.get( c ), choices, choices[ suggest[ c ] ] );

		gd2.addMessage( "" );

		final SequenceDescriptionMinimal desc = new SpimRegistrationSequence( conf ).getSequenceDescription();
		final Map< Integer, ExportMipmapInfo > proposedPerSetupExportMipmapInfo = ProposeMipmaps.proposeMipmaps( desc );
		final ExportMipmapInfo autoMipmapSettings = proposedPerSetupExportMipmapInfo.get( desc.getViewSetupsOrdered().get( 0 ).getId() );

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

		final String autoSubsampling = ProposeMipmaps.getArrayString( autoMipmapSettings.getExportResolutions() );
		final String autoChunkSizes = ProposeMipmaps.getArrayString( autoMipmapSettings.getSubdivisions() );
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

//		gd.addMessage("");
//		gd.addMessage("This Plugin is developed by Tobias Pietzsch (pietzsch@mpi-cbg.de)\n");
//		Bead_Registration.addHyperLinkListener( (MultiLineLabel) gd.getMessage(), "mailto:pietzsch@mpi-cbg.de");

		gd2.showDialog();

		if ( gd2.wasCanceled() )
			return null;

		// which channel uses which registration file from which channel to be
		// fused
		final int[][] registrationAssignment = new int[ channels.size() ][ 2 ];

		for ( int c = 0; c < channels.size(); ++c )
		{
			final int choice = gd2.getNextChoiceIndex();

			index = 0;
			for ( int c2 = 0; c2 < channels.size(); ++c2 )
			{
				final ArrayList< Integer > tps = timepoints.get( c2 );
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
				IOFunctions.println( "Inconsistent choice of reference timeseries, only same reference timepoints or individual registration are allowed." );
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
		if ( !seqFilename.endsWith( ".xml" ) )
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

		return new Parameters( conf, lastSetMipmapManual, resolutions, subdivisions, seqFile, hdf5File, lastDeflate, lastSplit, lastTimepointsPerPartition, lastSetupsPerPartition );
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

				if ( line.contains( "z-scaling:" ) )
					z = Double.parseDouble( line.substring( line.indexOf( "ing" ) + 4, line.length() ).trim() );
			}
		}
		catch ( final IOException e )
		{}

		return z;
	}

	protected static boolean init( final SPIMConfiguration conf )
	{
		// check the directory string
		conf.inputdirectory = conf.inputdirectory.replace( '\\', '/' );
		conf.inputdirectory = conf.inputdirectory.replaceAll( "//", "/" );

		conf.inputdirectory = conf.inputdirectory.trim();
		if ( conf.inputdirectory.length() > 0 && !conf.inputdirectory.endsWith( "/" ) )
			conf.inputdirectory = conf.inputdirectory + "/";

		conf.outputdirectory = conf.inputdirectory + "output/";
		conf.registrationFiledirectory = conf.inputdirectory + "registration/";

		try
		{
			if ( conf.isHuiskenFormat() )
				conf.getFilenamesHuisken();
			else
				conf.getFileNames();
		}
		catch ( final ConfigurationParserException e )
		{
			IJ.error( "Cannot parse input: " + e );
			return false;
		}

		// TODO: remove?
		// set interpolator stuff
		conf.interpolatorFactorOutput.setOutOfBoundsStrategyFactory( conf.strategyFactoryOutput );

		return true;
	}

	public static void main( final String[] args )
	{
		new ExportSpimSequencePlugIn().run( null );
	}
}
