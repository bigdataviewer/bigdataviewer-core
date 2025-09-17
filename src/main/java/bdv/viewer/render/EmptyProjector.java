/*
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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import bdv.viewer.render.ProjectorUtils.ArrayData;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.operators.SetZero;
import net.imglib2.util.StopWatch;

public class EmptyProjector< T extends NumericType< T> > implements VolatileProjector
{
	private final RandomAccessibleInterval< T > target;

	/**
	 * Time needed for rendering the last frame, in nano-seconds.
	 */
	private long lastFrameRenderNanoTime;

	private final AtomicBoolean canceled = new AtomicBoolean();

	private boolean valid = false;

	public EmptyProjector( final RandomAccessibleInterval< T > screenImage )
	{
		this.target = screenImage;
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		if ( canceled.get() )
			return false;

		final StopWatch stopWatch = StopWatch.createAndStart();
		if ( clearUntouchedTargetPixels )
		{
			final ArrayData array = ProjectorUtils.getARGBArrayData( target );
			if ( array != null )
			{
				for ( int y = 0; y < array.height(); ++y )
				{
					final int from = ( y + array.oy() ) * array.stride() + array.ox();
					Arrays.fill( array.data(), from, from + array.width(), 0 );
				}
			}
			else
			{
				target.forEach( SetZero::setZero );
			}
		}
		lastFrameRenderNanoTime = stopWatch.nanoTime();
		valid = true;
		return true;
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	@Override
	public void cancel()
	{
	}

	@Override
	public boolean isValid()
	{
		return valid;
	}
}
