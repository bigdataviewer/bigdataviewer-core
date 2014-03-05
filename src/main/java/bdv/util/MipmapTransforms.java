package bdv.util;

import net.imglib2.realtransform.AffineTransform3D;

public class MipmapTransforms
{
	/**
	 * Compute the transformation (scale and shift) that maps from coordinates
	 * in a down-scaled image to coordinates in the original image. This assumes
	 * that each down-scaled pixel is the average of a block of blocks of pixels
	 * in the original image. For down-scaling by a factor of 2, pixel (0,0,0)
	 * in the down-scaled image is the average of the 8 pixel block from (0,0,0)
	 * to (1,1,1) in the original image.
	 *
	 * @param resolution
	 *            the down-scaling factors in each dimension. {4,4,2} means
	 *            every pixel in the down-scaled image corresponds to a 4x4x2
	 *            pixel block in the original image.
	 * @return transformation from down-scaled image to original image.
	 */
	public static AffineTransform3D getMipmapTransformDefault( final double[] resolution )
	{
		assert resolution.length == 3;
		final AffineTransform3D mipmapTransform = new AffineTransform3D();
		for ( int d = 0; d < 3; ++d )
		{
			mipmapTransform.set( resolution[ d ], d, d );
			mipmapTransform.set( 0.5 * ( resolution[ d ] - 1 ), d, 3 );
		}
		return mipmapTransform;
	}
}
