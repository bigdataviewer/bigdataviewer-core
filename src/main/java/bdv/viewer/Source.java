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
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
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
	 * Get the transform from the {@link #getSource(int, int) source} at the
	 * given timepoint index and mipmap level into the global coordinate system.
	 *
	 * @param t
	 *            timepoint index
	 * @param level
	 *            mipmap level
	 * @param transform
	 *            is set to the source-to-global transform, that transforms
	 *            source coordinates into the global coordinates
	 */
	public void getSourceTransform( int t, int level, AffineTransform3D transform );

	@Deprecated
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
