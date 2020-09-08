/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package bdv.viewer.render;

import java.util.ArrayList;
import java.util.List;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.ARGBType;

/**
 * Maintains {@code byte[]} and {@code int[]} arrays for mask and intermediate images needed for rendering.
 * <p>
 * Call {@link #checkRenewData} to update number and size of arrays when number of visible sources or screen size changes.
 */
class RenderStorage
{
	/**
	 * Storage for mask images of {@link VolatileHierarchyProjector}. One array
	 * per visible source.
	 */
	private final List< byte[] > renderMaskArrays = new ArrayList<>();

	/**
	 * Storage for render images of {@link VolatileHierarchyProjector}.
	 * Used to render an individual source before combining to final target image.
	 * One array per visible source, if more than one source is visible.
	 * (If exactly one source is visible, it is rendered directly to the target image.)
	 */
	private final List< int[] > renderImageArrays = new ArrayList<>();

	public void checkRenewData( final int screenW, final int screenH, final int numVisibleSources )
	{
		final int size = screenW * screenH;
		final int currentSize = renderMaskArrays.isEmpty() ? 0 : renderMaskArrays.get( 0 ).length;
		if  ( size != currentSize )
			clear();

		while ( renderMaskArrays.size() > numVisibleSources )
			renderMaskArrays.remove( renderMaskArrays.size() - 1 );
		while ( renderMaskArrays.size() < numVisibleSources )
			renderMaskArrays.add( new byte[ size ] );

		final int numRenderImages = numVisibleSources > 1 ? numVisibleSources : 0;
		while ( renderImageArrays.size() > numRenderImages )
			renderImageArrays.remove( renderImageArrays.size() - 1 );
		while ( renderImageArrays.size() < numRenderImages )
			renderImageArrays.add( new int[ size ] );
	}

	public byte[] getMaskArray( final int index )
	{
		return renderMaskArrays.get( index );
	}

	public RandomAccessibleInterval< ARGBType > getRenderImage( final int width, final int height, final int index )
	{
		return ArrayImgs.argbs( renderImageArrays.get( index ), width, height );
	}

	public void clear()
	{
		renderMaskArrays.clear();
		renderImageArrays.clear();
	}
}
