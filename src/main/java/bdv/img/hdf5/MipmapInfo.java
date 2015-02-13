package bdv.img.hdf5;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Description of available mipmap levels for a {@link BasicViewSetup}.
 * Contains for each mipmap level, the subsampling factors and subdivision
 * block sizes.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class MipmapInfo
{
	/**
	 * subsampling factors. indexed by mipmap level, dimension.
	 */
	private final double[][] resolutions;

	/**
	 * transformation from coordinates of subsampled image to full resolution.
	 * indexed by mipmap level.
	 */
	private final AffineTransform3D[] transforms;

	/**
	 * subdivision block sizes. indexed by mipmap level, dimension.
	 */
	private final int[][] subdivisions;

	/**
	 * maximum mipmap level.
	 */
	private final int maxLevel;

	public MipmapInfo( final double[][] resolutions, final AffineTransform3D[] transforms, final int[][] subdivisions )
	{
		this.resolutions = resolutions;
		this.transforms = transforms;
		this.subdivisions = subdivisions;
		this.maxLevel = resolutions.length - 1;
	}

	/**
	 * Get the subsampling factors, indexed by mipmap level and dimension. For
	 * example, a subsampling factor of 2 means the respective mipmap level is
	 * scaled by 0.5 in the respective dimension.
	 */
	public double[][] getResolutions()
	{
		return resolutions;
	}

	/**
	 * Get the transformation from coordinates of the subsampled image of a
	 * mipmap level to coordinates of the full resolution image. The array of
	 * transforms is indexed by mipmap level.
	 */
	public AffineTransform3D[] getTransforms()
	{
		return transforms;
	}

	/**
	 * Get the subdivision block sizes, indexed by mipmap level and dimension.
	 */
	public int[][] getSubdivisions()
	{
		return subdivisions;
	}

	/**
	 * Get the maximum mipmap level.
	 */
	public int getMaxLevel()
	{
		return maxLevel;
	}

	/**
	 * Get the number of mipmap levels ({@link #getMaxLevel()} + 1).
	 * @return
	 */
	public int getNumLevels()
	{
		return maxLevel + 1;
	}
}
