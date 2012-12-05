package mpicbg.tracking.data;

import mpicbg.tracking.data.io.XmlHelpers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A collection of parameters describing the setup for a particular view, e.g.
 * angle, illumination direction, etc.
 *
 * @author Tobias Pietzsch
 */
public final class ViewSetup implements Comparable< ViewSetup >
{
	/**
	 * index.
	 */
	private final int id;

	/**
	 * Angle in degrees.
	 */
	private final int angle;

	/**
	 * Illumination direction index.
	 */
	private final int illumination;

	/**
	 * Channel index
	 */
	private final int channel;

	/**
	 * width of stack slice in pixels.
	 */
	private final int width;

	/**
	 * height of stack slice in pixels.
	 */
	private final int height;

	/**
	 * number of slices.
	 */
	private final int depth;

	/**
	 * width of a pixel in um.
	 */
	private final double pixelWidth;

	/**
	 * height of a pixel in um.
	 */
	private final double pixelHeight;

	/**
	 * depth of a pixel in um.
	 */
	private final double pixelDepth;

	public ViewSetup( final int id, final int angle, final int illumination, final int channel, final int width, final int height, final int depth, final double pixelWidth, final double pixelHeight, final double pixelDepth )
	{
		this.id = id;
		this.angle = angle;
		this.illumination = illumination;
		this.channel = channel;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.pixelWidth = pixelWidth;
		this.pixelHeight = pixelHeight;
		this.pixelDepth = pixelDepth;
	}

	/**
	 * Get ViewSetup index.
	 *
	 * @return index.
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Get stage rotation angle in degrees.
	 *
	 * @return angle in degrees
	 */
	public int getAngle()
	{
		return angle;
	}

	/**
	 * Get index of illumination direction.
	 *
	 * @return illumination direction index
	 */
	public int getIllumination()
	{
		return illumination;
	}

	/**
	 * Get channel index.
	 *
	 * @return channel index
	 */
	public int getChannel()
	{
		return channel;
	}

	/**
	 * Get width of stack slice in pixels.
	 *
	 * @return width in pixels
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * Get height of stack slice in pixels.
	 *
	 * @return height in pixels
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * Get number of slices.
	 *
	 * @return number of slices
	 */
	public int getDepth()
	{
		return depth;
	}

	/**
	 * Get width of a pixel in um.
	 *
	 * @return width of a pixel in um
	 */
	public double getPixelWidth()
	{
		return pixelWidth;
	}

	/**
	 * Get height of a pixel in um.
	 *
	 * @return height of a pixel in um
	 */
	public double getPixelHeight()
	{
		return pixelHeight;
	}

	/**
	 * Get depth of a pixel in um.
	 *
	 * @return depth of a pixel in um
	 */
	public double getPixelDepth()
	{
		return pixelDepth;
	}

	public static ViewSetup fromXml( final Element setup )
	{
		final int id = Integer.parseInt( setup.getElementsByTagName( "id" ).item( 0 ).getTextContent() );
		final int angle = Integer.parseInt( setup.getElementsByTagName( "angle" ).item( 0 ).getTextContent() );
		final int illumination = Integer.parseInt( setup.getElementsByTagName( "illumination" ).item( 0 ).getTextContent() );
		final int channel = Integer.parseInt( setup.getElementsByTagName( "channel" ).item( 0 ).getTextContent() );

		final int w = Integer.parseInt( setup.getElementsByTagName( "width" ).item( 0 ).getTextContent() );
		final int h = Integer.parseInt( setup.getElementsByTagName( "height" ).item( 0 ).getTextContent() );
		final int d = Integer.parseInt( setup.getElementsByTagName( "depth" ).item( 0 ).getTextContent() );

		final double pw = Double.parseDouble( setup.getElementsByTagName( "pixelWidth" ).item( 0 ).getTextContent() );
		final double ph = Double.parseDouble( setup.getElementsByTagName( "pixelHeight" ).item( 0 ).getTextContent() );
		final double pd = Double.parseDouble( setup.getElementsByTagName( "pixelDepth" ).item( 0 ).getTextContent() );

		return new ViewSetup( id, angle, illumination, channel, w, h, d, pw, ph, pd );
	}

	public static Element toXml( final Document doc, final ViewSetup setup )
	{
		final Element elem = doc.createElement( "ViewSetup" );

		elem.appendChild( XmlHelpers.intElement( doc, "id", setup.getId() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "angle", setup.getAngle() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "illumination", setup.getIllumination() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "channel", setup.getChannel() ) );

		elem.appendChild( XmlHelpers.intElement( doc, "width", setup.getWidth() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "height", setup.getHeight() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "depth", setup.getDepth() ) );

		elem.appendChild( XmlHelpers.doubleElement( doc, "pixelWidth", setup.getPixelWidth() ) );
		elem.appendChild( XmlHelpers.doubleElement( doc, "pixelHeight", setup.getPixelHeight() ) );
		elem.appendChild( XmlHelpers.doubleElement( doc, "pixelDepth", setup.getPixelDepth() ) );

		return elem;
	}

	@Override
	public int compareTo( final ViewSetup o )
	{
		return id - o.id;
	}
}
