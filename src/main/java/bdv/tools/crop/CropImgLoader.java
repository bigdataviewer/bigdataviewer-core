/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.tools.crop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import bdv.util.IntervalBoundingBox;
import bdv.viewer.Source;

/**
 * This {@link ImgLoader} provides views and transformations into a cropped
 * region of a data-set (provided by list of {@link Source Sources}).
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class CropImgLoader implements BasicImgLoader
{
	private final AffineTransform3D globalToCropTransform;

	private final RealInterval cropInterval;

	private final Map< Integer, Integer > timepointIdToTimepointIndex;

	private final Map< Integer, SetupImgLoader< ? > > setupImgLoaders;

	public CropImgLoader(
			final ArrayList< Source< ? > > sources,
			final AffineTransform3D globalToCropTransform,
			final RealInterval cropInterval,
			final Map< Integer, Integer > timepointIdToTimepointIndex,
			final Map< Integer, Integer > setupIdToSourceIndex )
	{
		this.globalToCropTransform = globalToCropTransform;
		this.cropInterval = cropInterval;
		this.timepointIdToTimepointIndex = timepointIdToTimepointIndex;
		this.setupImgLoaders = new HashMap< Integer, SetupImgLoader< ? > >();
		for ( final Entry< Integer, Integer > entry : setupIdToSourceIndex.entrySet() )
			setupImgLoaders.put( entry.getKey(), createSetupImgLoader( sources.get( entry.getValue() ) ) );
	}

	public AffineTransform3D getCroppedTransform( final ViewId view )
	{
		final Pair< ?, AffineTransform3D > pair = getSetupImgLoader( view.getViewSetupId() ).cropView( view.getTimePointId() );
		return pair.getB();
	}

	@Override
	public SetupImgLoader< ? > getSetupImgLoader( final int setupId )
	{
		return setupImgLoaders.get( setupId );
	}

	/**
	 * Crop a region from a {@link Source}.
	 *
	 * @param globalToCropTransform
	 *            transform from global into crop-local coordinates.
	 * @param interval
	 *            the interval (in crop-local space) to crop.
	 * @param source
	 *            which source to crop from.
	 * @param timepoint
	 *            which timepoint to crop from.
	 * @return a zero-min view of the cropped region and a transform from that
	 *         view into global coordinates.
	 */
	public static < T > Pair< RandomAccessibleInterval< T >, AffineTransform3D > crop(
			final AffineTransform3D globalToCropTransform,
			final RealInterval interval,
			final Source< T > source,
			final int timepoint )
	{
		final int n = interval.numDimensions();
		final AffineTransform3D cropToGlobal = globalToCropTransform.inverse();

		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		source.getSourceTransform( timepoint, 0, sourceToGlobal );
		final AffineTransform3D globalToSource = sourceToGlobal.inverse();
		final AffineTransform3D cropToSource = globalToSource.copy();
		cropToSource.concatenate( cropToGlobal );

		final ArrayList< RealPoint > sourceCorners = new ArrayList< RealPoint >();
		for ( final RealLocalizable corner : IntervalBoundingBox.getCorners( interval ) )
		{
			final RealPoint sourceCorner = new RealPoint( n );
			cropToSource.apply( corner, sourceCorner );
			sourceCorners.add( sourceCorner );
		}
		final RandomAccessibleInterval< T > sourceImg = source.getSource( timepoint, 0 );
		final Interval cropBoundingBox = Intervals.smallestContainingInterval( IntervalBoundingBox.getBoundingBox( sourceCorners ) );
		Interval sourceInterval = Intervals.intersect( cropBoundingBox, sourceImg );

		final RandomAccessibleInterval< T > croppedSourceImg;
		final AffineTransform3D croppedSourceTransform = new AffineTransform3D();

		if ( Intervals.isEmpty( sourceInterval ) )
		{
			final long[] minsize = new long[ n * 2 ];
			for ( int d = 0; d < n; ++d )
				minsize[ d ] = cropBoundingBox.min( d ) + cropBoundingBox.dimension( d ) / 2;
			Arrays.fill( minsize, n, n * 2, 1 );
			sourceInterval = Intervals.createMinSize( minsize );
		}

		croppedSourceImg = Views.zeroMin( Views.interval( Views.extendBorder( sourceImg ), sourceInterval ) );
		croppedSourceTransform.set(
			1, 0, 0, sourceInterval.min( 0 ),
			0, 1, 0, sourceInterval.min( 1 ),
			0, 0, 1, sourceInterval.min( 2 ) );
		croppedSourceTransform.preConcatenate( sourceToGlobal );

		return new ValuePair< RandomAccessibleInterval<T>, AffineTransform3D >( croppedSourceImg, croppedSourceTransform );
	}

	private < T > SetupImgLoader< T > createSetupImgLoader( final Source< T > source )
	{
		return new SetupImgLoader< T >( source );
	}

	public class SetupImgLoader< T > implements BasicSetupImgLoader< T >
	{
		private final Source< T > source;

		protected SetupImgLoader( final Source< T > source )
		{
			this.source = source;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
		{
			return cropView( timepointId ).getA();
		}

		@Override
		public T getImageType()
		{
			return source.getType();
		}

		private Pair< RandomAccessibleInterval< T >, AffineTransform3D > cropView( final int timepointId )
		{
			final int timepointIndex = timepointIdToTimepointIndex.get( timepointId );
			return crop( globalToCropTransform, cropInterval, source, timepointIndex );
		}
	}
}
