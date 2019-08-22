package bdv.util;

import java.util.function.Supplier;

/**
 * Class that provides a triple-buffer-algorithm. For communication between a painter
 * thread and a display.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Multiple_buffering">Wikipedia Multiple Buffering</a>
 * @param <T> Type of the buffer.
 *
 * @author Matthias Arzt
 */
public class TripleBuffer<T> {

	private T writable;

	private T readable;

	private T exchange;

	private boolean pending = false;

	private final Supplier<T> factory;

	/**
	 * Creates a triple buffer.
	 * @param factory Factory method, that is used to create the three buffers.
	 */
	public TripleBuffer(Supplier<T> factory) {
		this.factory = factory;
	}

	/** Returns a buffer that can be used in the painter thread for rendering. */
	public synchronized T getWritableBuffer() {
		if(writable == null)
			writable = factory.get();
		return writable;
	}

	/**
	 * This method should be called by the painter thread, to signal that the rendering is completed.
	 * Assumes that the buffer used for rendering was returned by the last call to {@link #getWritableBuffer()}
	 */
	public synchronized void doneWriting() {
		doneWriting(writable);
	}

	/** This method should be called by the painter thread, to signal that the rendering is completed. */
	public synchronized void doneWriting(T value) {
		writable = exchange;
		exchange = value;
		pending = true;
	}

	/** This method should be called in the display thread. To get the latest buffer that was completely rendered. */
	public synchronized T getReadableBuffer() {
		if(pending)	{
			T tmp = exchange;
			exchange = readable;
			readable = tmp;
			pending = false;
		}
		return readable;
	}

	/** Free all buffers. */
	public synchronized void clear() {
		writable = null;
		readable = null;
		exchange = null;
	}

	/** Returns true, if {@link #doneWriting()} was called after the las call to {@link #getReadableBuffer()}. */
	public boolean hasUpdate() {
		return pending;
	}
}
