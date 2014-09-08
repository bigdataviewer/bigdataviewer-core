package bdv.util;

import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;

/**
 * A {@link Source} wrapping some {@link RealRandomAccessible}. Derived concrete
 * classes must implement {@link #getInterval(int, int)} to specify which
 * interval to use for {@link #getSource(int, int)}.
 *
 * @param <T>
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public abstract class RealRandomAccessibleSource< T extends Type< T > > implements Source< T >
{
	protected RealRandomAccessible< T > accessible;

	protected final T type;

	protected final String name;

	protected final VoxelDimensions voxelDimensions;

	protected final AffineTransform3D identity;

	public RealRandomAccessibleSource( final RealRandomAccessible< T > accessible, final String name )
	{
		this( accessible, name, null );
	}

	public RealRandomAccessibleSource( final RealRandomAccessible< T > accessible, final String name, final VoxelDimensions voxelDimensions )
	{
		this.accessible = accessible;
		this.type = Util.getTypeFromRealRandomAccess( accessible ).createVariable();
		this.name = name;
		this.voxelDimensions = voxelDimensions;
		this.identity = new AffineTransform3D();
	}

	@Override
	public boolean isPresent( final int t )
	{
		return true;
	}

	/**
	 * Return the interval which should be used to bound the wrapped
	 * (unbounded) {@link RealRandomAccessible} for {@link #getSource(int, int)}.
	 *
	 * @return image interval
	 */
	public abstract Interval getInterval( final int t, final int level );

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return Views.interval( Views.raster( accessible ), getInterval( t, level ) );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return accessible;
	}

	@Override
	public AffineTransform3D getSourceTransform( final int t, final int level )
	{
		return identity;
	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 1;
	}
}
