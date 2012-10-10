package viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import mpicbg.tracking.data.ViewRegistration;
import mpicbg.tracking.data.ViewRegistrations;
import mpicbg.tracking.transform.AffineModel3D;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.img.ImgPlus;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.xml.sax.SAXException;

public class ViewRegisteredAngles
{
	/**
	 * Apply view registration transformations to the (local) detections for
	 * each view to transform them to global coordinates.
	 */
	public static void viewRegisteredAngles( final String viewRegistrationsFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		new ViewRegisteredAngles( viewRegistrationsFilename );
	}

	final private SequenceDescription seq;

	final private ArrayList< View > views;

	private Multi3DViewer viewer = null;

	public < T extends NumericType< T > > void show3d( final RandomAccessibleInterval< T > img, final Converter< T, ARGBType > converter, final AffineTransform3D sourceTransform, final String name )
	{
		final int width = 400;
		final int height = 300;
		final T template = Views.iterable( img ).firstElement().copy();
		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > source = Views.extendValue( img, template );
		if ( viewer == null )
		{
			final ArrayList< Multi3DViewer.SourceAndConverter< ? > > sources = new ArrayList< Multi3DViewer.SourceAndConverter< ? > >();
			sources.add( new Multi3DViewer.SourceAndConverter< T >( source, converter, sourceTransform, name ) );
			viewer = new Multi3DViewer( width, height, sources, img );
		}
		else
			viewer.addSource( new Multi3DViewer.SourceAndConverter< T >( source, converter, sourceTransform, name ) );
	}

	public View getView( final int timepoint, final int setup )
	{
		return views.get( timepoint * seq.numViewSetups() + setup );
	}

	private void createViews( final ViewRegistrations regs )
	{
		views.clear();

		if ( seq.numTimepoints() * seq.numViewSetups() != regs.registrations.size() )
			throw new RuntimeException( "ViewRegistrations does not match SequenceDescription" );

		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >( regs.registrations );
		Collections.sort( registrations, new Comparator< ViewRegistration >()
		{
			@Override
			public int compare( final ViewRegistration o1, final ViewRegistration o2 )
			{
				final int dt = o1.getTimepointIndex() - o2.getTimepointIndex();
				if ( dt == 0 )
					return o1.getSetupIndex() - o2.getSetupIndex();
				else
					return dt;
			}
		} );

		final Iterator< ViewRegistration > ri = registrations.iterator();
		for ( int ti = 0; ti < seq.timepoints.length; ++ti )
			for ( int si = 0; si < seq.setups.length; ++si )
			{
				final ViewRegistration reg = ri.next();
				if ( reg.getTimepointIndex() == ti && reg.getSetupIndex() == si )
					views.add( new View( seq, ti, si, reg.getModel() ) );
				else
					throw new RuntimeException( "ViewRegistrations does not match SequenceDescription" );
			}
	}

	private ViewRegisteredAngles( final String viewRegistrationsFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final ViewRegistrations regs = ViewRegistrations.load( viewRegistrationsFilename );
		seq = SequenceDescription.load( regs.sequenceDescriptionName, true );
		views = new ArrayList< View >();
		createViews( regs );

		final double[][] data = new double[3][4];

		final int timepoint = 0;
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
		{
			final View view = getView( timepoint, setup );
			final AffineModel3D reg = view.getModel();
			final AffineTransform3D viewTransform = new AffineTransform3D();
			reg.toMatrix( data );
			viewTransform.set( data );

			final ImgPlus< FloatType > img = seq.imgLoader.getImage( view );
			show3d( img, new RealARGBConverter< FloatType >(), viewTransform, view.getBasename() );
		}
	}

	public static void main( final String[] args )
	{
		final String fn = "/home/tobias/workspace/data/fast fly/111010_weber/e012-reg.xml";
		try
		{
			viewRegisteredAngles( fn );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
