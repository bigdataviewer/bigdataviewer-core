/*
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
package bdv.viewer.overlay;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import bdv.ui.UIUtils;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

/**
 * Render multibox overlay corresponding to a {@link ViewerState} into a
 * {@link Graphics2D}.
 *
 * @author Tobias Pietzsch
 */
public class MultiBoxOverlayRenderer
{
	/**
	 * Navigation wire-frame cube.
	 */
	protected final MultiBoxOverlay box;

	/**
	 * Screen interval in which to display navigation wire-frame cube.
	 */
	protected Interval boxInterval;

	/**
	 * scaled screenImage interval for {@link #box} rendering
	 */
	protected Interval virtualScreenInterval;

	protected final ArrayList< IntervalAndTransform > boxSources;

	public MultiBoxOverlayRenderer()
	{
		this( 800, 600 );
	}

	public MultiBoxOverlayRenderer( final int screenWidth, final int screenHeight )
	{
		box = new MultiBoxOverlay();

		final double uiScale = UIUtils.getUIScaleFactor();
		boxInterval = Intervals.createMinSize(
				( int )Math.round( 10 * uiScale ),
				( int )Math.round( 10 * uiScale ),
				( int )Math.round( 160 * uiScale ),
				( int )Math.round( 120 * uiScale ) );
		virtualScreenInterval = Intervals.createMinSize( 0, 0, screenWidth, screenHeight );
		boxSources = new ArrayList<>();
	}

	public synchronized void paint( final Graphics2D g )
	{
		box.paint( g, boxSources, virtualScreenInterval, boxInterval );
	}

	// TODO
	public boolean isHighlightInProgress()
	{
		return box.isHighlightInProgress();
	}

	// TODO
	public void highlight( final int sourceIndex )
	{
		box.highlight( sourceIndex );
	}

	/**
	 * Update the screen interval. This is the target 2D interval into which
	 * pixels are rendered. (In the box overlay it is shown as a filled grey
	 * rectangle.)
	 */
	public synchronized void updateVirtualScreenSize( final int screenWidth, final int screenHeight )
	{
		final long oldW = virtualScreenInterval.dimension( 0 );
		final long oldH = virtualScreenInterval.dimension( 1 );
		if ( screenWidth != oldW || screenHeight != oldH )
			virtualScreenInterval = Intervals.createMinSize( 0, 0, screenWidth, screenHeight );
	}

	/**
	 * Update the box interval. This is the screen interval in which to display
	 * navigation wire-frame cube.
	 */
	public synchronized void setBoxInterval( final Interval interval )
	{
		boxInterval = interval;
	}

	/**
	 * Update data to show in the box overlay.
	 */
	@Deprecated
	public synchronized void setViewerState( final bdv.viewer.state.ViewerState viewerState )
	{
		synchronized ( viewerState )
		{
			setViewerState( viewerState.getState() );
		}
	}

	/**
	 * Update data to show in the box overlay.
	 */
	public synchronized void setViewerState( final ViewerState state )
	{
		synchronized ( state )
		{
			final List< SourceAndConverter< ? > > sources = state.getSources();
			final int timepoint = state.getCurrentTimepoint();

			final List< SourceAndConverter< ? > > presentSources = new ArrayList<>();
			sources.forEach( s -> {
				if ( s.getSpimSource().isPresent( timepoint ) )
					presentSources.add( s );
			} );

			final int numPresentSources = presentSources.size();
			if ( boxSources.size() != numPresentSources )
			{
				while ( boxSources.size() < numPresentSources )
					boxSources.add( new IntervalAndTransform() );
				while ( boxSources.size() > numPresentSources )
					boxSources.remove( boxSources.size() - 1 );
			}

			final AffineTransform3D sourceToViewer = state.getViewerTransform();
			final AffineTransform3D sourceTransform = new AffineTransform3D();
			int i = 0;
			for ( final SourceAndConverter< ? > source : presentSources )
			{
				final IntervalAndTransform boxsource = boxSources.get( i++ );
				source.getSpimSource().getSourceTransform( timepoint, 0, sourceTransform );
				sourceTransform.preConcatenate( sourceToViewer );
				boxsource.setSourceToViewer( sourceTransform );
				boxsource.setSourceInterval( source.getSpimSource().getSource( timepoint, 0 ) );
				boxsource.setVisible( state.isSourceVisible( source ) );
			}
		}
	}
}
