package bdv.ui.viewermodepanel;

import bdv.viewer.DisplayMode;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

/**
 * This model handles the translation-
 * and rotation-block behaviours.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG, Dresden
 */
public class NavigationModesModel
{
	private static final String BLOCK_ROTATION_KEY = "block rotation";
	private static final String BLOCK_TRANSLATION_KEY = "block translation";

	private final TriggerBehaviourBindings bindings;

	private BehaviourMap blockRotationBehaviourMap;
	private BehaviourMap blockTranslationBehaviourMap;

	private boolean isRotationBlocked = false;
	private boolean isTranslationBlocked = false;

	public interface NavigationModeChangeListener
	{
		void navigationModeChanged();
	}

	private final Listeners.List< NavigationModeChangeListener > listeners;

	/**
	 * Keeps track of the current viewer state and handles the transformation blocking.
	 *
	 * @param triggerBindings
	 */
	public NavigationModesModel( final TriggerBehaviourBindings triggerBindings )
	{
		bindings = triggerBindings;
		listeners = new Listeners.List<>();
	}

	private BehaviourMap getBlockTranslationBehaviourMap()
	{
		if ( blockTranslationBehaviourMap == null )
		{
			blockTranslationBehaviourMap = new BehaviourMap();
			final Behaviour blocked = new Behaviour() {};
			blockTranslationBehaviourMap.put( "drag translate", blocked );

			// 2D
			blockTranslationBehaviourMap.put( "2d drag translate", blocked );
		}
		return blockTranslationBehaviourMap;
	}

	private BehaviourMap getBlockRotationBehaviourMap()
	{
		if ( blockRotationBehaviourMap == null )
		{
			blockRotationBehaviourMap = new BehaviourMap();
			final Behaviour blocked = new Behaviour() {};
			blockRotationBehaviourMap.put( "rotate left", blocked );
			blockRotationBehaviourMap.put( "rotate left slow", blocked );
			blockRotationBehaviourMap.put( "rotate left fast", blocked );

			blockRotationBehaviourMap.put( "rotate right", blocked );
			blockRotationBehaviourMap.put( "rotate right slow", blocked );
			blockRotationBehaviourMap.put( "rotate right fast", blocked );

			blockRotationBehaviourMap.put( "drag rotate", blocked );
			blockRotationBehaviourMap.put( "drag rotate slow", blocked );
			blockRotationBehaviourMap.put( "drag rotate fast", blocked );

			// 2D
			blockRotationBehaviourMap.put( "2d drag rotate", blocked );
			blockRotationBehaviourMap.put( "2d scroll rotate", blocked );
			blockRotationBehaviourMap.put( "2d scroll rotate slow", blocked );
			blockRotationBehaviourMap.put( "2d scroll rotate fast", blocked );
			blockRotationBehaviourMap.put( "2d scroll translate", blocked );
			blockRotationBehaviourMap.put( "2d rotate left", blocked );
			blockRotationBehaviourMap.put( "2d rotate right", blocked );
		}
		return blockRotationBehaviourMap;
	}

	/**
	 * {@code NavigationModeChangeListener}s can be added/removed here.
	 */
	public Listeners< NavigationModeChangeListener > changeListeners()
	{
		return listeners;
	}

	public boolean isTranslationBlocked()
	{
		return isTranslationBlocked;
	}

	public void setTranslationBlocked( boolean isBlocked )
	{
		System.out.println( "NavigationModesModel.setTranslationBlocked" );
		System.out.println( "isBlocked = " + isBlocked );
		final boolean notify = isTranslationBlocked != isBlocked;
		isTranslationBlocked = isBlocked;
		if ( isBlocked )
			bindings.addBehaviourMap( BLOCK_TRANSLATION_KEY, getBlockTranslationBehaviourMap() );
		else
			bindings.removeBehaviourMap( BLOCK_TRANSLATION_KEY );
		if ( notify )
			listeners.list.forEach( NavigationModeChangeListener::navigationModeChanged );
	}

	public boolean isRotationBlocked()
	{
		return isRotationBlocked;
	}

	public void setRotationBlocked( boolean isBlocked )
	{
		System.out.println( "NavigationModesModel.setRotationBlocked" );
		System.out.println( "isBlocked = " + isBlocked );
		final boolean notify = isRotationBlocked != isBlocked;
		isRotationBlocked = isBlocked;
		if ( isBlocked )
			bindings.addBehaviourMap( BLOCK_ROTATION_KEY, getBlockRotationBehaviourMap() );
		else
			bindings.removeBehaviourMap( BLOCK_ROTATION_KEY );
		if ( notify )
			listeners.list.forEach( NavigationModeChangeListener::navigationModeChanged );
	}
}
