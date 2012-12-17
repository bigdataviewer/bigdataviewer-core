package viewer;

/**
 * Thread to repaint display.
 */
final public class MappingThread extends Thread
{
	private boolean pleaseRepaint = true;

	private Paintable paintable;

	public MappingThread()
	{
		this.setName( "MappingThread" );
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
	public void repaint()
	{
		synchronized ( this )
		{
			pleaseRepaint = true;
			notify();
		}
	}

	public void setPaintable( final Paintable paintable )
	{
		this.paintable = paintable;
	}
}
