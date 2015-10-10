/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import bdv.util.AbstractNamedAction;
import bdv.util.AbstractNamedAction.NamedActionAdder;
import bdv.util.KeyProperties;
import bdv.util.KeyProperties.KeyStrokeAdder;
import bdv.viewer.ViewerPanel.AlignPlane;

public class NavigationActions
{
	public static final String TOGGLE_INTERPOLATION = "toggle interpolation";
	public static final String TOGGLE_FUSED_MODE = "toggle fused mode";
	public static final String TOGGLE_GROUPING = "toggle grouping";
	public static final String SET_CURRENT_SOURCE = "set current source %d";
	public static final String TOGGLE_SOURCE_VISIBILITY = "toggle source visibility %d";
	public static final String ALIGN_PLANE = "align %s plane";
	public static final String NEXT_TIMEPOINT = "next timepoint";
	public static final String PREVIOUS_TIMEPOINT = "previous timepoint";

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
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final ViewerPanel viewer,
			final KeyProperties keyProperties )
	{
		inputActionBindings.addActionMap( "navigation", createActionMap( viewer ) );
		inputActionBindings.addInputMap( "navigation", createInputMap( keyProperties ) );
	}

	public static InputMap createInputMap( final KeyProperties keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.adder( inputMap );

		map.put( TOGGLE_INTERPOLATION, "I" );
		map.put( TOGGLE_FUSED_MODE, "F" );
		map.put( TOGGLE_GROUPING, "G" );
		map.put( NEXT_TIMEPOINT, "CLOSE_BRACKET", "M" );
		map.put( PREVIOUS_TIMEPOINT, "OPEN_BRACKET", "N" );

		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		for ( int i = 0; i < numkeys.length; ++i )
		{
			map.put( String.format( SET_CURRENT_SOURCE, i ), numkeys[ i ] );
			map.put( String.format( TOGGLE_SOURCE_VISIBILITY, i ), "shift " + numkeys[ i ] );
		}

		map.put( String.format( ALIGN_PLANE, AlignPlane.XY ), "shift Z" );
		map.put( String.format( ALIGN_PLANE, AlignPlane.ZY ), "shift X" );
		map.put( String.format( ALIGN_PLANE, AlignPlane.XZ ), "shift Y", "shift A" );

		return inputMap;
	}

	public static ActionMap createActionMap( final ViewerPanel viewer )
	{
		return createActionMap( viewer, 10 );
	}

	public static ActionMap createActionMap( final ViewerPanel viewer, final int numSourceKeys )
	{
		final ActionMap actionMap = new ActionMap();
		addToActionMap( actionMap, viewer, numSourceKeys );
		return actionMap;
	}

	public static void addToActionMap( final ActionMap actionMap, final ViewerPanel viewer, final int numSourceKeys )
	{
		final NamedActionAdder map = new NamedActionAdder( actionMap );

		map.put( new ToggleInterPolationAction( viewer ) );
		map.put( new ToggleFusedModeAction( viewer ) );
		map.put( new ToggleGroupingAction( viewer ) );
		map.put( new NextTimePointAction( viewer ) );
		map.put( new PreviousTimePointAction( viewer ) );

		for ( int i = 0; i < numSourceKeys; ++i )
		{
			map.put( new SetCurrentSourceOrGroupAction( viewer, i ) );
			map.put( new ToggleSourceOrGroupVisibilityAction( viewer, i ) );
		}

		for ( final AlignPlane plane : AlignPlane.values() )
			map.put( new AlignPlaneAction( viewer, plane ) );
	}

	private static abstract class NavigationAction extends AbstractNamedAction
	{
		protected final ViewerPanel viewer;

		public NavigationAction( final String name, final ViewerPanel viewer )
		{
			super( name );
			this.viewer = viewer;
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleInterPolationAction extends NavigationAction
	{
		public ToggleInterPolationAction( final ViewerPanel viewer )
		{
			super( TOGGLE_INTERPOLATION, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.toggleInterpolation();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleFusedModeAction extends NavigationAction
	{
		public ToggleFusedModeAction( final ViewerPanel viewer )
		{
			super( TOGGLE_FUSED_MODE, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.getVisibilityAndGrouping().setFusedEnabled( !viewer.visibilityAndGrouping.isFusedEnabled() );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleGroupingAction extends NavigationAction
	{
		public ToggleGroupingAction( final ViewerPanel viewer )
		{
			super( TOGGLE_GROUPING, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.getVisibilityAndGrouping().setGroupingEnabled( !viewer.visibilityAndGrouping.isGroupingEnabled() );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class SetCurrentSourceOrGroupAction extends NavigationAction
	{
		private final int sourceIndex;

		public SetCurrentSourceOrGroupAction( final ViewerPanel viewer, final int sourceIndex )
		{
			super( String.format( SET_CURRENT_SOURCE, sourceIndex ), viewer );
			this.sourceIndex = sourceIndex;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.getVisibilityAndGrouping().setCurrentGroupOrSource( sourceIndex );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ToggleSourceOrGroupVisibilityAction extends NavigationAction
	{
		private final int sourceIndex;

		public ToggleSourceOrGroupVisibilityAction( final ViewerPanel viewer, final int sourceIndex )
		{
			super( String.format( TOGGLE_SOURCE_VISIBILITY, sourceIndex ), viewer );
			this.sourceIndex = sourceIndex;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.getVisibilityAndGrouping().toggleActiveGroupOrSource( sourceIndex );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class AlignPlaneAction extends NavigationAction
	{
		private final AlignPlane plane;

		public AlignPlaneAction( final ViewerPanel viewer, final AlignPlane plane )
		{
			super( String.format( ALIGN_PLANE, plane.getName() ), viewer );
			this.plane = plane;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.align( plane );
		}

		private static final long serialVersionUID = 1L;
	}

	public static class NextTimePointAction extends NavigationAction
	{
		public NextTimePointAction( final ViewerPanel viewer )
		{
			super( NEXT_TIMEPOINT, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.nextTimePoint();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class PreviousTimePointAction extends NavigationAction
	{
		public PreviousTimePointAction( final ViewerPanel viewer )
		{
			super( PREVIOUS_TIMEPOINT, viewer );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			viewer.previousTimePoint();
		}

		private static final long serialVersionUID = 1L;
	}

	private NavigationActions()
	{}
}
