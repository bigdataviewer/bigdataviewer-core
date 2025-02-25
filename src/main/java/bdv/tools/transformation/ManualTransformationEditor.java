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
package bdv.tools.transformation;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import net.imglib2.realtransform.AffineTransform3D;

import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerState;

// TODO: what happens when the current source, display mode, etc is changed while the editor is active? deactivate?
public class ManualTransformationEditor implements TransformListener< AffineTransform3D >
{
	private boolean active = false;

	private final InputActionBindings bindings;

	private final AffineTransform3D frozenTransform;

	private final AffineTransform3D liveTransform;

	private final ArrayList< TransformedSource< ? > > sourcesToModify;

	private final ArrayList< TransformedSource< ? > > sourcesToFix;

	private final InputMap inputMap;

	private final Listeners.List< ManualTransformActiveListener > manualTransformActiveListeners;

	private final Listeners< TransformListener< AffineTransform3D > > viewerTransformListeners;

	private final ViewerState viewerState;

	private final Consumer< String > viewerMessageDisplay;

	public ManualTransformationEditor( final AbstractViewerPanel viewer, final InputActionBindings inputActionBindings )
	{
		this( viewer.transformListeners(), viewer.state(), viewer::showMessage, inputActionBindings );
	}

	/**
	 * @param viewerTransformListeners
	 * 		the editor will register here for listening to transform changes while active
	 * @param viewerState
	 * 		the state which is manipulated by the editor
	 * @param viewerMessageDisplay
	 * 		messages will be displayed here
	 * @param inputActionBindings
	 * 		the editors actionMap will be registered here while active
	 */
	public ManualTransformationEditor(
			final Listeners< TransformListener< AffineTransform3D > > viewerTransformListeners,
			final ViewerState viewerState,
			final Consumer< String > viewerMessageDisplay,
			final InputActionBindings inputActionBindings )
	{
		this.viewerTransformListeners = viewerTransformListeners;
		this.viewerState = viewerState;
		this.viewerMessageDisplay = viewerMessageDisplay;

		bindings = inputActionBindings;
		frozenTransform = new AffineTransform3D();
		liveTransform = new AffineTransform3D();
		sourcesToModify = new ArrayList<>();
		sourcesToFix = new ArrayList<>();
		manualTransformActiveListeners = new Listeners.SynchronizedList<>();

		final KeyStroke abortKey = KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 );
		final Action abortAction = new RunnableAction( "abort manual transformation", this::abort );
		final KeyStroke resetKey = KeyStroke.getKeyStroke( KeyEvent.VK_R, 0 );
		final Action resetAction = new RunnableAction( "reset manual transformation", this::reset );
		final ActionMap actionMap = new ActionMap();
		inputMap = new InputMap();
		actionMap.put( "abort manual transformation", abortAction );
		inputMap.put( abortKey, "abort manual transformation" );
		actionMap.put( "reset manual transformation", resetAction );
		inputMap.put( resetKey, "reset manual transformation" );
		bindings.addActionMap( "manual transform", actionMap );
	}

	/**
	 * Get the set of current sources (in the given {@code state}). This
	 * contains all sources in the current group if grouping is active, and the
	 * single current source otherwise.
	 *
	 * @return the set of current sources
	 */
	private static Set< SourceAndConverter< ? > > getCurrentSources( final ViewerState state )
	{
		if ( state.getDisplayMode().hasGrouping() )
			return state.getSourcesInGroup( state.getCurrentGroup() );
		else
			return Collections.singleton( state.getCurrentSource() );
	}

	/**
	 * Initiate a manual transformation modifying the given {@code
	 * sourcesToTransform}. If {@code sourcesToTransform == null}, use the
	 * current source (or source group).
	 */
	public synchronized void transform( Collection< SourceAndConverter< ? > > sourcesToTransform )
	{
		if ( active )
		{
			// if there is an on-going manual transformation, abort it first.
			abort();
		}

		// Enter manual edit mode
		final ViewerState state = this.viewerState.snapshot();
		if ( sourcesToTransform == null )
		{
			if ( !state.getDisplayMode().hasFused() )
			{
				// NB: If a non-null collection of sourcesToTransform was
				// passed, we assume that the caller knows what they are doing.
				// Otherwise, if not in a FUSED display mode, abort.
				viewerMessageDisplay.accept( "Can only do manual transformation when in FUSED mode." );
				return;
			}
			sourcesToTransform = getCurrentSources( state );
		}
		state.getViewerTransform( frozenTransform );
		sourcesToModify.clear();
		sourcesToFix.clear();
		for ( final SourceAndConverter< ? > source : state.getSources() )
		{
			if ( source.getSpimSource() instanceof TransformedSource )
			{
				if ( sourcesToTransform.contains( source ) )
					sourcesToModify.add( ( TransformedSource< ? > ) source.getSpimSource() );
				else
					sourcesToFix.add( ( TransformedSource< ? > ) source.getSpimSource() );
			}
		}
		viewerTransformListeners.add( this );
		bindings.addInputMap( "manual transform", inputMap );
		viewerMessageDisplay.accept( "starting manual transform" );

		active = true;
		manualTransformActiveListeners.list.forEach( l -> l.manualTransformActiveChanged( active ) );
	}

	/**
	 * During an ongoing manual transformation, reset the transforming sources
	 * to their original transformations. Note that this will discard previous
	 * manual transformations that were already applied to the transforming
	 * sources. The ongoing manual transformation is not terminated by this.
	 */
	public synchronized void reset()
	{
		if ( active )
		{
			final AffineTransform3D identity = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
			{
				source.setIncrementalTransform( identity );
				source.setFixedTransform( identity );
			}
			for ( final TransformedSource< ? > source : sourcesToFix )
			{
				source.setIncrementalTransform( identity );
			}
			viewerState.setViewerTransform( frozenTransform );
			viewerMessageDisplay.accept( "reset manual transform" );
		}
	}

	/**
	 * End the ongoing manual transformation and fix the incremental
	 * transformation.
	 */
	public synchronized void apply()
	{
		if ( active )
		{
			// Exit manual edit mode.
			final AffineTransform3D tmp = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
			{
				tmp.identity();
				source.setIncrementalTransform( tmp );
				source.getFixedTransform( tmp );
				tmp.preConcatenate( liveTransform );
				source.setFixedTransform( tmp );
			}
			tmp.identity();
			for ( final TransformedSource< ? > source : sourcesToFix )
				source.setIncrementalTransform( tmp );

			terminate( "fixed manual transform" );
		}
	}

	/**
	 * End the ongoing manual transformation and discard the incremental
	 * transformation.
	 */
	public synchronized void abort()
	{
		if ( active )
		{
			final AffineTransform3D identity = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
				source.setIncrementalTransform( identity );

			terminate("aborted manual transform");
		}
	}

	private void terminate( final String message )
	{
		viewerTransformListeners.remove( this );
		bindings.removeInputMap( "manual transform" );
		viewerState.setViewerTransform( frozenTransform );
		active = false;
		if ( message != null )
			viewerMessageDisplay.accept( message );
		manualTransformActiveListeners.list.forEach( l -> l.manualTransformActiveChanged( active ) );
	}

	public synchronized void setActive( final boolean a )
	{
		if ( this.active == a )
			return;

		if ( a )
			transform( null );
		else
			apply();
	}

	public synchronized void toggle()
	{
		setActive( !active );
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		if ( !active )
		{
			return;
		}

		liveTransform.set( transform );
		liveTransform.preConcatenate( frozenTransform.inverse() );
		sourcesToFix.forEach( s -> s.setIncrementalTransform( liveTransform.inverse() ) );
	}

	public Listeners< ManualTransformActiveListener > manualTransformActiveListeners()
	{
		return manualTransformActiveListeners;
	}
}
