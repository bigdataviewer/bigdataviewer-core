package net.imglib2.img.basictypeaccess.volatiles.array;

import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileShortAccess;

/**
 * A {@link ShortArray} with an {@link #isValid()} flag.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class VolatileShortArray extends AbstractVolatileArray< VolatileShortArray > implements VolatileShortAccess
{
	protected short data[];

	public VolatileShortArray( final int numEntities, final boolean isValid )
	{
		super( isValid );
		this.data = new short[ numEntities ];
	}

	public VolatileShortArray( final short[] data, final boolean isValid )
	{
		super( isValid );
		this.data = data;
	}

	@Override
	public short getValue( final int index )
	{
		return data[ index ];
	}

	@Override
	public void setValue( final int index, final short value )
	{
		data[ index ] = value;
	}

	@Override
	public VolatileShortArray createArray( final int numEntities )
	{
		return new VolatileShortArray( numEntities, true );
	}

	@Override
	public short[] getCurrentStorageArray()
	{
		return data;
	}
}
