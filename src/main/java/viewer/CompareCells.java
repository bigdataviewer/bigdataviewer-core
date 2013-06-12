package viewer;

import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;



public class CompareCells 
{
	public CompareCells( final File xml1, final File xml2, final File image ) throws ImgIOException
	{
		final Img< FloatType > img;
		
		if ( image != null )
			img = ImageJFunctions.wrapFloat( new ImagePlus( image.getAbsolutePath() ) );
		else
			img = null;
		
		final HashMap< Integer, Cell > cells1 = loadAnnotations( xml2 );
		final HashMap< Integer, Cell > cells2 = loadAnnotations( xml1 );

		DistanceCellColor distColor = new DistanceCellColorOverlay<FloatType>( cells2.values(), img );
		DistanceCellColor distColor2 = new DistanceCellColor( cells2.values() );
		distColor.norm( 10 );
		distColor2.norm( 10 );
		
		final int numDimensions = cells1.values().iterator().next().getPosition().numDimensions();
		final double scale = 1.0;
		
		final double[] min = new double[ numDimensions ];
		final double[] max = new double[ numDimensions ];

		if ( img == null )
		{
			for ( int d = 0; d < numDimensions; ++d )
			{
				min[ d ] = Double.MAX_VALUE;
				max[ d ] = -Double.MAX_VALUE;
			}
			
			getMinMax( min, max, scale, cells1.values() );
			getMinMax( min, max, scale, cells2.values() );
		}
		else
		{
			for ( int d = 0; d < numDimensions; ++d )
			{
				min[ d ] = img.min( d );
				max[ d ] = img.max( d ) + 1;
			}
		}
		
		System.out.println( "min: " + Util.printCoordinates( min ) );		
		System.out.println( "max: " + Util.printCoordinates( max ) );

		new ImageJ();
		
		final Img< ARGBType > diffOverlay = visualize( cells1, scale, distColor, min, max );
		ImageJFunctions.show( diffOverlay ).setTitle( "diff_overlay" );

		final Img< ARGBType > diff = visualize( cells1, scale, distColor2, min, max );
		ImageJFunctions.show( diff ).setTitle( "diff" );
		
		final Img< ARGBType > img1 = visualize( cells1, scale, new RandomCellColor(), min, max );
		ImageJFunctions.show( img1 ).setTitle( "steffi" );
		
		final Img< ARGBType > img2 = visualize( cells2, scale, new RandomCellColor(), min, max );
		ImageJFunctions.show( img2 ).setTitle( "misho" );
		
	}
	
	//public Im

	public Img< ARGBType > visualize( final HashMap< Integer, Cell > cells )
	{
		return visualize( cells, 1.0, new RandomCellColor(), null, null );
	}
	
	public Img< ARGBType > visualize( final HashMap< Integer, Cell > cells, final double scale, final CellColorInterface color, double[] min, double[] max )
	{
		final int numDimensions = cells.values().iterator().next().getPosition().numDimensions();
		final long[] dim = new long[ numDimensions ];
				
		// iterate over all cells to find min/max
		if ( min == null || max == null )
		{
			min = new double[ dim.length ];
			max = new double[ dim.length ];
			
			for ( int d = 0; d < numDimensions; ++d )
			{
				min[ d ] = Double.MAX_VALUE;
				max[ d ] = -Double.MAX_VALUE;
			}
			
			getMinMax( min, max, scale, cells.values() );
		}
		
		// determine size and little bit of space around it
		for ( int d = 0; d < numDimensions; ++d )
			dim[ d ] = Math.round( max[ d ] - min[ d ] );

		System.out.println( "dim: " + Util.printCoordinates( dim ) );

		final Img< ARGBType > img = color.createImage( new ArrayImgFactory<ARGBType>(), dim );
		final RandomAccessible< ARGBType > infinite = Views.extendValue( img, new ARGBType() );
		
		for ( final Cell cell : cells.values() )
		{
			final Point p = new Point( numDimensions ); 

			for ( int d = 0; d < numDimensions; ++d )
				p.setPosition( Math.round( cell.getPosition().getDoublePosition( d ) * scale - min[ d ] ), d );
			
			final HyperSphere< ARGBType > hs = new HyperSphere<ARGBType>( infinite, p, Math.round( cell.getRadius() * scale ) );
		
			final int rgb = color.getColorForCell( cell );
			
			for ( final ARGBType t : hs )
				t.set( rgb );
		}
		
		return img;
	}

	public void getMinMax( final double[] min, final double max[], final double scale, final Collection< Cell > cells )
	{
		final int numDimensions = min.length;
		
		for ( final Cell cell : cells )
		{
			final RealPoint p = cell.getPosition();
			
			for ( int d = 0; d < numDimensions; ++d )
			{
				double l = p.getDoublePosition( d );
				
				min[ d ] = Math.min( ( l - cell.getRadius() ) * scale, min[ d ] );
				max[ d ] = Math.max( ( l + cell.getRadius() ) * scale, max[ d ] );
			}
		}
	}
	
	public HashMap< Integer, Cell > loadAnnotations( final File file )
	{
		System.out.print( "loading annotions from " + file + " ... " );

		HashMap< Integer, Cell > cells = new HashMap< Integer, Cell >();

		int nextCellId = 0;

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
				if ( cell.getId() >= nextCellId )
					nextCellId = cell.getId() + 1;
			}
			System.out.println( " loaded " + cells.size() + " cells." );
		}
		catch ( final Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return cells;
	}

	public static void main( String[] args ) throws ImgIOException
	{
		new CompareCells( 
				new File( "/Users/preibischs/Documents/Microscopy/HDF5/l1-reconstructed.cells.xml"), 
				new File( "/Users/preibischs/Documents/Microscopy/HDF5/l1SP2K.cells.xml" ),
				new File( "/Users/preibischs/Documents/Microscopy/l1 fixed - SP2 - nuclear envelope 2/l1-reconstructed_32.tif" ) );
	}
}
