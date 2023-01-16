package bdv.img.n5;

import java.util.EnumMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.imglib2.Volatile;
import net.imglib2.img.basictypeaccess.DataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileByteType;
import net.imglib2.type.volatiles.VolatileDoubleType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileIntType;
import net.imglib2.type.volatiles.VolatileLongType;
import net.imglib2.type.volatiles.VolatileShortType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedIntType;
import net.imglib2.type.volatiles.VolatileUnsignedLongType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import org.janelia.saalfeldlab.n5.DataType;

import static org.janelia.saalfeldlab.n5.DataType.FLOAT32;
import static org.janelia.saalfeldlab.n5.DataType.FLOAT64;
import static org.janelia.saalfeldlab.n5.DataType.INT16;
import static org.janelia.saalfeldlab.n5.DataType.INT32;
import static org.janelia.saalfeldlab.n5.DataType.INT64;
import static org.janelia.saalfeldlab.n5.DataType.INT8;
import static org.janelia.saalfeldlab.n5.DataType.UINT16;
import static org.janelia.saalfeldlab.n5.DataType.UINT32;
import static org.janelia.saalfeldlab.n5.DataType.UINT64;
import static org.janelia.saalfeldlab.n5.DataType.UINT8;

/**
 * Types and creators for N5 {@code DataType}.
 *
 * @param <T>
 * 		type
 * @param <V>
 * 		volatile type
 * @param <P>
 * 		primitive array type
 * @param <A>
 * 		corresponding {@code VolatileArrayDataAccess} type
 */
public class DataTypeProperties< T extends NativeType< T >, V extends Volatile< T > & NativeType< V >, P, A extends DataAccess >
{
	private DataType dataType;

	private final T type;

	private final V volatileType;

	private final IntFunction< P > createPrimitiveArray;

	private final Function< P, A > createVolatileArrayAccess;

	private static final EnumMap< DataType, DataTypeProperties< ?, ?, ?, ? > > props = new EnumMap<>( DataType.class );

	static {
		props.put( INT8, new DataTypeProperties<>( INT8,
				new ByteType(), new VolatileByteType(),
				byte[]::new, data -> new VolatileByteArray( data, true ) ) );
		props.put( UINT8, new DataTypeProperties<>( UINT8,
				new UnsignedByteType(), new VolatileUnsignedByteType(),
				byte[]::new, data -> new VolatileByteArray( data, true ) ) );
		props.put( INT16, new DataTypeProperties<>( INT16,
				new ShortType(), new VolatileShortType(),
				short[]::new, data -> new VolatileShortArray( data, true ) ) );
		props.put( UINT16, new DataTypeProperties<>( UINT16,
				new UnsignedShortType(), new VolatileUnsignedShortType(),
				short[]::new, data -> new VolatileShortArray( data, true ) ) );
		props.put( INT32, new DataTypeProperties<>( INT32,
				new IntType(), new VolatileIntType(),
				int[]::new, data -> new VolatileIntArray( data, true ) ) );
		props.put( UINT32, new DataTypeProperties<>( UINT32,
				new UnsignedIntType(), new VolatileUnsignedIntType(),
				int[]::new, data -> new VolatileIntArray( data, true ) ) );
		props.put( INT64, new DataTypeProperties<>( INT64,
				new LongType(), new VolatileLongType(),
				long[]::new, data -> new VolatileLongArray( data, true ) ) );
		props.put( UINT64, new DataTypeProperties<>( UINT64,
				new UnsignedLongType(), new VolatileUnsignedLongType(),
				long[]::new, data -> new VolatileLongArray( data, true ) ) );
		props.put( FLOAT32, new DataTypeProperties<>( FLOAT32,
				new FloatType(), new VolatileFloatType(),
				float[]::new, data -> new VolatileFloatArray( data, true ) ) );
		props.put( FLOAT64, new DataTypeProperties<>( FLOAT64,
				new DoubleType(), new VolatileDoubleType(),
				double[]::new, data -> new VolatileDoubleArray( data, true ) ) );
	}

	private DataTypeProperties(
			final DataType dataType,
			final T type, final V volatileType,
			final IntFunction< P > createPrimitiveArray, final Function< P, A > createVolatileArrayAccess )
	{
		this.dataType = dataType;
		this.type = type;
		this.volatileType = volatileType;
		this.createPrimitiveArray = createPrimitiveArray;
		this.createVolatileArrayAccess = createVolatileArrayAccess;
	}

	/**
	 * @return a function to create a primitive array of type {@code P}
	 * given the number of elements.
	 */
	public IntFunction< P > createPrimitiveArray()
	{
		return createPrimitiveArray;
	}

	/**
	 * @return a function that wraps a primitive array of type {@code P}
	 * into a {@code VolatileArrayDataAccess} of appropriate type.
	 */
	public Function< P, A > createVolatileArrayAccess()
	{
		return createVolatileArrayAccess;
	}

	/**
	 * @return ImgLib2 type
	 */
	public T type()
	{
		return type;
	}

	/**
	 * @return ImgLib2 Volatile type
	 */
	public V volatileType()
	{
		return volatileType;
	}

	public DataType dataType()
	{
		return dataType;
	}

	public static DataTypeProperties< ?, ?, ?, ? > of( final DataType dataType )
	{
		return props.get( dataType );
	}
}
