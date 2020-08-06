/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package bdv.img.cache;

import net.imglib2.cache.volatiles.CreateInvalid;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
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
 * {@link #get(CellGrid, Fraction, PrimitiveType, boolean)} or
 * {@link #get(CellGrid, NativeType, boolean)} to get the desired
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
			final boolean dirty ) throws IllegalArgumentException
	{
		return get( grid, type.getEntitiesPerPixel(), type.getNativeTypeFactory().getPrimitiveType(), dirty );
	}

	public static < A extends VolatileArrayDataAccess< A > > CreateInvalidVolatileCell< A > get(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final PrimitiveType primitiveType,
			final boolean dirty ) throws IllegalArgumentException
	{
		if ( primitiveType == PrimitiveType.UNDEFINED )
			throw new IllegalArgumentException( "Cannot instantiate " + CreateInvalidVolatileCell.class.getSimpleName() + " for unrecognized primitive type" );
		return new CreateInvalidVolatileCell< A >( grid, entitiesPerPixel, EmptyArrayCreator.get( primitiveType, dirty ) );
	}
}

