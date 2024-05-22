/*-
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
package bdv.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.LongStream;

import net.imglib2.Dimensions;
import net.imglib2.EuclideanSpace;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

public enum AxisOrder implements EuclideanSpace
{
	XYZ   ( 3,  2, -1, -1 ), // --> XYZ
	XYZC  ( 4,  2,  3, -1 ), // --> XYZ
	XYZT  ( 4,  2, -1,  3 ), // --> XYZT
	XYZCT ( 5,  2,  3,  4 ), // --> XYZT
	XYZTC ( 5,  2,  4,  3 ), // --> XYZT
	XYCZT ( 5,  3,  2,  4 ), // --> XYZT
	XY    ( 2, -1, -1, -1 ), // --> XY   --> XYZ
	XYC   ( 3, -1,  2, -1 ), // --> XY   --> XYZ
	XYT   ( 3, -1, -1,  2 ), // --> XYT  --> XYTZ --> XYZT
	XYCT  ( 4, -1,  2,  3 ), // --> XYT  --> XYTZ --> XYZT
	XYTC  ( 4, -1,  3,  2 ), // --> XYT  --> XYTZ --> XYZT
	XYCZ  ( 4,  3,  2, -1 ), // --> XYZ
	DEFAULT( 0, 0, 0, 0 );

	final int numDimensions;

	final int zDimension;

	final int channelDimension;

	final int timeDimension;

	AxisOrder(
			final int numDimensions,
			final int zDimension,
			final int channelDimension,
			final int timeDimension )
	{
		this.numDimensions = numDimensions;
		this.zDimension = zDimension;
		this.channelDimension = channelDimension;
		this.timeDimension = timeDimension;
	}

	public static AxisOrder getAxisOrder( final AxisOrder axisOrder, final EuclideanSpace space, final boolean viewerIs2D )
	{
		if ( axisOrder == DEFAULT )
		{
			if ( viewerIs2D )
			{
				switch ( space.numDimensions() )
				{
				case 2:
					return XY;
				case 3:
					return XYT;
				case 4:
					return XYTC;
				case 5:
					return XYZTC;
				}
			}
			else
			{
				switch ( space.numDimensions() )
				{
				case 2:
					return XY;
				case 3:
					return XYZ;
				case 4:
					return XYZT;
				case 5:
					return XYZTC;
				}
			}
			throw new IllegalArgumentException( "image dimensionality " + space.numDimensions() + " is not supported" );
		}
		return axisOrder;
	}

	public static < T > ArrayList< RandomAccessibleInterval< T > > splitInputStackIntoSourceStacks(
			final RandomAccessibleInterval< T > img,
			final AxisOrder axisOrder )
	{
		if ( img.numDimensions() != axisOrder.numDimensions )
			throw new IllegalArgumentException( "provided AxisOrder doesn't match dimensionality of image" );

		final ArrayList< RandomAccessibleInterval< T > > sourceStacks = new ArrayList< >();

		/*
		 * If there a channels dimension, slice img along that dimension.
		 */
		final int c = axisOrder.channelDimension;
		if ( c != -1 )
		{
			final int numSlices = ( int ) img.dimension( c );
			for ( int s = 0; s < numSlices; ++s )
				sourceStacks.add( Views.hyperSlice( img, c, s + img.min( c ) ) );
		}
		else
			sourceStacks.add( img );

		/*
		 * If AxisOrder is a 2D variant (has no Z dimension), augment the
		 * sourceStacks by a Z dimension.
		 */
		final boolean addZ = !axisOrder.hasZ();
		if ( addZ )
			for ( int i = 0; i < sourceStacks.size(); ++i )
				sourceStacks.set( i, Views.addDimension( sourceStacks.get( i ), 0, 0 ) );

		/*
		 * If at this point the dim order is XYTZ, permute to XYZT
		 */
		final boolean flipZ = !axisOrder.hasZ() && axisOrder.hasTimepoints();
		if ( flipZ )
			for ( int i = 0; i < sourceStacks.size(); ++i )
				sourceStacks.set( i, Views.permute( sourceStacks.get( i ), 2, 3 ) );

		return sourceStacks;
	}

	public static < T > Pair< ArrayList< RandomAccessible< T > >, Interval > splitInputStackIntoSourceStacks(
			final RandomAccessible< T > img,
			Interval interval,
			final AxisOrder axisOrder )
	{
		if ( img.numDimensions() != axisOrder.numDimensions )
			throw new IllegalArgumentException( "provided AxisOrder doesn't match dimensionality of image" );

		final ArrayList< RandomAccessible< T > > sourceStacks = new ArrayList<>();

		/*
		 * If there a channels dimension, slice img along that dimension.
		 */
		final int c = axisOrder.channelDimension;
		if ( c != -1 )
		{
			final long[] min = new long[ interval.numDimensions() -1 ];
			final long[] max = new long[ interval.numDimensions() -1 ];
			for ( int dim = 0; dim < min.length; ++dim ) {
				final int otherIndex = dim >= axisOrder.channelDimension ? dim + 1 : dim;
				min[ dim ] = interval.min( otherIndex );
				max[ dim ] = interval.max( otherIndex );
			}
			interval = new FinalInterval( min, max );
			final int numSlices = ( int ) interval.dimension( c );
			for ( int s = 0; s < numSlices; ++s )
				sourceStacks.add( Views.hyperSlice( img, c, s + interval.min( c ) ) );
		}
		else
			sourceStacks.add( img );

		/*
		 * If AxisOrder is a 2D variant (has no Z dimension), augment the
		 * sourceStacks by a Z dimension.
		 */
		final boolean addZ = !axisOrder.hasZ();
		if ( addZ )
		{
			final long[] min = LongStream.concat( Arrays.stream( Intervals.minAsLongArray( interval ) ), LongStream.of( 0 ) ).toArray();
			final long[] max = LongStream.concat( Arrays.stream( Intervals.maxAsLongArray( interval ) ), LongStream.of( 0 ) ).toArray();
			interval = new FinalInterval( min, max );
			for ( int i = 0; i < sourceStacks.size(); ++i )
				sourceStacks.set( i, Views.addDimension( sourceStacks.get( i ) ) );
		}

		/*
		 * If at this point the dim order is XYTZ, permute to XYZT
		 */
		final boolean flipZ = !axisOrder.hasZ() && axisOrder.hasTimepoints();
		if ( flipZ )
		{
			final long[] min = Intervals.minAsLongArray( interval );
			final long[] max = Intervals.maxAsLongArray( interval );
			final long minTmp = min[ 3 ];
			final long maxTmp = max[ 3 ];
			min[ 3 ] = min[ 2 ];
			max[ 3 ] = max[ 2 ];
			min[ 2 ] = minTmp;
			max[ 2 ] = maxTmp;
			interval = new FinalInterval( min, max );
			for ( int i = 0; i < sourceStacks.size(); ++i )
				sourceStacks.set( i, Views.permute( sourceStacks.get( i ), 2, 3 ) );
		}

		return new ValuePair<>( sourceStacks, interval );
	}

	public int zDimension()
	{
		return zDimension;
	}

	public boolean hasZ()
	{
		return zDimension >= 0;
	}

	public int channelDimension()
	{
		return channelDimension;
	}

	public boolean hasChannels()
	{
		return channelDimension >= 0;
	}

	public long numChannels( Dimensions dimensions )
	{
		return hasChannels() ? dimensions.dimension( channelDimension ) : 1;
	}

	public int timeDimension()
	{
		return timeDimension;
	}

	public boolean hasTimepoints()
	{
		return timeDimension >= 0;
	}

	public long numTimepoints( Dimensions dimensions )
	{
		return hasTimepoints() ? dimensions.dimension( timeDimension ) : 1;
	}

	@Override
	public int numDimensions()
	{
		return numDimensions;
	}
}
