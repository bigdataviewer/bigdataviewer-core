/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

/**
 * Find out which sources are visible for the current view and rendered area, as
 * well as their projected bounding boxes.
 * <p>
 * Look at all sources that are visible in the given {@code ViewerState} (see {@link
 * ViewerState#getVisibleAndPresentSources()}), and
 * <ul>
 *     <li>For sources that participate in bounding box culling: record the source's
 *         bounding box, if it overlaps the rendered area when transformed to viewer
 *         coordinates. (See {@link #sourceBoundsForVisibleSource()})</li>
 *     <li>For sources that do not participate in bounding box culling: always record
 *         the source. (See {@link #alwaysVisibleSources()}</li>
 * </ul>
 */
class VisibleSourcesOnScreenBounds
{
	/**
	 * List of SourceBounds for sources, that
	 * <ul>
	 *     <li>participate in bounding box culling, and</li>
	 *     <li>when transformed to viewer coordinates, overlap the screen area.</li>
	 * </ul>
	 */
	private final List< SourceBounds > bounds;

	/**
	 * List of sources, that do not participate in bounding box culling (and are
	 * therefore always rendered).
	 */
	private final List< SourceAndConverter< ? > > unculledSources;

	private final int screenMinX;
	private final int screenMinY;
	private final int screenMaxX;
	private final int screenMaxY;

	/**
	 * Find out which sources are visible for the current view and rendered area, as
	 * well as their projected bounding boxes.
	 *
	 * @param viewerState
	 * 		provides list of possibly visible sources and transform into viewer coordinates.
	 * @param screenScale
	 * 		provides screen interval and transform from viewer to screen coordinates.
	 */
	public VisibleSourcesOnScreenBounds(
				final ViewerState viewerState,
				final ScreenScales.ScreenScale screenScale )
	{
		this( viewerState,
				Intervals.createMinSize( 0, 0, screenScale.width(), screenScale.height() ),
				viewerState.getViewerTransform().preConcatenate( screenScale.scaleTransform() ) );
	}

	/**
	 * Find out which sources are visible for the current view and rendered area, as
	 * well as their projected bounding boxes.
	 *
	 * @param viewerState
	 * 		provides list of possibly visible sources and transform into viewer coordinates.
	 * @param screenInterval
	 * 		the screen interval to be rendered.
	 * @param screenTransform
	 * 		transforms viewer coordinates into screen coordinates (accounts for screen scale and interval offset).
	 */
	public VisibleSourcesOnScreenBounds(
			final ViewerState viewerState,
			final Interval screenInterval,
			final AffineTransform3D screenTransform )
	{
		bounds = new ArrayList<>();
		unculledSources = new ArrayList<>();

		screenMinX = (int) screenInterval.min( 0 );
		screenMinY = (int) screenInterval.min( 1 );
		screenMaxX = (int) screenInterval.max( 0 );
		screenMaxY = (int) screenInterval.max( 1 );

		final Set< SourceAndConverter< ? > > sources = viewerState.getVisibleAndPresentSources();
		// TODO: Eventually, for thousands of sources, this could be moved to ViewerState,
		//  in order to avoid creating a new intermediate HashSet for every
		//  viewerState.getVisibleAndPresentSources().
		//  However, other issues will become bottlenecks before that, e.g.,
		//  copying source list when taking snapshots of ViewerState every frame,
		//  painting MultiBoxOverlay, etc.
		final int t = viewerState.getCurrentTimepoint();
		final double expand = viewerState.getInterpolation() == Interpolation.NEARESTNEIGHBOR ? 0.5 : 1.0;

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		final double[] sourceMin = new double[ 3 ];
		final double[] sourceMax = new double[ 3 ];

		for ( final SourceAndConverter< ? > source : sources )
		{
			if ( !source.getSpimSource().doBoundingBoxCulling() )
			{
				unculledSources.add( source );
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
				final int minX = ( int ) Math.floor( bb.realMin( 0 ) );
				final int maxX = ( int ) Math.ceil( bb.realMax( 0 ) );
				final int minY = ( int ) Math.floor( bb.realMin( 1 ) );
				final int maxY = ( int ) Math.ceil( bb.realMax( 1 ) );
				bounds.add( new SourceBounds( source, minX, minY, maxX, maxY ) );
			}
		}
	}

	/**
	 * Get list of SourceBounds for sources, that
	 * <ul>
	 *     <li>participate in bounding box culling, and</li>
	 *     <li>when transformed to viewer coordinates, overlap the screen area.</li>
	 * </ul>
	 * Note that the returned list is disjoint with {@link #alwaysVisibleSources()}.
	 */
	public List< SourceBounds > sourceBoundsForVisibleSource()
	{
		return bounds;
	}

	/**
	 * Get list of sources, that do not participate in bounding box culling (and are
	 * therefore always rendered).
	 * <p>
	 * Note that the returned list is disjoint with {@link #sourceBoundsForVisibleSource()}.
	 */
	public List< SourceAndConverter< ? > > alwaysVisibleSources()
	{
		return unculledSources;
	}

	public Interval screenInterval()
	{
		return Intervals.createMinMax( screenMinX, screenMinY, screenMaxX, screenMaxY );
	}

	/**
	 * Estimate the number of screen pixels rendered multiplied by the number of
	 * sources at each pixel.
	 * <p>
	 * Assuming exhaustive tiling, for every visible sources, the number of pixels
	 * rendered for that source is the number of pixels in its bounding box, clipped to
	 * the screen area.
	 *
	 * @return the estimated number of screen pixels rendered summed over all sources
	 */
	public int estimateNumRenderedPixels()
	{
		int numPixels = 0;

		// sum over all sources in bounds:
		// pixels in bounding box clipped to the screen area
		for ( SourceBounds sourceBounds : bounds )
		{
			final int minX = Math.max( sourceBounds.minX(), screenMinX );
			final int minY = Math.max( sourceBounds.minY(), screenMinY );
			final int maxX = Math.min( sourceBounds.maxX(), screenMaxX );
			final int maxY = Math.min( sourceBounds.maxY(), screenMaxY );
			final int sizeX = maxX - minX + 1;
			final int sizeY = maxY - minY + 1;
			numPixels += sizeX * sizeY;
		}

		// sum over all sources in unculledSources:
		// pixels in screen area
		numPixels += unculledSources.size() * (screenMaxX - screenMinX + 1 ) * (screenMaxY - screenMinY + 1 );

		return numPixels;
	}

	/**
	 * Estimate the average number of sources for one rendered pixel on screen.
	 *
	 * @return average number of sources for one rendered pixel
	 */
	public double estimateNumSourcesPerPixel()
	{
		final int screenSize = ( screenMaxX - screenMinX + 1 ) * ( screenMaxY - screenMinY + 1 );
		return ( ( double ) estimateNumRenderedPixels() ) / screenSize;
	}
}
