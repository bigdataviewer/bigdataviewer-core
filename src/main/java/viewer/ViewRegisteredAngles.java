package viewer;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.KeyStroke;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import org.xml.sax.SAXException;

import viewer.hdf5.Hdf5ImageLoader;
import viewer.refactor.Interpolation;

public class ViewRegisteredAngles implements BrightnessDialog.MinMaxListener
{
	final int brightnessDialogKey = KeyEvent.VK_S;

	class Keys implements KeyListener
	{

		@Override
		public void keyTyped( final KeyEvent e )
		{
		}

		@Override
		public void keyPressed( final KeyEvent e )
		{
			final int keyCode = e.getKeyCode();
			// final int modifiers = e.getModifiersEx();
			if ( keyCode == brightnessDialogKey )
			{
				brightnessDialog.setVisible( ! brightnessDialog.isVisible() );
			}
			else if ( keyCode == KeyEvent.VK_F1 )
			{
				new HelpFrame();
			}
		}

		@Override
		public void keyReleased( final KeyEvent e )
		{
		}
	};

	class Source implements SpimSource< UnsignedShortType >
	{
		int currentTimepoint;

		RandomAccessibleInterval< UnsignedShortType >[] currentSources;

		RealRandomAccessible< UnsignedShortType >[][] currentInterpolatedSources;

		final AffineTransform3D[] currentSourceTransforms;

		final int setup;

		final String name;

		final protected static int numInterpolationMethods = 2;

		final protected static int iNearestNeighborMethod = 0;

		final protected static int iNLinearMethod = 1;

		final protected InterpolatorFactory< UnsignedShortType, RandomAccessible< UnsignedShortType > >[] interpolatorFactories;

		@SuppressWarnings( "unchecked" )
		Source( final int setup, final String name )
		{
			this.setup = setup;
			this.name = name;
			final int levels = imgLoader.numMipmapLevels();
			currentSources = new RandomAccessibleInterval[ levels ];
			currentInterpolatedSources = new RealRandomAccessible[ levels ][ 2 ];
			currentSourceTransforms = new AffineTransform3D[ levels ];
			for ( int level = 0; level < levels; level++ )
				currentSourceTransforms[ level ] = new AffineTransform3D();
			interpolatorFactories = new InterpolatorFactory[ numInterpolationMethods ];
			interpolatorFactories[ iNearestNeighborMethod ] = new NearestNeighborInterpolatorFactory< UnsignedShortType >();
			interpolatorFactories[ iNLinearMethod ] = new NLinearInterpolatorFactory< UnsignedShortType >();
			loadTimepoint( 0 );
		}

		final AffineTransform3D mipmapTransform = new AffineTransform3D();

		void loadTimepoint( final int timepoint )
		{
			currentTimepoint = timepoint;
			if ( isPresent( timepoint ) )
			{
				final UnsignedShortType zero = new UnsignedShortType();
				zero.setZero();
				final View view = loader.getView( timepoint, setup );
				final AffineTransform3D reg = view.getModel();
				for ( int level = 0; level < currentSources.length; level++ )
				{
					final double[] resolution = imgLoader.getMipmapResolutions()[ level ];
					for ( int d = 0; d < 3; ++d )
					{
						mipmapTransform.set( resolution[ d ], d, d );
						mipmapTransform.set( 0.5 * ( resolution[ d ] - 1 ), d, 3 );
					}
					currentSourceTransforms[ level ].set( reg );
					currentSourceTransforms[ level ].concatenate( mipmapTransform );
					currentSources[ level ] = imgLoader.getUnsignedShortImage( view, level );
					for ( int method = 0; method < numInterpolationMethods; ++method )
						currentInterpolatedSources[ level ][ method ] = Views.interpolate( Views.extendValue( currentSources[ level ], zero ), interpolatorFactories[ method ] );
				}
			}
			else
			{
				for ( int level = 0; level < currentSources.length; level++ )
				{
					currentSourceTransforms[ level ].identity();
					currentSources[ level ] = null;
					for ( int method = 0; method < numInterpolationMethods; ++method )
						currentInterpolatedSources[ level ][ method ] = null;
				}
			}
		}

		@Override
		public boolean isPresent( final int t )
		{
			return t >= 0 && t < seq.numTimepoints();
		}

		@Override
		public synchronized RandomAccessibleInterval< UnsignedShortType > getSource( final int t, final int level )
		{
			if ( t != currentTimepoint )
				loadTimepoint( t );
			return currentSources[ level ];
		}


		@Override
		public RealRandomAccessible< UnsignedShortType > getInterpolatedSource( final int t, final int level, final Interpolation method )
		{
			if ( t != currentTimepoint )
				loadTimepoint( t );
			return currentInterpolatedSources[ level ][ method == Interpolation.NLINEAR ? iNLinearMethod : iNearestNeighborMethod ];
		}

		@Override
		public synchronized AffineTransform3D getSourceTransform( final int t, final int level )
		{
			if ( t != currentTimepoint )
				loadTimepoint( t );
			return currentSourceTransforms[ level ];
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public UnsignedShortType getType()
		{
			return new UnsignedShortType();
		}
	}

	final SpimViewer viewer;

	final SequenceViewsLoader loader;

	final SequenceDescription seq;

	final Hdf5ImageLoader imgLoader;

	final ArrayList< AbstractLinearRange > displayRanges;

	final BrightnessDialog brightnessDialog;

	@Override
	public void setMinMax( final int min, final int max )
	{
		for ( final AbstractLinearRange r : displayRanges )
		{
			r.setMin( min );
			r.setMax( max );
			viewer.requestRepaint();
		}
	}

	private ViewRegisteredAngles( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		loader = new SequenceViewsLoader( xmlFilename );
		seq = loader.getSequenceDescription();
		imgLoader = ( Hdf5ImageLoader ) seq.imgLoader;
		displayRanges = new ArrayList< AbstractLinearRange >();

		final int width = 800;
		final int height = 600;

		final ArrayList< SpimSourceAndConverter< ? > > sources = new ArrayList< SpimSourceAndConverter< ? > >();
		final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 65535 );
		displayRanges.add( converter );
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
			sources.add( new SpimSourceAndConverter< UnsignedShortType >( new Source( setup, "angle " + setup ), converter ) );

		final int numMipmapLevels = imgLoader.getMipmapResolutions().length;
		viewer = new SpimViewer( width, height, sources, seq.numTimepoints(), numMipmapLevels );
		brightnessDialog = new BrightnessDialog( viewer.frame, KeyStroke.getKeyStroke( brightnessDialogKey, 0 ) );
		brightnessDialog.setListener( this );
		final Keys keys = new Keys();
		viewer.addHandler( keys );
	}

	public static void view( final String filename ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ParserConfigurationException, SAXException, IOException
	{
		new ViewRegisteredAngles( filename );
	}

	public static void main( final String[] args )
	{
		final String fn = "/Users/tobias/workspace/data/fast fly/111010_weber/combined.xml";
//		final String fn = "/Users/tobias/Desktop/openspim-deconvolved.xml";
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
