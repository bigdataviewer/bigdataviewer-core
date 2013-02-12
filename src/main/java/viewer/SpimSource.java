package viewer;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import viewer.refactor.Interpolation;

/**
 * Provides image data for all time-points of one view setup.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public interface SpimSource< T >
{
	/**
	 * Is there a stack at timepoint t?
	 *
	 * @param t
	 *            timepoint index
	 * @return true, if there is data for timepoint t.
	 */
	public boolean isPresent( int t );

	/**
	 * Get the 3D stack at timepoint t.
	 *
	 * @param t
	 *            timepoint index
	 * @param level
	 * 			  mipmap level
	 * @return the {@link RandomAccessibleInterval stack}.
	 */
	public RandomAccessibleInterval< T > getSource( int t, int level );

	/**
	 * Get the 3D stack at timepoint t, extended to infinity and interpolated.
	 *
	 * @param t
	 *            timepoint index
	 * @param level
	 * 			  mipmap level
	 * @param method
	 * 			  interpolation method to use
	 * @return the extended and interpolated {@link RandomAccessible stack}.
	 */
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method );

	/**
	 * Get the transform from the {@link #getSource(long) source} at timepoint t
	 * into the global coordinate system.
	 *
	 * @param t
	 *            timepoint index
	 * @param level
	 * 			  mipmap level
	 * @return transforms source into the global coordinate system.
	 */
	public AffineTransform3D getSourceTransform( int t, int level );

	/**
	 * Get an instance of the pixel type.
	 * @return instance of pixel type.
	 */
	public T getType();

	public String getName();
}
