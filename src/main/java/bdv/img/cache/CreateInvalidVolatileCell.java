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
	public static final int INITIAL_EMPTY_SIZE = 32 * 32 * 32;

	private final CellGrid grid;

	private final Fraction entitiesPerPixel;

	private final A creator;

	private class EmptyArray
	{
		A data;

		int numEntities;

		EmptyArray( final int numEntities )
		{
			this.data = creator.createArray( numEntities, false );
			this.numEntities = numEntities;
		}
	}

	private EmptyArray theEmptyArray;

	public CreateInvalidVolatileCell(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final A creator )
	{
		this.grid = grid;
		this.entitiesPerPixel = entitiesPerPixel;
		this.creator = creator;
		this.theEmptyArray = new EmptyArray( INITIAL_EMPTY_SIZE );
	}

	@Override
	public Cell< A > createInvalid( final Long key ) throws Exception
	{
		final long index = key;
		final long[] cellMin = new long[ grid.numDimensions() ];
		final int[] cellDims = new int[ grid.numDimensions() ];
		grid.getCellDimensions( index, cellMin, cellDims );
		final long numEntities = entitiesPerPixel.mulCeil( Intervals.numElements( cellDims ) );

		EmptyArray empty = theEmptyArray;
		if ( empty.numEntities < numEntities )
		{
			empty = new EmptyArray( ( int ) numEntities );
			theEmptyArray = empty;
		}
		return new Cell<>( cellDims, cellMin, empty.data );
	}

	public static < T extends NativeType< T >, A extends VolatileArrayDataAccess< A > > CreateInvalidVolatileCell< A > get(
			final CellGrid grid,
			final T type,
			final AccessFlags ... flags )
	{
		return get( grid, type.getEntitiesPerPixel(), PrimitiveType.forNativeType( type ), flags );
	}

	@SuppressWarnings( "unchecked" )
	public static < A extends VolatileArrayDataAccess< A > > CreateInvalidVolatileCell< A > get(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final PrimitiveType primitiveType,
			final AccessFlags ... flags )
	{
		final boolean dirty = Arrays.asList( flags ).contains( AccessFlags.DIRTY );
		switch ( primitiveType )
		{
		case BYTE:
			return dirty
					? ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new DirtyVolatileByteArray( 0, true ) )
					: ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new VolatileByteArray( 0, true ) );
		case CHAR:
			return dirty
					? ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new DirtyVolatileCharArray( 0, true ) )
					: ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new VolatileCharArray( 0, true ) );
		case DOUBLE:
			return dirty
					? ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new DirtyVolatileDoubleArray( 0, true ) )
					: ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new VolatileDoubleArray( 0, true ) );
		case FLOAT:
			return dirty
					? ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new DirtyVolatileFloatArray( 0, true ) )
					: ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new VolatileFloatArray( 0, true ) );
		case INT:
			return dirty
					? ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new DirtyVolatileIntArray( 0, true ) )
					: ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new VolatileIntArray( 0, true ) );
		case LONG:
			return dirty
					? ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new DirtyVolatileLongArray( 0, true ) )
					: ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new VolatileLongArray( 0, true ) );
		case SHORT:
			return dirty
					? ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new DirtyVolatileShortArray( 0, true ) )
					: ( CreateInvalidVolatileCell< A > ) new CreateInvalidVolatileCell<>( grid, entitiesPerPixel, new VolatileShortArray( 0, true ) );
		default:
			return null;
		}
	}
}
