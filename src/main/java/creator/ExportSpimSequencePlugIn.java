package creator;

import fiji.plugin.Bead_Registration;
import fiji.plugin.Multi_View_Fusion;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.TextField;
import java.awt.event.TextEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.io.TextFileAccess;
import mpicbg.spim.registration.ViewStructure;
import spimopener.SPIMExperiment;
import viewer.hdf5.Hdf5ImageLoader;
import creator.spim.SpimRegistrationSequence;

public class ExportSpimSequencePlugIn implements PlugIn
{
	@Override
	public void run( final String arg0 )
	{
		final Parameters params = getParameters();

		// cancelled
		if ( params == null )
			return;

		IJ.log( "starting export..." );
		final SpimRegistrationSequence sequence = new SpimRegistrationSequence( params.conf );
		final SequenceDescription desc = sequence.getSequenceDescription();
		WriteSequenceToHdf5.writeHdf5File( desc, params.perSetupResolutions, params.perSetupSubdivisions, params.hdf5File, new PluginHelper.ProgressListenerIJ( 0, 0.95 ) );

		final Hdf5ImageLoader loader = new Hdf5ImageLoader( params.hdf5File, null, false );
		final SequenceDescription sequenceDescription = new SequenceDescription( desc.setups, desc.timepoints, params.seqFile.getParentFile(), loader );
		final ViewRegistrations viewRegistrations = sequence.getViewRegistrations();
		try
		{
			WriteSequenceToXml.writeSequenceToXml( sequenceDescription, viewRegistrations, params.seqFile.getAbsolutePath() );
			IJ.showProgress( 1 );
		}
		catch ( final Exception e )
		{
			IJ.error( "Failed to write xml file " + params.seqFile );
			e.printStackTrace();
		}
		IJ.log( "done" );
	}

	public static String fusionType[] = new String[] { "Single-channel", "Multi-channel" };
	public static String allChannels = "0, 1";

	protected static class Parameters
	{
		final SPIMConfiguration conf;
		final ArrayList< int[][] > perSetupResolutions;
		final ArrayList< int[][] > perSetupSubdivisions;
		final File seqFile;
		final File hdf5File;

		public Parameters( final SPIMConfiguration conf, final ArrayList< int[][] > perSetupResolutions, final ArrayList< int[][] > perSetupSubdivisions, final File seqFile, final File hdf5File )
		{
			this.conf = conf;
			this.perSetupResolutions = perSetupResolutions;
			this.perSetupSubdivisions = perSetupSubdivisions;
			this.seqFile = seqFile;
			this.hdf5File = hdf5File;
		}
	}


	protected Parameters getParameters()
	{
		final GenericDialog gd0 = new GenericDialogPlus( "SpimViewer Import" );
		gd0.addChoice( "Select_channel type", fusionType, fusionType[ Multi_View_Fusion.defaultFusionType ] );
		gd0.showDialog();
		if ( gd0.wasCanceled() )
			return null;
		final int channelChoice = gd0.getNextChoiceIndex();
		Multi_View_Fusion.defaultFusionType = channelChoice;
		final boolean multichannel = channelChoice == 1;

		final GenericDialogPlus gd = new GenericDialogPlus( "SpimViewer Import" );

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
		if ( ! init( conf ) )
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

		final GenericDialogPlus gd2 = new GenericDialogPlus( "SpimViewer Import" );

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

		gd2.addMessage("");

		final String defaultMipmapResolutions = "{1,1,1}, {2,2,1}, {4,4,2}";
		final String defaultCellSizes = "{64,64,16}, {32,32,16}, {8,8,8}";

		gd2.addMessage( "Mip-map definition:" );
		gd2.addStringField( "Subsampling factors", defaultMipmapResolutions, 25 );
		gd2.addStringField( "Hdf5 chunk sizes", defaultCellSizes, 25 );

		final ViewStructure viewStructure = ViewStructure.initViewStructure( conf, 0, new mpicbg.models.AffineModel3D(), "ViewStructure Timepoint " + conf.timepoints[ 0 ], conf.debugLevelInt );

		gd2.addMessage( "" );
		PluginHelper.addSaveAsFileField( gd2, "Export path", conf.inputdirectory + "export.xml", 25 );

//		gd.addMessage("");
//		gd.addMessage("This Plugin is developed by Tobias Pietzsch (pietzsch@mpi-cbg.de)\n");
//		Bead_Registration.addHyperLinkListener( (MultiLineLabel) gd.getMessage(), "mailto:pietzsch@mpi-cbg.de");

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

		// parse mipmap resolutions and cell sizes
		final String subsampling = gd2.getNextString();
		final String chunksizes = gd2.getNextString();
		final int[][] resolutions = PluginHelper.parseResolutionsString( subsampling );
		final int[][] subdivisions = PluginHelper.parseResolutionsString( chunksizes );
		if ( resolutions.length == 0 )
		{
			IOFunctions.println( "Cannot parse subsampling factors " + subsampling );
			return null;
		}
		if ( subdivisions.length == 0 )
		{
			IOFunctions.println( "Cannot parse hdf5 chunk sizes " + chunksizes );
			return null;
		}
		else if ( resolutions.length != subdivisions.length )
		{
			IOFunctions.println( "subsampling factors and hdf5 chunk sizes must have the same number of elements" );
			return null;
		}
		final ArrayList< int[][] > perSetupResolutions = new ArrayList< int[][] >();
		final ArrayList< int[][] > perSetupSubdivisions = new ArrayList< int[][] >();
		for ( int i = 0; i < viewStructure.getViews().size(); ++i )
		{
			perSetupResolutions.add( resolutions );
			perSetupSubdivisions.add( subdivisions );
		}

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

		return new Parameters( conf, perSetupResolutions, perSetupSubdivisions, seqFile, hdf5File );
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

	protected static boolean init( final SPIMConfiguration conf )
	{
		// check the directory string
		conf.inputdirectory = conf.inputdirectory.replace('\\', '/');
		conf.inputdirectory = conf.inputdirectory.replaceAll( "//", "/" );

		conf.inputdirectory = conf.inputdirectory.trim();
		if (conf.inputdirectory.length() > 0 && !conf.inputdirectory.endsWith("/"))
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


	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(final String[] args)
	{
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		final Class<?> clazz = ExportSpimSequencePlugIn.class;
		final String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		final String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
