package viewer;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.util.Util;

class Cell
{
	private final int id;

	private int size;

	private final RealPoint position;

	public Cell( final int id, final RealLocalizable position, final int size )
	{
		this.id = id;
		this.position = new RealPoint( position );
		this.size = size;
	}

	@Override
	public String toString()
	{
		return "cell( " + id + ", " + Util.printCoordinates( position ) + ", " + size + " )";
	}

	public int getSize()
	{
		return size;
	}

	public void setSize( final int size )
	{
		this.size = size;
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