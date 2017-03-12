package bdv.img.cache;

import java.util.Arrays;

import net.imglib2.cache.img.AccessFlags;
import net.imglib2.cache.img.PrimitiveType;
import net.imglib2.cache.volatiles.CreateInvalid;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileCharArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileCharArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * A {@link CreateInvalid} that produces empty cells of
 * {@link VolatileArrayDataAccess} type {@code A}, with the correct dimensions,
 * etc.
 * <p>
 * The same {@link VolatileArrayDataAccess} is re-used for many cells. If a Cell
 * needs a bigger access, we allocate a new one and then re-use that, and so on.
 * </p>
 * <p>
 * Usually, {@link CreateInvalidVolatileCell} should be created through static
 * helper methods
 * {@link #get(CellGrid, Fraction, PrimitiveType, AccessFlags...)} or
 * {@link #get(CellGrid, NativeType, AccessFlags...)} to get the desired
 * primitive type and dirty variant.
 * </p>
 *
 * @param <A>
 *            access type
 *
 * @author Tobias Pietzsch
 */
public class CreateInvalidVolatileCell< A extends VolatileArrayDataAccess< A > > implements CreateInvalid< Long, Cell< A > >
{
	private final CellGrid grid;

	private final Fraction entitiesPerPixel;

	private final EmptyArrayCreator< A > creator;

	public CreateInvalidVolatileCell(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final EmptyArrayCreator< A > creator )
	{
		this.grid = grid;
		this.entitiesPerPixel = entitiesPerPixel;
		this.creator = creator;
	}

	@Override
	public Cell< A > createInvalid( final Long key ) throws Exception
	{
		final long index = key;
		final long[] cellMin = new long[ grid.numDimensions() ];
		final int[] cellDims = new int[ grid.numDimensions() ];
		grid.getCellDimensions( index, cellMin, cellDims );
		final long numEntities = entitiesPerPixel.mulCeil( Intervals.numElements( cellDims ) );
		final A data = creator.getEmptyArray( numEntities ).data;
		return new Cell<>( cellDims, cellMin, data );
	}

	public static < T extends NativeType< T >, A extends VolatileArrayDataAccess< A > > CreateInvalidVolatileCell< A > get(
			final CellGrid grid,
			final T type,
			final AccessFlags ... flags )
	{
		return get( grid, type.getEntitiesPerPixel(), PrimitiveType.forNativeType( type ), flags );
	}

	public static < A extends VolatileArrayDataAccess< A > > CreateInvalidVolatileCell< A > get(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final PrimitiveType primitiveType,
			final AccessFlags ... flags )
	{
		return new CreateInvalidVolatileCell< A >( grid, entitiesPerPixel, EmptyArrayCreator.get( primitiveType, flags ) );
	}
}


class EmptyArrayCreator< A extends VolatileArrayDataAccess< A > >
{
	public static final int INITIAL_EMPTY_SIZE = 32 * 32 * 32;

	private final A creator;

	private EmptyArray< A > theEmptyArray;

	static class EmptyArray< A extends VolatileArrayDataAccess< A > >
	{
		A data;

		int numEntities;

		EmptyArray( final int numEntities, final A creator )
		{
			this.data = creator.createArray( numEntities, false );
			this.numEntities = numEntities;
		}
	}

	public EmptyArrayCreator( final A creator )
	{
		this.creator = creator;
		this.theEmptyArray = new EmptyArray<>( INITIAL_EMPTY_SIZE, creator );
	}

	public EmptyArray< A > getEmptyArray( final long numEntities )
	{
		EmptyArray< A > empty = theEmptyArray;
		if ( empty.numEntities < numEntities )
		{
			empty = new EmptyArray<>( ( int ) numEntities, creator );
			theEmptyArray = empty;
		}
		return empty;
	}

	static EmptyArrayCreator< DirtyVolatileByteArray > dirtyBytes;

	static EmptyArrayCreator< VolatileByteArray > bytes;

	static EmptyArrayCreator< DirtyVolatileCharArray > dirtyChars;

	static EmptyArrayCreator< VolatileCharArray > chars;

	static EmptyArrayCreator< DirtyVolatileDoubleArray > dirtyDoubles;

	static EmptyArrayCreator< VolatileDoubleArray > doubles;

	static EmptyArrayCreator< DirtyVolatileFloatArray > dirtyFloats;

	static EmptyArrayCreator< VolatileFloatArray > floats;

	static EmptyArrayCreator< DirtyVolatileIntArray > dirtyInts;

	static EmptyArrayCreator< VolatileIntArray > ints;

	static EmptyArrayCreator< DirtyVolatileLongArray > dirtyLongs;

	static EmptyArrayCreator< VolatileLongArray > longs;

	static EmptyArrayCreator< DirtyVolatileShortArray > dirtyShorts;

	static EmptyArrayCreator< VolatileShortArray > shorts;

	@SuppressWarnings( "unchecked" )
	public static < A extends VolatileArrayDataAccess< A > > EmptyArrayCreator< A > get(
			final PrimitiveType primitiveType,
			final AccessFlags ... flags )
	{
		final boolean dirty = Arrays.asList( flags ).contains( AccessFlags.DIRTY );
		switch ( primitiveType )
		{
		case BYTE:
			if ( dirty )
			{
				if ( dirtyBytes == null )
					dirtyBytes = new EmptyArrayCreator<>( new DirtyVolatileByteArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) dirtyBytes;
			}
			else
			{
				if ( bytes == null )
					bytes = new EmptyArrayCreator<>( new VolatileByteArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) bytes;
			}
		case CHAR:
			if ( dirty )
			{
				if ( dirtyChars == null )
					dirtyChars = new EmptyArrayCreator<>( new DirtyVolatileCharArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) dirtyChars;
			}
			else
			{
				if ( chars == null )
					chars = new EmptyArrayCreator<>( new VolatileCharArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) chars;
			}
		case DOUBLE:
			if ( dirty )
			{
				if ( dirtyDoubles == null )
					dirtyDoubles = new EmptyArrayCreator<>( new DirtyVolatileDoubleArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) dirtyDoubles;
			}
			else
			{
				if ( doubles == null )
					doubles = new EmptyArrayCreator<>( new VolatileDoubleArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) doubles;
			}
		case FLOAT:
			if ( dirty )
			{
				if ( dirtyFloats == null )
					dirtyFloats = new EmptyArrayCreator<>( new DirtyVolatileFloatArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) dirtyFloats;
			}
			else
			{
				if ( floats == null )
					floats = new EmptyArrayCreator<>( new VolatileFloatArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) floats;
			}
		case INT:
			if ( dirty )
			{
				if ( dirtyInts == null )
					dirtyInts = new EmptyArrayCreator<>( new DirtyVolatileIntArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) dirtyInts;
			}
			else
			{
				if ( ints == null )
					ints = new EmptyArrayCreator<>( new VolatileIntArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) ints;
			}
		case LONG:
			if ( dirty )
			{
				if ( dirtyLongs == null )
					dirtyLongs = new EmptyArrayCreator<>( new DirtyVolatileLongArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) dirtyLongs;
			}
			else
			{
				if ( longs == null )
					longs = new EmptyArrayCreator<>( new VolatileLongArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) longs;
			}
		case SHORT:
			if ( dirty )
			{
				if ( dirtyShorts == null )
					dirtyShorts = new EmptyArrayCreator<>( new DirtyVolatileShortArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) dirtyShorts;
			}
			else
			{
				if ( shorts == null )
					shorts = new EmptyArrayCreator<>( new VolatileShortArray( 0, false ) );
				return ( EmptyArrayCreator< A > ) shorts;
			}
		default:
			return null;
		}
	}
}

