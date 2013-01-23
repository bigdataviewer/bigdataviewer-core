package viewer;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import mpicbg.tracking.data.io.XmlHelpers;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.util.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

	public static void main( final String[] args ) throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException, SAXException, IOException
	{
		final Cell cell = new Cell( 12, new RealPoint( 100.0, 200.0, 300.12312 ), 8 );
		final Document doc = XmlHelpers.newXmlDocument();
		final Element cells = doc.createElement( "cells" );
		doc.appendChild( cells );
		cells.appendChild( Cell.toXml( doc, cell ) );
		XmlHelpers.writeXmlDocument( doc, "/Users/tobias/Desktop/celltest.xml" );

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document dom = db.parse( "/Users/tobias/Desktop/celltest.xml" );
		final Element root = dom.getDocumentElement();
		final NodeList nodes = root.getElementsByTagName( "sphere" );
		for ( int i = 0; i < nodes.getLength(); ++i )
			System.out.println( Cell.fromXml( ( Element ) nodes.item( i ) ) );
	}
}