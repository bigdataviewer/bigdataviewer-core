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
package bdv.viewer.overlay;

import static bdv.ui.UIUtils.TextPosition.TOP_CENTER;
import static bdv.ui.UIUtils.TextPosition.TOP_RIGHT;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import bdv.ui.UIUtils;
import bdv.util.Prefs.OverlayPosition;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import mpicbg.spim.data.sequence.TimePoint;

/**
 * Render current source name and current timepoint of a {@link ViewerState}
 * into a {@link Graphics2D}.
 *
 * @author Tobias Pietzsch
 */
public class SourceInfoOverlayRenderer
{
	protected List< TimePoint > timePointsOrdered;

	protected String sourceName;

	protected String groupName;

	protected String timepointString;

	protected OverlayPosition sourceNameOverlayPosition = OverlayPosition.TOP_RIGHT;

	public synchronized void paint( final Graphics2D g )
	{
		final Font font = UIUtils.getFont( "monospaced.small.font" );

		g.setColor( Color.WHITE );
		g.setFont( font );

		UIUtils.drawString( g, TOP_RIGHT, 0, timepointString );

		switch ( sourceNameOverlayPosition )
		{
		default:
		case TOP_CENTER:
			UIUtils.drawString( g, TOP_CENTER, 0, sourceName );
			UIUtils.drawString( g, TOP_CENTER, 1, groupName );
			break;
		case TOP_RIGHT:
			UIUtils.drawString( g, TOP_RIGHT, 2, sourceName );
			UIUtils.drawString( g, TOP_RIGHT, 3, groupName );
			break;
		}
	}

	public synchronized void setTimePointsOrdered( final List< TimePoint > timePointsOrdered )
	{
		this.timePointsOrdered = timePointsOrdered;
	}

	/**
	 * Update data to show in the overlay.
	 */
	@Deprecated
	public synchronized void setViewerState( final bdv.viewer.state.ViewerState state )
	{
		synchronized ( state )
		{
			setViewerState( state.getState() );
		}
	}

	/**
	 * Update data to show in the overlay.
	 */
	public synchronized void setViewerState( final ViewerState state )
	{
		synchronized ( state )
		{
			final SourceAndConverter< ? > currentSource = state.getCurrentSource();
			sourceName = currentSource != null
					? currentSource.getSpimSource().getName() : "";

			final bdv.viewer.SourceGroup currentGroup = state.getCurrentGroup();
			groupName = currentGroup != null && state.getDisplayMode().hasGrouping()
					? state.getGroupName( currentGroup ) : "";

			final int t = state.getCurrentTimepoint();
			if ( timePointsOrdered != null && t >= 0 && t < timePointsOrdered.size() )
				timepointString = String.format( "t = %s", timePointsOrdered.get( t ).getName() );
			else
				timepointString = String.format( "t = %d", t );
		}
	}

	public void setSourceNameOverlayPosition( final OverlayPosition position )
	{
		this.sourceNameOverlayPosition = position;
	}
}
