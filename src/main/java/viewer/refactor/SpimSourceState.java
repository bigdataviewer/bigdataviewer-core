package viewer.refactor;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import viewer.SpimSource;
import viewer.SpimSourceAndConverter;

/**
 * SpimSource with some attached state needed for rendering.
 */
public class SpimSourceState< T extends NumericType< T > > extends SpimSourceAndConverter< T >
{
	/**
	 * Current transformation from {@link #source} to screen. This is
	 * a concatenation of {@link SpimSource#getSourceTransform(long)
	 * source transform}, the interactive viewer
	 * transform, and the viewer-to-screen transform.
	 *
	 * TODO: remove?
	 */
	@Deprecated
	final protected AffineTransform3D sourceToScreen;

	/**
	 * Whether the source is active (visible in fused mode).
	 */
	protected boolean isActive;

	/**
	 * Whether the source is current.
	 */
	protected boolean isCurrent;

	public SpimSourceState( final SpimSourceAndConverter< T > soc )
	{
		super( soc.getSpimSource(), soc.getConverter() );
		sourceToScreen = new AffineTransform3D();
		isActive = true;
		isCurrent = false;
	}

	/**
	 * Is the source is active (visible in fused mode)?
	 *
	 * @return whether the source is active.
	 */
	public boolean isActive()
	{
		return isActive;
	}

	/**
	 * Set the source active (visible in fused mode) or inactive
	 */
	public void setActive( final boolean isActive )
	{
		this.isActive = isActive;
	}

	/**
	 * Is this source the current source?
	 *
	 * @return whether the source is current.
	 */
	public boolean isCurrent()
	{
		return isCurrent;
	}

	/**
	 * Set this source current (or not).
	 */
	public void setCurrent( final boolean isCurrent )
	{
		this.isCurrent = isCurrent;
	}

	/**
	 * Is the source visible? The source is visible if it is active in
	 * <em>fused-mode</em> or it is current in <em>single-source</em> mode.
	 *
	 * @param singleSourceMode
	 *            Is the display mode <em>single-source</em> (true) or
	 *            <em>fused</em> (false).
	 *
	 * @return true, if the source is visible.
	 */
	public boolean isVisible( final boolean singleSourceMode )
	{
		return singleSourceMode ? isCurrent() : isActive();
	}

	/**
	 * Create a {@link SpimSourceState} from a {@link SpimSourceAndConverter}.
	 */
	public static < T extends NumericType< T > > SpimSourceState< T > create( final SpimSourceAndConverter< T > soc )
	{
		return new SpimSourceState< T >( soc );
	}
}

