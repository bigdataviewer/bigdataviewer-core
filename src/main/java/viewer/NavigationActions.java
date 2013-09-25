package viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import viewer.SpimViewer.AlignPlane;

public class NavigationActions
{

	private NavigationActions()
	{}

	public static final Action getToggleInterpolationAction( final SpimViewer viewer )
	{
		return new AbstractAction( "toggle interpolation" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				viewer.toggleInterpolation();
			}

			private static final long serialVersionUID = 1L;
		};
	}

	public static final Action getToggleFusedModeAction( final SpimViewer viewer )
	{
		return new AbstractAction( "toggle fused mode" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				viewer.visibilityAndGrouping.setFusedEnabled( !viewer.visibilityAndGrouping.isFusedEnabled() );
			}

			private static final long serialVersionUID = 1L;
		};

	}

	public static final Action getToggleGroupingAction( final SpimViewer viewer )
	{
		return new AbstractAction( "toggle grouping" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				viewer.visibilityAndGrouping.setGroupingEnabled( !viewer.visibilityAndGrouping.isGroupingEnabled() );
			}

			private static final long serialVersionUID = 1L;
		};
	}

	public static final Action getSetCurrentSource( final SpimViewer viewer, final int sourceIndex )
	{
		return new AbstractAction( "set current source " + sourceIndex )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				viewer.setCurrentGroupOrSource( sourceIndex );
			}

			private static final long serialVersionUID = 1L;
		};
	}

	public static final Action getToggleSourceVisibilityAction( final SpimViewer viewer, final int sourceIndex )
	{
		return new AbstractAction( "toggle source visibility " + sourceIndex )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				viewer.toggleActiveGroupOrSource( sourceIndex );
			}

			private static final long serialVersionUID = 1L;
		};
	}

	public static final Action getAlignPlaneAction( final SpimViewer viewer, final AlignPlane plane )
	{
		return new AbstractAction( "align plane " + plane.getName() )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				viewer.align( plane );
			}

			private static final long serialVersionUID = 1L;
		};
	}

	public static final Action getNextTimePointAction( final SpimViewer viewer )
	{
		return new AbstractAction( "next timepoint" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				viewer.sliderTime.setValue( viewer.sliderTime.getValue() + 1 );
			}

			private static final long serialVersionUID = 1L;
		};
	}

	public static Action getPreviousTimePointAction( final SpimViewer viewer )
	{
		return new AbstractAction( "next timepoint" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				viewer.sliderTime.setValue( viewer.sliderTime.getValue() - 1 );
			}

			private static final long serialVersionUID = 1L;
		};
	}
}
