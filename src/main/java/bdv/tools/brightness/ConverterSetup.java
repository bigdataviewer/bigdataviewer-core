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
package bdv.tools.brightness;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.type.numeric.ARGBType;

/**
 * Modify the range and color of the converter for a source. Because each source
 * can have its own converter, this is used to adjust brightness, contrast, and
 * color of individual sources.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface ConverterSetup
{
	/**
	 * Get the id of the {@link BasicViewSetup} this converter acts on.
	 *
	 * @return the id of the {@link BasicViewSetup} this converter acts on.
	 */
	public int getSetupId();

	/**
	 * Set the range of source values that is mapped to the full range of the
	 * target type. Source values outside of the specified range are clamped.
	 *
	 * @param min
	 *            source value to map to minimum of the target range.
	 * @param max
	 *            source value to map to maximum of the target range.
	 */
	public void setDisplayRange( double min, double max );

	/**
	 * Set the color for this converter.
	 */
	public void setColor( final ARGBType color );

	public boolean supportsColor();

	/**
	 * Get the (largest) source value that is mapped to the minimum of the
	 * target range.
	 *
	 * @return source value that is mapped to the minimum of the target range.
	 */
	public double getDisplayRangeMin();

	/**
	 * Get the (smallest) source value that is mapped to the maximum of the
	 * target range.
	 *
	 * @return source value that is mapped to the maximum of the target range.
	 */
	public double getDisplayRangeMax();

	/**
	 * Get the color for this converter.
	 *
	 * @return the color for this converter.
	 */
	public ARGBType getColor();
}
