package bdv.viewer.render;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.algorithm.blocks.BlockProcessor;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.DefaultUnaryBlockOperator;
import net.imglib2.algorithm.blocks.UnaryBlockOperator;
import net.imglib2.algorithm.blocks.convert.Convert;
import net.imglib2.algorithm.blocks.transform.Transform;
import net.imglib2.blocks.SubArrayCopy;
import net.imglib2.blocks.TempArray;
import net.imglib2.blocks.VolatileArray;
import net.imglib2.converter.Converter;
import net.imglib2.interpolation.Interpolant;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.operators.SetZero;
import net.imglib2.util.Cast;

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
public class VolatileHierarchyBlockProjector< S extends NativeType< S >, T extends NativeType< T >, I, O >
		extends AbstractVolatileHierarchyBlockProjector
{
	public static boolean DEBUG_USE_BLK_AFFINE = true;

	static < S extends Volatile< ? > & NativeType< S >, T extends NativeType< T > & SetZero > VolatileProjector tryCreate(
			final List< ? extends RandomAccessible< S > > sources,
			final Converter< ? super S, T > converter,
			final RandomAccessibleInterval< T > target,
			final byte[] maskArray )
	{
		if ( !DEBUG_USE_BLK_AFFINE )
			return null;

		final List< BlockSupplier< S > > blks = new ArrayList<>();
		for ( RandomAccessible< S > source : sources )
		{
			final BlockSupplier< S > sourceBlocks = getBlockSupplierIfPossible( source );
			if ( sourceBlocks == null )
				return null;
			blks.add( sourceBlocks );
		}

		return new VolatileHierarchyBlockProjector<>( blks, converter, target, maskArray );
	}

	static < A extends NativeType< A > > BlockSupplier< A > getBlockSupplierIfPossible(
			final RandomAccessible< A > source )
	{
		if ( !( source instanceof AffineRandomAccessible ) )
			return null;
		final AffineRandomAccessible< A, ? > s0 = ( AffineRandomAccessible< A, ? > ) source;
		final AffineGet transformFromSource = s0.getTransformToSource().inverse();

		final RealRandomAccessible< A > s1 = s0.getSource();
		if ( !( s1 instanceof Interpolant ) )
			return null;
		final Interpolant< A, ? > s2 = ( Interpolant< A, ? > ) s1;

		final InterpolatorFactory< A, ? > f = s2.getInterpolatorFactory();
		final Transform.Interpolation interpolation;
		if ( f instanceof ClampingNLinearInterpolatorFactory )
		{
			interpolation = Transform.Interpolation.NLINEAR;
		}
		else if ( f instanceof NearestNeighborInterpolatorFactory )
		{
			interpolation = Transform.Interpolation.NEARESTNEIGHBOR;
		}
		else
		{
			System.out.println( "cannot handle " + f.getClass() + " (yet)" );
			return null;
		}

		final RandomAccessible< A > s3 = ( RandomAccessible< A > ) s2.getSource();
		return BlockSupplier.of( s3 )
				.andThen( Transform.affine( transformFromSource, interpolation ) );
	}














	public static < S extends NativeType< S >, T extends NativeType< T > > VolatileHierarchyBlockProjector< S, T, ?, ? > of(
			final List< BlockSupplier< S > > sources,
			final Converter< ? super S, T > converter,
			final RandomAccessibleInterval< T > target,
			final byte[] maskArray )
	{
		return new VolatileHierarchyBlockProjector<>( sources, converter, target, maskArray );
	}

	private List< BlockSupplier< S > > sources;

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

	private final VolatileArray< I > transformedSourceI;

	private final CopyValidPixels< I > copyValidPixels;

	/**
	 *
	 * @param sources
	 * @param converter A converter from the source pixel type to the target pixel type.
	 * @param target
	 * @param maskArray
	 */
	VolatileHierarchyBlockProjector(
			final List< BlockSupplier< S > > sources,
			final Converter< ? super S, T > converter,
			final RandomAccessibleInterval< T > target,
			final byte[] maskArray )
	{
		super( sources.size(),
				Math.max( 2, sources.get( 0 ).numDimensions() ),
				target, maskArray );

		final S sourceType = sources.get( 0 ).getType();
		assert sourceType instanceof Volatile;
		final PrimitiveType sourcePrimitiveType = sourceType.getNativeTypeFactory().getPrimitiveType();

		final T targetType = target.getType();
		final PrimitiveType targetPrimitiveType = targetType.getNativeTypeFactory().getPrimitiveType();

		this.sources = sources;

		destI = Cast.unchecked( TempArray.forPrimitiveType( sourcePrimitiveType, false ).get( length ) );
		destO = Cast.unchecked( TempArray.forPrimitiveType( targetPrimitiveType, false ).get( length ) );
		transformedSourceI = Cast.unchecked( TempArray.forPrimitiveType( sourcePrimitiveType, true ).get( length ) );
		copyValidPixels = CopyValidPixels.of( sourcePrimitiveType );

		// TODO: Pass in ProjectorUtils.ArrayData instead of target RAI.
		//       We need to provide it to the super() constructor as an Interval.
		//       Maybe just let ArrayData implement interval?
		targetData = ProjectorUtils.getARGBArrayData( target );

		final UnaryBlockOperator< S, T > op = Convert.createOperator( sourceType, targetType, () -> converter );
		// NB: cast to DefaultUnaryBlockOperator so that we can extract the BlockProcessor
		convertBlockProcessor = ( ( DefaultUnaryBlockOperator< S, T > ) op ).blockProcessor();
		convertBlockProcessor.setTargetInterval( sourceInterval );
	}

	@Override
	int map( final byte resolutionIndex )
	{
		final BlockSupplier< S > source = sources.get( resolutionIndex );
		source.copy( sourceInterval, transformedSourceI );
		return copyValidPixels.copy( transformedSourceI, mask, resolutionIndex, destI, length );
	}

	@Override
	void convert()
	{
		convertBlockProcessor.compute( destI, destO );

		final int[] o_src = { 0, 0 };
		final int[] size_src = sourceInterval.size();
		final int[] o_dst = { targetData.ox(), targetData.oy() };
		final int[] size_dst = { targetData.stride(), 1 };

		SubArrayCopy
				.forPrimitiveType( PrimitiveType.INT )
				.copy( destO, size_src, o_src, targetData.data(), size_dst, o_dst, size_src );
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
			switch ( primitiveType )
			{
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
				if ( mask[ i ] > resolutionIndex )
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
