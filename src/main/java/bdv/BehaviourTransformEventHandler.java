package bdv;

import bdv.viewer.TriggerBehaviourBindings;
import net.imglib2.ui.TransformEventHandler;

public interface BehaviourTransformEventHandler< A > extends TransformEventHandler< A >
{
	public void install( final TriggerBehaviourBindings bindings );
}
