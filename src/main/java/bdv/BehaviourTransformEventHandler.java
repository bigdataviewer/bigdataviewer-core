package bdv;

import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import net.imglib2.ui.TransformEventHandler;

public interface BehaviourTransformEventHandler< A > extends TransformEventHandler< A >
{
	public void install( final TriggerBehaviourBindings bindings );
}
