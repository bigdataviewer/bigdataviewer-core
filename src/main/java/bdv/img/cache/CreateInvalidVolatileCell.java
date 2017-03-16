package bdv.img.cache;

import net.imglib2.cache.img.AccessFlags;
import net.imglib2.cache.img.PrimitiveType;
import net.imglib2.cache.volatiles.CreateInvalid;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
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
public class CreateInvalidVolatileCell< A > implements CreateInvalid< Long, Cell< A > >
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
		final A data = creator.getEmptyArray( numEntities );
		return new Cell<>( cellDims, cellMin, data );
	}

	public static < T extends NativeType< T >, A extends VolatileArrayDataAccess< A > > CreateInvalidVolatileCell< A > get(
			final CellGrid grid,
			final T type,
			final AccessFlags ... flags ) throws IllegalArgumentException
	{
		return get( grid, type.getEntitiesPerPixel(), PrimitiveType.forNativeType( type ), flags );
	}

	public static < A extends VolatileArrayDataAccess< A > > CreateInvalidVolatileCell< A > get(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final PrimitiveType primitiveType,
			final AccessFlags ... flags ) throws IllegalArgumentException
	{
		if ( primitiveType == PrimitiveType.UNDEFINED )
			throw new IllegalArgumentException( "Cannot instantiate " + CreateInvalidVolatileCell.class.getSimpleName() + " for unrecognized primitive type" );
		return new CreateInvalidVolatileCell< A >( grid, entitiesPerPixel, DefaultEmptyArrayCreator.get( primitiveType, flags ) );
	}
}

