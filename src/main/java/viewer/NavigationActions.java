package viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import viewer.ViewerPanel.AlignPlane;

public class NavigationActions
{

	private NavigationActions()
	{}

	public static final Action getToggleInterpolationAction( final ViewerPanel viewer )
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

	public static final Action getToggleFusedModeAction( final ViewerPanel viewer )
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

	public static final Action getToggleGroupingAction( final ViewerPanel viewer )
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

	public static final Action getSetCurrentSource( final ViewerPanel viewer, final int sourceIndex )
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

	public static final Action getToggleSourceVisibilityAction( final ViewerPanel viewer, final int sourceIndex )
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

	public static final Action getAlignPlaneAction( final ViewerPanel viewer, final AlignPlane plane )
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

	public static final Action getNextTimePointAction( final ViewerPanel viewer )
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

	public static Action getPreviousTimePointAction( final ViewerPanel viewer )
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
