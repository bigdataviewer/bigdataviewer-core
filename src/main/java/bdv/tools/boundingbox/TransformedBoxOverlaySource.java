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
package bdv.tools.boundingbox;

import static bdv.viewer.ViewerStateChange.VISIBILITY_CHANGED;

import java.awt.Color;

import net.imglib2.type.numeric.ARGBType;

import bdv.util.Bounds;
import bdv.util.PlaceHolderConverterSetup;
import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.ConverterSetups;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;

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

	private final SourceAndConverter< Void > boxSourceAndConverter;

	private final AbstractViewerPanel viewer;

	private final ConverterSetups setups;

	private boolean isVisible;

	public TransformedBoxOverlaySource(
			final String name,
			final TransformedBoxOverlay boxOverlay,
			final TransformedBox bbSource,
			final AbstractViewerPanel viewer,
			final ConverterSetups converterSetups,
			final int setupId )
	{
		this.boxOverlay = boxOverlay;
		this.viewer = viewer;
		this.setups = converterSetups;

		boxConverterSetup = new PlaceHolderConverterSetup( setupId, 0, 128, new ARGBType( 0x00994499) );

		boxConverterSetup.setupChangeListeners().add( s -> this.repaint() );
		boxSourceAndConverter = new SourceAndConverter<>(
				new TransformedBoxPlaceHolderSource( name, bbSource ),
				( input, output ) -> output.set( 0 ) );
	}

	public void addToViewer()
	{
		final ViewerState state = viewer.state();
		synchronized ( state )
		{
			if ( state.getDisplayMode() != DisplayMode.FUSED )
			{
				for ( final SourceAndConverter< ? > source : state.getSources() )
					state.setSourceActive( source, state.isSourceVisible( source ) );
				state.setDisplayMode( DisplayMode.FUSED );
			}

			state.addSource( boxSourceAndConverter );
			state.changeListeners().add( viewerStateChangeListener );
			state.setSourceActive( boxSourceAndConverter, true );
			state.setCurrentSource( boxSourceAndConverter );

			isVisible = state.isSourceVisible( boxSourceAndConverter );
		}

		setups.put( boxSourceAndConverter, boxConverterSetup );
		setups.getBounds().setBounds( boxConverterSetup, new Bounds( 0, 255 ) );

		repaint();
	}

	public void removeFromViewer()
	{
		viewer.state().changeListeners().remove( viewerStateChangeListener );
		viewer.state().removeSource( boxSourceAndConverter );
	}

	private final ViewerStateChangeListener viewerStateChangeListener = this::viewerStateChanged;

	private void viewerStateChanged( final ViewerStateChange change )
	{
		if ( change == VISIBILITY_CHANGED )
		{
			final boolean wasVisible = isVisible;
			isVisible = viewer.state().isSourceVisible( boxSourceAndConverter );
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
		viewer.getDisplayComponent().repaint();
	}
}
