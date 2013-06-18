package viewer;

import java.util.Collection;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class DistanceCellColorOverlay< T extends RealType< T > > extends DistanceCellColor
{
	final Img< T > overlay;
	final double min, max;
	
	public DistanceCellColorOverlay( final Collection<Cell> cells, final Img< T > overlay )
	{
		super( cells );
		
		this.overlay = overlay;		
		
		double min = overlay.firstElement().getRealDouble();
		double max = min;
		
		for ( final  RealType<?> t : overlay )
		{
			min = Math.min( min, t.getRealDouble() );
			max = Math.max( max, t.getRealDouble() );
		}
		
		this.min = min;
		this.max = max;
	}
		
	@Override
	public int getColorForCell( final Cell cell ) 
	{
		search.search( cell.getPosition() );
		final double d = search.getDistance();
		
		int r = 255;
		int g = (int)Math.round( 255 - (d / maxDist) * 255.0 );
		int b = (int)Math.round( 255 - (d / maxDist) * 255.0 );
		
		return ARGBType.rgba( r, g, b, 0 );
	}

	@Override
	public Img<ARGBType> createImage( final ImgFactory<ARGBType> factory, final long[] dimensions )
	{
		long[] dimImg = new long[ dimensions.length ];
		overlay.dimensions( dimImg );
		
		if ( dimensions.length != overlay.numDimensions() )
			throw new RuntimeException( "dimensions are not compatible: " + Util.printCoordinates( dimensions ) + " <-> " + Util.printCoordinates( dimImg ) );

		for ( int d = 0; d < dimensions.length; ++d )
			if ( dimensions[ d ] != overlay.dimension( d  ) )
				throw new RuntimeException( "dimensions are not compatible: " + Util.printCoordinates( dimensions ) + " <-> " + Util.printCoordinates( dimImg ) );
		
		final Img<ARGBType> img = factory.create( dimensions, new ARGBType() );
		
		final Cursor< ARGBType > c = img.localizingCursor();
		final RandomAccess< T > r = overlay.randomAccess();
		
		while ( c.hasNext() )
		{
			c.fwd();
			r.setPosition( c );
			
			int v = (int)Math.round( r.get().getRealDouble() - min / ( max - min ) * 255 );
			
			c.get().set( ARGBType.rgba( v, v, v, 0 ) );
		}
		
		return img;
	}
}
