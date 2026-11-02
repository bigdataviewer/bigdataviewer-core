/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2026 BigDataViewer developers.
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
package bdv.tools.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.interval.Corners;
import net.imglib2.realtransform.interval.FacesSteps;
import net.imglib2.realtransform.interval.IntervalSamplingMethod;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * A {@link Source} that wraps another {@link Source} and warps it with an
 * {@link RealTransform} applied in <em>world</em> coordinates. This class is
 * intended for use with general, potentially non-affine transformations. For
 * affines, use {@link TransformedSource}.
 * <p>
 * The supplied transform is interpreted in the world coordinate system that the
 * wrapped source maps into via its {@link Source#getSourceTransform
 * getSourceTransform}. Since the images returned by {@link #getSource} and
 * {@link #getInterpolatedSource} are expected to be in the scale levels' pixel
 * coordinates, this class internally maps pixel coordinate to world coordinates
 * before applying the provided transformation, then maps back to pixel
 * coordinates.
 * <p>
 * The provided transformation must be the inverse transformation - taking
 * pixels from target space to source space (see
 * {@link RealTransformRealRandomAccessible}). Note this is the opposite convention
 * as used by {@code TransformedSource}.
 * <p>
 * The bounding intervals for each mipmap level are estimated using the passed
 * {@code boundingBoxEstimator}, using {@link FacesSteps} with 5 steps as the
 * default.
 *
 * @param <T>
 *            the type of the original source
 */
public class RealTransformedSource<T> implements Source<T>, MipmapOrdering
{
	protected final Source< T > source;

	/**
	 * optional new name. if null, the name of the original source will be used.
	 */
	protected final String name;

	/**
	 * This is either the {@link #source} itself, if it implements
	 * {@link MipmapOrdering}, or a {@link DefaultMipmapOrdering}.
	 */
	protected final MipmapOrdering sourceMipmapOrdering;

	/**
	 * The transformation to apply.
	 */
	protected RealTransform transform;

	/**
	 * Pre-computed total transform, one per mipmap level. For each level, the
	 * sequence takes that level's pixel coordinates to world via the source
	 * transform, applies {@link #transform}, then maps back. Rebuilt whenever
	 * {@link #setTransform} is called.
	 */
	private List< RealTransformSequence > transformSequences;

	/**
	 * Pre-computed bounding interval for {@link #getSource}, one per mipmap
	 * level. Rebuilt whenever {@link #setTransform} is called, using timepoint
	 * {@code 0}.
	 */
	private List< Interval > boundingIntervals;

	private final boolean doBoundingBoxCulling;

	/**
	 * Wraps {@code source} with the given world-coordinate {@code transform},
	 * using a default bounding-box estimator that samples the interval faces
	 * (5 steps per axis) and inherits bounding-box culling from {@code source}.
	 *
	 * @param source
	 *            the source to warp
	 * @param name
	 *            optional new name; if {@code null} the wrapped source's name is used
	 * @param transform
	 *            the transform to apply, in world coordinates
	 */
	public RealTransformedSource( final Source< T > source, final String name,
			final RealTransform transform )
	{
		this( source, name, transform, appropriateEstimator(transform, 5), null );
	}

	/**
	 * Wraps {@code source} with the given world-coordinate {@code transform},
	 * using the supplied bounding-box estimator, and inherits bounding-box
	 * culling from {@code source}.
	 *
	 * @param source
	 *            the source to warp
	 * @param name
	 *            optional new name; if {@code null} the wrapped source's name
	 *            is used
	 * @param boundingBoxEstimator
	 *            estimates the world-space interval that {@link #getSource}
	 *            rasterizes over, given the transform and the wrapped interval
	 * @param transform
	 *            the transform to apply, in world coordinates
	 */
	public RealTransformedSource( final Source< T > source, final String name,
		    final BiFunction< RealTransform, RealInterval, RealInterval > boundingBoxEstimator,
			final RealTransform transform )
	{
		this( source, name, transform, boundingBoxEstimator, null );
	}

	/**
	 * Wraps {@code source} with the given world-coordinate {@code transform}.
	 *
	 * @param source
	 *            the source to warp
	 * @param name
	 *            optional new name; if {@code null} the wrapped source's name is used
	 * @param transform
	 *            the transform to apply, in world coordinates
	 * @param boundingBoxEstimator
	 *            estimates the world-space interval that {@link #getSource}
	 *            rasterizes over, given the transform and the wrapped interval
	 * @param doBoundingBoxCulling
	 *            if null, the wrapped source's setting is used
	 */
	public RealTransformedSource( final Source< T > source, final String name,
		    final RealTransform transform,
		    final BiFunction< RealTransform, RealInterval, RealInterval > boundingBoxEstimator,
			final Boolean doBoundingBoxCulling )
	{
		this.source = source;
		this.name = name;
		this.doBoundingBoxCulling = doBoundingBoxCulling == null ? source.doBoundingBoxCulling() : doBoundingBoxCulling;
		setTransform( transform, boundingBoxEstimator );

		sourceMipmapOrdering = MipmapOrdering.class.isInstance( source ) ?
				( MipmapOrdering ) source : new DefaultMipmapOrdering( source );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		return doBoundingBoxCulling;
	}

	/**
	 * Sets the world-coordinate transform to apply and (re)builds the
	 * per-mipmap-level caches derived from it.
	 * <p>
	 * For each level a total transform sequence is precomputed that maps that
	 * level's pixel coordinates to world via the source transform, applies a
	 * {@link RealTransform#copy() copy} of {@code transform}, then
	 * maps back to pixels; a copy is used so each sequence has its own
	 * instance. The bounding interval that {@link #getSource} rasterizes over
	 * is estimated once per level (at timepoint {@code 0}) using the
	 * {@code boundingBoxEstimator}.
	 *
	 * @param transform
	 *            the transform to apply, in world coordinates
	 * @param boundingBoxEstimator
	 *            used to find the new bounding box after applying the
	 *            transformation
	 */
	public void setTransform( final RealTransform transform, BiFunction<RealTransform, RealInterval, RealInterval> boundingBoxEstimator)
	{
		this.transform = transform;

		final int numLevels = getNumMipmapLevels();
		final List< RealTransformSequence > sequences = new ArrayList<>( numLevels );
		final List< Interval > intervals = new ArrayList<>( numLevels );
		for ( int level = 0; level < numLevels; level++ )
		{
			final AffineTransform3D affine = new AffineTransform3D();
			source.getSourceTransform( 0, level, affine );

			final RealTransformSequence seq = new RealTransformSequence();
			seq.add( affine );
			seq.add( transform.copy() );
			seq.add( affine.inverse() );
			sequences.add( seq );

			intervals.add(Intervals.smallestContainingInterval(
					estimateBounds(affine, boundingBoxEstimator, transform, source.getSource(0, level))));
		}
		transformSequences = sequences;
		boundingIntervals = intervals;
	}

	/**
	 * Sets the world-coordinate transform to apply and (re)builds the
	 * per-mipmap-level caches derived from it, using a default bounding-box
	 * estimator appropriate for the given transform (see
	 * {@link #appropriateEstimator}).
	 *
	 * @param transform
	 *            the transform to apply, in world coordinates
	 */
	public void setTransform(final RealTransform transform) {

		// create an appropriate BiFunction for this class of transform
		setTransform(transform, appropriateEstimator(transform, 5));
	}
	
	/**
	 * Return an appropriate bounding box estimator for the given transform.
	 * Uses a {@link FacesSteps} instance with the given number of steps, unless
	 * the transform is affine, in which case it uses {@link Corners}.
	 *
	 * @param transform
	 *            the transform
	 * @param numStepsLongestDimension
	 *            if needed, the number of steps
	 * @return a bounding-box estimator appropriate for the transform
	 */
	private static BiFunction<RealTransform, RealInterval, RealInterval> appropriateEstimator(
			final RealTransform transform, int numStepsLongestDimension) {

		/**
		 * TODO drop this in favor of an identical method in imglib2-realtransform
		 * once an equivalent is available there.
		 */
		final BiFunction<RealTransform, RealInterval, RealInterval> estimator = (t, i) -> {
			if (t instanceof AffineGet) {
				// some AffineGet implementations ignore the IntervalSamplingMethod,
				// but even if used, CORNERS is appropriate for an affine
				return t.boundingInterval(i, IntervalSamplingMethod.CORNERS);
			}
			return facesEstimator(i, numStepsLongestDimension).bounds(i, t);
		};
		return estimator;
	}

	/**
	 * Returns an {@link FacesSteps} interval sampler with steps chosen such
	 * that the largest dimensions has the specified number of steps. Other
	 * dimensions should have a number of steps that has the same spacing as the
	 * longest dimension. At least two points are sampled for every dimension.
	 *
	 * @param interval
	 *            the interval whose per-dimension widths set the step counts
	 * @param numStepsLongestDimension
	 *            the number of steps to sample along the longest dimension
	 * @return a {@link FacesSteps} sampler with per-dimension step counts
	 *         proportional to each dimension's width
	 */
	private static FacesSteps facesEstimator( RealInterval interval, int numStepsLongestDimension ) {

		final int nd = interval.numDimensions();

		double longestWidth = 0.0;
		for ( int i = 0; i < nd; i++ )
		{
			final double w = interval.realMax( i ) - interval.realMin( i );
			if ( w > longestWidth )
				longestWidth = w;
		}

		// the step spacing that yields numStepsLongestDimension steps along the
		// longest dimension; other dimensions use the same spacing so that the
		// number of steps is proportional to their width.
		final double spacing = longestWidth / numStepsLongestDimension;

		final long[] steps = new long[ nd ];
		for ( int i = 0; i < nd; i++ )
		{
			final double w = interval.realMax( i ) - interval.realMin( i );
			// at least one step (two sampled points) per dimension
			steps[ i ] = spacing > 0 ? Math.max( 1, Math.round( w / spacing ) ) : 1;
		}

		return new FacesSteps( steps );
	}

	/**
	 * Estimates the world-space {@link RealInterval} that results from mapping
	 * a pixel interval to world coordinates with the affine {@code pixelToPhysical},
	 * warping it with the inverse of {@code transform}, then mapping back to
	 * pixel coordinates with the inverse of {@code pixelToPhysical}.
	 * <p>
	 * If {@code transform} is not an {@link InvertibleRealTransform}, the inverse
	 * is esimated with a {@link WrappedIterativeInvertibleRealTransform}.
	 * <p>
	 * The affine legs are bounded exactly using their corners; only the
	 * (potentially non-linear) transform {@code transform} is estimated 
	 * using {@code boundingBoxEstimator}.
	 * <p>
	 * This method is used so that the provided {@code boundingBoxEstimator} can be
	 * applied to only the world transformation, ignoring the pixelToPhysical
	 * transformation. Without this, faces bounding box estimator would not be
	 * able to take into account the physical bounding box.
	 *
	 * @param pixelToPhysical 
	 *            the affine mapping pixel to world coordinates
	 * @param boundingBoxEstimator
	 *            function estimating a bounding box from a transformation
	 * @param transform
	 *            the general transform applied in world coordinates
	 * @param interval
	 *            the pixel interval
	 * @return the estimated pixel-space bounding interval
	 */
	private static RealInterval estimateBounds(
			final AffineTransform3D pixelToPhysical,
			final BiFunction<RealTransform, RealInterval, RealInterval> boundingBoxEstimator,
			final RealTransform transform,
			final RealInterval interval)
	{
		/**
		 * The total forward transformation needed for bounding box estimation is:
		 * pixelToPhysical
		 * transform.inverse
		 * pixelToPhysical.inverse
		 */
		final RealInterval physical = pixelToPhysical.estimateBounds(interval);
		final RealInterval transformedPhysical = boundingBoxEstimator.apply(directOrEstimatedInverse(transform), physical);
		final FinalRealInterval transformedPixel = pixelToPhysical.inverse().estimateBounds(transformedPhysical);
		return transformedPixel;
	}

	private static RealTransform directOrEstimatedInverse(RealTransform transform) {

		return makeInvertible(transform).inverse();
	}

	private static InvertibleRealTransform makeInvertible(RealTransform transform) {

		if (transform instanceof InvertibleRealTransform)
			return ((InvertibleRealTransform)transform);

		return new WrappedIterativeInvertibleRealTransform<>(transform);
	}

	public Source< T > getWrappedSource()
	{
		return source;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		// TODO expose interp method? 
		final RealRandomAccessible< T > interpSrc = getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR );
		return Views.interval( Views.raster( interpSrc ), boundingIntervals.get( level ) );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		final RealRandomAccessible<T> realSrc = source.getInterpolatedSource( t, level, method );
		return new RealTransformRealRandomAccessible< T, RealTransform >( realSrc, transformSequences.get( level ) );
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		source.getSourceTransform( t, level, transform );
	}

	public RealTransform getTransform()
	{
		return transform;
	}

	@Override
	public T getType()
	{
		return source.getType();
	}

	@Override
	public String getName()
	{
		if ( name != null ) return name;
		else return source.getName();
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return source.getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return source.getNumMipmapLevels();
	}

	@Override
	public synchronized MipmapHints getMipmapHints( final AffineTransform3D screenTransform, final int timepoint, final int previousTimepoint )
	{
		return sourceMipmapOrdering.getMipmapHints( screenTransform, timepoint, previousTimepoint );
	}

}
