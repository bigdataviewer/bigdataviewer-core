package bdv;

import org.scijava.ui.behaviour.io.InputTriggerConfig;

import net.imglib2.ui.TransformEventHandlerFactory;

public interface BehaviourTransformEventHandlerFactory< A > extends TransformEventHandlerFactory< A >
{
	public void setConfig( final InputTriggerConfig config );
}
