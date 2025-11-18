package bdv.viewer.render;

import java.util.List;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.algorithm.blocks.BlockProcessor;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.DefaultUnaryBlockOperator;
import net.imglib2.algorithm.blocks.UnaryBlockOperator;
import net.imglib2.algorithm.blocks.convert.Convert;
import net.imglib2.blocks.BlockInterval;
import net.imglib2.blocks.SubArrayCopy;
import net.imglib2.blocks.TempArray;
import net.imglib2.blocks.VolatileArray;
import net.imglib2.converter.Converter;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

/**
 *
 * @param <S>
 * 		source type (for all sources)
 * @param <T>
 * 		target type (typically ARGB)
 * @param <I>
 * 		input primitive array type, e.g., float[]. Must correspond to S.
 * @param <O>
 * 		output primitive array type, e.g., int[]. Must correspond to T.
 */
public class ProjectHierarchy< S extends NativeType< S >, T extends NativeType< T >, I, O >
{
	// TODO: Probably this will not be a BlockSupplier in the end, because the
	//       target interval is always the same, and must be the same, because
	//       it corresponds to the mask array dimensions.
	//       For now it's convenient. After working out the rough outlines of
	//       what needs to happen, look at what AbstractBlockSupplier provides
	//       in terms of useful inherited behaviour. And replicate that.
	//       ==>
	//       Should have a public map() method that just does everything that
	//       VolatileHierarchyProjector does


	public static < S extends NativeType< S >, T extends NativeType< T > > ProjectHierarchy<S,T,?,?> of(
			final List< BlockSupplier< S > > sources,
			final Converter< ? super S, T > converter,
			final RandomAccessibleInterval< T > target,
			final byte[] maskArray )
	{
		return new ProjectHierarchy<>( sources, converter, target, maskArray );
	}

	private List< BlockSupplier< S > > sources;

	private final BlockInterval interval;

	private final int length;

	/**
	 * Records, for every target pixel, the best (smallest index) source
	 * resolution level that has provided a valid value. Only better (lower
	 * index) resolutions are re-tried in successive {@link #map(byte)}
	 * calls.
	 */
	private final byte[] mask;

	/**
	 * Currently rendered output, before converting to the target type.
	 */
	private final I destI;

	/**
	 * Currently rendered output, converted to the target type.
	 */
	private final O destO;

	/**
	 * Convert I to O using the Converter provided in the constructor
	 */
	private final BlockProcessor< I, O > convertBlockProcessor;

	/**
	 * Underlying primitive array of target
	 */
	private final ProjectorUtils.ArrayData targetData;

	/**
	 * How many levels (starting from level {@code 0}) have to be re-rendered in
	 * the next rendering pass, i.e., {@code map()} call.
	 */
	private int numInvalidLevels;

	private final VolatileArray< I > transformedSourceI;

	private final CopyValidPixels< I > copyValidPixels;

	private ProjectHierarchy(
			final List< BlockSupplier< S > > sources,
			final Converter< ? super S, T > converter,
			final RandomAccessibleInterval< T > target,
			final byte[] maskArray )
	{
		final S sourceType = sources.get(0).getType();
		assert sourceType instanceof Volatile;
		final PrimitiveType sourcePrimitiveType = sourceType.getNativeTypeFactory().getPrimitiveType();

		final T targetType = target.getType();
		final PrimitiveType targetPrimitiveType = targetType.getNativeTypeFactory().getPrimitiveType();

		this.sources = sources;
		interval = BlockInterval.asBlockInterval( target );
		length = Util.safeInt( Intervals.numElements( interval ) );
		mask = maskArray;

		destI = Cast.unchecked( TempArray.forPrimitiveType( sourcePrimitiveType, false ).get( length ) );
		destO = Cast.unchecked( TempArray.forPrimitiveType( targetPrimitiveType, false ).get( length ) );
		transformedSourceI = Cast.unchecked( TempArray.forPrimitiveType( sourcePrimitiveType, true ).get( length ) );
		copyValidPixels = CopyValidPixels.of( sourcePrimitiveType );

		targetData = ProjectorUtils.getARGBArrayData( target );

		final UnaryBlockOperator< S, T > op = Convert.createOperator( sourceType, targetType, () -> converter );
		// NB: cast to DefaultUnaryBlockOperator so that we can extract the BlockProcessor
		convertBlockProcessor = ( ( DefaultUnaryBlockOperator< S, T > ) op ).blockProcessor();
		convertBlockProcessor.setTargetInterval( interval );

		numInvalidLevels = sources.size();
	}

	public void map()
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

		convertBlockProcessor.compute( destI, destO );

		final int[] o_src = { 0, 0 };
		final int[] size_src = interval.size();
		final int[] o_dst = { targetData.ox(), targetData.oy() };
		final int[] size_dst = { targetData.stride(), 1 };
		SubArrayCopy
				.forPrimitiveType( PrimitiveType.INT )
				.copy( destO, size_src, o_src, targetData.data(), size_dst, o_dst, size_src );
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
		final BlockSupplier< S > source = sources.get( resolutionIndex );
		source.copy( interval, transformedSourceI );
		return copyValidPixels.copy( transformedSourceI, mask, resolutionIndex, destI, length );

		// TODO:
		//   We maybe want to track which pixels were modified in this pass
		//   to avoid having to convert everything to ARGB in every pass?
	}

















	private interface CopyValidPixels< T >
	{
		/**
		 * Given a source {@code VolatileArray} affine-transformed image for
		 * resolution level {@code resolutionIndex}, updates both the {@code mask}
		 * array (containing for every pixel the resolution level at which
		 * the pixel was successfully rendered and the {@code dest} array
		 * (containing rendered pixel values).
		 *
		 * @param src
		 * 		source (data and validity)
		 * @param mask
		 * 		target mask (updated in place)
		 * @param resolutionIndex
		 * 		pixels with mask value greater than this are still invalid pixels
		 * @param dest
		 * 		target data (updated in place)
		 * @param length
		 * 		length of src (and mask, and dest) array
		 *
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
}
