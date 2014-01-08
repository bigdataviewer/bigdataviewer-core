package bdv.img.hdf5;

import org.jdom2.Element;

public class ImageDimension
{
	protected final int timepoint;
	protected final int setup;
	protected final int level;
	protected final long[] dimensions;

	/**
	 * Load a ImageDimension from an XML file.
	 *
	 * @param elem
	 *            The "dimension" DOM element.
	 */
	public ImageDimension( final Element elem )
	{

		timepoint = Integer.parseInt( elem.getAttributeValue( "timepoint" ) );
		setup = Integer.parseInt( elem.getAttributeValue( "setup" ) );
		level = Integer.parseInt( elem.getAttributeValue( "level" ) );
		dimensions = new long[ 3 ];
		final String data = elem.getText();
		final String[] fields = data.split( "\\s+" );
		for ( int i = 0; i < 3; ++i )
			dimensions[ i ] = Long.parseLong( fields[ i ] );
	}

	public Element toXml()
	{
		return toXml( timepoint, setup, level, dimensions );
	}

	public static Element toXml( final int timepoint, final int setup, final int level, final long[] dimensions )
	{
		final Element elem = new Element( "dimension" );
		elem.setAttribute( "timepoint", Integer.toString( timepoint ) );
		elem.setAttribute( "setup", Integer.toString( setup ) );
		elem.setAttribute( "level", Integer.toString( level ) );
		elem.setText( dimensions[ 0 ] + " " + dimensions[ 1 ] + " " + dimensions[ 2 ] );
		return elem;
	}

	public static Element imageDimensionsToXml( final Hdf5ImageLoader imgLoader )
	{
		final Element elem = new Element( "ImageDimensions" );
		for ( int t = 0; t < imgLoader.numTimepoints; ++t )
			for ( int s = 0; s < imgLoader.numSetups; ++s )
				for ( int l = 0; l <= imgLoader.maxLevels[ s ]; ++l )
					elem.addContent( toXml( t, s, l, imgLoader.getImageDimension( t, s, l ) ) );
		return elem;
	}

	public int getTimepoint()
	{
		return timepoint;
	}

	public int getSetup()
	{
		return setup;
	}

	public int getLevel()
	{
		return level;
	}

	public long[] getDimensions()
	{
		return dimensions;
	}
}
