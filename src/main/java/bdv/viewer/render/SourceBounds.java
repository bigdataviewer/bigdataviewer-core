package bdv.viewer.render;

import bdv.viewer.SourceAndConverter;

/**
 * A {@code SourceAndConverter} and its 2D bounding box.
 */
class SourceBounds
{
	private final SourceAndConverter< ? > source;

	private final int minX;
	private final int minY;
	private final int maxX;
	private final int maxY;

	public SourceBounds( final SourceAndConverter< ? > source, final int minX, final int minY, final int maxX, final int maxY )
	{
		this.source = source;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public SourceAndConverter< ? > source()
	{
		return source;
	}

	public int minX()
	{
		return minX;
	}

	public int minY()
	{
		return minY;
	}

	public int maxX()
	{
		return maxX;
	}

	public int maxY()
	{
		return maxY;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder( "SourceBounds{" );
		sb.append( "\"" ).append( source.getSpimSource().getName() ).append( "\" " );
		sb.append( "(" ).append( minX );
		sb.append( ", " ).append( minY );
		sb.append( ") -- (" ).append( maxX );
		sb.append( ", " ).append( maxY );
		sb.append( ")}" );
		return sb.toString();
	}
}
