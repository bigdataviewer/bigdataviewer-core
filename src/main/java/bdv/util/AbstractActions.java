package bdv.util;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import org.scijava.ui.behaviour.KeyStrokeAdder;

import bdv.viewer.InputActionBindings;

public class AbstractActions
{
	protected final InputMap inputMap;

	protected final ActionMap actionMap;

	protected final KeyStrokeAdder.Factory keyConfig;

	protected final KeyStrokeAdder keyStrokeAdder;

	/**
	 * Install an {@link InputMap} and a {@link ActionMap} with the given
	 * {@code name} in {@code inputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            where to install the new {@link InputMap}/{@link ActionMap}.
	 * @param inputActionName
	 *            name under which the new {@link InputMap}/{@link ActionMap} is
	 *            installed.
	 * @param keyConfig
	 *            overrides default key strokes.
	 * @param keyConfigContexts
	 *            for which context names in the keyConfig should key strokes be
	 *            retrieved.
	 */
	public AbstractActions(
			final InputActionBindings inputActionBindings,
			final String inputActionName,
			final KeyStrokeAdder.Factory keyConfig,
			final String[] keyConfigContexts )
	{
		this.keyConfig = keyConfig;
		actionMap = new ActionMap();
		inputMap = new InputMap();
		inputActionBindings.addActionMap( inputActionName, actionMap );
		inputActionBindings.addInputMap( inputActionName, inputMap );
		keyStrokeAdder = keyConfig.keyStrokeAdder( inputMap, keyConfigContexts );
	}

	public void runnableAction( final Runnable action, final String name, final String... defaultKeyStrokes )
	{
		keyStrokeAdder.put( name, defaultKeyStrokes );
		new RunnableAction( name, action ).put( actionMap );
	}
}
