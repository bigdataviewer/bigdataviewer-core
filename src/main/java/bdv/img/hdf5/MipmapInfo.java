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
package bdv.img.hdf5;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Description of available mipmap levels for a {@link BasicViewSetup}.
 * Contains for each mipmap level, the subsampling factors and subdivision
 * block sizes.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
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
