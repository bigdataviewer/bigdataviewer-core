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

	private static final Behaviour blocked = new Behaviour() {};

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
