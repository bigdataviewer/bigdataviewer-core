package bdv.viewer.render;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import bdv.viewer.Source;

public interface AccumulateProjectorFactory< A >
{
	/**
	 * @param sourceProjectors
	 *            projectors that will be used to render {@code sources}.
	 * @param sources
	 *            sources to identify which channels are being rendered
	 * @param sourceScreenImages
	 *            rendered images that will be accumulated into
	 *            {@code target}.
	 * @param targetScreenImage
	 *            final image to render.
	 * @param numThreads
	 *            how many threads to use for rendering.
	 * @param executorService
	 *            {@link ExecutorService} to use for rendering. may be null.
	 */
	public VolatileProjector createAccumulateProjector(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< Source< ? > > sources,
			final ArrayList< ? extends RandomAccessible< A > > sourceScreenImages,
			final RandomAccessibleInterval< A > targetScreenImage,
			final int numThreads,
			final ExecutorService executorService );
}