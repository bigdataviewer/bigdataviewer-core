/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
package bdv.tools.boundingbox;

import bdv.util.ModifiableRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

public class TransformedRealBoxModel extends AbstractTransformedBoxModel implements RealBoxSelectionPanel.RealBox
{
	private final ModifiableRealInterval interval;

	public TransformedRealBoxModel(
			final RealInterval interval,
			final AffineTransform3D transform )
	{
		this( new ModifiableRealInterval( interval ), transform );
	}

	public TransformedRealBoxModel(
			final ModifiableRealInterval interval,
			final AffineTransform3D transform )
	{
		super( transform );
		this.interval = interval;
	}

	@Override
	public RealInterval getInterval()
	{
		return interval;
	}

	@Override
	public void setInterval( final RealInterval i )
	{
		if ( ! equals( interval, i ) )
		{
			interval.set( i );
			notifyIntervalChanged();
		}
	}

	private static boolean equals( final RealInterval a, final RealInterval b )
	{
		if ( a.numDimensions() != b.numDimensions() )
			return false;

		for ( int d = 0; d < a.numDimensions(); ++d )
			if ( a.realMin( d ) != b.realMin( d ) || a.realMax( d ) != b.realMax( d ) )
				return false;

		return true;
	}
}
