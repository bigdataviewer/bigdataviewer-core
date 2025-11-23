package bdv.viewer.render;

import static net.imglib2.type.PrimitiveType.BOOLEAN;
import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.CHAR;
import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;
import static net.imglib2.type.PrimitiveType.INT;
import static net.imglib2.type.PrimitiveType.LONG;
import static net.imglib2.type.PrimitiveType.SHORT;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.algorithm.blocks.BlockSupplier;
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
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.PrimitiveType;
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
	private final ConvertProcessor< S, T, I, O > convertProcessor;

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
	public VolatileHierarchyBlockProjector(
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
		convertProcessor = new ConvertProcessor<>( sourceType, targetType, converter );
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
		convertProcessor.compute( destI, destO, length );

		final int[] o_src = { 0, 0 };
		final int[] size_src = Arrays.copyOf( sourceInterval.size(), 2 );
		final int[] o_dst = { targetData.ox(), targetData.oy() };
		final int[] size_dst = { targetData.stride(), 1 };

		SubArrayCopy
				.forPrimitiveType( PrimitiveType.INT )
				.copy( destO, size_src, o_src, targetData.data(), size_dst, o_dst, size_src );
	}

	/**
	 * Convert primitive arrays between ImgLib2 {@code NativeType}s using a {@link Converter}.
	 * <p>
	 * This is a modified version of {@code net.imglib2.algorithm.blocks.convert.ConverterBlockProcessor}.
	 * It uses the modified {@code PrimitiveTypeProperties} below. THis also wraps {@code VolatileNativeType}
	 * and just ignores the isValid flag. This is useful here, because we just want to convert the rendered
	 * values using a {@code Converter} from {@code VolatileNativeType} to (typically) {@code ARGBType}.
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
	static class ConvertProcessor< S extends NativeType< S >, T extends NativeType< T >, I, O >
	{
		private final Converter< ? super S, T > converter;

		private final Wrapper< S > wrapSource;

		private final Wrapper< T > wrapTarget;

		ConvertProcessor(
				final S sourceType,
				final T targetType,
				final Converter< ? super S, T > converter )
		{
			this.converter = converter;
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

			static < T extends NativeType< T > > Wrapper< T > of( T type )
			{
				return new WrapperImpl<>( type );
			}
		}

		private static class WrapperImpl< T extends NativeType< T >, A > extends AbstractImg< T > implements NativeImg< T, A >, Wrapper< T >
		{
			private final PrimitiveTypeProperties< ?, A > props; // TODO: get rid of this. just put final Function< P, A > wrapAsAccess directly
			private Object array;
			private final T wrapper;

			WrapperImpl( T type )
			{
				super( new long[ 0 ] );
				final NativeTypeFactory< T, A > nativeTypeFactory = Cast.unchecked( type.getNativeTypeFactory() );
				props = Cast.unchecked( PrimitiveTypeProperties.get( nativeTypeFactory.getPrimitiveType(), type instanceof Volatile ) );
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
		 * @param <A> the corresponding {@code ArrayDataAccess}
		 */
		private static class PrimitiveTypeProperties< P, A >
		{
			final Class< P > primitiveArrayClass;

			final Function< P, A > wrapAsAccess;

			static PrimitiveTypeProperties< ?, ? > get( final PrimitiveType primitiveType, final boolean isVolatile )
			{
				final PrimitiveTypeProperties< ?, ? > props = isVolatile
						? volatileCreators.get( primitiveType )
						: creators.get( primitiveType );
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

			private PrimitiveTypeProperties(
					final Class< P > primitiveArrayClass,
					final Function< P, A > wrapAsAccess )
			{
				this.primitiveArrayClass = primitiveArrayClass;
				this.wrapAsAccess = wrapAsAccess;
			}

			private static final EnumMap< PrimitiveType, PrimitiveTypeProperties< ?, ? > > creators = new EnumMap<>( PrimitiveType.class );
			private static final EnumMap< PrimitiveType, PrimitiveTypeProperties< ?, ? > > volatileCreators = new EnumMap<>( PrimitiveType.class );

			static
			{
				creators.put( BOOLEAN, new PrimitiveTypeProperties<>( boolean[].class, BooleanArray::new ) );
				creators.put( BYTE, new PrimitiveTypeProperties<>( byte[].class, ByteArray::new ) );
				creators.put( CHAR, new PrimitiveTypeProperties<>( char[].class, CharArray::new ) );
				creators.put( SHORT, new PrimitiveTypeProperties<>( short[].class, ShortArray::new ) );
				creators.put( INT, new PrimitiveTypeProperties<>( int[].class, IntArray::new ) );
				creators.put( LONG, new PrimitiveTypeProperties<>( long[].class, LongArray::new ) );
				creators.put( FLOAT, new PrimitiveTypeProperties<>( float[].class, FloatArray::new ) );
				creators.put( DOUBLE, new PrimitiveTypeProperties<>( double[].class, DoubleArray::new ) );
				volatileCreators.put( BOOLEAN, new PrimitiveTypeProperties<>( boolean[].class, a -> new VolatileBooleanArray( a, true ) ) );
				volatileCreators.put( BYTE, new PrimitiveTypeProperties<>( byte[].class, a -> new VolatileByteArray( a, true ) ) );
				volatileCreators.put( CHAR, new PrimitiveTypeProperties<>( char[].class, a -> new VolatileCharArray( a, true ) ) );
				volatileCreators.put( SHORT, new PrimitiveTypeProperties<>( short[].class, a -> new VolatileShortArray( a, true ) ) );
				volatileCreators.put( INT, new PrimitiveTypeProperties<>( int[].class, a -> new VolatileIntArray( a, true ) ) );
				volatileCreators.put( LONG, new PrimitiveTypeProperties<>( long[].class, a -> new VolatileLongArray( a, true ) ) );
				volatileCreators.put( FLOAT, new PrimitiveTypeProperties<>( float[].class, a -> new VolatileFloatArray( a, true ) ) );
				volatileCreators.put( DOUBLE, new PrimitiveTypeProperties<>( double[].class, a -> new VolatileDoubleArray( a, true ) ) );
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
				return ( CopyValidPixels< T > ) _boolean;
			case BYTE:
				return ( CopyValidPixels< T > ) _byte;
			case CHAR:
				return ( CopyValidPixels< T > ) _char;
			case SHORT:
				return ( CopyValidPixels< T > ) _short;
			case INT:
				return ( CopyValidPixels< T > ) _int;
			case LONG:
				return ( CopyValidPixels< T > ) _long;
			case FLOAT:
				return ( CopyValidPixels< T > ) _float;
			case DOUBLE:
				return ( CopyValidPixels< T > ) _double;
			case UNDEFINED:
				throw new UnsupportedOperationException();
			}
			throw new IllegalArgumentException();
		}

		CopyValidPixels< boolean[] > _boolean = ( src, mask, resolutionIndex, dest, length ) ->
		{
			final boolean[] data = src.data();
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

		CopyValidPixels< byte[] > _byte = ( src, mask, resolutionIndex, dest, length ) ->
		{
			final byte[] data = src.data();
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

		CopyValidPixels< char[] > _char = ( src, mask, resolutionIndex, dest, length ) ->
		{
			final char[] data = src.data();
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

		CopyValidPixels< int[] > _int = ( src, mask, resolutionIndex, dest, length ) ->
		{
			final int[] data = src.data();
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

		CopyValidPixels< long[] > _long = ( src, mask, resolutionIndex, dest, length ) ->
		{
			final long[] data = src.data();
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

		CopyValidPixels< float[] > _float = ( src, mask, resolutionIndex, dest, length ) ->
		{
			final float[] data = src.data();
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

		CopyValidPixels< double[] > _double = ( src, mask, resolutionIndex, dest, length ) ->
		{
			final double[] data = src.data();
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
