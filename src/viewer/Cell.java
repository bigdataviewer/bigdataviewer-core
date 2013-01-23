package viewer;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.util.Util;

class Cell
{
	private final int id;

	private int radius;

	private final RealPoint position;

	public Cell( final int id, final RealLocalizable position, final int size )
	{
		this.id = id;
		this.position = new RealPoint( position );
		this.radius = size;
	}

	@Override
	public String toString()
	{
		return "cell( " + id + ", " + Util.printCoordinates( position ) + ", " + radius + " )";
	}

	public int getRadius()
	{
		return radius;
	}

	public void setRadius( final int radius )
	{
		this.radius = radius;
	}

	public int getId()
	{
		return id;
	}

	public RealPoint getPosition()
	{
		return position;
	}
}