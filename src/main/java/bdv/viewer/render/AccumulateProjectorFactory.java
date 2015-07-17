package bdv.viewer.render;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;

public interface AccumulateProjectorFactory< A >
{
	/**
	 * @param sourceProjectors
	 *            projectors that will be used to render {@code sources}.
	 * @param sources
	 *            rendered images that will be accumulated into
	 *            {@code target}.
	 * @param target
	 *            final image to render.
	 * @param numThreads
	 *            how many threads to use for rendering.
	 * @param executorService
	 *            {@link ExecutorService} to use for rendering. may be null.
	 */
	public VolatileProjector createAccumulateProjector(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< ? extends RandomAccessible< A > > sources,
			final RandomAccessibleInterval< A > target,
			final int numThreads,
			final ExecutorService executorService );
}