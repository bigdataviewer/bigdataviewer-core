package viewer;

/**
 * Thread to repaint display.
 */
final public class PainterThread extends Thread
{
	public static interface Paintable
	{
		/**
		 * This is called by the painter thread to repaint the display.
		 */
		public void paint();
	}

	private final Paintable paintable;

	private boolean pleaseRepaint;

	public PainterThread( final Paintable paintable )
	{
		this.paintable = paintable;
		this.pleaseRepaint = false;
		this.setName( "PainterThread" );
	}

	@Override
	public void run()
	{
		while ( !isInterrupted() )
		{
			final boolean b;
			synchronized ( this )
			{
				b = pleaseRepaint;
				pleaseRepaint = false;
			}
			if ( b )
				paintable.paint();
			synchronized ( this )
			{
				try
				{
					if ( !pleaseRepaint )
						wait();
				}
				catch ( final InterruptedException e )
				{}
			}
		}
	}

	/**
	 * request repaint.
	 */
	public void requestRepaint()
	{
		synchronized ( this )
		{
			pleaseRepaint = true;
			notify();
		}
	}
}
