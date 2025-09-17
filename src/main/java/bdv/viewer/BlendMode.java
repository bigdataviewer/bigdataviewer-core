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
package bdv.viewer;

import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.AlphaWeightedAccumulateProjectorARGB;
import net.imglib2.type.numeric.ARGBType;

/**
 * Blending modes.
 * <p>
 * The {@code BlendMode} is determined by the {@link AccumulateProjectorFactory}
 * used for combining overlapping sources into the final screen image.
 */
public enum BlendMode
{
	SUM( "sum sources" ),
	AVG( "average sources" ),
	CUSTOM( "custom source accumulator" );

	private final String name;

	BlendMode( final String name )
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public static BlendMode of( final AccumulateProjectorFactory< ARGBType > factory )
	{
		if ( factory instanceof AccumulateProjectorARGB.Factory )
			return BlendMode.SUM;
		else if ( factory instanceof AlphaWeightedAccumulateProjectorARGB.Factory )
			return BlendMode.AVG;
		else
			return BlendMode.CUSTOM;
	}
}
