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
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import viewer.hdf5.Hdf5ImageLoader;
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

		public SetupCollector( final List< Integer > timepoints, final int referenceTimePoint )
		{
			this.timepoints = new ArrayList< Integer >( timepoints );
			this.referenceTimePoint = referenceTimePoint;
			registrations = new ArrayList< ViewRegistration >();
		}

		public void add( final SequenceDescription sourceSequence, final ViewSetup sourceSetup, final ViewRegistrations sourceRegs, final int[][] resolutions, final int[][] subdivisions )
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
						System.out.println( "timepointIdx = " + timepointIdx );
						System.out.println( "setupIdx = " + setupIdx );
						registrations.add( new ViewRegistration( timepointIdx, setupIdx, r.getModel() ) );
						break;
					}
				if ( !found )
					throw new RuntimeException( "could not find ViewRegistration for timepoint " + tp + " in the source sequence." );
			}
			perSetupResolutions.add( resolutions );
			perSetupSubdivisions.add( subdivisions );
		}

		public SequenceDescription createSequenceDescription( final File basePath )
		{
			return new SequenceDescription( setups, timepoints, basePath, imgLoader );
		}

		public ViewRegistrations createViewRegistrations()
		{
			return new ViewRegistrations( registrations, referenceTimePoint );
		}

		public ArrayList< int[][] > getPerSetupResolutions()
		{
			return perSetupResolutions;
		}

		public ArrayList< int[][] > getPerSetupSubdivisions()
		{
			return perSetupSubdivisions;
		}
	}


	public static void main( final String[] args ) throws ConfigurationParserException, TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException
	{
		final String huiskenExperimentXmlFile = "/Users/pietzsch/workspace/data/fast fly/111010_weber/e012.xml";
		final String angles = "0,120,240";
		final String timepoints = "1-3";
		final int referenceTimePoint = 50;

		final String filepath = "/Users/pietzsch/workspace/data/fast fly/111010_weber/e012/output/";
		final String filepattern = filepath + "img_tl%d_ch1_z%03d.tif";
		final int numSlices = 387;
		final double sliceValueMin = 0;
		final double sliceValueMax = 8000;

		final SpimRegistrationSequence spimseq = new SpimRegistrationSequence( huiskenExperimentXmlFile, angles, timepoints, referenceTimePoint );
		final SequenceDescription spimdesc = spimseq.getSequenceDescription();
		final ViewRegistrations spimregs = spimseq.getViewRegistrations();
		final int[][] spimresolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 2 } };
		final int[][] spimsubdivisions = { { 32, 32, 4 }, { 16, 16, 8 }, { 8, 8, 8 } };

		final ImgLoader fusionLoader = new FusionImageLoader< FloatType >( filepattern, numSlices, new FusionImageLoader.Gray32ImagePlusLoader(), sliceValueMin, sliceValueMax );
		final SequenceDescription fusiondesc = new SequenceDescription( Arrays.asList( new ViewSetup( 0, 0, 0, 0, 0, 0, numSlices, 1, 1, 1 ) ), spimdesc.timepoints, null, fusionLoader );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		final int cropOffsetX = 58;
		final int cropOffsetY = 74;
		final int cropOffsetZ = 6;
		final int scale = 1;
		final SpimRegistrationSequence spimseq2 = new SpimRegistrationSequence( huiskenExperimentXmlFile, angles, ""+referenceTimePoint, referenceTimePoint );
		final AffineTransform3D transform = spimseq2.getFusionTransform( cropOffsetX, cropOffsetY, cropOffsetZ, scale );
		for ( int timepoint = 0; timepoint < spimdesc.numTimepoints(); ++timepoint )
			registrations.add( new ViewRegistration( timepoint, 0, transform ) );
		final ViewRegistrations fusionregs = new ViewRegistrations( registrations, spimregs.referenceTimePoint );
		final int[][] fusionresolutions = { { 1, 1, 1 }, { 2, 2, 2 }, { 4, 4, 4 } };
		final int[][] fusionsubdivisions = { { 16, 16, 16 }, { 16, 16, 16 }, { 8, 8, 8 } };

		final SetupCollector collector = new SetupCollector( spimdesc.timepoints, spimregs.referenceTimePoint );
		for ( final ViewSetup setup : spimdesc.setups )
			collector.add( spimdesc, setup, spimregs, spimresolutions, spimsubdivisions );
		for ( final ViewSetup setup : fusiondesc.setups )
			collector.add( fusiondesc, setup, fusionregs, fusionresolutions, fusionsubdivisions );

		final File seqFile = new File( "/Users/pietzsch/Desktop/data/everything.xml" );
		final File hdf5File = new File( "/Users/pietzsch/Desktop/data/everything.h5" );

//		CreateCells.createHdf5File( collector.createSequenceDescription( null ), collector.getPerSetupResolutions(), collector.getPerSetupSubdivisions(), hdf5File, null );

		final Hdf5ImageLoader loader = new Hdf5ImageLoader( hdf5File, false );
		final SequenceDescription sequenceDescription = new SequenceDescription( collector.setups, collector.timepoints, seqFile.getParentFile(), loader );
		WriteSequenceToXml.writeSequenceToXml( sequenceDescription, collector.createViewRegistrations(), seqFile.getAbsolutePath() );
	}
}
