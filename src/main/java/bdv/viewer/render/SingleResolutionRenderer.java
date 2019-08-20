/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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

import bdv.img.cache.VolatileCachedCellImg;
import bdv.util.MipmapTransforms;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.render.MipmapOrdering.Level;
import bdv.viewer.render.MipmapOrdering.MipmapHints;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.cache.iotiming.CacheIoTiming;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.SimpleInterruptibleProjector;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class SingleResolutionRenderer {

	/**
	 * How many threads to use for rendering.
	 */
	private final int numRenderingThreads;

	/**
	 * {@link ExecutorService} used for rendering.
	 */
	private final ExecutorService renderingExecutorService;

	/**
	 * TODO
	 */
	private final AccumulateProjectorFactory< ARGBType > accumulateProjectorFactory;

	/**
	 * Whether volatile versions of sources should be used if available.
	 */
	private final boolean useVolatileIfAvailable;

	/**
	 * Whether a repaint was requested.
	 */
	private boolean newFrameRequest = false;

	/**
	 * The timepoint for which last a projector was
	 * {@link #createProjector  created}.
	 */
	private int previousTimepoint;

	// TODO: should be settable
	private final long[] iobudget = new long[] { 100l * 1000000l,  10l * 1000000l };

	// TODO: should be settable
	private final boolean prefetchCells = true;

	public SingleResolutionRenderer(int numRenderingThreads, ExecutorService renderingExecutorService, AccumulateProjectorFactory<ARGBType> accumulateProjectorFactory, boolean useVolatileIfAvailable)
	{
		this.numRenderingThreads = numRenderingThreads;
		this.renderingExecutorService = renderingExecutorService;
		this.accumulateProjectorFactory = accumulateProjectorFactory;
		this.useVolatileIfAvailable = useVolatileIfAvailable;
		this.previousTimepoint = -1;
	}


	public VolatileProjector createProjector(
			final RenderState renderState,
			final RandomAccessibleInterval<ARGBType> screenImage,
			final List<? extends RandomAccessibleInterval<ARGBType>> renderImages,
			final byte[][] renderMaskArrays)
	{
		this.newFrameRequest = false;
		/*
	  Storage for mask images of {@link VolatileHierarchyProjector}.
	  One array per visible source. (First) index is index in list of visible sources.
	 */
		/*
		 * This shouldn't be necessary, with
		 * CacheHints.LoadingStrategy==VOLATILE
		 */
//		CacheIoTiming.getIoTimeBudget().clear(); // clear time budget such that prefetching doesn't wait for loading blocks.
		final List<RenderSource<?>> sourceStates = renderState.getSources();
		VolatileProjector projector;
		if (sourceStates.isEmpty() || Intervals.isEmpty(screenImage))
			projector = new EmptyProjector<>( screenImage );
		else if ( sourceStates.size() == 1 )
		{
			projector = createSingleSourceProjector(
					renderState, sourceStates.get( 0 ),
					Views.zeroMin(screenImage), renderMaskArrays[ 0 ] );
		}
		else
		{
			final ArrayList< VolatileProjector > sourceProjectors = new ArrayList<>();
			final ArrayList< RandomAccessibleInterval< ARGBType >> sourceImages = new ArrayList<>();
			final ArrayList< Source< ? > > sources = new ArrayList<>();
			for (int i = 0; i < sourceStates.size(); i++) {
				final RandomAccessibleInterval< ARGBType > renderImage = Views.interval(renderImages.get( i ), screenImage);
				final VolatileProjector p = createSingleSourceProjector(
						renderState, sourceStates.get( i ),
						Views.zeroMin(renderImage), renderMaskArrays[ i ]);
				sourceProjectors.add( p );
				sources.add( sourceStates.get( i ).getSpimSource() );
				sourceImages.add( renderImage );
			}
			projector = accumulateProjectorFactory.createAccumulateProjector( sourceProjectors, sources, sourceImages, screenImage, numRenderingThreads, renderingExecutorService );
		}
		previousTimepoint = renderState.getCurrentTimepoint();
		CacheIoTiming.getIoTimeBudget().reset( iobudget );
		return projector;
	}

	public boolean isNewFrameRequest() {
		return newFrameRequest;
	}

	private static class SimpleVolatileProjector< A, B > extends SimpleInterruptibleProjector< A, B > implements VolatileProjector
	{
		private boolean valid = false;

		public SimpleVolatileProjector(
				final RandomAccessible< A > source,
				final Converter< ? super A, B > converter,
				final RandomAccessibleInterval< B > target,
				final int numThreads,
				final ExecutorService executorService )
		{
			super( source, converter, target, numThreads, executorService );
		}

		@Override
		public boolean map( final boolean clearUntouchedTargetPixels )
		{
			final boolean success = super.map();
			valid |= success;
			return success;
		}

		@Override
		public boolean isValid()
		{
			return valid;
		}
	}

	private < T > VolatileProjector createSingleSourceProjector(
			final RenderState renderState,
			final RenderSource<T> source,
			final RandomAccessibleInterval<ARGBType> screenImage,
			final byte[] maskArray)
	{
		if ( useVolatileIfAvailable )
		{
			if ( source.asVolatile() != null )
				return createSingleSourceVolatileProjector(renderState, source.asVolatile(), screenImage, maskArray );
			else if ( source.getSpimSource().getType() instanceof Volatile )
			{
				@SuppressWarnings( "unchecked" )
				final RenderSource< ? extends Volatile< ? > > vsource = (RenderSource< ? extends Volatile< ? > >) source;
				return createSingleSourceVolatileProjector(renderState, vsource, screenImage, maskArray );
			}
		}

		int bestLevel = bestMipMapLevel(renderState, source);
		return new SimpleVolatileProjector<>(
				getTransformedSource(renderState, source, bestLevel, null ),
				source.getConverter(), screenImage, numRenderingThreads, renderingExecutorService );
	}

	private <T> int bestMipMapLevel(RenderState renderState, RenderSource<T> source) {
		return MipmapTransforms.getBestMipMapLevel(
					renderState.getViewerTransform(),
					source.getSpimSource(),
					renderState.getCurrentTimepoint());
	}

	private < T extends Volatile< ? > > VolatileProjector createSingleSourceVolatileProjector(
			final RenderState renderState,
			final RenderSource<T> source,
			final RandomAccessibleInterval<ARGBType> screenImage,
			final byte[] maskArray)
	{
		final ArrayList< RandomAccessible< T > > renderList = new ArrayList<>();
		final Source< T > spimSource = source.getSpimSource();

		final MipmapOrdering ordering = spimSource instanceof MipmapOrdering ?
			( MipmapOrdering ) spimSource : new DefaultMipmapOrdering( spimSource );

		final MipmapHints hints = ordering.getMipmapHints( renderState.getViewerTransform(),
				renderState.getCurrentTimepoint(), previousTimepoint );
		final List< Level > levels = hints.getLevels();

		if ( prefetchCells )
		{
			Collections.sort( levels, MipmapOrdering.prefetchOrderComparator );
			for ( final Level l : levels )
			{
				final CacheHints cacheHints = l.getPrefetchCacheHints();
				if ( cacheHints == null || cacheHints.getLoadingStrategy() != LoadingStrategy.DONTLOAD )
					prefetch(renderState, source, l.getMipmapLevel(), cacheHints, screenImage );
			}
		}

		Collections.sort( levels, MipmapOrdering.renderOrderComparator );
		for ( final Level l : levels )
			renderList.add( getTransformedSource(renderState, source, l.getMipmapLevel(), l.getRenderCacheHints() ) );

		if ( hints.renewHintsAfterPaintingOnce() )
			newFrameRequest = true;

		return new VolatileHierarchyProjector<>( renderList, source.getConverter(), screenImage, maskArray, numRenderingThreads, renderingExecutorService );
	}

	private static < T > RandomAccessible< T > getTransformedSource(
			final RenderState renderState,
			final RenderSource<T> source,
			final int mipmapIndex,
			final CacheHints cacheHints)
	{
		final int timepoint = renderState.getCurrentTimepoint();
		Source<T> spimSource = source.getSpimSource();
		final RandomAccessibleInterval< T > img = spimSource.getSource( timepoint, mipmapIndex );
		if ( VolatileCachedCellImg.class.isInstance( img ) )
			( ( VolatileCachedCellImg< ?, ? > ) img ).setCacheHints( cacheHints );

		final Interpolation interpolation = source.getInterpolation();
		final RealRandomAccessible< T > ipimg = spimSource.getInterpolatedSource( timepoint, mipmapIndex, interpolation );

		final AffineTransform3D sourceToScreen  = renderState.getViewerTransform().copy();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		spimSource.getSourceTransform( timepoint, mipmapIndex, sourceTransform );
		sourceToScreen.concatenate( sourceTransform );

		return RealViews.affine( ipimg, sourceToScreen );
	}

	private static < T > void prefetch(
			final RenderState renderState,
			final RenderSource<T> source,
			final int mipmapIndex,
			final CacheHints prefetchCacheHints,
			final Dimensions screenInterval)
	{
		final int timepoint = renderState.getCurrentTimepoint();
		final RandomAccessibleInterval< T > img = source.getSpimSource().getSource( timepoint, mipmapIndex );
		if ( VolatileCachedCellImg.class.isInstance( img ) )
		{
			final VolatileCachedCellImg< ?, ? > cellImg = ( VolatileCachedCellImg< ?, ? > ) img;

			CacheHints hints = prefetchCacheHints;
			if ( hints == null )
			{
				final CacheHints d = cellImg.getDefaultCacheHints();
				hints = new CacheHints( LoadingStrategy.VOLATILE, d.getQueuePriority(), false );
			}
			cellImg.setCacheHints( hints );
			final int[] cellDimensions = new int[ 3 ];
			cellImg.getCellGrid().cellDimensions( cellDimensions );
			final long[] dimensions = new long[ 3 ];
			cellImg.dimensions( dimensions );
			final RandomAccess< ? > cellsRandomAccess = cellImg.getCells().randomAccess();

			final Interpolation interpolation = source.getInterpolation();
			final AffineTransform3D sourceToScreen  = renderState.getViewerTransform().copy();
			final AffineTransform3D sourceTransform = new AffineTransform3D();
			source.getSpimSource().getSourceTransform( timepoint, mipmapIndex, sourceTransform );
			sourceToScreen.concatenate( sourceTransform );

			Prefetcher.fetchCells( sourceToScreen, cellDimensions, dimensions, screenInterval, interpolation, cellsRandomAccess );
		}
	}
}
