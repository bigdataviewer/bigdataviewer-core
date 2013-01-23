package viewer;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import mpicbg.tracking.transform.AffineModel3D;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.region.localneighborhood.HyperSphereShape;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;

import org.xml.sax.SAXException;

import viewer.display.LabelingTypeARGBConverter;
import viewer.hdf5.Hdf5ImageLoader;

public class CountCells
{
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
			final int modifiers = e.getModifiersEx();
			final boolean remove = ( modifiers & KeyEvent.SHIFT_DOWN_MASK ) != 0;

			if ( keyCode == KeyEvent.VK_C )
			{
				final RealPoint p = new RealPoint( 3 );
				viewer.getGlobalMouseCoordinates( p );
				if ( remove )
					removeCellsAt( p );
				else
					addCellAt( p );
			}

		}

		@Override
		public void keyReleased( final KeyEvent e )
		{
		}
	}

	int nextCellId = 1;

	HashMap< Integer, Cell > cells = new HashMap< Integer, Cell >();

	void removeCellsAt( final RealLocalizable p )
	{
		final RandomAccess< LabelingType< Integer > > a = overlay.currentSource.randomAccess();
		new Round<>( a ).setPosition( p );
		for ( final Integer label : a.get().getLabeling() )
		{
			final Cell cell = cells.get( label );
			cells.remove( label );
			final Integer cellId = new Integer( cell.getId() );
			final HyperSphereShape sphere = new HyperSphereShape( cell.getSize() );
			final RandomAccess< Neighborhood< LabelingType< Integer > > > na = sphere.neighborhoodsRandomAccessible( overlay.currentSource ).randomAccess( /* TODO provide bounding box interval */ );
			new Round<>( na ).setPosition( cell.getPosition() );
			for ( final LabelingType< Integer > t : na.get() )
			{
				final ArrayList< Integer > labels = new ArrayList< Integer >( t.getLabeling() );
				labels.remove( cellId );
				t.setLabeling( labels );
			}
		}

		overlay.updateColorTable();
		viewer.requestRepaint();
	}

	void addCellAt( final RealLocalizable p )
	{
		final int cellId = nextCellId;
		System.out.println("adding cell (" + cellId + ") at " + Util.printCoordinates( p ) );
		++nextCellId;
		final int radius = 10;
		cells.put( cellId, new Cell( cellId, p, radius ) );

		final HyperSphereShape sphere = new HyperSphereShape( radius );
		final RandomAccess< Neighborhood< LabelingType< Integer > > > na = sphere.neighborhoodsRandomAccessible( overlay.currentSource ).randomAccess( /* TODO provide bounding box interval */ );
		new Round<>( na ).setPosition( p );
		for ( final LabelingType< Integer > t : na.get() )
		{
//			t.setLabel( cellId );
			final ArrayList< Integer > labels = new ArrayList< Integer >( t.getLabeling() );
			labels.add( cellId );
			t.setLabeling( labels );
		}

		System.out.println("cell added [id = " + cellId + "]" );
		overlay.updateColorTable();
		viewer.requestRepaint();
	}

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
			if ( viewer != null && viewer.getCurrentAlpha() != oldAlpha )
				updateColorTable();
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

		volatile int oldAlpha = -1;

		private void updateColorTable()
		{
			if ( viewer == null )
				return;
			final int a = viewer.getCurrentAlpha();
			final HashMap< List< Integer >, ARGBType > colorTable = new HashMap< List< Integer >, ARGBType >();
			final LabelingMapping< Integer > mapping = currentSource.getMapping();
			final int numLists = mapping.numLists();
			final Random random = new Random( 1 );
			for ( int i = 0; i < numLists; ++i )
			{
				final List< Integer > list = mapping.listAtIndex( i );
				final int r = random.nextInt( 256 );
				final int g = random.nextInt( 256 );
				final int b = random.nextInt( 256 );
				colorTable.put( list, new ARGBType( ARGBType.rgba( r, g, b, a ) ) );
			}
			colorTable.put( mapping.emptyList(), new ARGBType( 0 ) );
			converter.setColorTable( colorTable );
			oldAlpha = a;
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

	final Overlay overlay;

	private CountCells( final String viewRegistrationsFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		loader = new SequenceViewsLoader( viewRegistrationsFilename );
		seq = loader.getSequenceDescription();
		imgLoader = ( Hdf5ImageLoader ) seq.imgLoader;

		final int width = 400;
		final int height = 300;

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 65536 /*16384*/ );
		final Source source = new Source( 0, "image " );
		sources.add( new SourceAndConverter< UnsignedShortType >( source, converter ) );
		overlay = new Overlay( source );
		sources.add( new SourceAndConverter< ARGBType >( overlay, new TypeIdentity< ARGBType >() ) );
		overlay.getSource( 0, 0 );

		final int numMipmapLevels = imgLoader.getMipmapResolutions().length;
		viewer = new SpimViewer( width, height, sources, seq.numTimepoints(), numMipmapLevels );
		viewer.addHandler( new Keys() );
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
