package viewer;

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.util.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class Cell
{
	private final int id;

	private int radius;

	private final RealPoint position;

	public Cell( final int id, final RealLocalizable position, final int radius )
	{
		this.id = id;
		this.position = new RealPoint( position );
		this.radius = radius;
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

	public static Cell fromXml( final Element cell )
	{
		final int id = Integer.parseInt( cell.getElementsByTagName( "id" ).item( 0 ).getTextContent() );
		final String data = ( ( Element ) cell.getElementsByTagName( "position" ).item( 0 ) ).getAttribute( "data" );
		final String[] fields = data.split( "\\s+" );
		final int n = fields.length;
		final RealPoint position = new RealPoint( n );
		for ( int d = 0; d < n; ++d )
			position.setPosition( Double.parseDouble( fields[ d ] ), d );
		final int radius = Integer.parseInt( cell.getElementsByTagName( "radius" ).item( 0 ).getTextContent() );
		return new Cell( id, position, radius );
	}

	public static Element toXml( final Document doc, final Cell cell )
	{
		final Element elem = doc.createElement( "sphere" );

		elem.appendChild( XmlHelpers.intElement( doc, "id", cell.getId() ) );
		final RealLocalizable pos = cell.getPosition();
		final int n = pos.numDimensions();
		String data = "";
		for ( int d = 0; d < n; ++d )
			data += pos.getDoublePosition( d ) + ( ( d == n - 1 ) ? "" : " " );
		final Element poselem = doc.createElement( "position" );
		poselem.setAttribute( "data", data );
		elem.appendChild( poselem );
		elem.appendChild( XmlHelpers.intElement( doc, "radius", cell.getRadius() ) );

		return elem;
	}
}