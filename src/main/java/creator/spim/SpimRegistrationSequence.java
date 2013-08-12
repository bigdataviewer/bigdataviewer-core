package creator.spim;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.vecmath.Point3f;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import mpicbg.spim.fusion.SPIMImageFusion;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.BeadRegistration;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import spimopener.SPIMExperiment;
import creator.spim.imgloader.HuiskenImageLoader;
import creator.spim.imgloader.StackImageLoader;

public class SpimRegistrationSequence
{
	private final SequenceDescription sequenceDescription;

	private final ViewRegistrations viewRegistrations;

	private final SPIMConfiguration conf;

	public SpimRegistrationSequence( final SPIMConfiguration conf )
	{
		this.conf = conf;
		final ArrayList< ViewSetup > setups = createViewSetups( conf );
		final ImgLoader imgLoader = createImageLoader( conf, setups );

		viewRegistrations = createViewRegistrations( conf, setups );
		sequenceDescription = new SequenceDescription( setups, makeList( conf.timepoints ), new File( conf.inputdirectory ), imgLoader );
	}

	public SpimRegistrationSequence( final String huiskenExperimentXmlFile, final String channels, final String angles, final String timepoints, final int referenceTimePoint ) throws ConfigurationParserException
	{
		this( initExperimentConfiguration( huiskenExperimentXmlFile, "", channels, angles, timepoints, referenceTimePoint, false, 0 ) );
	}

	public SpimRegistrationSequence( final String inputDirectory, final String inputFilePattern, final String channels, final String angles, final String timepoints, final int referenceTimePoint, final boolean overrideImageZStretching, final double zStretching ) throws ConfigurationParserException
	{
		this( initExperimentConfiguration( inputDirectory, inputFilePattern, channels, angles, timepoints, referenceTimePoint, overrideImageZStretching, zStretching ) );
	}

	public SequenceDescription getSequenceDescription()
	{
		return sequenceDescription;
	}

	public ViewRegistrations getViewRegistrations()
	{
		return viewRegistrations;
	}

	protected static ImgLoader createImageLoader( final SPIMConfiguration conf, final ArrayList< ViewSetup > setups )
	{
		final int numTimepoints = conf.timepoints.length;
		final int numSetups = setups.size();
		final String[] filenames = new String[ numTimepoints * numSetups ];
		for ( int timepoint = 0; timepoint < numTimepoints; ++timepoint )
		{
			System.out.println( "(" + new Date( System.currentTimeMillis() ) + "): Loading timepoint " + conf.timepoints[ timepoint ] );

			final ViewStructure viewStructure = ViewStructure.initViewStructure( conf, timepoint, new mpicbg.models.AffineModel3D(), "ViewStructure Timepoint " + conf.timepoints[ timepoint ], conf.debugLevelInt );

			for ( final ViewDataBeads viewDataBeads : viewStructure.getViews() )
			{
				// get setup id
				final int angle = viewDataBeads.getAcqusitionAngle();
				final int illumination = viewDataBeads.getIllumination();
				final int channel = viewDataBeads.getChannel();
				final int setup = getSetupIndex( setups, angle, illumination, channel );
				filenames[ timepoint * numSetups + setup ] = viewDataBeads.getFileName();
			}
		}
		if ( conf.isHuiskenFormat() )
		{
			final String exp = conf.inputdirectory.endsWith( "/" ) ? conf.inputdirectory.substring( 0, conf.inputdirectory.length() - 1 ) : conf.inputdirectory;
			return new HuiskenImageLoader( new File( exp + ".xml" ) );
		}
		else
		{
			final boolean useImageJOpener = conf.inputFilePattern.endsWith( ".tif" );
			return new StackImageLoader( Arrays.asList( filenames ), numSetups, useImageJOpener );
		}
	}

	/**
	 * Instantiate the SPIM configuration only with the necessary parameters
	 * @return
	 */
	protected static SPIMConfiguration initExperimentConfiguration( final String inputDirectory, final String inputFilePattern, final String angles, final String timepoints, final int referenceTimePoint, final boolean overrideImageZStretching, final double zStretching ) throws ConfigurationParserException
	{
		return initExperimentConfiguration( inputDirectory, inputFilePattern, "", angles, timepoints, referenceTimePoint, overrideImageZStretching, zStretching );
	}

	/**
	 * Instantiate the SPIM configuration only with the necessary parameters
	 * @return
	 */
	protected static SPIMConfiguration initExperimentConfiguration( final String inputDirectory, final String inputFilePattern, final String channels, final String angles, final String timepoints, final int referenceTimePoint, final boolean overrideImageZStretching, final double zStretching ) throws ConfigurationParserException
	{
		final SPIMConfiguration conf = new SPIMConfiguration();
		conf.timepointPattern = timepoints;
		conf.channelPattern = conf.channelsToRegister = conf.channelsToFuse = (channels == null) ? "" : channels;
		conf.anglePattern = angles;

		final File f = new File( inputDirectory );
		if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
		{
			conf.spimExperiment = new SPIMExperiment( f.getAbsolutePath() );
			conf.inputdirectory = f.getAbsolutePath().substring( 0, f.getAbsolutePath().length() - 4 ) + "/";
		}
		else
		{
			conf.inputdirectory = inputDirectory;
		}

		conf.inputFilePattern = inputFilePattern;

		if ( referenceTimePoint >= 0 )
			conf.timeLapseRegistration = true;
		conf.referenceTimePoint = referenceTimePoint;

		// check the directory string
		conf.inputdirectory = conf.inputdirectory.replace( '\\', '/' );
		conf.inputdirectory = conf.inputdirectory.replaceAll( "//", "/" );
		conf.inputdirectory = conf.inputdirectory.trim();
		if (conf.inputdirectory.length() > 0 && !conf.inputdirectory.endsWith("/"))
			conf.inputdirectory = conf.inputdirectory + "/";

		conf.outputdirectory = conf.inputdirectory + "output/";
		conf.registrationFiledirectory = conf.inputdirectory + "registration/";

		conf.overrideImageZStretching = overrideImageZStretching;
		conf.zStretching = zStretching;

		conf.fuseOnly= true; // this is to avoid an exception in the multi-channel case

		if ( conf.isHuiskenFormat() )
			conf.getFilenamesHuisken();
		else
			conf.getFileNames();

		return conf;
	}

	protected static ArrayList< ViewSetup > createViewSetups( final SPIMConfiguration conf )
	{
		final ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >();
		int setup_id = 0;
		for ( int channelIndex = 0; channelIndex < conf.file[ 0 ].length; channelIndex++ )
			for ( int angleIndex = 0; angleIndex < conf.file[ 0 ][ channelIndex ].length; angleIndex++ )
				for ( int illuminationIndex = 0; illuminationIndex < conf.file[ 0 ][ channelIndex ][ angleIndex ].length; ++illuminationIndex )
				{
					// ... TODO ...
					final int width = 0;
					final int height = 0;
					final int depth = 0;
					final double pixelWidth = 0;
					final double pixelHeight = 0;
					final double pixelDepth = 0;
					setups.add( new ViewSetup( setup_id++, conf.angles[ angleIndex ], conf.illuminations[ illuminationIndex ], conf.channels[ channelIndex ], width, height, depth, pixelWidth, pixelHeight, pixelDepth ) );
				}
		return setups;
	}

	public List< AffineTransform3D > getFusionTransforms( final int cropOffsetX, final int cropOffsetY, final int cropOffsetZ, final int scale )
	{
		conf.cropOffsetX = cropOffsetX;
		conf.cropOffsetY = cropOffsetY;
		conf.cropOffsetZ = cropOffsetZ;
		conf.scale = scale;

		final ArrayList< AffineTransform3D > transforms = new ArrayList< AffineTransform3D >();
		if ( conf.timeLapseRegistration )
		{
			SPIMConfiguration refconf = conf;
			if ( conf.getTimePointIndex( conf.referenceTimePoint ) < 0 )
			{
				try
				{
					final String inputdirectory;
					if ( conf.isHuiskenFormat() )
						inputdirectory = conf.inputdirectory.substring( 0, conf.inputdirectory.length() - 1 ) + ".xml";
					else
						inputdirectory = conf.inputdirectory;
					refconf = initExperimentConfiguration( inputdirectory, conf.inputFilePattern, conf.anglePattern, "" + conf.referenceTimePoint, conf.referenceTimePoint, conf.overrideImageZStretching, conf.zStretching );
					refconf.cropOffsetX = cropOffsetX;
					refconf.cropOffsetY = cropOffsetY;
					refconf.cropOffsetZ = cropOffsetZ;
					refconf.scale = scale;
				}
				catch ( final ConfigurationParserException e )
				{
					e.printStackTrace();
				}
			}
			final RealInterval interval = getFusionBoundingBox( refconf, 0 );
			final double tx = interval.realMin( 0 );
			final double ty = interval.realMin( 1 );
			final double tz = interval.realMin( 2 );
			final double s = scale;
			System.out.println( "tx = " + tx + " ty = " + ty + " tz = " + tz + " scale = " + scale );
			final AffineTransform3D transform = new AffineTransform3D();
			transform.set( s, 0, 0, tx, 0, s, 0, ty, 0, 0, s, tz );
			final int numTimepoints = sequenceDescription.numTimepoints();
			for ( int i = 0; i < numTimepoints; ++i )
				transforms.add( transform );
		}
		else
		{
			final int numTimepoints = sequenceDescription.numTimepoints();
			for ( int i = 0; i < numTimepoints; ++i )
			{
				final RealInterval interval = getFusionBoundingBox( conf, i );
				final double tx = interval.realMin( 0 );
				final double ty = interval.realMin( 1 );
				final double tz = interval.realMin( 2 );
				final double s = scale;
				System.out.println( "tx = " + tx + " ty = " + ty + " tz = " + tz + " scale = " + scale );
				final AffineTransform3D transform = new AffineTransform3D();
				transform.set( s, 0, 0, tx, 0, s, 0, ty, 0, 0, s, tz );
				transforms.add( transform );
			}
		}
		return transforms;
	}

	protected static RealInterval getFusionBoundingBox( final SPIMConfiguration conf, final int timepointIndex )
	{
			final Point3f min = new Point3f();
			final Point3f max = new Point3f();
			final Point3f size = new Point3f();

			final int tp = conf.timeLapseRegistration ? conf.getTimePointIndex( conf.referenceTimePoint ) : timepointIndex;

			@SuppressWarnings( "unchecked" )
			final ViewStructure reference = ViewStructure.initViewStructure( conf, tp, conf.getModel(), "Reference ViewStructure Timepoint " + conf.referenceTimePoint, conf.debugLevelInt );
			for ( final ViewDataBeads viewDataBeads : reference.getViews() )
			{
				// coordinate system)
				if ( conf.timeLapseRegistration )
					viewDataBeads.loadRegistrationTimePoint( conf.referenceTimePoint );
				else
					viewDataBeads.loadRegistration();

				// apply the z-scaling to the transformation
				BeadRegistration.concatenateAxialScaling( viewDataBeads, reference.getDebugLevel() );
			}
			SPIMImageFusion.computeImageSize( reference.getViews(), min, max, size, conf.scale, conf.cropSizeX, conf.cropSizeY, conf.cropSizeZ, reference.getDebugLevel() );

			final int scale = conf.scale;
			final int cropOffsetX = conf.cropOffsetX;
			final int cropOffsetY = conf.cropOffsetY;
			final int cropOffsetZ = conf.cropOffsetZ;
			final int imgW;
			final int imgH;
			final int imgD;

			if (conf.cropSizeX == 0)
				imgW = (Math.round((float)Math.ceil(size.x)) + 1)/scale;
			else
				imgW = conf.cropSizeX/scale;

			if (conf.cropSizeY == 0)
				imgH = (Math.round((float)Math.ceil(size.y)) + 1)/scale;
			else
				imgH = conf.cropSizeY/scale;

			if (conf.cropSizeZ == 0)
				imgD = (Math.round((float)Math.ceil(size.z)) + 1)/scale;
			else
				imgD = conf.cropSizeZ/scale;

			// TODO: this should be a RealInterval
			return FinalRealInterval.createMinSize( ( int ) min.x + cropOffsetX, (int ) min.y + cropOffsetY, ( int ) min.z + cropOffsetZ, imgW, imgH, imgD );
	}

	protected static ViewRegistrations createViewRegistrations( final SPIMConfiguration conf, final ArrayList< ViewSetup > setups )
	{
		final ArrayList< ViewRegistration > regs = new ArrayList< ViewRegistration >();

		// for each time-point initialize the view structure, load&apply
		// registrations, instantiate the View objects for Tracking
		for ( int i = 0; i < conf.timepoints.length; ++i )
		{
//			System.out.println( "(" + new Date( System.currentTimeMillis() ) + "): Loading timepoint " + conf.timepoints[ i ] );

			final ViewStructure viewStructure = ViewStructure.initViewStructure( conf, i, new mpicbg.models.AffineModel3D(), "ViewStructure Timepoint " + conf.timepoints[ i ], conf.debugLevelInt );

			for ( final ViewDataBeads viewDataBeads : viewStructure.getViews() )
			{
				// load time-point registration (to map into the global
				// coordinate system)
				if ( conf.timeLapseRegistration )
					viewDataBeads.loadRegistrationTimePoint( conf.referenceTimePoint );
				else
					viewDataBeads.loadRegistration();

				// apply the z-scaling to the transformation
				BeadRegistration.concatenateAxialScaling( viewDataBeads, viewStructure.getDebugLevel() );

				final int angle = viewDataBeads.getAcqusitionAngle();
				final int illumination = viewDataBeads.getIllumination();
				final int channel = viewDataBeads.getChannel();
				final AffineTransform3D model = new AffineTransform3D();
				final double[][] tmp = new double[3][4];
				( ( mpicbg.models.AffineModel3D ) viewDataBeads.getTile().getModel() ).toMatrix( tmp );
				model.set( tmp );

//				System.out.println( "creating view: timepoint = " + conf.timepoints[ i ] + " | angle = " + angle + " | illumination = " + illumination );

				// get corresponding setup id
				final int setup = getSetupIndex( setups, angle, illumination, channel );

				// create ViewRegistration
				regs.add( new ViewRegistration( i, setup, model ) );
			}
		}

		return new ViewRegistrations( regs, conf.referenceTimePoint );
	}

	protected static ArrayList< Integer > makeList( final int[] ints )
	{
		final ArrayList< Integer > list = new ArrayList< Integer >( ints.length );
		for ( final int i : ints )
			list.add( i );
		return list;
	}

	/**
	 * find ViewSetup index corresponding to given (angle, illumination,
	 * channel) triple.
	 *
	 * @return setup index or -1 if no corresponding setup was found.
	 */
	protected static int getSetupIndex( final ArrayList< ViewSetup > setups, final int angle, final int illumination, final int channel )
	{
		for ( final ViewSetup s : setups )
			if ( s.getAngle() == angle && s.getIllumination() == illumination && s.getChannel() == channel )
				return s.getId();
		return -1;
	}

	public SPIMConfiguration getConf()
	{
		return conf;
	}
}

