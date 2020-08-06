/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< SourceAndConverter< ? > > sources,
			final ArrayList< ? extends RandomAccessible< ? extends A > > sourceScreenImages,
			final RandomAccessibleInterval< A > targetScreenImage,
			final int numThreads,
			final ExecutorService executorService )
	{
		final ArrayList< Source< ? > > spimSources = new ArrayList<>();
		for ( SourceAndConverter< ? > source : sources )
			spimSources.add( source.getSpimSource() );
		return createAccumulateProjector( sourceProjectors, spimSources, sourceScreenImages, targetScreenImage, numThreads, executorService );
	}

	/**
	 * @deprecated Use {@link #createProjector(ArrayList, ArrayList, ArrayList, RandomAccessibleInterval, int, ExecutorService)} instead.
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
