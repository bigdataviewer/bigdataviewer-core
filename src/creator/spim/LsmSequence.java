package creator.spim;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewSetup;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.BeadRegistration;
import net.imglib2.realtransform.AffineTransform3D;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LsmSequence
{
	final String timepoints;

	final String angles;

	final int referenceTimePoint;

	final SPIMConfiguration conf;

	final ArrayList< ViewSetup > setups;

	final ArrayList< ViewRegistration > regs;


	// for standard lsm / tif sequences the following parameters are required

	final String inputFilePattern = "spim_TL{tt}_Angle{aaa}.lsm";

	final String inputDirectory = "/Users/tobias/workspace/data/parhyale/";

	public LsmSequence( final String angles, final String timepoints, final int referenceTimePoint )
	{
		this.angles = angles;
		this.timepoints = timepoints;
		this.referenceTimePoint = referenceTimePoint;
		this.conf = new SPIMConfiguration();
		this.setups = new ArrayList< ViewSetup >();
		this.regs = new ArrayList< ViewRegistration >();

		initExperimentConfiguration();
		createViewSetups();
		createViewRegistrations();
	}

	/**
	 * Instantiate the SPIM configuration only with the necessary parameters
	 */
	protected void initExperimentConfiguration()
	{
		conf.timepointPattern = timepoints;
		conf.channelPattern = "";
		conf.channelsToRegister = "";
		conf.channelsToFuse = "";
		conf.anglePattern = angles;

		conf.inputdirectory = inputDirectory;
		conf.inputFilePattern = inputFilePattern;

		if ( referenceTimePoint >= 0 )
			conf.timeLapseRegistration = true;
		conf.referenceTimePoint = referenceTimePoint;

		// check the directory string
		conf.inputdirectory = conf.inputdirectory.replace( '\\', '/' );
		conf.inputdirectory = conf.inputdirectory.replaceAll( "//", "/" );

		conf.outputdirectory = conf.inputdirectory + "output/";
		conf.registrationFiledirectory = conf.inputdirectory + "registration/";

		// TODO: remove?
//		conf.numberOfThreads = Runtime.getRuntime().availableProcessors();
//		conf.scaleSpaceNumberOfThreads = Runtime.getRuntime().availableProcessors();

		// TODO:
//		conf.zStretching = conf.getZStretchingHuisken();

		// TODO: this should not be required:
		conf.overrideImageZStretching = true;
		conf.zStretching = 5.46448087431694;

		try
		{
			conf.getFileNames();
		}
		catch ( final ConfigurationParserException e )
		{
			System.err.println( "Cannot parse input: " + e );
		}
	}

	private void createViewSetups()
	{
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
	}

	/**
	 * Instantiates the {@link View} object from a typical SPIM configuration.
	 * It does not contain {@link Candidate}s yet, but all the other variables
	 * are set properly.
	 *
	 * @param conf
	 *            the SPIM configuration object
	 */
	protected void createViewRegistrations()
	{
		regs.clear();

		// for each time-point initialize the view structure, load&apply
		// registrations, instantiate the View objects for Tracking
		for ( int i = 0; i < conf.timepoints.length; ++i )
		{
			System.out.println( "(" + new Date( System.currentTimeMillis() ) + "): Loading timepoint " + conf.timepoints[ i ] );

			final ViewStructure viewStructure = ViewStructure.initViewStructure( conf, i, new mpicbg.models.AffineModel3D(), "ViewStructure Timepoint " + conf.timepoints[ i ], conf.debugLevelInt );

			for ( final ViewDataBeads viewDataBeads : viewStructure.getViews() )
			{
				// load time-point registration (to map into the global
				// coordinate system)
				viewDataBeads.loadRegistrationTimePoint( this.referenceTimePoint );

				// apply the z-scaling to the transformation
				BeadRegistration.concatenateAxialScaling( viewDataBeads, viewStructure.getDebugLevel() );

				final int angle = viewDataBeads.getAcqusitionAngle();
				final int illumination = viewDataBeads.getIllumination();
				final int channel = viewDataBeads.getChannel();
				final AffineTransform3D model = new AffineTransform3D();
				final double[] tmp = new double[ 12 ];
				( ( mpicbg.models.AffineModel3D ) viewDataBeads.getTile().getModel() ).toArray( tmp );
				model.set( tmp );

				System.out.println( "creating view: timepoint = " + conf.timepoints[ i ] + " | angle = " + angle + " | illumination = " + illumination );

				// get corresponding setup id
				final int setup = getSetupIndex( angle, illumination, channel );

				// create ViewRegistration
				regs.add( new ViewRegistration( i, setup, model ) );
			}
		}
	}

	/**
	 * find ViewSetup index corresponding to given (angle, illumination,
	 * channel) triple.
	 *
	 * @return setup index or -1 if no corresponding setup was found.
	 */
	protected int getSetupIndex( final int angle, final int illumination, final int channel )
	{
		for ( final ViewSetup s : setups )
			if ( s.getAngle() == angle && s.getIllumination() == illumination && s.getChannel() == channel )
				return s.getId();
		return -1;
	}

	protected void writeSequenceDescription( final String xmlFilename ) throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
	{
		System.out.println( "writing sequence description to " + xmlFilename );

		final Document doc = XmlHelpers.newXmlDocument();

		// add root element: SequenceDescription
		final Element sequence = doc.createElement( "SequenceDescription" );
		doc.appendChild( sequence );

		// add BasePath
		sequence.appendChild( XmlHelpers.textElement( doc, "BasePath", new File( conf.inputdirectory ).getAbsolutePath() ) );

		// add ImageLoader
		final Element imageLoader = doc.createElement( "ImageLoader" );
//		imageLoader.setAttribute( "class", HuiskenImageLoader.class.getCanonicalName() );
//		final Element sourceXmlPath = doc.createElement( "path" );
//		sourceXmlPath.appendChild( doc.createTextNode( expFile.getAbsolutePath() ) );
//		imageLoader.appendChild( sourceXmlPath );
		sequence.appendChild( imageLoader );

		// add ViewSetups
		for ( final ViewSetup setup : setups )
			sequence.appendChild( setup.toXml( doc ) );

		addTimepoints( doc, sequence );

		XmlHelpers.writeXmlDocument( doc, xmlFilename );
	}

	/**
	 * TODO: Add support for non-contiguous range of time-points. (Timepoints
	 * type="list")
	 *
	 * @param doc
	 * @param sequence
	 */
	private void addTimepoints( final Document doc, final Element sequence )
	{
		final Element tp = doc.createElement( "Timepoints" );
		tp.setAttribute( "type", "range" );
		tp.appendChild( XmlHelpers.intElement( doc, "first", conf.timepoints[ 0 ] ) );
		tp.appendChild( XmlHelpers.intElement( doc, "last", conf.timepoints[ conf.timepoints.length - 1 ] ) );
		sequence.appendChild( tp );
	}

	protected void writeViewRegistrations( final String xmlFilename, final String sequenceDescriptionName ) throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
	{
		System.out.println( "writing registrations to " + xmlFilename );

		final Document doc = XmlHelpers.newXmlDocument();

		// add root element: ViewRegistrations
		final Element registrations = doc.createElement( "ViewRegistrations" );
		doc.appendChild( registrations );

		// add sequence name
		registrations.appendChild( XmlHelpers.textElement( doc, "SequenceDescriptionName", sequenceDescriptionName ) );

		// add reference timepoint
		registrations.appendChild( XmlHelpers.intElement( doc, "ReferenceTimepoint", referenceTimePoint ) );

		// add ViewSetups
		for ( final ViewRegistration reg : regs )
			registrations.appendChild( reg.toXml( doc ) );

		XmlHelpers.writeXmlDocument( doc, xmlFilename );
	}

	public static void main( final String[] args )
	{
		final String angles = "150,190,230";
		final String timepoints = "71-90";
		final int referenceTimePoint = 269;

		final String sequenceDescriptionFilename = "/Users/tobias/workspace/data/parhyale/desc.xml";
		final String viewRegistrationsFilename = "/Users/tobias/workspace/data/parhyale/reg.xml";

		final LsmSequence lsmseq = new LsmSequence( angles, timepoints, referenceTimePoint );

		try
		{
			lsmseq.writeSequenceDescription( sequenceDescriptionFilename );
			lsmseq.writeViewRegistrations( viewRegistrationsFilename, sequenceDescriptionFilename );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}

