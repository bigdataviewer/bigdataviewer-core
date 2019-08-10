package bdv.util;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class DoubleBuffer<T> {

	private T writable;

	private T readable;

	private T exchange;

	private boolean pending = false;

	private final Supplier<T> factory;

	public DoubleBuffer(Supplier<T> factory) {
		this.factory = factory;
	}

	public T getWritableBuffer() {
		return getWritableBuffer(value -> value == null ? factory.get() : value);
	}

	public synchronized T getWritableBuffer(UnaryOperator<T> factory) {
		writable = factory.apply(writable);
		return writable;
	}

	public synchronized void doneWriting() {
		doneWriting(writable);
	}

	public synchronized void doneWriting(T value) {
		writable = exchange;
		exchange = value;
		pending = true;
	}

	public synchronized T getReadableBuffer() {
		if(pending)	{
			T tmp = exchange;
			exchange = readable;
			readable = tmp;
			pending = false;
		}
		return readable;
	}

	public synchronized void clear() {
		writable = null;
		readable = null;
		exchange = null;
	}

	public boolean hasUpdate() {
		return pending;
	}
}
