package bdv.ui.viewermodepanel;

import bdv.viewer.DisplayMode;
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

	private final TriggerBehaviourBindings bindings;

	private BehaviourMap blockRotationBehaviourMap;

	private BehaviourMap blockTranslationBehaviourMap;

	/**
	 * Keeps track of the current viewer state and handles the transformation blocking.
	 *
	 * @param triggerBindings
	 */
	public ViewerModesModel( final TriggerBehaviourBindings triggerBindings )
	{
		this.bindings = triggerBindings;
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
}
