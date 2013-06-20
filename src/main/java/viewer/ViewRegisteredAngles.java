package viewer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.SequenceDescription;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.histogram.DiscreteFrequencyDistribution;
import net.imglib2.algorithm.histogram.Histogram1d;
import net.imglib2.algorithm.histogram.Real1dBinMapper;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;

import org.xml.sax.SAXException;

import viewer.NewBrightnessDialog.ConverterSetup;
import viewer.render.Source;
import viewer.render.SourceAndConverter;
import viewer.render.SourceState;
import viewer.render.ViewerState;

public class ViewRegisteredAngles implements BrightnessDialog.MinMaxListener
{
	final KeyStroke brightnessKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_S, 0 );

	final KeyStroke helpKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 );

	final SpimViewer viewer;

	final ArrayList< AbstractLinearRange > displayRanges;

	final BrightnessDialog brightnessDialog;

	public void toggleBrightnessDialog()
	{
		brightnessDialog.setVisible( ! brightnessDialog.isVisible() );
	}

	public void showHelp()
	{
		new HelpFrame();
	}

	@Override
	public void setMinMax( final int min, final int max )
	{
		for ( final AbstractLinearRange r : displayRanges )
		{
			r.setMin( min );
			r.setMax( max );
		}
		viewer.requestRepaint();
	}

	private ViewRegisteredAngles( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final int width = 800;
		final int height = 600;

		final SequenceViewsLoader loader = new SequenceViewsLoader( xmlFilename );
		final SequenceDescription seq = loader.getSequenceDescription();

		displayRanges = new ArrayList< AbstractLinearRange >();

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
		{
			final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 65535 );
			sources.add( new SourceAndConverter< UnsignedShortType >( new SpimSource( loader, setup, "angle " + seq.setups.get( setup ).getAngle() ), converter ) );
			displayRanges.add( converter );
			final int id = setup;
			converterSetups.add( new ConverterSetup()
			{
				@Override
				public void setDisplayRange( final int min, final int max )
				{
					converter.setMin( min );
					converter.setMax( max );
					viewer.requestRepaint();
				}

				@Override
				public int getSetupId()
				{
					return id;
				}

				@Override
				public int getDisplayRangeMin()
				{
					return ( int ) converter.getMin();
				}

				@Override
				public int getDisplayRangeMax()
				{
					return ( int ) converter.getMax();
				}
			} );
		}

		viewer = new SpimViewer( width, height, sources, seq.numTimepoints() );

		viewer.addKeyAction( brightnessKeystroke, new AbstractAction( "brightness settings" )
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				toggleBrightnessDialog();
			}

			private static final long serialVersionUID = 1L;
		} );

		viewer.addKeyAction( helpKeystroke, new AbstractAction( "help" )
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				showHelp();
			}

			private static final long serialVersionUID = 1L;
		} );

		brightnessDialog = new BrightnessDialog( viewer.frame );
		viewer.installKeyActions( brightnessDialog );
		brightnessDialog.setListener( this );

		final NewBrightnessDialog nbd = new NewBrightnessDialog( viewer.frame, converterSetups );
		nbd.setVisible( true );

		initTransform( width, height );
//		initBrightness( 0.001, 0.999 );
	}

	void initTransform( final int viewerWidth, final int viewerHeight )
	{
		final int cX = viewerWidth / 2;
		final int cY = viewerHeight / 2;

		final ViewerState state = viewer.getState();
		final SourceState< ? > source = state.getSources().get( state.getCurrentSource() );
		final int timepoint = state.getCurrentTimepoint();
		final AffineTransform3D sourceTransform = source.getSpimSource().getSourceTransform( timepoint, 0 );

		final Interval sourceInterval = source.getSpimSource().getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		final double sZ0 = sourceInterval.min( 2 );
		final double sZ1 = sourceInterval.max( 2 );
		final double sX = ( sX0 + sX1 + 1 ) / 2;
		final double sY = ( sY0 + sY1 + 1 ) / 2;
		final double sZ = ( sZ0 + sZ1 + 1 ) / 2;

		final double[][] m = new double[3][4];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		RotationAnimator.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( m );

		// scale
		final double[] pSource = new double[] { sX1, sY1, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY, 1, 3 );

		viewer.setCurrentViewerTransform( viewerTransform );
	}

	void initBrightness( final double cumulativeMinCutoff, final double cumulativeMaxCutoff )
	{
		final ViewerState state = viewer.getState();
		final Source< ? > source = state.getSources().get( state.getCurrentSource() ).getSpimSource();
		final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval ) source.getSource( state.getCurrentTimepoint(), source.getNumMipmapLevels() - 1 );
		final long z = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

		final int numBins = 6535;
		final Histogram1d< UnsignedShortType > histogram = new Histogram1d< UnsignedShortType >( Views.iterable( Views.hyperSlice( img, 2, z ) ), new Real1dBinMapper< UnsignedShortType >( 0, 65535, numBins, false ) );
		final DiscreteFrequencyDistribution dfd = histogram.dfd();
		final long[] bin = new long[] {0};
		double cumulative = 0;
		int i = 0;
		for ( ; i < numBins && cumulative < cumulativeMinCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int min = i * 65535 / numBins;
		for ( ; i < numBins && cumulative < cumulativeMaxCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int max = i * 65535 / numBins;
		brightnessDialog.setMinMax( min, max );
	}

	public static void view( final String filename ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ParserConfigurationException, SAXException, IOException
	{
		new ViewRegisteredAngles( filename );
	}

	public static void main( final String[] args )
	{
//		final String fn = "/Users/tobias/workspace/data/fast fly/111010_weber/combined.xml";
//		final String fn = "/Users/tobias/Desktop/openspim.xml";
//		final String fn = "/Users/pietzsch/Desktop/data/fibsem.xml";
//		final String fn = "/Users/pietzsch/Desktop/url-valia.xml";
		final String fn = "/Users/pietzsch/Desktop/Valia/valia.xml";
		try
		{
			new ViewRegisteredAngles( fn );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
