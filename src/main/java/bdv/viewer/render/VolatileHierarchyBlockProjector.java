package bdv.viewer.render;

import static net.imglib2.type.PrimitiveType.BOOLEAN;
import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.CHAR;
import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;
import static net.imglib2.type.PrimitiveType.INT;
import static net.imglib2.type.PrimitiveType.LONG;
import static net.imglib2.type.PrimitiveType.SHORT;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.algorithm.blocks.BlockProcessor;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.DefaultUnaryBlockOperator;
import net.imglib2.algorithm.blocks.UnaryBlockOperator;
import net.imglib2.algorithm.blocks.VolatileBlockSupplier;
import net.imglib2.algorithm.blocks.convert.Convert;
import net.imglib2.algorithm.blocks.transform.Transform;
import net.imglib2.blocks.SubArrayCopy;
import net.imglib2.blocks.TempArray;
import net.imglib2.blocks.VolatileArray;
import net.imglib2.converter.Converter;
import net.imglib2.img.AbstractImg;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.array.BooleanArray;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.CharArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileBooleanArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileCharArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.interpolation.Interpolant;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
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

	static < A extends Volatile< ? > & NativeType< A > > BlockSupplier< A > getBlockSupplierIfPossible(
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
		return VolatileBlockSupplier.of( s3 )
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

	/**
	 * Convert primitive arrays between ImgLib2 {@code NativeType}s using a {@link Converter}.
	 *
	 * @param <S>
	 * 		source type
	 * @param <T>
	 * 		target type
	 * @param <I>
	 * 		input primitive array type, e.g., float[]. Must correspond to S.
	 * @param <O>
	 * 		output primitive array type, e.g., float[]. Must correspond to T.
	 */
	// TODO rename
	static class ConverterBlockProcessor< S extends NativeType< S >, T extends NativeType< T >, I, O >
	{
		private final S sourceType;

		private final T targetType;

		private final Supplier< Converter< ? super S, T > > converterSupplier;

		private final Converter< ? super S, T > converter;

		private final Wrapper< S > wrapSource;

		private final Wrapper< T > wrapTarget;

		ConverterBlockProcessor(
				final S sourceType,
				final T targetType,
				final Supplier< Converter< ? super S, T > > converterSupplier )
		{
			this.sourceType = sourceType;
			this.targetType = targetType;
			this.converterSupplier = converterSupplier;

			converter = converterSupplier.get();
			wrapSource = Wrapper.of( sourceType );
			wrapTarget = Wrapper.of( targetType );
		}

		void compute( final I src, final O dest, final int len )
		{
			final S in = wrapSource.wrap( src );
			final T out = wrapTarget.wrap( dest );
			for ( int i = 0; i < len; i++ )
			{
				in.index().set( i );
				out.index().set( i );
				converter.convert( in, out );
			}
		}

		// ------------------------------------------------------------------------
		//
		//   Wrap primitive array into a Type that can be passed to the Converter
		//

		private interface Wrapper< T extends NativeType< T > >
		{
			T wrap( final Object array );

			static < T extends NativeType< T > > Wrapper<T> of( T type )
			{
				return new WrapperImpl<>( type );
			}
		}

		private static class WrapperImpl< T extends NativeType< T >, A > extends AbstractImg< T > implements NativeImg< T, A >, Wrapper< T >
		{
			private final PrimitiveTypeProperties< ?, A, ? > props;
			private Object array;
			private final T wrapper;

			WrapperImpl( T type )
			{
				super( new long[ 0 ] );
				final NativeTypeFactory< T, A > nativeTypeFactory = Cast.unchecked( type.getNativeTypeFactory() );
				props = Cast.unchecked( PrimitiveTypeProperties.get( nativeTypeFactory.getPrimitiveType() ) );
				wrapper = nativeTypeFactory.createLinkedType( this );
			}

			@Override
			public T wrap( final Object array )
			{
				this.array = array;
				wrapper.updateContainer( null );
				return wrapper;
			}

			@Override
			public A update( final Object updater )
			{
				return props.wrap( array );
			}

			@Override public void setLinkedType( final T type ) {throw new UnsupportedOperationException();}
			@Override public T createLinkedType() {throw new UnsupportedOperationException();}
			@Override public Cursor< T > cursor() {throw new UnsupportedOperationException();}
			@Override public Cursor< T > localizingCursor() {throw new UnsupportedOperationException();}
			@Override public Object iterationOrder() {throw new UnsupportedOperationException();}
			@Override public RandomAccess< T > randomAccess() {throw new UnsupportedOperationException();}
			@Override public ImgFactory< T > factory() {throw new UnsupportedOperationException();}
			@Override public Img< T > copy() {throw new UnsupportedOperationException();}
			@Override public T getType() {throw new UnsupportedOperationException();}
		}

		/**
		 * @param <P> a primitive array type, e.g., {@code byte[]}.
		 * @param <A> the corresponding {@code ArrayDataAccess} type.
		 * @param <V> the corresponding {@code VolatileArrayDataAccess} type.
		 */
		private static class PrimitiveTypeProperties< P, A, V >
		{
			final Class< P > primitiveArrayClass;

			final IntFunction< P > createPrimitiveArray;

			final ToIntFunction< P > primitiveArrayLength;

			final Function< P, A > wrapAsAccess;

			final Function< P, V > wrapAsVolatileAccess;

			static PrimitiveTypeProperties< ?, ?, ? > get( final PrimitiveType primitiveType )
			{
				final PrimitiveTypeProperties< ?, ?, ? > props = creators.get( primitiveType );
				if ( props == null )
					throw new IllegalArgumentException();
				return props;
			}

			/**
			 * Wrap a primitive array {@code data} into a corresponding {@code ArrayDataAccess}.
			 *
			 * @param data primitive array to wrap (actually type {@code P} instead of {@code Object}, but its easier to use this way)
			 * @return {@code ArrayDataAccess} wrapping {@code data}
			 */
			A wrap( Object data )
			{
				if ( data == null )
					throw new NullPointerException();
				if ( !primitiveArrayClass.isInstance( data ) )
					throw new IllegalArgumentException( "expected " + primitiveArrayClass.getSimpleName() + " argument" );
				return wrapAsAccess.apply( ( P ) data );
			}

			/**
			 * Allocate a primitive array (type {@code P}) with {@code length} elements.
			 */
			P allocate( int length )
			{
				return createPrimitiveArray.apply( length );
			}

			/**
			 * Get the length of a primitive array (type {@code P}).
			 */
			int length( P array )
			{
				return primitiveArrayLength.applyAsInt( array );
			}

			private PrimitiveTypeProperties(
					final Class< P > primitiveArrayClass,
					final IntFunction< P > createPrimitiveArray,
					final ToIntFunction< P > primitiveArrayLength,
					final Function< P, A > wrapAsAccess,
					final Function< P, V > wrapAsVolatileAccess )
			{
				this.primitiveArrayClass = primitiveArrayClass;
				this.createPrimitiveArray = createPrimitiveArray;
				this.primitiveArrayLength = primitiveArrayLength;
				this.wrapAsAccess = wrapAsAccess;
				this.wrapAsVolatileAccess = wrapAsVolatileAccess;
			}

			private static final EnumMap< PrimitiveType, PrimitiveTypeProperties< ?, ?, ? > > creators = new EnumMap<>( PrimitiveType.class );

			static
			{
				creators.put( BOOLEAN, new PrimitiveTypeProperties<>( boolean[].class, boolean[]::new, a -> a.length, BooleanArray::new, a -> new VolatileBooleanArray( a, true ) ) );
				creators.put( BYTE, new PrimitiveTypeProperties<>( byte[].class, byte[]::new, a -> a.length, ByteArray::new, a -> new VolatileByteArray( a, true ) ) );
				creators.put( CHAR, new PrimitiveTypeProperties<>( char[].class, char[]::new, a -> a.length, CharArray::new, a -> new VolatileCharArray( a, true ) ) );
				creators.put( SHORT, new PrimitiveTypeProperties<>( short[].class, short[]::new, a -> a.length, ShortArray::new, a -> new VolatileShortArray( a, true ) ) );
				creators.put( INT, new PrimitiveTypeProperties<>( int[].class, int[]::new, a -> a.length, IntArray::new, a -> new VolatileIntArray( a, true ) ) );
				creators.put( LONG, new PrimitiveTypeProperties<>( long[].class, long[]::new, a -> a.length, LongArray::new, a -> new VolatileLongArray( a, true ) ) );
				creators.put( FLOAT, new PrimitiveTypeProperties<>( float[].class, float[]::new, a -> a.length, FloatArray::new, a -> new VolatileFloatArray( a, true ) ) );
				creators.put( DOUBLE, new PrimitiveTypeProperties<>( double[].class, double[]::new, a -> a.length, DoubleArray::new, a -> new VolatileDoubleArray( a, true ) ) );
			}
		}
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
