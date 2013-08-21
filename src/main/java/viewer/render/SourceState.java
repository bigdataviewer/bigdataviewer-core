package viewer.render;

import static viewer.render.DisplayMode.FUSED;
import static viewer.render.DisplayMode.SINGLE;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.LinAlgHelpers;

/**
 * Source with some attached state needed for rendering.
 */
public class SourceState< T extends NumericType< T > > extends SourceAndConverter< T >
{
	/**
	 * Whether the source is active in each of the {@link DisplayMode}s (visible if the respective mode is active).
	 */
	protected boolean[] isActive;

	public SourceState( final SourceAndConverter< T > soc )
	{
		super( soc );
		isActive = new boolean[ DisplayMode.length ];
		isActive[ FUSED.id() ] = true;
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected SourceState( final SourceState< T > s )
	{
		super( s );
		isActive = s.isActive.clone();
	}

	public SourceState< T > copy()
	{
		return new SourceState< T >( this );
	}

	/**
	 * Is the source is active (visible in the specified mode)?
	 *
	 * @return whether the source is active.
	 */
	public boolean isActive( final DisplayMode mode )
	{
		return isActive[ mode.id() ];
	}

	/**
	 * TODO
	 * Set the source active (visible in fused mode) or inactive
	 */
	public void setActive( final DisplayMode mode, final boolean isActive )
	{
		this.isActive[ mode.id() ] = isActive;
	}

	/**
	 * Is this source the current source?
	 *
	 * @return whether the source is current.
	 */
	public boolean isCurrent()
	{
		return isActive[ SINGLE.id() ];
	}

	/**
	 * Set this source current (or not).
	 */
	public void setCurrent( final boolean isCurrent )
	{
		isActive[ SINGLE.id() ] = isCurrent;
	}

	// TODO: Should this be removed, and isActive() used instead?
	/**
	 * TODO
	 * Is the source visible? The source is visible if it is active in
	 * <em>fused-mode</em> or it is current in <em>single-source</em> mode.
	 *
	 * @param singleSourceMode
	 *            Is the display mode <em>single-source</em> (true) or
	 *            <em>fused</em> (false).
	 *
	 * @return true, if the source is visible.
	 */
	public boolean isVisible( final DisplayMode mode )
	{
		return isActive( mode );
	}

	/**
	 * Create a {@link SourceState} from a {@link SourceAndConverter}.
	 */
	public static < T extends NumericType< T > > SourceState< T > create( final SourceAndConverter< T > soc )
	{
		return new SourceState< T >( soc );
	}

	/**
	 * Compute the projected voxel size at the given screen transform and mipmap
	 * level. Take a source voxel (0,0,0)-(1,1,1) at the given mipmap level and
	 * transform it to the screen image at the given screen scale. Take the
	 * maximum of the screen extends of the transformed projected voxel edges.
	 *
	 * @param source
	 * @param screenTransform
	 *            transforms screen coordinates to global coordinates.
	 * @param timepoint
	 *            for which timepoint to query the source
	 * @param mipmapIndex
	 *            mipmap level
	 * @return pixel size
	 */
	public double getVoxelScreenSize( final AffineTransform3D screenTransform, final int timepoint, final int mipmapIndex )
	{
		double pixelSize = 0;
		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		sourceToScreen.set( screenTransform );
		sourceToScreen.concatenate( spimSource.getSourceTransform( timepoint, mipmapIndex ) );
		final double[] zero = new double[] { 0, 0, 0 };
		final double[] tzero = new double[ 3 ];
		final double[] one = new double[ 3 ];
		final double[] tone = new double[ 3 ];
		final double[] diff = new double[ 2 ];
		sourceToScreen.apply( zero, tzero );
		for ( int i = 0; i < 3; ++i )
		{
			for ( int d = 0; d < 3; ++d )
				one[ d ] = d == i ? 1 : 0;
			sourceToScreen.apply( one, tone );
			LinAlgHelpers.subtract( tone, tzero, tone );
			diff[0] = tone[0];
			diff[1] = tone[1];
			final double l = LinAlgHelpers.length( diff );
			if ( l > pixelSize )
				pixelSize = l;
		}
		return pixelSize;
	}
}

