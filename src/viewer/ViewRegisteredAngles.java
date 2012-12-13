package viewer;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import mpicbg.tracking.transform.AffineModel3D;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.xml.sax.SAXException;

import viewer.hdf5.Hdf5ImageLoader;
import viewer.hdf5.MipMapDefinition;
import viewer.hdf5.Util;

public class ViewRegisteredAngles
{
	class Source implements SpimAngleSource< UnsignedShortType >
	{
		int currentTimepoint;

		int currentLevel;

		RandomAccessibleInterval< UnsignedShortType > currentSource;

		final AffineTransform3D currentSourceTransform = new AffineTransform3D();

		final int setup;

		final String name;

		Source( final int setup, final String name )
		{
			this.setup = setup;
			this.name = name;
			loadTimepoint( 0, 0 );
		}

		final double[][] tmp = new double[3][4];

		final AffineTransform3D mipmapTransform = new AffineTransform3D();

		void loadTimepoint( final int timepoint, final int level )
		{
			currentTimepoint = timepoint;
			currentLevel = level;
			if ( isPresent( timepoint ) )
			{
				final View view = loader.getView( timepoint, setup );
				final AffineModel3D reg = view.getModel();
				reg.toMatrix( tmp );
				currentSourceTransform.set( tmp );
				for ( int d = 0; d < 3; ++d )
				{
					mipmapTransform.set( MipMapDefinition.resolutions[ currentLevel ][ d ], d, d );
					mipmapTransform.set( 0.5 * ( MipMapDefinition.resolutions[ currentLevel ][ d ] - 1 ), d, 3 );
				}
				currentSourceTransform.concatenate( mipmapTransform );
				currentSource = imgLoader.getUnsignedShortImage( view, currentLevel );
			}
			else
			{
				currentSourceTransform.identity();
				currentSource = null;
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
			if ( t != currentTimepoint || level != currentLevel )
				loadTimepoint( t, level );
			return currentSource;
		}

		@Override
		public synchronized AffineTransform3D getSourceTransform( final int t, final int level )
		{
			if ( t != currentTimepoint || level != currentLevel )
				loadTimepoint( t, level );
			return currentSourceTransform;
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

	private ViewRegisteredAngles( final String viewRegistrationsFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		loader = new SequenceViewsLoader( viewRegistrationsFilename );
		seq = loader.getSequenceDescription();
		imgLoader = ( Hdf5ImageLoader ) seq.imgLoader;

		final int width = 400;
		final int height = 300;

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 16384 );
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
			sources.add( new SourceAndConverter< UnsignedShortType >( new Source( setup, "angle " + setup ), converter ) );

		viewer = new SpimViewer( width, height, sources, seq.numTimepoints(), imgLoader.getCache() );
	}

	public static void main( final String[] args )
	{
		final String fn = "/home/tobias/workspace/data/fast fly/111010_weber/e012-reg-hdf5.xml";
		Util.timer = new Util.Timer();
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
