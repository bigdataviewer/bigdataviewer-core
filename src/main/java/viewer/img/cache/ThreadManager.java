package viewer.img.cache;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * TODO
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
// TODO: remove and make addConsumer / removeConsumer a part of Cache interface?
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
