/*
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
package bdv.tools.transformation;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

	private final ActionMap actionMap;

	private final InputMap inputMap;

	private final Listeners.List< ManualTransformActiveListener > manualTransformActiveListeners;

	private final Listeners< TransformListener< AffineTransform3D > > viewerTransformListeners;

	private final ViewerState viewerState;

	private final Consumer< String > viewerMessageDisplay;

	private Collection< SourceAndConverter< ? > > customSources;

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
		actionMap = new ActionMap();
		inputMap = new InputMap();
		actionMap.put( "abort manual transformation", abortAction );
		inputMap.put( abortKey, "abort manual transformation" );
		actionMap.put( "reset manual transformation", resetAction );
		inputMap.put( resetKey, "reset manual transformation" );
		bindings.addActionMap( "manual transform", actionMap );
	}

	public synchronized void abort()
	{
		if ( active )
		{
			final AffineTransform3D identity = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
				source.setIncrementalTransform( identity );
			viewerState.setViewerTransform( frozenTransform );
			viewerMessageDisplay.accept( "aborted manual transform" );
			active = false;
			manualTransformActiveListeners.list.forEach( l -> l.manualTransformActiveChanged( active ) );
		}
	}

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

	public synchronized void transform( Collection< SourceAndConverter< ? > > customSources )
	{
		this.customSources = customSources;
		setActive( true );
	}

	public synchronized void setActive( final boolean a )
	{
		if ( this.active == a )
			return;

		if ( a )
		{
			// Enter manual edit mode
			final ViewerState state = this.viewerState.snapshot();
			final List< SourceAndConverter< ? > > transformableSources = new ArrayList<>();
			if ( customSources != null )
			{
				transformableSources.addAll( customSources );
			}
			else
			{
				switch ( state.getDisplayMode() )
				{
					case FUSED:
						transformableSources.add( state.getCurrentSource() );
						break;
					case FUSEDGROUP:
						transformableSources.addAll( state.getSourcesInGroup( state.getCurrentGroup() ) );
						break;
					default:
						viewerMessageDisplay.accept( "Can only do manual transformation when in FUSED mode." );
						return;
				}
			}
			state.getViewerTransform( frozenTransform );
			sourcesToModify.clear();
			sourcesToFix.clear();
			for ( final SourceAndConverter< ? > source : state.getSources() )
			{
				if ( source.getSpimSource() instanceof TransformedSource )
				{
					if ( transformableSources.contains( source ) )
						sourcesToModify.add( ( TransformedSource< ? > ) source.getSpimSource() );
					else
						sourcesToFix.add( ( TransformedSource< ? > ) source.getSpimSource() );
				}
			}
			active = true;
			viewerTransformListeners.add( this );
			bindings.addInputMap( "manual transform", inputMap );
			viewerMessageDisplay.accept( "starting manual transform" );
		}
		else
		{
			// Exit manual edit mode.
			active = false;
			customSources = null;
			viewerTransformListeners.remove( this );
			bindings.removeInputMap( "manual transform" );
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
			viewerState.setViewerTransform( frozenTransform );
			viewerMessageDisplay.accept( "fixed manual transform" );
		}
		manualTransformActiveListeners.list.forEach( l -> l.manualTransformActiveChanged( active ) );
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

		for ( final TransformedSource< ? > source : sourcesToFix )
			source.setIncrementalTransform( liveTransform.inverse() );
	}

	public Listeners< ManualTransformActiveListener > manualTransformActiveListeners()
	{
		return manualTransformActiveListeners;
	}
}
