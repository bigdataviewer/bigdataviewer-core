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
// TODO clean up after spim_data switch
public class MipmapInfo
{
	/**
	 * subsampling factors. indexed by mipmap level, dimension.
	 */
	private final double[][] resolutions;

	private final AffineTransform3D[] transforms;

	/**
	 * subdivision block sizes. indexed by mipmap level, dimension.
	 */
	private final int[][] subdivisions;

	private final int maxLevel;

	public MipmapInfo( final double[][] resolutions, final AffineTransform3D[] transforms, final int[][] subdivisions )
	{
		this.resolutions = resolutions;
		this.transforms = transforms;
		this.subdivisions = subdivisions;
		this.maxLevel = resolutions.length - 1;
	}

	public double[][] getResolutions()
	{
		return resolutions;
	}

	public AffineTransform3D[] getTransforms()
	{
		return transforms;
	}

	public int[][] getSubdivisions()
	{
		return subdivisions;
	}

	public int getMaxLevel()
	{
		return maxLevel;
	}

	public int getNumLevels()
	{
		return maxLevel + 1;
	}
}