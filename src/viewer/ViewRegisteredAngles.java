package viewer;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.xml.sax.SAXException;

import viewer.hdf5.Hdf5ImageLoader;

public class ViewRegisteredAngles
{
	class Source implements SpimSource< UnsignedShortType >
	{
		int currentTimepoint;

		RandomAccessibleInterval< UnsignedShortType >[] currentSources;

		final AffineTransform3D[] currentSourceTransforms;

		final int setup;

		final String name;

		@SuppressWarnings( "unchecked" )
		Source( final int setup, final String name )
		{
			this.setup = setup;
			this.name = name;
			final int levels = imgLoader.numMipmapLevels();
			currentSources = new RandomAccessibleInterval[ levels ];
			currentSourceTransforms = new AffineTransform3D[ levels ];
			for ( int level = 0; level < levels; level++ )
				currentSourceTransforms[ level ] = new AffineTransform3D();
			loadTimepoint( 0 );
		}

		final AffineTransform3D mipmapTransform = new AffineTransform3D();

		void loadTimepoint( final int timepoint )
		{
			currentTimepoint = timepoint;
			if ( isPresent( timepoint ) )
			{
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
				}
			}
			else
			{
				for ( int level = 0; level < currentSources.length; level++ )
				{
					currentSourceTransforms[ level ].identity();
					currentSources[ level ] = null;
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

	private ViewRegisteredAngles( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		loader = new SequenceViewsLoader( xmlFilename );
		seq = loader.getSequenceDescription();
		imgLoader = ( Hdf5ImageLoader ) seq.imgLoader;

		final int width = 400;
		final int height = 300;

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 16384 );
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
			sources.add( new SourceAndConverter< UnsignedShortType >( new Source( setup, "angle " + setup ), converter ) );

		final int numMipmapLevels = imgLoader.getMipmapResolutions().length;
		viewer = new SpimViewer( width, height, sources, seq.numTimepoints(), numMipmapLevels );
	}

	public static void main( final String[] args )
	{
		final String fn = "/Users/tobias/workspace/data/fast fly/111010_weber/combined.xml";
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
