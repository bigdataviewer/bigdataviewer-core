package bdv.viewer.render;

import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;

class SourceRenderInfo
{
	private final SourceAndConverter< ? > source;

	private MipmapOrdering.MipmapHints mipmapHints;

	private SourceAndConverter< ? extends Volatile< ? > > volatileSource;

	SourceRenderInfo( final SourceAndConverter< ? > source )
	{
		this.source = source;
	}

	/**
	 * Returns the {@code SourceAndConverter} to which this RenderInfo is
	 * associated.
	 */
	public SourceAndConverter< ? > source()
	{
		return source;
	}

	/**
	 * Returns {@code true} if this source should be rendered as volatile (that
	 * means, using {@code VolatileHierarchyProjector}.
	 * <p>
	 * If {@code renderVolatile()==true},
	 * then {@link #getVolatileSource()} can be used to retrieve the volatile
	 * version of {@link #source()}, and {@link #getRenderSource()} will also
	 * return this volatile version,
	 */
	public boolean renderVolatile()
	{
		return volatileSource != null;
	}

	public MipmapOrdering.MipmapHints getMipmapHints()
	{
		return mipmapHints;
	}

	public void setMipmapHints( final MipmapOrdering.MipmapHints mipmapHints )
	{
		this.mipmapHints = mipmapHints;
	}

	public SourceAndConverter< ? extends Volatile< ? > > getVolatileSource()
	{
		return volatileSource;
	}

	public void setVolatileSource( final SourceAndConverter< ? extends Volatile< ? > > volatileSource )
	{
		this.volatileSource = volatileSource;
	}

	public SourceAndConverter< ? > getRenderSource()
	{
		return renderVolatile() ? volatileSource : source;
	}
}
