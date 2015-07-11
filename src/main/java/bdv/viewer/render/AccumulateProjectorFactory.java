package bdv.viewer.render;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

public interface AccumulateProjectorFactory
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
	public AccumulateProjector< ARGBType, ARGBType > createAccumulateProjector(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< ? extends RandomAccessible< ARGBType > > sources,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService );
}