package viewer;

public interface Paintable
{
	public void setPainterThread( final MappingThread painter );

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint()} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint()} has
	 * completed).
	 */
	public void requestRepaint();

	/**
	 * This is called by the painter thread to repaint the display.
	 */
	public void paint();

	/**
	 * Add new event handler.
	 * TODO: This should go to another interface
	 */
	public void addHandler( final Object handler );

}
