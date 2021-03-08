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
package bdv.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.KeyConfigContexts;
import bdv.KeyConfigScopes;
import bdv.viewer.AbstractViewerPanel.AlignPlane;

public class NavigationActions extends Actions
{
	public static final String TOGGLE_INTERPOLATION = "toggle interpolation";
	public static final String TOGGLE_FUSED_MODE = "toggle fused mode";
	public static final String TOGGLE_GROUPING = "toggle grouping";
	public static final String SET_CURRENT_SOURCE = "set current source %d";
	public static final String TOGGLE_SOURCE_VISIBILITY = "toggle source visibility %d";
	public static final String ALIGN_XY_PLANE = "align XY plane";
	public static final String ALIGN_ZY_PLANE = "align ZY plane";
	public static final String ALIGN_XZ_PLANE = "align XZ plane";
	public static final String NEXT_TIMEPOINT = "next timepoint";
	public static final String PREVIOUS_TIMEPOINT = "previous timepoint";

	public static final String[] TOGGLE_INTERPOLATION_KEYS = new String[] { "I" };
	public static final String[] TOGGLE_FUSED_MODE_KEYS = new String[] { "F" };
	public static final String[] TOGGLE_GROUPING_KEYS = new String[] { "G" };
	public static final String SET_CURRENT_SOURCE_KEYS_FORMAT = "%s";
	public static final String TOGGLE_SOURCE_VISIBILITY_KEYS_FORMAT = "shift %s";
	public static final String[] ALIGN_XY_PLANE_KEYS = new String[] { "shift Z" };
	public static final String[] ALIGN_ZY_PLANE_KEYS = new String[] { "shift X" };
	public static final String[] ALIGN_XZ_PLANE_KEYS = new String[] { "shift Y", "shift A" };
	public static final String[] NEXT_TIMEPOINT_KEYS = new String[] { "CLOSE_BRACKET", "M" };
	public static final String[] PREVIOUS_TIMEPOINT_KEYS = new String[] { "OPEN_BRACKET", "N" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigScopes.BIGDATAVIEWER, KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( TOGGLE_INTERPOLATION, TOGGLE_INTERPOLATION_KEYS, "Switch between nearest-neighbor and n-linear interpolation mode in BigDataViewer." );
			descriptions.add( TOGGLE_FUSED_MODE, TOGGLE_FUSED_MODE_KEYS, "TODO" );
			descriptions.add( TOGGLE_GROUPING, TOGGLE_GROUPING_KEYS, "TODO" );

			final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
			IntStream.range( 0, numkeys.length ).forEach( i -> {
				descriptions.add( String.format( SET_CURRENT_SOURCE, i ), new String[] { String.format( SET_CURRENT_SOURCE_KEYS_FORMAT, numkeys[ i ] ) }, "TODO" );
				descriptions.add( String.format( TOGGLE_SOURCE_VISIBILITY, i ), new String[] { String.format( TOGGLE_SOURCE_VISIBILITY_KEYS_FORMAT, numkeys[ i ] ) }, "TODO" );
			} );

			descriptions.add( NEXT_TIMEPOINT, NEXT_TIMEPOINT_KEYS, "TODO" );
			descriptions.add( PREVIOUS_TIMEPOINT, PREVIOUS_TIMEPOINT_KEYS, "TODO" );
			descriptions.add( ALIGN_XY_PLANE, ALIGN_XY_PLANE_KEYS, "TODO" );
			descriptions.add( ALIGN_ZY_PLANE, ALIGN_ZY_PLANE_KEYS, "TODO" );
			descriptions.add( ALIGN_XZ_PLANE, ALIGN_XZ_PLANE_KEYS, "TODO" );
		}
	}

	/**
	 * Create navigation actions and install them in the specified
	 * {@link Actions}.
	 *
	 * @param actions
	 *            navigation actions are installed here.
	 * @param viewer
	 *            Navigation actions are targeted at this {@link AbstractViewerPanel}.
	 */
	public static void install( final Actions actions, final AbstractViewerPanel viewer, final boolean is2D )
	{
		installModeActions( actions, viewer.state() );
		installSourceActions( actions, viewer.state() );
		installTimeActions( actions, viewer.state() );
		installAlignPlaneActions( actions, viewer, is2D );
	}

	public static void installModeActions( final Actions actions, final ViewerState state )
	{
		actions.runnableAction( () -> toggleInterpolation( state ), TOGGLE_INTERPOLATION, TOGGLE_INTERPOLATION_KEYS );
		actions.runnableAction(	() -> toggleFusedMode( state ), TOGGLE_FUSED_MODE, TOGGLE_FUSED_MODE_KEYS );
		actions.runnableAction(	() -> toggleGroupingMode( state ), TOGGLE_GROUPING, TOGGLE_GROUPING_KEYS );
	}

	public static void installTimeActions( final Actions actions, final ViewerState state )
	{
		actions.runnableAction( () -> nextTimePoint( state ), NEXT_TIMEPOINT, NEXT_TIMEPOINT_KEYS );
		actions.runnableAction( () -> previousTimePoint( state ), PREVIOUS_TIMEPOINT, PREVIOUS_TIMEPOINT_KEYS );
	}

	public static void installSourceActions( final Actions actions, final ViewerState state )
	{
		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		IntStream.range( 0, numkeys.length ).forEach( i -> {
			actions.runnableAction( () -> setCurrentGroupOrSource( state, i ),
					String.format( SET_CURRENT_SOURCE, i ),
					String.format( SET_CURRENT_SOURCE_KEYS_FORMAT, numkeys[ i ] ) );
			actions.runnableAction( () -> toggleGroupOrSourceActive( state, i ),
					String.format( TOGGLE_SOURCE_VISIBILITY, i ),
					String.format( TOGGLE_SOURCE_VISIBILITY_KEYS_FORMAT, numkeys[ i ] ) );
		} );
	}

	public static void installAlignPlaneActions( final Actions actions, final AbstractViewerPanel viewer, final boolean is2D )
	{
		actions.runnableAction( () -> viewer.align( AlignPlane.XY ), ALIGN_XY_PLANE, ALIGN_XY_PLANE_KEYS );
		if ( !is2D )
		{
			actions.runnableAction( () -> viewer.align( AlignPlane.ZY ), ALIGN_ZY_PLANE, ALIGN_ZY_PLANE_KEYS );
			actions.runnableAction( () -> viewer.align( AlignPlane.XZ ), ALIGN_XZ_PLANE, ALIGN_XZ_PLANE_KEYS );
		}
	}

	@Deprecated
	public static void installAlignPlaneAction( final Actions actions, final AbstractViewerPanel viewer, final AlignPlane plane, final String... defaultKeyStrokes )
	{
		switch ( plane )
		{
		case XY:
			actions.runnableAction( () -> viewer.align( AlignPlane.XY ), ALIGN_XY_PLANE, defaultKeyStrokes );
			break;
		case ZY:
			actions.runnableAction( () -> viewer.align( AlignPlane.ZY ), ALIGN_ZY_PLANE, defaultKeyStrokes );
			break;
		case XZ:
			actions.runnableAction( () -> viewer.align( AlignPlane.XZ ), ALIGN_XZ_PLANE, defaultKeyStrokes );
			break;
		}
	}

	public static void toggleInterpolation( final ViewerState state )
	{
		synchronized ( state )
		{
			state.setInterpolation( state.getInterpolation().next() );
		}
	}

	public static void nextTimePoint( final ViewerState state )
	{
		synchronized ( state )
		{
			final int t = state.getCurrentTimepoint() + 1;
			if ( t < state.getNumTimepoints() )
				state.setCurrentTimepoint( t );
		}
	}

	public static void previousTimePoint( final ViewerState state )
	{
		synchronized ( state )
		{
			final int t = state.getCurrentTimepoint() - 1;
			if ( t >= 0 )
				state.setCurrentTimepoint( t );
		}
	}

	private static void toggleFusedMode( final ViewerState state )
	{
		final DisplayMode mode = state.getDisplayMode();
		state.setDisplayMode( mode.withFused( !mode.hasFused() ) );
	}

	private static void toggleGroupingMode( final ViewerState state )
	{
		final DisplayMode mode = state.getDisplayMode();
		state.setDisplayMode( mode.withGrouping( !mode.hasGrouping() ) );
	}

	private static void setCurrentGroupOrSource( final ViewerState state, final int index )
	{
		synchronized ( state )
		{
			if ( state.getDisplayMode().hasGrouping() )
			{
				final List< SourceGroup > groups = state.getGroups();
				if ( index >= 0 && index < groups.size() )
				{
					final SourceGroup group = groups.get( index );
					state.setCurrentGroup( group );
					final List< SourceAndConverter< ? > > sources = new ArrayList<>( state.getSourcesInGroup( group ) );
					if ( !sources.isEmpty() )
					{
						sources.sort( state.sourceOrder() );
						state.setCurrentSource( sources.get( 0 ) );
					}
				}
			}
			else
			{
				final List< SourceAndConverter< ? > > sources = state.getSources();
				if ( index >= 0 && index < sources.size() )
					state.setCurrentSource( sources.get( index ) );
			}
		}
	}

	private static void toggleGroupOrSourceActive( final ViewerState state, final int index )
	{
		synchronized ( state )
		{
			if ( state.getDisplayMode().hasGrouping() )
			{
				final List< SourceGroup > groups = state.getGroups();
				if ( index >= 0 && index < groups.size() )
				{
					final SourceGroup group = groups.get( index );
					state.setGroupActive( group, !state.isGroupActive( group ) );
				}
			}
			else
			{
				final List< SourceAndConverter< ? > > sources = state.getSources();
				if ( index >= 0 && index < sources.size() )
				{
					final SourceAndConverter< ? > source = sources.get( index );
					state.setSourceActive( source, !state.isSourceActive( source ) );
				}
			}
		}
	}

	// -- deprecated API --

	@Deprecated
	public static final String ALIGN_PLANE = "align %s plane";

	/**
	 * Create navigation actions and install them in the specified
	 * {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param viewer
	 *            Navigation actions are targeted at this {@link ViewerPanel}.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 */
	@Deprecated
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final ViewerPanel viewer,
			final KeyStrokeAdder.Factory keyProperties )
	{
		final NavigationActions actions = new NavigationActions( keyProperties );

		actions.modes( viewer );
		actions.sources( viewer );
		actions.time( viewer );
		actions.alignPlanes( viewer );

		actions.install( inputActionBindings, "navigation" );
	}

	@Deprecated
	public NavigationActions( final KeyStrokeAdder.Factory keyConfig )
	{
		super( keyConfig, "bdv", "navigation" );
	}

	@Deprecated
	public void alignPlaneAction( final ViewerPanel viewer, final AlignPlane plane, final String... defaultKeyStrokes )
	{
		installAlignPlaneAction( this, viewer, plane, defaultKeyStrokes );
	}

	@Deprecated
	public void modes( final ViewerPanel viewer )
	{
		installModeActions( this, viewer.state() );
	}

	@Deprecated
	public void time( final ViewerPanel viewer )
	{
		installTimeActions( this, viewer.state() );
	}

	@Deprecated
	public void sources( final ViewerPanel viewer )
	{
		installSourceActions( this, viewer.state() );
	}

	@Deprecated
	public void alignPlanes( final ViewerPanel viewer )
	{
		installAlignPlaneActions( this, viewer, false );
	}
}
