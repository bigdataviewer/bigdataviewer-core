package viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;

import viewer.ViewerPanel.AlignPlane;

public class NavigationActions
{
	private NavigationActions()
	{}

	public static final String TOGGLE_INTERPOLATION = "toggle interpolation";
	public static final String TOGGLE_FUSED_MODE = "toggle fused mode";
	public static final String TOGGLE_GROUPING = "toggle grouping";
	public static final String SET_CURRENT_SOURCE = "set current source %d";
	public static final String TOGGLE_SOURCE_VISIBILITY = "toggle source visibility %d";
	public static final String ALIGN_PLANE = "align %s plane";
	public static final String NEXT_TIMEPOINT = "next timepoint";
	public static final String PREVIOUS_TIMEPOINT = "previous timepoint";

	public static ActionMap createActionMap( final ViewerPanel viewer, final int numSourceKeys )
	{
		final ActionMap map = new ActionMap();
		addToActionMap( map, viewer, numSourceKeys );
		return map;
	}

	public static void addToActionMap( final ActionMap map, final ViewerPanel viewer, final int numSourceKeys )
	{
		put( map, new ToggleInterPolationAction( viewer ) );
		put( map, new ToggleFusedModeAction( viewer ) );
		put( map, new ToggleGroupingAction( viewer ) );
		put( map, new NextTimePointAction( viewer ) );
		put( map, new PreviousTimePointAction( viewer ) );

		for ( int i = 0; i < numSourceKeys; ++i )
		{
			put( map, new SetCurrentSourceOrGroupAction( viewer, i ) );
			put( map, new ToggleSourceOrGroupVisibilityAction( viewer, i ) );
		}

		for ( final AlignPlane plane : AlignPlane.values() )
			put( map, new AlignPlaneAction( viewer, plane ) );
	}

	private static abstract class NavigationAction extends AbstractAction
	{
		protected final ViewerPanel viewer;

		public NavigationAction( final String name, final ViewerPanel viewer )
		{
			super( name );
			this.viewer = viewer;
		}

		public String name()
		{
			return ( String ) getValue( NAME );
		}

		private static final long serialVersionUID = 1L;
	}

	private static void put( final ActionMap map, final NavigationAction a )
	{
		map.put( a.name(), a );
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
}
