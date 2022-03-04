package bdv.viewer;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import org.scijava.listeners.Listeners;

public interface InteractiveDisplay
{
	/**
	 * OverlayRenderers can be added/removed here.
	 * {@link OverlayRenderer#drawOverlays} is invoked for each renderer (in the order they were added).
	 */
	Listeners< OverlayRenderer > overlays();

	/**
	 * Add new event handler. Depending on the interfaces implemented by
	 * <code>handler</code> calls {@link Component#addKeyListener(KeyListener)},
	 * {@link Component#addMouseListener(MouseListener)},
	 * {@link Component#addMouseMotionListener(MouseMotionListener)},
	 * {@link Component#addMouseWheelListener(MouseWheelListener)}.
	 *
	 * @param handler handler to remove
	 */
	void addHandler( Object handler );

	/**
	 * Remove an event handler. Add new event handler. Depending on the
	 * interfaces implemented by <code>handler</code> calls
	 * {@link Component#removeKeyListener(KeyListener)},
	 * {@link Component#removeMouseListener(MouseListener)},
	 * {@link Component#removeMouseMotionListener(MouseMotionListener)},
	 * {@link Component#removeMouseWheelListener(MouseWheelListener)}.
	 *
	 * @param handler handler to remove
	 */
	void removeHandler( Object handler );
}
