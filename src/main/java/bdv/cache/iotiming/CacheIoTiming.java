package bdv.cache.iotiming;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilities for per {@link ThreadGroup} measuring and budgeting of time spent
 * in (blocking) IO.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class CacheIoTiming
{
	private static final ConcurrentHashMap< ThreadGroup, IoStatistics > perThreadGroupIoStatistics = new ConcurrentHashMap<>();

	public static IoStatistics getIoStatistics()
	{
		return getIoStatistics( Thread.currentThread().getThreadGroup() );
	}

	public static IoStatistics getIoStatistics( final ThreadGroup key )
	{
		IoStatistics statistics = perThreadGroupIoStatistics.get( key );
		if ( statistics == null )
		{
			synchronized ( perThreadGroupIoStatistics )
			{
				statistics = perThreadGroupIoStatistics.get( key );
				if ( statistics == null )
				{
					statistics = new IoStatistics();
					perThreadGroupIoStatistics.put( key, statistics );
				}
			}
		}
		return statistics;
	}

	public static IoTimeBudget getIoTimeBudget()
	{
		return getIoStatistics().getIoTimeBudget();
	}

	public static IoTimeBudget getIoTimeBudget( final ThreadGroup key )
	{
		return getIoStatistics( key ).getIoTimeBudget();
	}

	private CacheIoTiming() {}
}
