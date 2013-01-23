package viewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import mpicbg.tracking.data.io.XmlHelpers;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
			else if ( keyCode == KeyEvent.VK_S )
			{
				final RealPoint p = new RealPoint( 3 );
				viewer.getGlobalMouseCoordinates( p );
				modifiyCellRadiusAt( p, -1 );
			}
			else if ( keyCode == KeyEvent.VK_D )
			{
				final RealPoint p = new RealPoint( 3 );
				viewer.getGlobalMouseCoordinates( p );
				modifiyCellRadiusAt( p, 1 );
			}

		}

		@Override
		public void keyReleased( final KeyEvent e )
		{
		}
	}

	int nextCellId = 0;

	final int minRadius = 3;

	int defaultRadius = 10;

	HashMap< Integer, Cell > cells = new HashMap< Integer, Cell >();

	synchronized void addCellAt( final RealLocalizable p )
	{
		final int cellId = nextCellId;
		++nextCellId;
		final int radius = defaultRadius;
		cells.put( cellId, new Cell( cellId, p, radius ) );
		addLabelHyperSphere( p, radius, cellId );
		overlay.updateColorTable();
		viewer.requestRepaint();
	}

	synchronized void removeCellsAt( final RealLocalizable p )
	{
		final RandomAccess< LabelingType< Integer > > a = overlay.currentSource.randomAccess();
		new Round<>( a ).setPosition( p );
		for ( final Integer label : a.get().getLabeling() )
		{
			final Cell cell = cells.remove( label );
			removeLabelHyperSphere( cell.getPosition(), cell.getRadius(), label );
		}
		overlay.updateColorTable();
		viewer.requestRepaint();
	}

	synchronized void modifiyCellRadiusAt( final RealPoint p, final int diff )
	{
		final RandomAccess< LabelingType< Integer > > a = overlay.currentSource.randomAccess();
		new Round<>( a ).setPosition( p );
		for ( final Integer label : a.get().getLabeling() )
		{
			final Cell cell = cells.get( label );
			final RealLocalizable pos = cell.getPosition();
			final int oldRadius = cell.getRadius();
			final int newRadius = oldRadius + diff;
			if ( newRadius >= minRadius )
			{
				removeLabelHyperSphere( pos, oldRadius, label );
				addLabelHyperSphere( pos, newRadius, label );
				cell.setRadius( newRadius );
				defaultRadius = newRadius;
			}
		}
		overlay.updateColorTable();
		viewer.requestRepaint();
	}

	private void addLabelHyperSphere( final RealLocalizable center, final int radius, final Integer label )
	{
		final HyperSphereShape sphere = new HyperSphereShape( radius );
		final RandomAccess< Neighborhood< LabelingType< Integer > > > na = sphere.neighborhoodsRandomAccessible( overlay.currentSource ).randomAccess( /* TODO provide bounding box interval */ );
		new Round<>( na ).setPosition( center );
		for ( final LabelingType< Integer > t : na.get() )
		{
			final ArrayList< Integer > labels = new ArrayList< Integer >( t.getLabeling() );
			labels.add( label );
			t.setLabeling( labels );
		}
	}

	private void removeLabelHyperSphere( final RealLocalizable center, final int radius, final Integer label )
	{
		final HyperSphereShape sphere = new HyperSphereShape( radius );
		final RandomAccess< Neighborhood< LabelingType< Integer > > > na = sphere.neighborhoodsRandomAccessible( overlay.currentSource ).randomAccess( /* TODO provide bounding box interval */ );
		new Round<>( na ).setPosition( center );
		for ( final LabelingType< Integer > t : na.get() )
		{
			final ArrayList< Integer > labels = new ArrayList< Integer >( t.getLabeling() );
			labels.remove( label );
			t.setLabeling( labels );
		}
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

	final JFileChooser fileChooser;

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

		final JMenuBar menubar = new JMenuBar();
		final JMenu menu = new JMenu("File");
		menubar.add( menu );
		final JMenuItem menuItemLoadAnnotations = new JMenuItem("Load annotations...");
		menuItemLoadAnnotations.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				loadAnnotations();
			}
		} );
		menu.add( menuItemLoadAnnotations );
		final JMenuItem menuItemSaveAnnotations = new JMenuItem("Save annotations...");
		menuItemSaveAnnotations.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				saveAnnotations();
			}
		} );
		menu.add( menuItemSaveAnnotations );
		viewer.setJMenuBar( menubar );

		fileChooser = new JFileChooser();
		fileChooser.setFileFilter( new FileFilter()
		{
			@Override
			public String getDescription()
			{
				return "xml files";
			}

			@Override
			public boolean accept( final File f )
			{
				if ( f.isDirectory() )
					return true;
				if ( f.isFile() )
				{
			        final String s = f.getName();
			        final int i = s.lastIndexOf('.');
			        if (i > 0 &&  i < s.length() - 1) {
			            final String ext = s.substring(i+1).toLowerCase();
			            return ext.equals( "xml" );
			        }
				}
				return false;
			}
		} );
	}

	protected synchronized void saveAnnotations()
	{
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			final File file = fileChooser.getSelectedFile();
			System.out.println( "saving annotions to " + file );

			Document doc;
			try
			{
				doc = XmlHelpers.newXmlDocument();
				final Element cellsElem = doc.createElement( "cells" );
				doc.appendChild( cellsElem );
				for ( final Cell cell : cells.values() )
					cellsElem.appendChild( Cell.toXml( doc, cell ) );
				XmlHelpers.writeXmlDocument( doc, file );
				System.out.println( "done" );
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	protected synchronized void loadAnnotations()
	{
		final int returnVal = fileChooser.showOpenDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			final File file = fileChooser.getSelectedFile();
			System.out.println( "loading annotions from " + file );

			for ( final Cell cell : cells.values() )
				removeLabelHyperSphere( cell.getPosition(), cell.getRadius(), new Integer( cell.getId() ) );
			cells.clear();
			nextCellId = 0;

			try
			{
				final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				final DocumentBuilder db = dbf.newDocumentBuilder();
				final Document dom = db.parse( file );
				final Element root = dom.getDocumentElement();
				final NodeList nodes = root.getElementsByTagName( "sphere" );
				for ( int i = 0; i < nodes.getLength(); ++i )
				{
					final Cell cell = Cell.fromXml( ( Element ) nodes.item( i ) );
					cells.put( cell.getId(), cell );
					addLabelHyperSphere( cell.getPosition(), cell.getRadius(), cell.getId() );
					if ( cell.getId() >= nextCellId )
						nextCellId = cell.getId() + 1;
				}
				overlay.updateColorTable();
				viewer.requestRepaint();
				System.out.println( "done" );
			}
			catch ( final Exception e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main( final String[] args )
	{
		final String fn = "/Users/tobias/Desktop/celegans/celegans-reg.xml";
		try
		{
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			new CountCells( fn );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

}
