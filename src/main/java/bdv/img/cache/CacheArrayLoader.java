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
package bdv.img.cache;

import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import bdv.ViewerImgLoader;
import bdv.img.catmaid.CatmaidImageLoader;

/**
 * Provider of {@link VolatileCell} data. This is implemented by data back-ends
 * to the {@link VolatileGlobalCellCache}.
 *
 * @param <A>
 *            type of access to cell data, currently always a
 *            {@link VolatileAccess}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface CacheArrayLoader< A >
{
	/**
	 * How many bytes does one element (voxel) occupy? This is used only for
	 * statistics, i.e., estimating I/O bandwidth. Implementing classes can
	 * return an estimate if element size is varying or unknown.
	 *
	 * @return number of bytes required to store one element.
	 */
	public int getBytesPerElement();

	/**
	 * Load cell data into memory. This method blocks until data is successfully
	 * loaded. If it completes normally, the returned data is always valid. If
	 * anything goes wrong, an {@link InterruptedException} is thrown.
	 *
	 * <p>
	 * Parameters specify the image within the data set and the location of the
	 * cell within the image: {@code timepoint}, {@code setup}, and
	 * {@code level} define the stack, where setup is a combination of angle,
	 * channel, etc., and level is the resolution level (for multi-resolution
	 * data). {@code dimensions} is the size of the block to load (in voxels).
	 * {@code min} is the starting coordinate of the block in the stack (in
	 * voxels).
	 *
	 * <p>
	 * Usually, the {@link CacheArrayLoader} interface is implemented in
	 * conjunction with implementing a {@link ViewerImgLoader} for a BDV data
	 * back-end. You do not need to be able to load blocks of arbitrary sizes
	 * and offsets here -- just the ones that you will use from the images
	 * returned by your {@link ViewerImgLoader}. For an example, look at
	 * {@link CatmaidImageLoader}. There, the blockDimensions are defined in the
	 * constructor, according to the tile size of the data set. These
	 * blockDimensions are then used for every image that the
	 * {@link CatmaidImageLoader} provides. Therefore, all calls to
	 * {@link #loadArray(int, int, int, int[], long[])} will have predictable
	 * {@code dimensions} (corresponding to tile size of the data set) and
	 * {@code min} offsets (multiples of the tile size).
	 *
	 * <p>
	 * The only exception to this rule is at the (max) border of the stack. The
	 * boundary cells may have a truncated shape. For example, if your image
	 * size is <em>20x20x1</em> and your cell size is <em>16x16x1</em>, you will
	 * have cell sizes <br/>
	 * <em>(16x16x1)</em>, <em>(4x16x1)</em> <br/>
	 * <em>(16x4x1)</em>, <em>(4x4x1)</em><br/>
	 *
	 * @param timepoint
	 *            the timepoint of the stack.
	 * @param setup
	 *            the setup of the stack.
	 * @param level
	 *            the resolution level of the stack (0 for full resolution).
	 * @param dimensions
	 *            the size of the block to load (in voxels).
	 * @param min
	 *            the min coordinate of the block in the stack (in voxels).
	 * @return loaded cell data.
	 */
	public A loadArray( final int timepoint, final int setup, final int level, int[] dimensions, long[] min ) throws InterruptedException;

	/**
	 * Return empty cell data. Usually, the return value {@code A} is a
	 * {@link VolatileAccess}. In this case, the returned data should be invalid
	 * (see {@link VolatileAccess#isValid()}). It is okay to return the same
	 * (empty and invalid) data for multiple calls to this method.
	 *
	 * @param dimensions
	 *            the size of the data block to return.
	 * @return empty (and invalid) cell data.
	 */
	public A emptyArray( final int[] dimensions );
}
