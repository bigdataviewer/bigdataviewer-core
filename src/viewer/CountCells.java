package viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import mpicbg.tracking.transform.AffineModel3D;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import org.xml.sax.SAXException;

import viewer.display.LabelingTypeARGBConverter;
import viewer.hdf5.Hdf5ImageLoader;

public class CountCells
{
	class Overlay implements SpimSource< ARGBType >
	{
		int currentTimepoint;

		NativeImgLabeling< Integer, IntType > currentSource;

		final Source imgSource;

		final String name;

		final LabelingTypeARGBConverter< Integer > converter;

		public Overlay( final Source imgSource )
		{
			this.imgSource = imgSource;
			name = imgSource.getName() + " annotations";
			converter = new LabelingTypeARGBConverter< Integer >( new HashMap< List< Integer >, ARGBType >() );
			loadTimepoint( 0 );
		}

		@Override
		public boolean isPresent( final int t )
		{
			return imgSource.isPresent( t );
		}

		@Override
		public RandomAccessibleInterval< ARGBType > getSource( final int t, final int level )
		{
			if ( t != currentTimepoint )
				loadTimepoint( t );
			return Converters.convert( ( RandomAccessibleInterval< LabelingType< Integer > > )currentSource, converter, new ARGBType() );
			// return new ConvertedRandomAccessibleInterval< LabelingType< Integer >, ARGBType >( currentSource, converter, new ARGBType() );
		}

		void loadTimepoint( final int timepoint )
		{
			currentTimepoint = timepoint;
			if ( isPresent( timepoint ) )
			{
				final Dimensions sourceDimensions = imgSource.getSource( timepoint, 0 );
				final ArrayImgFactory< IntType > factory = new ArrayImgFactory< IntType >();
				final Img< IntType > img = factory.create( sourceDimensions, new IntType() );
				final NativeImgLabeling< Integer, IntType > labeling = new NativeImgLabeling< Integer, IntType >( img );
				currentSource = labeling;

				// set label for pixels above threshold
				final Cursor< UnsignedShortType > ic = Views.flatIterable( imgSource.getSource( timepoint, 0 ) ).cursor();
				// final Cursor< LabelingType< Integer > > lc = Views.flatIterable( labeling ).cursor();
				for ( final LabelingType< Integer > l : Views.flatIterable( labeling ) )
				{
					if ( ic.next().get() > 30000 )
						l.setLabel( 1 );
				}
				updateColorTable();
			}
			else
				currentSource = null;
		}

		@Override
		public AffineTransform3D getSourceTransform( final int t, final int level )
		{
			return imgSource.getSourceTransform( t, 0 );
		}

		@Override
		public ARGBType getType()
		{
			return new ARGBType();
		}

		@Override
		public String getName()
		{
			return name;
		}

		public LabelingTypeARGBConverter< Integer > getConverter()
		{
			return null;
		}

		private void updateColorTable()
		{
			final HashMap< List< Integer >, ARGBType > colorTable = new HashMap< List< Integer >, ARGBType >();
			final LabelingMapping< Integer > mapping = currentSource.getMapping();
			final int numLists = mapping.numLists();
			final Random random = new Random();
			for ( int i = 0; i < numLists; ++i )
			{
				final List< Integer > list = mapping.listAtIndex( i );
				colorTable.put( list, new ARGBType( random.nextInt() ) );
			}
			colorTable.put( mapping.emptyList(), new ARGBType( 0 ) );
			converter.setColorTable( colorTable );
		}
	}

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

		final double[][] tmp = new double[3][4];

		final AffineTransform3D mipmapTransform = new AffineTransform3D();

		void loadTimepoint( final int timepoint )
		{
			currentTimepoint = timepoint;
			if ( isPresent( timepoint ) )
			{
				final View view = loader.getView( timepoint, setup );
				final AffineModel3D reg = view.getModel();
				reg.toMatrix( tmp );
				for ( int level = 0; level < currentSources.length; level++ )
				{
					final double[] resolution = imgLoader.getMipmapResolutions()[ level ];
					for ( int d = 0; d < 3; ++d )
					{
						mipmapTransform.set( resolution[ d ], d, d );
						mipmapTransform.set( 0.5 * ( resolution[ d ] - 1 ), d, 3 );
					}
					currentSourceTransforms[ level ].set( tmp );
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

	private CountCells( final String viewRegistrationsFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		loader = new SequenceViewsLoader( viewRegistrationsFilename );
		seq = loader.getSequenceDescription();
		imgLoader = ( Hdf5ImageLoader ) seq.imgLoader;

		final int width = 400;
		final int height = 300;

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 65536 /*16384*/ );
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
		{
			final Source source = new Source( setup, "angle " + setup );
			sources.add( new SourceAndConverter< UnsignedShortType >( source, converter ) );
			final Overlay overlay = new Overlay( source );
			sources.add( new SourceAndConverter< ARGBType >( overlay, new TypeIdentity< ARGBType >() ) );
			overlay.getSource( 0, 0 );
		}

		final int numMipmapLevels = imgLoader.getMipmapResolutions().length;
		viewer = new SpimViewer( width, height, sources, seq.numTimepoints(), numMipmapLevels );
	}

	public static void main( final String[] args )
	{
		final String fn = "/Users/tobias/Desktop/celegans/celegans-reg.xml";
		try
		{
			new CountCells( fn );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

}
