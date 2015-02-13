package bdv.img.cache;

import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Queueing structure (intended for cells to be fetched). There is an array of
 * {@link ArrayDeque}s, ordered by priority. Elements are
 * {@link #put(Object, int, boolean)} with a priority and added to one of the
 * queues, accordingly. {@link #take()} returns an element from the highest
 * priority non-empty queue. Furthermore, there is a prefetch deque of bounded
 * size to provides elements when all the queues are exhausted. {@link #clear()}
 * empties all queues, and moves the removed elements to the prefetch queue.
 * Locking is adapted from {@link ArrayBlockingQueue}.
 *
 * @param <E> element type.
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class BlockingFetchQueues< E >
{
	private final ArrayDeque< E >[] queues;

	private final int prefetchCapacity;

	private final ArrayDeque< E > prefetch;

	/** Number of elements in the queue */
	private int count;

	/** Main lock guarding all access */
	private final ReentrantLock lock;

	/** Condition for waiting takes */
	private final Condition notEmpty;

	public BlockingFetchQueues( final int numPriorities )
	{
		this( numPriorities, 16384 );
	}

	@SuppressWarnings( "unchecked" )
	public BlockingFetchQueues( final int numPriorities, final int prefetchCapacity )
	{
		queues = new ArrayDeque[ numPriorities ];
		for ( int i = 0; i < numPriorities; ++i )
			queues[ i ] = new ArrayDeque< E >();
		this.prefetchCapacity = prefetchCapacity;
		prefetch = new ArrayDeque< E >( prefetchCapacity );
		lock = new ReentrantLock();
		notEmpty = lock.newCondition();
	}

	/**
	 * Add element to the queue of the specified priority. The element can be
	 * added to the front or back of the queue.
	 *
	 * @param element
	 *            the element to enqueue
	 * @param priority
	 *            lower values mean higher priority
	 * @param enqueuToFront
	 *            if true, enqueu element at the front (LIFO). if false, enqueue
	 *            element at the back (FIFO)
	 */
	public void put( final E element, final int priority, final boolean enqueuToFront )
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			if ( enqueuToFront )
				queues[ priority ].addFirst( element );
			else
				queues[ priority ].add( element );
			++count;
			notEmpty.signal();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Remove and return an element from the highest priority non-empty queue. If all
	 * queues are empty, then return an element from the prefetch deque. If the
	 * prefetch deque is also empty, then block.
	 *
	 * @return element.
	 * @throws InterruptedException
	 */
	public E take() throws InterruptedException
	{
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			while ( count == 0 )
				notEmpty.await();
			--count;
			for ( final ArrayDeque< E > q : queues )
				if ( !q.isEmpty() )
					return q.remove();
			return prefetch.poll();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Atomically removes all of the elements from this queue. The queue will be
	 * empty after this call returns. Removed elements are moved to the
	 * {@link #prefetch} deque.
	 */
	public void clear()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
//			System.out.println( "prefetch size before clear = " + prefetch.size() );

			// make room in the prefetch deque
			final int toRemoveFromPrefetch = Math.max( 0, Math.min( prefetch.size(), count - prefetchCapacity ) );
//			System.out.println( "toRemoveFromPrefetch = " + toRemoveFromPrefetch );
			if ( toRemoveFromPrefetch == prefetch.size() )
				prefetch.clear();
			else
				for ( int i = 0; i < toRemoveFromPrefetch; ++i )
					prefetch.remove();

			// move queue contents to the prefetch
			int c = prefetchCapacity; // prefetch capacity left
			// add elements of first queue to the front of the prefetch
			final ArrayDeque< E > q0 = queues[ 0 ];
			final int q0n = Math.min( q0.size(), c );
			for ( int i = 0; i < q0n; ++i )
				prefetch.addFirst( q0.removeLast() );
			q0.clear();
			c -= q0n;
			// add elements of remaining queues to the end of the prefetch
			for ( int j = 1; j < queues.length; ++j )
			{
				final ArrayDeque< E > q = queues[ j ];
				final int qn = Math.min( q.size(), c );
				for ( int i = 0; i < qn; ++i )
					prefetch.addLast( q.removeFirst() );
				q.clear();
				c -= qn;
			}

			// update count: only prefetch is non-empty now
			count = prefetch.size();

//			System.out.println( "prefetch size after clear = " + prefetch.size() );
		}
		finally
		{
			lock.unlock();
		}
	}
}
