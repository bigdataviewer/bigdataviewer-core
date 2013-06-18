package viewer;

import java.util.ArrayList;
import java.util.Collection;

import net.imglib2.RealLocalizable;
import net.imglib2.collection.KDTree;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.ARGBType;

public class DistanceCellColor implements CellColorInterface
{
	final NearestNeighborSearchOnKDTree< Cell > search;
	
	double maxDist = 1;
	
	/**
	 * Color cells by distance to the next neighbor
	 * 
	 * @param cells - for finding nearest neighbors in (not the once that you then query with)
	 */
	public DistanceCellColor( final Collection< Cell > cells )
	{
		final ArrayList< RealLocalizable > l = new ArrayList<RealLocalizable>();
		final ArrayList< Cell > v = new ArrayList<Cell>();
		
		for ( final Cell cell : cells )
		{
			v.add( cell );
			l.add( cell.getPosition() );
		}
		
		final KDTree< Cell > k = new KDTree<Cell>( v, l );
		search = new NearestNeighborSearchOnKDTree<Cell>( k );
	}
	
	public void norm( final double maxDist )
	{
		this.maxDist = maxDist;
	}

	public double findMaxDistance( final Collection< Cell > cells )
	{
		double maxDist = 0;

		for ( final Cell cell : cells )
		{
			search.search( cell.getPosition() );
			maxDist = Math.max( maxDist, search.getDistance() );
		}
		
		return maxDist;
	}

	public double findMinDistance( final Collection< Cell > cells )
	{
		double minDist = Double.MAX_VALUE;

		for ( final Cell cell : cells )
		{
			search.search( cell.getPosition() );
			minDist = Math.min( minDist, search.getDistance() );
		}
		
		return minDist;
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
		return factory.create( dimensions, new ARGBType() );
	}
}
