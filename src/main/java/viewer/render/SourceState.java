package viewer.render;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.LinAlgHelpers;

/**
 * Source with some attached state needed for rendering.
 */
public class SourceState< T extends NumericType< T > > extends SourceAndConverter< T >
{
	/**
	 * Whether the source is active (visible in  {@link DisplayMode#FUSED} mode).
	 */
	protected boolean isActive;

	/**
	 * Whether the source is current.
	 */
	protected boolean isCurrent;

	public SourceState( final SourceAndConverter< T > soc )
	{
		super( soc );
		isActive = true;
		isCurrent = false;
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected SourceState( final SourceState< T > s )
	{
		super( s );
		isActive = s.isActive;
		isCurrent = s.isCurrent;
	}

	public SourceState< T > copy()
	{
		return new SourceState< T >( this );
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

