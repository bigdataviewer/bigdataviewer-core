/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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

import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.DragBehaviour;

/**
 * Modify a {@link AbstractTransformedBoxModel} by dragging its corners.
 * Tied to a {@link TransformedBoxOverlay} for determining box corner projections.
 *
 * @author Tobias Pietzsch
 */
final class DragBoxCornerBehaviour implements DragBehaviour
{
	private final TransformedBoxOverlay boxOverlay;

	private boolean moving = false;

	private final AbstractTransformedBoxModel model;

	private final double[] initMin = new double[ 3 ];

	private final double[] initMax = new double[ 3 ];

	private final double[] initCorner = new double[ 3 ];

	private int cornerId;

	public DragBoxCornerBehaviour( final TransformedBoxOverlay boxOverlay, final AbstractTransformedBoxModel model )
	{
		this.boxOverlay = boxOverlay;
		this.model = model;
	}

	@Override
	public void init( final int x, final int y )
	{
		cornerId = boxOverlay.getHighlightedCornerIndex();
		if ( cornerId < 0 )
			return;

		final RealInterval interval = model.getInterval();
		IntervalCorners.corner( interval, cornerId, initCorner );
		interval.realMin( initMin );
		interval.realMax( initMax );

		moving = true;
	}

	private final AffineTransform3D transform = new AffineTransform3D();

	@Override
	public void drag( final int x, final int y )
	{
		if ( !moving )
			return;

		boxOverlay.getBoxToViewerTransform( transform );
		final double[] gPos = new double[ 3 ];
		transform.apply( initCorner, gPos );
		final double[] lPos = boxOverlay.renderBoxHelper.reproject( x, y, gPos[ 2 ] );
		transform.applyInverse( gPos, lPos );

		final double[] min = new double[ 3 ];
		final double[] max = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			final double p = gPos[ d ];
			if ( ( cornerId & ( 1 << d ) ) == 0 )
			{
				min[ d ] = p;
				max[ d ] = initMax[ d ] = Math.max( initMax[ d ], p );
			}
			else
			{
				min[ d ] = initMin[ d ] = Math.min( initMin[ d ], p );
				max[ d ] = p;
			}
		}

		model.setInterval( new FinalRealInterval( min, max ) );
	}

	@Override
	public void end( final int x, final int y )
	{
		moving = false;
	}
}
