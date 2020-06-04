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

import static bdv.tools.boundingbox.TransformedBoxOverlay.BoxDisplayMode.FULL;

import bdv.viewer.ConverterSetups;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.scijava.listeners.ChangeListener;
import org.scijava.listeners.ListenableVar;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.tools.boundingbox.TransformedBoxOverlay.BoxDisplayMode;
import bdv.viewer.ViewerPanel;

/**
 * Installs an interactive box selection tool on a BDV.
 * <p>
 * The feature consists of an overlay added to the BDV and editing behaviours
 * where the user can edit the box directly by interacting with the overlay. The
 * mouse button is used to drag the corners of the box.
 * <p>
 * {@code TransformedBoxEditor} does not include any dialog or panel for
 * numerically adjusting the box parameters.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class TransformedBoxEditor
{
	private static final String BOUNDING_BOX_TOGGLE_EDITOR = "edit bounding-box";

	private static final String[] BOUNDING_BOX_TOGGLE_EDITOR_KEYS = new String[] { "button1" };

	private static final String BOUNDING_BOX_MAP = "bounding-box";

	private static final String BLOCKING_MAP = "bounding-box-blocking";

	private final TransformedBoxOverlay boxOverlay;

	private final TransformedBoxOverlaySource boxSource;

	private final ViewerPanel viewer;

	private final TriggerBehaviourBindings triggerbindings;

	private final Behaviours behaviours;

	private final BehaviourMap blockMap;

	private boolean editable = true;

	public enum BoxSourceType
	{
		NONE,
		PLACEHOLDER
	}

	public TransformedBoxEditor(
			final InputTriggerConfig keyconf,
			final ViewerPanel viewer,
			final ConverterSetups converterSetups,
			final int setupId,
			final TriggerBehaviourBindings triggerbindings,
			final AbstractTransformedBoxModel model )
	{
		this( keyconf, viewer, converterSetups, setupId, triggerbindings, model, "selection", BoxSourceType.PLACEHOLDER );
	}

	public TransformedBoxEditor(
			final InputTriggerConfig keyconf,
			final ViewerPanel viewer,
			final ConverterSetups converterSetups,
			final int setupId,
			final TriggerBehaviourBindings triggerbindings,
			final AbstractTransformedBoxModel model,
			final String boxSourceName,
			final BoxSourceType boxSourceType )
	{
		this.viewer = viewer;
		this.triggerbindings = triggerbindings;

		/*
		 * Create an Overlay to show 3D wireframe box
		 */
		boxOverlay = new TransformedBoxOverlay( model );
		boxOverlay.setPerspective( 0 );
		boxOverlay.boxDisplayMode().listeners().add( () -> {
			viewer.requestRepaint();
			updateEditability();
		} );

		/*
		 * Create a BDV source to show bounding box slice
		 */
		switch ( boxSourceType )
		{
		case PLACEHOLDER:
			boxSource = new TransformedBoxOverlaySource( boxSourceName, boxOverlay, model, viewer, converterSetups, setupId );
			break;
		case NONE:
		default:
			boxSource = null;
			break;
		}

		/*
		 * Create DragBoxCornerBehaviour
		 */

		behaviours = new Behaviours( keyconf, "bdv" );
		behaviours.behaviour( new DragBoxCornerBehaviour( boxOverlay, model ), BOUNDING_BOX_TOGGLE_EDITOR, BOUNDING_BOX_TOGGLE_EDITOR_KEYS );

		/*
		 * Create BehaviourMap to block behaviours interfering with
		 * DragBoxCornerBehaviour. The block map is only active while a corner
		 * is highlighted.
		 */
		blockMap = new BehaviourMap();
	}

	public void install()
	{
		viewer.getDisplay().overlays().add( boxOverlay );
		viewer.renderTransformListeners().add( boxOverlay );
		viewer.getDisplay().addHandler( boxOverlay.getCornerHighlighter() );

		refreshBlockMap();
		updateEditability();

		if ( boxSource != null )
			boxSource.addToViewer();
	}

	public void uninstall()
	{
		viewer.getDisplay().overlays().remove( boxOverlay );
		viewer.removeTransformListener( boxOverlay );
		viewer.getDisplay().removeHandler( boxOverlay.getCornerHighlighter() );

		triggerbindings.removeInputTriggerMap( BOUNDING_BOX_MAP );
		triggerbindings.removeBehaviourMap( BOUNDING_BOX_MAP );

		unblock();

		if ( boxSource != null )
			boxSource.removeFromViewer();
	}

	/**
	 * Only meaningful if {@code BoxSourceType == NONE}
	 */
	public boolean getFillIntersection()
	{
		return boxOverlay.getFillIntersection();
	}

	/**
	 * Only meaningful if {@code BoxSourceType == NONE}
	 */
	public void setFillIntersection( final boolean fill )
	{
		boxOverlay.fillIntersection( fill );
	}

	public ListenableVar< BoxDisplayMode, ChangeListener > boxDisplayMode()
	{
		return boxOverlay.boxDisplayMode();
	}

	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable( final boolean editable )
	{
		if ( this.editable == editable )
			return;
		this.editable = editable;
		boxOverlay.showCornerHandles( editable );
		updateEditability();
	}

	/**
	 * Sets up perspective projection for the overlay. Basically, the projection
	 * center is placed at distance {@code perspective * sourceSize} from the
	 * projection plane (screen). Specify {@code perspective = 0} to set
	 * parallel projection.
	 *
	 * @param perspective
	 *            the perspective value.
	 * @param sourceSize
	 *            the the size of the source.
	 */
	public void setPerspective( final double perspective, final double sourceSize )
	{
		boxOverlay.setPerspective( perspective );
		boxOverlay.setSourceSize( sourceSize );
	}


	private void updateEditability()
	{
		if ( editable && boxOverlay.boxDisplayMode().get() == FULL )
		{
			boxOverlay.setHighlightedCornerListener( this::highlightedCornerChanged );
			behaviours.install( triggerbindings, BOUNDING_BOX_MAP );
			highlightedCornerChanged();
		}
		else
		{
			boxOverlay.setHighlightedCornerListener( null );
			triggerbindings.removeInputTriggerMap( BOUNDING_BOX_MAP );
			triggerbindings.removeBehaviourMap( BOUNDING_BOX_MAP );
			unblock();
		}
	}

	private void block()
	{
		triggerbindings.addBehaviourMap( BLOCKING_MAP, blockMap );
	}

	private void unblock()
	{
		triggerbindings.removeBehaviourMap( BLOCKING_MAP );
	}

	private void highlightedCornerChanged()
	{
		final int index = boxOverlay.getHighlightedCornerIndex();
		if ( index < 0 )
			unblock();
		else
			block();
	}

	private void refreshBlockMap()
	{
		triggerbindings.removeBehaviourMap( BLOCKING_MAP );

		final Set< InputTrigger > moveCornerTriggers = new HashSet<>();
		for ( final String s : BOUNDING_BOX_TOGGLE_EDITOR_KEYS )
			moveCornerTriggers.add( InputTrigger.getFromString( s ) );

		final Map< InputTrigger, Set< String > > bindings = triggerbindings.getConcatenatedInputTriggerMap().getAllBindings();
		final Set< String > behavioursToBlock = new HashSet<>();
		for ( final InputTrigger t : moveCornerTriggers )
			behavioursToBlock.addAll( bindings.get( t ) );

		blockMap.clear();
		final Behaviour block = new Behaviour() {};
		for ( final String key : behavioursToBlock )
			blockMap.put( key, block );
	}
}
