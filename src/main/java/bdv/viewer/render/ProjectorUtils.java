/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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

import net.imglib2.RandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;

public class ProjectorUtils
{
	/**
	 * Extracts the underlying {@code int[]} array in case {@code img} is a
	 * standard {@code ArrayImg<ARGBType>}. This supports certain (optional)
	 * optimizations in projector implementations.
	 *
	 * @return the underlying {@code int[]} array of {@code img}, if it is a
	 * standard {@code ArrayImg<ARGBType>}. Otherwise {@code null}.
	 */
	public static int[] getARGBArrayImgData( final RandomAccessible< ? > img )
	{
		if ( ! ( img instanceof ArrayImg ) )
			return null;
		final ArrayImg< ?, ? > aimg = ( ArrayImg< ?, ? > ) img;
		if( ! ( aimg.firstElement() instanceof ARGBType ) )
			return null;
		final Object access = aimg.update( null );
		if ( ! ( access instanceof IntArray ) )
			return null;
		return ( ( IntArray ) access ).getCurrentStorageArray();
	}

	public static class ArrayData
	{
		private final int[] data;
		private final int ox;
		private final int oy;
		private final int width;
		private final int height;
		private final int stride;

		public ArrayData( final int[] data, final int ox, final int oy, final int width, final int height, final int stride )
		{
			this.data = data;
			this.ox = ox;
			this.oy = oy;
			this.width = width;
			this.height = height;
			this.stride = stride;
		}

		public int[] data()
		{
			return data;
		}

		public int ox()
		{
			return ox;
		}

		public int oy()
		{
			return oy;
		}

		public int width()
		{
			return width;
		}

		public int height()
		{
			return height;
		}

		public int size()
		{
			return width * height;
		}

		public int stride()
		{
			return stride;
		}

		@Override
		public String toString()
		{
			return "ArrayData{" +
					"ox=" + ox +
					", oy=" + oy +
					", width=" + width +
					", height=" + height +
					", stride=" + stride +
					'}';
		}
	}

	public static ArrayData getARGBArrayData( final RandomAccessible< ? > img )
	{
		if ( img.numDimensions() != 2 )
			return null;

		if ( img instanceof IntervalView )
		{
			final IntervalView view = ( IntervalView ) img;
			RandomAccessible source = view.getSource();
			int tx = 0;
			int ty = 0;
			if ( source instanceof MixedTransformView )
			{
				final MixedTransformView tview = ( MixedTransformView ) source;
				tx = ( int ) tview.getTransformToSource().getTranslation( 0 );
				ty = ( int ) tview.getTransformToSource().getTranslation( 1 );
				source = tview.getSource();
			}
			if ( source instanceof ArrayImg )
			{
				final ArrayImg< ?, ? > aimg = ( ArrayImg< ?, ? > ) source;
				if ( !( aimg.firstElement() instanceof ARGBType ) )
					return null;
				final Object access = aimg.update( null );
				if ( !( access instanceof IntArray ) )
					return null;
				final int[] data = ( ( IntArray ) access ).getCurrentStorageArray();
				final int ox = tx + ( int ) view.min( 0 );
				final int oy = ty + ( int ) view.min( 1 );
				final int width = ( int ) view.dimension( 0 );
				final int height = ( int ) view.dimension( 1 );
				final int stride = ( int ) aimg.dimension( 0 );
				return new ArrayData( data, ox, oy, width, height, stride );
			}
		}
		else if ( img instanceof ArrayImg )
		{
			final ArrayImg< ?, ? > aimg = ( ArrayImg< ?, ? > ) img;
			if ( !( aimg.firstElement() instanceof ARGBType ) )
				return null;
			final Object access = aimg.update( null );
			if ( !( access instanceof IntArray ) )
				return null;
			final int[] data = ( ( IntArray ) access ).getCurrentStorageArray();
			final int width = ( int ) aimg.dimension( 0 );
			final int height = ( int ) aimg.dimension( 1 );
			return new ArrayData( data, 0, 0, width, height, width );
		}
		return null;
	}
}
