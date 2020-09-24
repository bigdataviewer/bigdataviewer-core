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

import bdv.util.MipmapTransforms;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import java.util.List;
import java.util.Set;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;

class VisibilityUtils
{
	/**
	 * Compute a list of sources are currently visible on screen.
	 * <p>
	 * This means that the sources
	 * <ul>
	 *     <li>are visible in the given {@code ViewerState}, and,</li>
	 *     <li>when transformed to viewer coordinates, overlap the screen area.</li>
	 * </ul>
	 * The returned list of sources is sorted by {@code viewerState.sourceOrder()}.
	 *
	 * @param viewerState
	 * 		specifies sources, transform, and current timepoint
	 * @param screenScale
	 * 		specifies screen size and scale transform
	 * @param result
	 * 		list of currently visible sources is stored here
	 */
	static void computeVisibleSourcesOnScreen(
			final ViewerState viewerState,
			final ScreenScales.ScreenScale screenScale,
			final List< SourceAndConverter< ? > > result )
	{
		result.clear();

		final int screenMinX = 0;
		final int screenMinY = 0;
		final int screenMaxX = screenScale.width() - 1;
		final int screenMaxY = screenScale.height() - 1;

		final AffineTransform3D screenTransform = viewerState.getViewerTransform();
		screenTransform.preConcatenate( screenScale.scaleTransform() );

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		final double[] sourceMin = new double[ 3 ];
		final double[] sourceMax = new double[ 3 ];

		final Set< SourceAndConverter< ? > > sources = viewerState.getVisibleAndPresentSources();
		final int t = viewerState.getCurrentTimepoint();
		final double expand = viewerState.getInterpolation() == Interpolation.NEARESTNEIGHBOR ? 0.5 : 1.0;

		for ( final SourceAndConverter< ? > source : sources )
		{
			if( !source.getSpimSource().doBoundingBoxCulling() )
			{
				result.add( source );
				continue;
			}

			final Source< ? > spimSource = source.getSpimSource();
			final int level = MipmapTransforms.getBestMipMapLevel( screenTransform, spimSource, t );
			spimSource.getSourceTransform( t, level, sourceToScreen );
			sourceToScreen.preConcatenate( screenTransform );

			final Interval interval = spimSource.getSource( t, level );
			for ( int d = 0; d < 3; d++ )
			{
				sourceMin[ d ] = interval.realMin( d ) - expand;
				sourceMax[ d ] = interval.realMax( d ) + expand;
			}
			final FinalRealInterval bb = sourceToScreen.estimateBounds( new FinalRealInterval( sourceMin, sourceMax ) );

			if ( bb.realMax( 0 ) >= screenMinX
					&& bb.realMin( 0 ) <= screenMaxX
					&& bb.realMax( 1 ) >= screenMinY
					&& bb.realMin( 1 ) <= screenMaxY
					&& bb.realMax( 2 ) >= 0
					&& bb.realMin( 2 ) <= 0 )
			{
				result.add( source );
			}
		}

		result.sort( viewerState.sourceOrder() );
	}
	// TODO: Eventually, for thousands of sources, this could be moved to ViewerState,
	//  in order to avoid creating a new intermediate HashSet for every
	//  viewerState.getVisibleAndPresentSources().
	//  However, other issues will become bottlenecks before that, e.g.,
	//  copying source list when taking snapshots of ViewerState every frame,
	//  painting MultiBoxOverlay, etc.
}
