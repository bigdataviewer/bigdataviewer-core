/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.util;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import bdv.viewer.Source;

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

	/**
	 * Compute the projected voxel size at the given screen transform and mipmap
	 * level of a {@link Source}. Take a source voxel (0,0,0)-(1,1,1) at the
	 * given mipmap level and transform it to the screen image at the given
	 * screen scale. Take the maximum of the screen extends of the transformed
	 * projected voxel edges.
	 *
	 * @param screenTransform
	 *            transforms screen coordinates to global coordinates.
	 * @param source
	 *            the source
	 * @param timepoint
	 *            for which timepoint to query the source
	 * @param mipmapIndex
	 *            mipmap level
	 * @return pixel size
	 */
	public static double getVoxelScreenSize( final AffineTransform3D screenTransform, final Source< ? > source, final int timepoint, final int mipmapIndex )
	{
		double pixelSize = 0;
		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		sourceToScreen.set( screenTransform );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, mipmapIndex, sourceTransform );
		sourceToScreen.concatenate( sourceTransform );
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

	/**
	 * Get the mipmap level that best matches the given screen scale for the
	 * given source. Assumes that mipmap indices in the source are ordered by
	 * decreasing resolution.
	 *
	 * @param screenTransform
	 *            transforms screen coordinates to global coordinates.
	 * @param source
	 *            the source
	 * @param timepoint
	 *            for which timepoint to query the source
	 * @return index of the best-matching mipmap level
	 */
	public static int getBestMipMapLevel( final AffineTransform3D screenTransform, final Source< ? > source, final int timepoint )
	{
		int targetLevel = source.getNumMipmapLevels() - 1;
		for ( int level = targetLevel - 1; level >= 0; level-- )
		{
			if ( getVoxelScreenSize( screenTransform, source, timepoint, level ) >= 0.99 /* 1.0 */)
				targetLevel = level;
			else
				break;
		}
		if ( targetLevel > 0 )
		{
			final double size1 = getVoxelScreenSize( screenTransform, source, timepoint, targetLevel );
			final double size0 = getVoxelScreenSize( screenTransform, source, timepoint, targetLevel - 1 );
			if ( Math.abs( size1 - 1.0 ) / 2 > Math.abs( size0 - 1.0 ) )
				targetLevel--;
		}
		return targetLevel;
	}
}
