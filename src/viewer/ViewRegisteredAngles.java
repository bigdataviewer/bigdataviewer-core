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

public class ViewRegisteredAngles
{
	class Source implements SpimAngleSource< UnsignedShortType >
	{
		int currentTimepoint;

		RandomAccessibleInterval< UnsignedShortType > currentSource;

		final AffineTransform3D currentSourceTransform = new AffineTransform3D();

		final int setup;

		final String name;

		Source( final int setup, final String name )
		{
			this.setup = setup;
			this.name = name;
			loadTimepoint( 0 );
		}

		final double[][] tmp = new double[3][4];

		void loadTimepoint( final int timepoint )
		{
			currentTimepoint = timepoint;
			if ( isPresent( timepoint ) )
			{
				final View view = loader.getView( timepoint, setup );
				final AffineModel3D reg = view.getModel();
				reg.toMatrix( tmp );
				currentSourceTransform.set( tmp );
				currentSource = seq.imgLoader.getUnsignedShortImage( view );
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
		public RandomAccessibleInterval< UnsignedShortType > getSource( final int t )
		{
			if ( t != currentTimepoint )
				loadTimepoint( t );
			return currentSource;
		}

		@Override
		public AffineTransform3D getSourceTransform( final int t )
		{
			if ( t != currentTimepoint )
				loadTimepoint( t );
			return currentSourceTransform;
		}

		@Override
		public String getName()
		{
			return name;
		}
	}

	final SpimViewer viewer;

	final SequenceViewsLoader loader;

	final SequenceDescription seq;

	private ViewRegisteredAngles( final String viewRegistrationsFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		loader = new SequenceViewsLoader( viewRegistrationsFilename );
		seq = loader.getSequenceDescription();


		final int width = 400;
		final int height = 300;

		final ArrayList< SpimViewer.SourceAndConverter< ? > > sources = new ArrayList< SpimViewer.SourceAndConverter< ? > >();
		final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 16384 );
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
			sources.add( new SpimViewer.SourceAndConverter< UnsignedShortType >( new Source( setup, "angle " + setup ), converter ) );

		viewer = new SpimViewer( width, height, sources, 50 ); //seq.numTimepoints() );
	}

	public static void main( final String[] args )
	{
		final String fn = "/home/tobias/workspace/data/fast fly/111010_weber/e012-reg-hdf5.xml";
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
