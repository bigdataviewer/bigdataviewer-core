/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.viewer.render;

import bdv.viewer.SourceAndConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import bdv.viewer.Source;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;

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
	default VolatileProjector createProjector(
			final List< VolatileProjector > sourceProjectors,
			final List< SourceAndConverter< ? > > sources,
			final List< ? extends RandomAccessible< ? extends A > > sourceScreenImages,
			final RandomAccessibleInterval< A > targetScreenImage,
			final int numThreads,
			final ExecutorService executorService )
	{
		final ArrayList< Source< ? > > spimSources = new ArrayList<>();
		for ( SourceAndConverter< ? > source : sources )
			spimSources.add( source.getSpimSource() );
		final ArrayList< VolatileProjector > sp = sourceProjectors instanceof ArrayList
				? ( ArrayList ) sourceProjectors
				: new ArrayList<>( sourceProjectors );
		final ArrayList< ? extends RandomAccessible< ? extends A > > si = sourceScreenImages instanceof ArrayList
				? ( ArrayList ) sourceScreenImages
				: new ArrayList<>( sourceScreenImages );
		return createAccumulateProjector( sp, spimSources, si, targetScreenImage, numThreads, executorService );
	}

	/**
	 * @deprecated Use {@link #createProjector(List, List, List, RandomAccessibleInterval, int, ExecutorService)} instead.
	 * The new variant of the method is named "createProjector" instead of
	 * "createAccumulateProjector" because it has the same erasure. The default
	 * implementation of "createProjector" is forwarded to the (deprecated)
	 * "createAccumulateProjector", so existing AccumulateProjectorFactory
	 * implementations should keep working.
	 *
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
	@Deprecated
	default VolatileProjector createAccumulateProjector(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< Source< ? > > sources,
			final ArrayList< ? extends RandomAccessible< ? extends A > > sourceScreenImages,
			final RandomAccessibleInterval< A > targetScreenImage,
			final int numThreads,
			final ExecutorService executorService )
	{
		throw new UnsupportedOperationException( "AccumulateProjectorFactory::createAccumulateProjector is deprecated and by default not implemented" );
	}
}
