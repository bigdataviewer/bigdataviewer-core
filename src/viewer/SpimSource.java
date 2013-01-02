package viewer;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;

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
	 * 			  mip level
	 * @return the {@link RandomAccessibleInterval stack}.
	 */
	public RandomAccessibleInterval< T > getSource( int t, int level );

	/**
	 * Get the transform from the {@link #getSource(long) source} at timepoint t
	 * into the global coordinate system.
	 *
	 * @param t
	 *            timepoint index
	 * @param level
	 * 			  mip level
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
