package bdv.util;

import java.util.function.Supplier;

/**
 * Class that provides a triple-buffer-algorithm. For communication between a
 * painter thread and a display.
 *
 * @param <T>
 *     Type of the buffer.
 *
 * @author Matthias Arzt
 * @author Tobias Pietzsch
 * @see <a href="https://en.wikipedia.org/wiki/Multiple_buffering">Wikipedia Multiple Buffering</a>
 */
public class TripleBuffer< T >
{
	private T writable;
	private T readable;
	private T exchange;

	private boolean pending = false;

	private final Supplier< T > factory;

	/**
	 * Creates a triple buffer.
	 *
	 * @param factory
	 * 		Factory method, that is used to create the three buffers.
	 */
	public TripleBuffer( Supplier< T > factory )
	{
		this.factory = factory;
	}

	public TripleBuffer()
	{
		this( () -> null );
	}

	/**
	 * Returns a buffer that can be used in the painter thread for rendering.
	 * The returned buffer might be a previously {@link #doneWriting submitted}
	 * buffer that is not used by the display thread anymore.
	 *
	 * @return buffer that can be used for rendering, may be {@code null}.
	 */
	public synchronized T getWritableBuffer()
	{
		if ( writable == null )
			writable = factory.get();
		return writable;
	}

	/**
	 * This method should be called by the painter thread, to signal that the rendering is completed.
	 * Assumes that the buffer used for rendering was returned by the last call to {@link #getWritableBuffer()}
	 */
	public synchronized void doneWriting()
	{
		doneWriting( writable );
	}

	/**
	 * This method should be called by the painter thread, to submit a completed
	 * buffer. This buffer will be supplied to the display thread with the next
	 * {@link #getReadableBuffer} (unless another {@link #doneWriting} happens
	 * in between).
	 */
	public synchronized void doneWriting( final T value )
	{
		writable = exchange;
		exchange = value;
		pending = true;
	}

	/**
	 * This method should be called in the display thread. To get the latest
	 * buffer that was completely rendered.
	 */
	public synchronized ReadableBuffer< T > getReadableBuffer()
	{
		final boolean updated = pending;
		if ( pending )
		{
			final T tmp = exchange;
			exchange = readable;
			readable = tmp;
			pending = false;
		}
		return new ReadableBuffer<>( readable, updated );
	}

	/**
	 * Returned from {@link #getReadableBuffer}.
	 * A buffer and a flag indicating whether the buffer is different than the one
	 * returned from the previous {@link #getReadableBuffer}
	 */
	public static class ReadableBuffer< T >
	{
		private final T buffer;

		private final boolean updated;

		ReadableBuffer( final T buffer, final boolean updated )
		{
			this.buffer = buffer;
			this.updated = updated;
		}

		public T getBuffer()
		{
			return buffer;
		}

		public boolean isUpdated()
		{
			return updated;
		}
	}

	/**
	 * Free all buffers.
	 */
	public synchronized void clear()
	{
		writable = null;
		readable = null;
		exchange = null;
	}
}
