/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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

import bdv.tools.brightness.SetupAssignments;
import bdv.util.PlaceHolderConverterSetup;
import bdv.viewer.DisplayMode;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.VisibilityAndGrouping.Event;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import java.awt.Color;
import net.imglib2.type.numeric.ARGBType;

import static bdv.viewer.VisibilityAndGrouping.Event.SOURCE_ACTVITY_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.VISIBILITY_CHANGED;

/**
 * A BDV source (and converter etc) representing a {@code TransformedBox}.
 * The intersection of the box with the viewer plane is drawn by
 * {@link TransformedBoxOverlay}, so we just install a
 * placeholder source to set the visibility, color, and opacity of that
 * intersection via the standard bdv dialog.
 *
 * @author Tobias Pietzsch
 */
public class TransformedBoxOverlaySource
{
	private final TransformedBoxOverlay boxOverlay;

	private final PlaceHolderConverterSetup boxConverterSetup;

	private final Source< Void > boxSource;

	private final SourceAndConverter< Void > boxSourceAndConverter;

	private final ViewerPanel viewer;

	private final SetupAssignments setupAssignments;

	private boolean isVisible;

	public TransformedBoxOverlaySource(
			final String name,
			final TransformedBoxOverlay boxOverlay,
			final TransformedBox bbSource,
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments )
	{
		this.boxOverlay = boxOverlay;
		this.viewer = viewer;
		this.setupAssignments = setupAssignments;

		final int setupId = SetupAssignments.getUnusedSetupId( setupAssignments );
		boxConverterSetup = new PlaceHolderConverterSetup( setupId, 0, 128, new ARGBType( 0x00994499) );

		boxConverterSetup.setViewer( this::repaint );
		boxSource = new TransformedBoxPlaceHolderSource( name, bbSource );
		boxSourceAndConverter = new SourceAndConverter<>( boxSource, ( input, output ) -> output.set( 0 ) );
	}

	public void addToViewer()
	{
		final VisibilityAndGrouping vg = viewer.getVisibilityAndGrouping();
		if ( vg.getDisplayMode() != DisplayMode.FUSED )
		{
			final int numSources = vg.numSources();
			for ( int i = 0; i < numSources; ++i )
				vg.setSourceActive( i, vg.isSourceVisible( i ) );
			vg.setDisplayMode( DisplayMode.FUSED );
		}

		viewer.addSource( boxSourceAndConverter );
		vg.addUpdateListener( visibilityChanged );
		vg.setSourceActive( boxSource, true );
		vg.setCurrentSource( boxSource );

		setupAssignments.addSetup( boxConverterSetup );
		setupAssignments.getMinMaxGroup( boxConverterSetup ).setRange( 0, 255 );

		isVisible = isVisible();
		repaint();
	}

	public void removeFromViewer()
	{
		final VisibilityAndGrouping vg = viewer.getVisibilityAndGrouping();
		vg.removeUpdateListener( visibilityChanged );
		viewer.removeSource( boxSource );
		setupAssignments.removeSetup( boxConverterSetup );
	}

	private boolean isVisible()
	{
		final ViewerState state = viewer.getState();
		int sourceIndex = 0;
		for ( final SourceState< ? > s : state.getSources() )
			if ( s.getSpimSource() == boxSource )
				break;
			else
				++sourceIndex;
		switch ( state.getDisplayMode() )
		{
		case SINGLE:
			return ( sourceIndex == state.getCurrentSource() );
		case GROUP:
			return state.getSourceGroups().get( state.getCurrentGroup() ).getSourceIds().contains( sourceIndex );
		case FUSED:
			return state.getSources().get( sourceIndex ).isActive();
		case FUSEDGROUP:
		default:
			for ( final SourceGroup group : state.getSourceGroups() )
				if ( group.isActive() && group.getSourceIds().contains( sourceIndex ) )
					return true;
		}
		return false;
	}

	private final VisibilityAndGrouping.UpdateListener visibilityChanged = this::visibilityChanged;

	private void visibilityChanged( final Event e )
	{
		if ( e.id == VISIBILITY_CHANGED || e.id == SOURCE_ACTVITY_CHANGED )
		{
			final boolean wasVisible = isVisible;
			isVisible = isVisible();
			if ( wasVisible != isVisible )
				repaint();
		}
	}

	private void repaint()
	{
		boxOverlay.fillIntersection( isVisible );
		if ( isVisible )
		{
			final int alpha = Math.min( 255, ( int ) boxConverterSetup.getDisplayRangeMax() );
			final int argb = ( boxConverterSetup.getColor().get() & 0x00ffffff ) | ( alpha << 24 );
			boxOverlay.setIntersectionFillColor( new Color( argb, true ) );
		}
		viewer.getDisplay().repaint();
	}
}
