package mpicbg.spim.data;

import org.jdom2.Element;

/**
 * A collection of parameters describing the setup for a particular view, e.g.
 * angle, illumination direction, etc.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class ViewSetup implements Comparable< ViewSetup >
{
	/**
	 * This unique id is the index of this {@link ViewSetup} in {@link SequenceDescription#setups}.
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
	 * Load a ViewSetup from an XML file.
	 *
	 * @param elem
	 *            The "ViewSetup" DOM element.
	 */
	public ViewSetup( final Element elem )
	{
		this(
				Integer.parseInt( elem.getChildText( "id" ) ),
				Integer.parseInt( elem.getChildText( "angle" ) ),
				Integer.parseInt( elem.getChildText( "illumination" ) ),
				Integer.parseInt( elem.getChildText( "channel" ) ),

				Integer.parseInt( elem.getChildText( "width" ) ),
				Integer.parseInt( elem.getChildText( "height" ) ),
				Integer.parseInt( elem.getChildText( "depth" ) ),

				Double.parseDouble( elem.getChildText( "pixelWidth" ) ),
				Double.parseDouble( elem.getChildText( "pixelHeight" ) ),
				Double.parseDouble( elem.getChildText( "pixelDepth" ) )
		);
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

	public Element toXml()
	{
		final Element elem = new Element( "ViewSetup" );

		elem.addContent( XmlHelpers.intElement( "id", getId() ) );
		elem.addContent( XmlHelpers.intElement( "angle", getAngle() ) );
		elem.addContent( XmlHelpers.intElement( "illumination", getIllumination() ) );
		elem.addContent( XmlHelpers.intElement( "channel", getChannel() ) );

		elem.addContent( XmlHelpers.intElement( "width", getWidth() ) );
		elem.addContent( XmlHelpers.intElement( "height", getHeight() ) );
		elem.addContent( XmlHelpers.intElement( "depth", getDepth() ) );

		elem.addContent( XmlHelpers.doubleElement( "pixelWidth", getPixelWidth() ) );
		elem.addContent( XmlHelpers.doubleElement( "pixelHeight", getPixelHeight() ) );
		elem.addContent( XmlHelpers.doubleElement( "pixelDepth", getPixelDepth() ) );

		return elem;
	}

	@Override
	public int compareTo( final ViewSetup o )
	{
		return id - o.id;
	}
}
