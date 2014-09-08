package bdv.viewer;

import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Provides image data for all timepoints of one view setup.
 *
 * Note that (partly for convenience and partly for historical reasons) the
 * time-point <em>index</em> is used here instead of the timepoint
 * {@link TimePoint#getId() id}. This timepoint index is an index into the
 * ordered list of timepoints {@link TimePoints#getTimePointsOrdered()}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public interface Source< T >
{
	/**
	 * Is there a stack at timepoint index t?
	 *
	 * @param t
	 *            timepoint index
	 * @return true, if there is data for timepoint index t.
	 */
	public boolean isPresent( int t );

	/**
	 * Get the 3D stack at timepoint index t.
	 *
	 * @param t
	 *            timepoint index
	 * @param level
	 * 			  mipmap level
	 * @return the {@link RandomAccessibleInterval stack}.
	 */
	public RandomAccessibleInterval< T > getSource( int t, int level );

	/**
	 * Get the 3D stack at timepoint index t, extended to infinity and interpolated.
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
	 * Get the transform from the {@link #getSource(long) source} at timepoint index t
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

	/**
	 * Get the name of the source.
	 * @return the name of the source.
	 */
	public String getName();

	/**
	 * Get voxel size and unit for this source. May return null.
	 *
	 * @return voxel size and unit or {@code null}.
	 */
	public VoxelDimensions getVoxelDimensions();

	public int getNumMipmapLevels();
}
