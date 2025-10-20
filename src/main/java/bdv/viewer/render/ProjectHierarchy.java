package bdv.viewer.render;

import java.util.List;

import net.imglib2.Interval;
import net.imglib2.Volatile;
import net.imglib2.algorithm.blocks.AbstractBlockSupplier;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.blocks.TempArray;
import net.imglib2.blocks.VolatileArray;
import net.imglib2.cache.iotiming.CacheIoTiming;
import net.imglib2.cache.iotiming.IoStatistics;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;
import net.imglib2.util.Util;

public class ProjectHierarchy
{
	public static < T extends NativeType< T > > BlockSupplier< ARGBType > of(
			final List< BlockSupplier< T > > sources,
			final byte[] maskArray )
	{
		return new ProjectHierarchyBlockSupplier<>( sources, maskArray );
	}

	// TODO: Probably this will not be a BlockSupplier in the end, because the
	//       target interval is always the same, and must be the same, because
	//       it corresponds to the mask array dimensions.
	//       For now it's convenient. After working out the rough outlines of
	//       what needs to happen, look at what AbstractBlockSupplier provides
	//       in terms of useful inherited behaviour. And replicate that.

	private static class ProjectHierarchyBlockSupplier< T extends NativeType< T >, A > extends AbstractBlockSupplier< ARGBType >
	{
		// TODO: when to set this? should be final
		private Interval interval;

		private List< BlockSupplier< T > > sources;

		/**
		 * Records, for every target pixel, the best (smallest index) source
		 * resolution level that has provided a valid value. Only better (lower
		 * index) resolutions are re-tried in successive {@link #map(boolean)}
		 * calls.
		 */
		private final byte[] mask;

		/**
		 * How many levels (starting from level {@code 0}) have to be re-rendered in
		 * the next rendering pass, i.e., {@code map()} call.
		 */
		private int numInvalidLevels;

		private final TempArray< A > target; // TODO: this should just be A

		private final TempArray< VolatileArray< A > > temp;

		private final CopyValidPixels< A > copyValidPixels;

		ProjectHierarchyBlockSupplier(
				final List< BlockSupplier< T > > sources,
				final byte[] maskArray )
		{
			final T type = sources.get(0).getType();
			assert type instanceof Volatile;
			final PrimitiveType primitiveType = type.getNativeTypeFactory().getPrimitiveType();

			this.sources = sources;
			mask = maskArray;
			numInvalidLevels = sources.size();
			target = TempArray.forPrimitiveType( primitiveType, false );
			temp = TempArray.forPrimitiveType( primitiveType, true );
			copyValidPixels = CopyValidPixels.of( primitiveType );
		}

		// TODO: Doesn't make much sense, because we have to share the mask
		//       array, so probably this won't be used multithreaded anyway.
		//       But let's see where this goes...
		private ProjectHierarchyBlockSupplier( final ProjectHierarchyBlockSupplier s )
		{
			mask = s.mask;
			target = s.target;
			temp = s.temp.newInstance();
			copyValidPixels = s.copyValidPixels;
		}

		@Override
		public void copy( final Interval interval, final Object dest )
		{
			this.interval = interval;
			map();
		}

		private void map()
		{
			// TODO: Later, probably we want to allow cancelling between levels.
			//       But first get it right without thinking about that...

			/*
			 * After the for loop, numInvalidLevels is the highest (coarsest)
			 * resolution for which all pixels could be filled from valid data. This
			 * means that in the next pass, i.e., map() call, levels up to
			 * numInvalidLevels have to be re-rendered.
			 */
			for ( int resolutionLevel = 0; resolutionLevel < numInvalidLevels; ++resolutionLevel )
			{
				int numInvalidPixels = map( ( byte ) resolutionLevel);
				if ( numInvalidPixels == 0 )
					// if this pass was all valid
					numInvalidLevels = resolutionLevel;
			}
		}

		/**
		 * Copy from source {@code resolutionIndex} to target.
		 * <p>
		 * Only valid source pixels with a current mask value
		 * {@code mask>resolutionIndex} are copied to target, and their mask value
		 * is set to {@code mask=resolutionIndex}. Invalid source pixels are
		 * ignored. Pixels with {@code mask<=resolutionIndex} are ignored, because
		 * they have already been written to target during a previous pass.
		 * <p>
		 *
		 * @param resolutionIndex
		 *     index of source resolution level
		 * @return the number of pixels that remain invalid afterward
		 */
		private int map( final byte resolutionIndex )
		{
			final BlockSupplier< T > source = sources.get( resolutionIndex );
			final int length = Util.safeInt( Intervals.numElements( interval ) );
			final VolatileArray< A > transformed = temp.get( length );
			source.copy( interval, transformed );
			final A dest = target.get( length ); // TODO: target should be type A, and we should just use target
			return copyValidPixels.copy( transformed, mask, resolutionIndex, dest, length );

			// TODO:
			//   We maybe want to track which pixels were modified in this pass
			//   to avoid having to convert everything to ARGB in every pass?
		}


		interface CopyValidPixels< T >
		{
			/**
			 * Given a source {@code VolatileArray} affine-transformed image for
			 * resolution level {@code resolutionIndex}, updates both the {@code mask}
			 * array (containing for every pixel the resolution level at which
			 * the pixel was successfully rendered and the {@code dest} array
			 * (containing rendered pixel values).
			 *
			 * @param src source (data and validity)
			 * @param mask target mask (updated in place)
			 * @param resolutionIndex pixels with mask value greater than this are still invalid pixels
			 * @param dest target data (updated in place)
			 * @param length length of src (and mask, and dest) array
			 * @return number of invalid pixels remaining afterwards
			 */
			int copy( VolatileArray< T > src, byte[] mask, byte resolutionIndex, T dest, int length );

			static < T > CopyValidPixels< T > of( final PrimitiveType primitiveType )
			{
				switch(primitiveType) {
				case BOOLEAN:
					throw new UnsupportedOperationException();
				case BYTE:
					throw new UnsupportedOperationException();
				case CHAR:
					throw new UnsupportedOperationException();
				case SHORT:
					return ( CopyValidPixels< T > ) _short;
				case INT:
					throw new UnsupportedOperationException();
				case LONG:
					throw new UnsupportedOperationException();
				case FLOAT:
					throw new UnsupportedOperationException();
				case DOUBLE:
					throw new UnsupportedOperationException();
				case UNDEFINED:
					throw new UnsupportedOperationException();
				}
				throw new IllegalArgumentException();
			}

			CopyValidPixels< short[] > _short = ( src, mask, resolutionIndex, dest, length ) ->
			{
				final short[] data = src.data();
				final byte[] valid = src.valid();
				int numInvalidPixels = 0;
				for ( int i = 0; i < length; ++i )
				{
					if( mask[ i ] > resolutionIndex )
					{
						if ( valid[ i ] != ( byte ) 0 )
						{
							dest[ i ] = data[ i ];
							mask[ i ] = resolutionIndex;
						}
						else
							++numInvalidPixels;
					}
				}
				return numInvalidPixels;
			};
		}


		@Override
		public BlockSupplier< ARGBType > independentCopy()
		{
			return new ProjectHierarchyBlockSupplier( this );
		}

		@Override
		public int numDimensions()
		{
			return 2;
		}

		private static final ARGBType type = new ARGBType();

		@Override
		public ARGBType getType()
		{
			return type;
		}
	}
}
