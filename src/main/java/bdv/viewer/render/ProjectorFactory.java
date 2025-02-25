/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;

import bdv.img.cache.VolatileCachedCellImg;
import bdv.util.MipmapTransforms;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import net.imglib2.view.Views;

/**
 * Creates projectors for rendering a given {@code ViewerState} to a given
 * {@code screenImage}, with the current visible sources and timepoint of the
 * {@code ViewerState}, and a given {@code screenTransform} from global
 * coordinates to coordinates in the {@code screenImage}.
 */
class ProjectorFactory
{
	/**
	 * How many threads to use for rendering.
	 */
	private final int numRenderingThreads;

	/**
	 * {@link ExecutorService} used for rendering.
	 */
	private final ExecutorService renderingExecutorService;

	/**
	 * Whether volatile versions of sources should be used if available.
	 */
	private final boolean useVolatileIfAvailable;

	/**
	 * Constructs projector that combines rendere ARGB images for individual
	 * sources to the final screen image. This can be used to customize how
	 * sources are combined.
	 */
	private final AccumulateProjectorFactory< ARGBType > accumulateProjectorFactory;

	/**
	 * Whether repainting should be triggered after the previously
	 * {@link #createProjector constructed} projector returns an incomplete
	 * result.
	 * <p>
	 * The use case for this is the following:
	 * <p>
	 * When scrolling through time, we often get frames for which no data was
	 * loaded yet. To speed up rendering in these cases, use only two mipmap
	 * levels: the optimal and the coarsest. By doing this, we require at most
	 * two passes over the image at the expense of ignoring data present in
	 * intermediate mipmap levels. The assumption is, that we will either be
	 * moving back and forth between images that have all data present already
	 * or that we move to a new image with no data present at all.
	 * <p>
	 * However, we only want this two-pass rendering to happen once, then switch
	 * to normal multi-pass rendering if we remain longer on the same frame.
	 * <p>
	 * (This method is implemented by {@link DefaultMipmapOrdering}.)
	 */
	private boolean newFrameRequest = false;

	/**
	 * The timepoint for which last a projector was {@link #createProjector
	 * created}.
	 */
	private int previousTimepoint = -1;

	// TODO: should be settable
	private final boolean prefetchCells = true;

	/**
	 * @param numRenderingThreads
	 *     How many threads to use for rendering.
	 * @param renderingExecutorService
	 *     if non-null, this is used for rendering. Note, that it is still
	 *     important to supply the numRenderingThreads parameter, because that
	 *     is used to determine into how many sub-tasks rendering is split.
	 * @param useVolatileIfAvailable
	 *     whether volatile versions of sources should be used if available.
	 * @param accumulateProjectorFactory
	 *     can be used to customize how sources are combined.
	 */
	public ProjectorFactory(
			final int numRenderingThreads,
			final ExecutorService renderingExecutorService,
			final boolean useVolatileIfAvailable,
			final AccumulateProjectorFactory< ARGBType > accumulateProjectorFactory )
	{
		this.numRenderingThreads = numRenderingThreads;
		this.renderingExecutorService = renderingExecutorService;
		this.useVolatileIfAvailable = useVolatileIfAvailable;
		this.accumulateProjectorFactory = accumulateProjectorFactory;
	}

	/**
	 * Create a projector for rendering the specified {@code ViewerState} to the
	 * specified {@code screenImage}, with the current visible sources (visible
	 * in {@code ViewerState} and actually currently visible on screen) and
	 * timepoint of the {@code ViewerState}, and the specified
	 * {@code screenTransform} from global coordinates to coordinates in the
	 * {@code screenImage}.
	 */
	public VolatileProjector createProjector(
			final ViewerState viewerState,
			final List< SourceAndConverter< ? > > visibleSourcesOnScreen,
			final RandomAccessibleInterval< ARGBType > screenImage,
			final AffineTransform3D screenTransform,
			final RenderStorage renderStorage )
	{
		/*
		 * This shouldn't be necessary, with
		 * CacheHints.LoadingStrategy==VOLATILE
		 */
//		CacheIoTiming.getIoTimeBudget().clear(); // clear time budget such that prefetching doesn't wait for loading blocks.

		final int width = ( int ) screenImage.dimension( 0 );
		final int height = ( int ) screenImage.dimension( 1 );

		VolatileProjector projector;
		if ( visibleSourcesOnScreen.isEmpty() )
			projector = new EmptyProjector<>( screenImage );
		else if ( visibleSourcesOnScreen.size() == 1 )
		{
			final byte[] maskArray = renderStorage.getMaskArray( 0 );
			projector = createSingleSourceProjector( viewerState, visibleSourcesOnScreen.get( 0 ), screenImage, screenTransform, maskArray );
		}
		else
		{
			final int offsetX = ( int ) screenImage.min( 0 );
			final int offsetY = ( int ) screenImage.min( 1 );
			final ArrayList< VolatileProjector > sourceProjectors = new ArrayList<>();
			final ArrayList< RandomAccessibleInterval< ARGBType > > sourceImages = new ArrayList<>();
			int j = 0;
			for ( final SourceAndConverter< ? > source : visibleSourcesOnScreen )
			{
				final RandomAccessibleInterval< ARGBType > renderImage = renderStorage.getRenderImage( width, height, j );
				final byte[] maskArray = renderStorage.getMaskArray( j );
				++j;
				final AffineTransform3D renderTransform = screenTransform.copy();
				renderTransform.translate( -offsetX, -offsetY, 0 );
				final VolatileProjector p = createSingleSourceProjector( viewerState, source, renderImage, renderTransform, maskArray );
				sourceProjectors.add( p );
				sourceImages.add( renderImage );
			}
			projector = accumulateProjectorFactory.createProjector( sourceProjectors, visibleSourcesOnScreen, sourceImages, Views.zeroMin( screenImage ), numRenderingThreads, renderingExecutorService );
		}
		return projector;
	}

	private < T > VolatileProjector createSingleSourceProjector(
			final ViewerState viewerState,
			final SourceAndConverter< T > source,
			final RandomAccessibleInterval< ARGBType > screenImage,
			final AffineTransform3D screenTransform,
			final byte[] maskArray )
	{
		if ( useVolatileIfAvailable )
		{
			if ( source.asVolatile() != null )
				return createSingleSourceVolatileProjector( viewerState, source.asVolatile(), screenImage, screenTransform, maskArray );
			else if ( source.getSpimSource().getType() instanceof Volatile )
			{
				@SuppressWarnings( "unchecked" )
				final SourceAndConverter< ? extends Volatile< ? > > vsource = ( SourceAndConverter< ? extends Volatile< ? > > ) source;
				return createSingleSourceVolatileProjector( viewerState, vsource, screenImage, screenTransform, maskArray );
			}
		}

		final int bestLevel = getBestMipMapLevel( viewerState, source, screenTransform );
		return new SimpleVolatileProjector<>(
				getTransformedSource( viewerState, source.getSpimSource(), screenTransform, bestLevel, null ),
				source.getConverter(), screenImage );
	}

	private < T extends Volatile< ? > > VolatileProjector createSingleSourceVolatileProjector(
			final ViewerState viewerState,
			final SourceAndConverter< T > source,
			final RandomAccessibleInterval< ARGBType > screenImage,
			final AffineTransform3D screenTransform,
			final byte[] maskArray )
	{
		final ArrayList< RandomAccessible< T > > renderList = new ArrayList<>();
		final Source< T > spimSource = source.getSpimSource();
		final int t = viewerState.getCurrentTimepoint();

		final MipmapOrdering ordering = spimSource instanceof MipmapOrdering ?
				( MipmapOrdering ) spimSource : new DefaultMipmapOrdering( spimSource );

		final MipmapOrdering.MipmapHints hints = ordering.getMipmapHints( screenTransform, t, previousTimepoint );
		final List< MipmapOrdering.Level > levels = hints.getLevels();

		if ( prefetchCells )
		{
			levels.sort( MipmapOrdering.prefetchOrderComparator );
			for ( final MipmapOrdering.Level l : levels )
			{
				final CacheHints cacheHints = l.getPrefetchCacheHints();
				if ( cacheHints == null || cacheHints.getLoadingStrategy() != LoadingStrategy.DONTLOAD )
					prefetch( viewerState, spimSource, screenTransform, l.getMipmapLevel(), cacheHints, screenImage );
			}
		}

		levels.sort( MipmapOrdering.renderOrderComparator );
		for ( final MipmapOrdering.Level l : levels )
			renderList.add( getTransformedSource( viewerState, spimSource, screenTransform, l.getMipmapLevel(), l.getRenderCacheHints() ) );

		if ( hints.renewHintsAfterPaintingOnce() )
			newFrameRequest = true;

		return new VolatileHierarchyProjector<>( renderList, source.getConverter(), screenImage, maskArray );
	}

	/**
	 * Get the mipmap level that best matches the given screen scale for the
	 * given source.
	 *
	 * @param screenTransform
	 *     transforms screen image coordinates to global coordinates.
	 *
	 * @return mipmap level
	 */
	private static int getBestMipMapLevel(
			final ViewerState viewerState,
			final SourceAndConverter< ? > source,
			final AffineTransform3D screenTransform )
	{
		return MipmapTransforms.getBestMipMapLevel( screenTransform, source.getSpimSource(), viewerState.getCurrentTimepoint() );
	}

	private static < T > RandomAccessible< T > getTransformedSource(
			final ViewerState viewerState,
			final Source< T > source,
			final AffineTransform3D screenTransform,
			final int mipmapIndex,
			final CacheHints cacheHints )
	{
		final int timepoint = viewerState.getCurrentTimepoint();

		final RandomAccessibleInterval< T > img = source.getSource( timepoint, mipmapIndex );
		if ( img instanceof VolatileCachedCellImg )
			( ( VolatileCachedCellImg< ?, ? > ) img ).setCacheHints( cacheHints );

		final Interpolation interpolation = viewerState.getInterpolation();
		final RealRandomAccessible< T > ipimg = source.getInterpolatedSource( timepoint, mipmapIndex, interpolation );

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		source.getSourceTransform( timepoint, mipmapIndex, sourceToScreen );
		sourceToScreen.preConcatenate( screenTransform );

		return RealViews.affine( ipimg, sourceToScreen );
	}

	private static < T > void prefetch(
			final ViewerState viewerState,
			final Source< T > source,
			final AffineTransform3D screenTransform,
			final int mipmapIndex,
			final CacheHints prefetchCacheHints,
			final Dimensions screenInterval )
	{
		final int timepoint = viewerState.getCurrentTimepoint();
		final RandomAccessibleInterval< T > img = source.getSource( timepoint, mipmapIndex );
		if ( img instanceof VolatileCachedCellImg )
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

			final Interpolation interpolation = viewerState.getInterpolation();

			final AffineTransform3D sourceToScreen = new AffineTransform3D();
			source.getSourceTransform( timepoint, mipmapIndex, sourceToScreen );
			sourceToScreen.preConcatenate( screenTransform );

			Prefetcher.fetchCells( sourceToScreen, cellDimensions, dimensions, screenInterval, interpolation, cellsRandomAccess );
		}
	}

	public void setPreviousTimepoint( final int t )
	{
		previousTimepoint = t;
		newFrameRequest = false;
	}

	public boolean requestNewFrameIfIncomplete()
	{
		return newFrameRequest;
	}
}
