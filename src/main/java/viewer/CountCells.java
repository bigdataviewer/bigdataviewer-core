package viewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.region.localneighborhood.HyperSphereShape;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import viewer.display.LabelingTypeARGBConverter;
import viewer.render.Interpolation;
import viewer.render.Source;
import viewer.render.SourceAndConverter;

public class CountCells implements BrightnessDialog.MinMaxListener
{
	final KeyStroke brightnessKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK );

	final KeyStroke helpKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 );

	final KeyStroke addCellKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_C, 0 );

	final KeyStroke removeCellKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_C, KeyEvent.SHIFT_DOWN_MASK );

	final KeyStroke increaseCellRadius = KeyStroke.getKeyStroke( KeyEvent.VK_D, 0 );

	final KeyStroke decreaseCellRadius = KeyStroke.getKeyStroke( KeyEvent.VK_S, 0 );

	final SpimViewer viewer;

	final ArrayList< AbstractLinearRange > displayRanges;

	final BrightnessDialog brightnessDialog;

	final Overlay overlay;

	final JFileChooser fileChooser;

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
		new Round<RandomAccess< LabelingType< Integer >>>( a ).setPosition( p );
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
		new Round<RandomAccess< LabelingType< Integer >>>( a ).setPosition( p );
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
		final IntervalView< LabelingType< Integer >> ext = Views.interval( Views.extendValue( overlay.currentSource, new LabelingType< Integer >() ), Intervals.expand( overlay.currentSource, radius ) );
		final RandomAccess< Neighborhood< LabelingType< Integer > > > na = sphere.neighborhoodsRandomAccessible( ext ).randomAccess( /* TODO provide bounding box interval */ );
		new Round< RandomAccess< Neighborhood< LabelingType< Integer > > > >( na ).setPosition( center );
		for ( final LabelingType< Integer > t : na.get() )
		{
			final List< Integer > l = t.getLabeling();
			if ( ! l.contains( label ) )
			{
				final ArrayList< Integer > labels = new ArrayList< Integer >( t.getLabeling() );
				labels.add( label );
				t.setLabeling( labels );
			}
		}
	}

	private void removeLabelHyperSphere( final RealLocalizable center, final int radius, final Integer label )
	{
		final HyperSphereShape sphere = new HyperSphereShape( radius );
		final IntervalView< LabelingType< Integer >> ext = Views.interval( Views.extendValue( overlay.currentSource, new LabelingType< Integer >() ), Intervals.expand( overlay.currentSource, radius ) );
		final RandomAccess< Neighborhood< LabelingType< Integer > > > na = sphere.neighborhoodsRandomAccessible( ext ).randomAccess( /* TODO provide bounding box interval */ );
		new Round< RandomAccess< Neighborhood< LabelingType< Integer > > > >( na ).setPosition( center );
		for ( final LabelingType< Integer > t : na.get() )
		{
			final List< Integer > l = t.getLabeling();
			if ( l.contains( label ) )
			{
				final ArrayList< Integer > labels = new ArrayList< Integer >( t.getLabeling() );
				labels.remove( label );
				t.setLabeling( labels );
			}
		}
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

	class Overlay implements Source< ARGBType >
	{
		int currentTimepoint;

		NativeImgLabeling< Integer, IntType > currentSource;

		final SpimSource imgSource;

		final String name;

		final LabelingTypeARGBConverter< Integer > converter;

		final protected static int numInterpolationMethods = 2;

		final protected static int iNearestNeighborMethod = 0;

		final protected static int iNLinearMethod = 1;

		final protected InterpolatorFactory< ARGBType, RandomAccessible< ARGBType > >[] interpolatorFactories;

		@SuppressWarnings( "unchecked" )
		public Overlay( final SpimSource imgSource )

		{
			this.imgSource = imgSource;
			name = imgSource.getName() + " annotations";
			converter = new LabelingTypeARGBConverter< Integer >( new HashMap< List< Integer >, ARGBType >() );
			interpolatorFactories = new InterpolatorFactory[ numInterpolationMethods ];
			interpolatorFactories[ iNearestNeighborMethod ] = new NearestNeighborInterpolatorFactory< ARGBType >();
			interpolatorFactories[ iNLinearMethod ] = new NLinearInterpolatorFactory< ARGBType >();
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


		@Override
		public RealRandomAccessible< ARGBType > getInterpolatedSource( final int t, final int level, final Interpolation method )
		{
			return Views.interpolate( Views.extendValue( getSource( t, level ), new ARGBType() ), interpolatorFactories[ method == Interpolation.NLINEAR ? iNLinearMethod : iNearestNeighborMethod ] );
		}

		@Override
		public int getNumMipmapLevels()
		{
			return 1;
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

	private CountCells( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final int width = 800;
		final int height = 600;

		final SequenceViewsLoader loader = new SequenceViewsLoader( xmlFilename );
		final SequenceDescription seq = loader.getSequenceDescription();

		displayRanges = new ArrayList< AbstractLinearRange >();
		final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 65535 );
		displayRanges.add( converter );

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();

		final SpimSource source = new SpimSource( loader, 0, "image" );
		sources.add( new SourceAndConverter< UnsignedShortType >( source, converter ) );
		overlay = new Overlay( source );
		sources.add( new SourceAndConverter< ARGBType >( overlay, new TypeIdentity< ARGBType >() ) );
		overlay.getSource( 0, 0 );

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

		viewer.addKeyAction( addCellKeystroke, new AbstractAction( "add cell" )
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				final RealPoint p = new RealPoint( 3 );
				viewer.getGlobalMouseCoordinates( p );
				addCellAt( p );
			}

			private static final long serialVersionUID = 1L;
		} );

		viewer.addKeyAction( removeCellKeystroke, new AbstractAction( "remove cell" )
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				final RealPoint p = new RealPoint( 3 );
				viewer.getGlobalMouseCoordinates( p );
				removeCellsAt( p );
			}

			private static final long serialVersionUID = 1L;
		} );

		viewer.addKeyAction( increaseCellRadius, new AbstractAction( "increase cell radius" )
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				final RealPoint p = new RealPoint( 3 );
				viewer.getGlobalMouseCoordinates( p );
				modifiyCellRadiusAt( p, 1 );
			}

			private static final long serialVersionUID = 1L;
		} );

		viewer.addKeyAction( decreaseCellRadius, new AbstractAction( "decrease cell radius" )
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				final RealPoint p = new RealPoint( 3 );
				viewer.getGlobalMouseCoordinates( p );
				modifiyCellRadiusAt( p, -1 );
			}

			private static final long serialVersionUID = 1L;
		} );


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

		brightnessDialog = new BrightnessDialog( viewer.frame );
		viewer.installKeyActions( brightnessDialog );
		brightnessDialog.setListener( this );
	}

	public static void main( final String[] args )
	{
		final String fn = "/Users/tobias/Desktop/celegans/celegans.xml";
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
