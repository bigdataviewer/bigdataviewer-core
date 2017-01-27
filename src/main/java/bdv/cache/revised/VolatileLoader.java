package bdv.cache.revised;

import java.util.concurrent.Callable;

public interface VolatileLoader< V > extends Callable< V >
{
	public V createInvalid();
}
