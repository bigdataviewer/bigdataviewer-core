package viewer.render;

import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

/**
 * Source with some attached state needed for rendering.
 */
public class SourceState< T > extends SourceAndConverter< T >
{
	protected static class Data
	{
		/**
		 * Whether the source is active (visible in  {@link DisplayMode#FUSED} mode).
		 */
		protected boolean isActive;

		/**
		 * Whether the source is current.
		 */
		protected boolean isCurrent;

		public Data()
		{
			isActive = true;
			isCurrent = false;
		}

		protected Data( final Data d )
		{
			isActive = d.isActive;
			isCurrent = d.isCurrent;
		}

		public Data copy()
		{
			return new Data( this );
		}
	}

	static class VolatileSourceState< T, V extends Volatile< T > > extends SourceState< V >
	{
		public VolatileSourceState( final SourceAndConverter< V > soc, final Data data )
		{
			super( soc, data );
		}

		public static < T, V extends Volatile< T > > VolatileSourceState< T, V > create( final SourceAndConverter< V > soc, final Data data )
		{
			if ( soc == null )
				return null;
			else
				return new VolatileSourceState< T, V >( soc, data );
		}
	}

	final Data data;

	final VolatileSourceState< T, ? extends Volatile< T > > volatileSourceState;

	public SourceState( final SourceAndConverter< T > soc )
	{
		super( soc );
		data = new Data();
		volatileSourceState = VolatileSourceState.create( soc.volatileSourceAndConverter, data );
	}

	protected SourceState( final SourceAndConverter< T > soc, final Data data )
	{
		super( soc );
		this.data = data;
		volatileSourceState = VolatileSourceState.create( soc.volatileSourceAndConverter, data );
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected SourceState( final SourceState< T > s )
	{
		super( s );
		data = s.data.copy();
		volatileSourceState = VolatileSourceState.create( s.volatileSourceAndConverter, data );
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
		return data.isActive;
	}

	/**
	 * Set the source active (visible in fused mode) or inactive
	 */
	public void setActive( final boolean isActive )
	{
		data.isActive = isActive;
	}

	/**
	 * Is this source the current source?
	 *
	 * @return whether the source is current.
	 */
	public boolean isCurrent()
	{
		return data.isCurrent;
	}

	/**
	 * Set this source current (or not).
	 */
	public void setCurrent( final boolean isCurrent )
	{
		data.isCurrent = isCurrent;
	}

	/**
	 * Create a {@link SourceState} from a {@link SourceAndConverter}.
	 */
	public static < T > SourceState< T > create( final SourceAndConverter< T > soc )
	{
		return new SourceState< T >( soc );
	}

	@Override
	public SourceState< ? extends Volatile< T > > asVolatile()
	{
		return volatileSourceState;
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

