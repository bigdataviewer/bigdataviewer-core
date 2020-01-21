package bdv.ui.viewermodepanel;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.ViewerState;
import bdv.viewer.ViewerStateChange;
import java.util.ArrayList;
import java.util.List;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

/**
 * This model handles the {@link DisplayMode}s and toggles the translation-
 * and rotation-block behaviours.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG, Dresden
 */
public class ViewerModesModel
{
	private static final String BLOCK_ROTATION_KEY = "block rotation";

	private static final String BLOCK_TRANSLATION_KEY = "block translation";

	private final ViewerState state;

	private final TriggerBehaviourBindings bindings;

	private BehaviourMap blockRotationBehaviourMap;

	private BehaviourMap blockTranslationBehaviourMap;

	private final List< ViewerModeListener > viewerModeListeners = new ArrayList<>();

	/**
	 * Keeps track of the current viewer state and handles the transformation blocking.
	 *
	 * @param state
	 * @param triggerBindings
	 */
	public ViewerModesModel( final ViewerState state, final TriggerBehaviourBindings triggerBindings )
	{
		this.state = state;
		state.changeListeners().add( e -> {
			if ( e == ViewerStateChange.DISPLAY_MODE_CHANGED )
			{
				changeDisplayMode();
			} else if ( e == ViewerStateChange.INTERPOLATION_CHANGED )
			{
				fireInterpolationMode( state.getInterpolation() );
			}
		} );

		this.bindings = triggerBindings;
	}

	private void changeDisplayMode()
	{
		final DisplayMode dm = state.getDisplayMode();
		switch ( dm )
		{
		case FUSED:
			fireFusedMode();
			fireSourceMode();
			break;
		case SINGLE:
			fireSingleMode();
			fireSourceMode();
			break;
		case FUSEDGROUP:
			fireFusedMode();
			fireGroupMode();
			break;
		case GROUP:
			fireSingleMode();
			fireGroupMode();
			break;
		}
	}

	private BehaviourMap getBlockTranslationBehaviourMap()
	{
		if ( blockTranslationBehaviourMap == null )
		{
			blockTranslationBehaviourMap = new BehaviourMap();
			blockTranslationBehaviourMap.put( "drag translate", new Behaviour()
			{
			} );

			// 2D
			blockTranslationBehaviourMap.put( "2d drag translate", new Behaviour()
			{
			} );
		}
		return blockTranslationBehaviourMap;
	}

	private BehaviourMap getBlockRotationBehaviourMap()
	{
		if ( blockRotationBehaviourMap == null )
		{
			blockRotationBehaviourMap = new BehaviourMap();
			blockRotationBehaviourMap.put( "rotate left", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "rotate left slow", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "rotate left fast", new Behaviour()
			{
			} );

			blockRotationBehaviourMap.put( "rotate right", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "rotate right slow", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "rotate right fast", new Behaviour()
			{
			} );

			blockRotationBehaviourMap.put( "drag rotate", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "drag rotate slow", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "drag rotate fast", new Behaviour()
			{
			} );

			// 2D
			blockRotationBehaviourMap.put( "2d drag rotate", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "2d scroll rotate", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "2d scroll rotate slow", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "2d scroll rotate fast", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "2d scroll translate", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "2d rotate left", new Behaviour()
			{
			} );
			blockRotationBehaviourMap.put( "2d rotate right", new Behaviour()
			{
			} );
		}
		return blockRotationBehaviourMap;
	}

	public void blockTranslation()
	{
		bindings.addBehaviourMap( BLOCK_TRANSLATION_KEY, getBlockTranslationBehaviourMap() );
	}

	public void unblockTranslation()
	{
		bindings.removeBehaviourMap( BLOCK_TRANSLATION_KEY );
	}

	public void blockRotation()
	{
		bindings.addBehaviourMap( BLOCK_ROTATION_KEY, getBlockRotationBehaviourMap() );
	}

	public void unblockRotation()
	{
		bindings.removeBehaviourMap( BLOCK_ROTATION_KEY );
	}

	/**
	 * Set fused-mode:
	 * * {@link DisplayMode#SINGLE} <-> {@link DisplayMode#FUSED}
	 * * {@link DisplayMode#GROUP} <-> {@link DisplayMode#FUSEDGROUP}
	 *
	 * @param fused
	 */
	public void setFused( final boolean fused )
	{
		if ( fused )
		{
			switch ( state.getDisplayMode() )
			{
			case SINGLE:
				state.setDisplayMode( DisplayMode.FUSED );
				break;
			case GROUP:
				state.setDisplayMode( DisplayMode.FUSEDGROUP );
				break;
			case FUSED:
				break;
			case FUSEDGROUP:
				break;
			}
		}
		else
		{
			switch ( state.getDisplayMode() )
			{
			case SINGLE:
				break;
			case GROUP:
				break;
			case FUSED:
				state.setDisplayMode( DisplayMode.SINGLE );
				break;
			case FUSEDGROUP:
				state.setDisplayMode( DisplayMode.GROUP );
				break;
			}
		}
	}

	/**
	 * Set grouping-mode:
	 * * {@link DisplayMode#SINGLE} <-> {@link DisplayMode#GROUP}
	 * * {@link DisplayMode#FUSED} <-> {@link DisplayMode#FUSEDGROUP}
	 *
	 * @param selected
	 */
	public void setGrouping( final boolean selected )
	{
		if ( selected )
		{
			switch ( state.getDisplayMode() )
			{
			case SINGLE:
				state.setDisplayMode( DisplayMode.GROUP );
				break;
			case GROUP:
				break;
			case FUSED:
				state.setDisplayMode( DisplayMode.FUSEDGROUP );
				break;
			case FUSEDGROUP:
				break;
			}
		}
		else
		{
			switch ( state.getDisplayMode() )
			{
			case SINGLE:
				break;
			case GROUP:
				state.setDisplayMode( DisplayMode.SINGLE );
				break;
			case FUSED:
				break;
			case FUSEDGROUP:
				state.setDisplayMode( DisplayMode.FUSED );
				break;
			}
		}
	}

	/**
	 * Set the viewer {@link Interpolation}-mode.
	 *
	 * @param interpolation_mode
	 */
	public void setInterpolation( final Interpolation interpolation_mode )
	{
		state.setInterpolation( interpolation_mode );
	}

	interface ViewerModeListener
	{
		void fusedMode();

		void singleMode();

		void sourceMode();

		void groupMode();

		void interpolationMode( final Interpolation interpolation_mode );
	}

	public void addViewerModeListener( final ViewerModeListener listener )
	{
		viewerModeListeners.add( listener );
	}

	private void fireFusedMode()
	{
		viewerModeListeners.forEach( l -> l.fusedMode() );
	}

	private void fireSingleMode()
	{
		viewerModeListeners.forEach( l -> l.singleMode() );
	}

	private void fireSourceMode()
	{
		viewerModeListeners.forEach( l -> l.sourceMode() );
	}

	private void fireGroupMode()
	{
		viewerModeListeners.forEach( l -> l.groupMode() );
	}

	private void fireInterpolationMode( final Interpolation interpolation_mode )
	{
		viewerModeListeners.forEach( l -> l.interpolationMode( interpolation_mode ) );
	}
}
