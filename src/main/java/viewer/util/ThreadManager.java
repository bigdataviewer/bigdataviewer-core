package viewer.util;

import java.util.ArrayList;
import java.util.HashSet;

public class ThreadManager
{
	private final HashSet< Object > consumers;

	private final ArrayList< Thread > threads;

	public ThreadManager()
	{
		consumers = new HashSet< Object >();
		threads = new ArrayList< Thread >();
	}

	public void addConsumer( final Object o )
	{
		consumers.add( o );
	}

	public void removeConsumer( final Object o )
	{
		consumers.remove( o );
		if ( consumers.isEmpty() )
			for ( final Thread t : threads )
				t.interrupt();
	}

	public void addThread( final Thread thread )
	{
		threads.add( thread );
	}
}
