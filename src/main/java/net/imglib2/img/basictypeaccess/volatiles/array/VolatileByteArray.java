package net.imglib2.img.basictypeaccess.volatiles.array;

import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileByteAccess;

/**
 * A {@link ByteArray} with an {@link #isValid()} flag.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch
 */
public class VolatileByteArray extends AbstractVolatileArray< VolatileByteArray > implements VolatileByteAccess
{
	private static final long serialVersionUID = -2609245209721069962L;

	protected byte[] data;

	public VolatileByteArray( final int numEntities, final boolean isValid )
	{
		super( isValid );
		this.data = new byte[ numEntities ];
	}

	public VolatileByteArray( final byte[] data, final boolean isValid )
	{
		super( isValid );
		this.data = data;
	}

	@Override
	public byte getValue( final int index )
	{
		return data[ index ];
	}

	@Override
	public void setValue( final int index, final byte value )
	{
		data[ index ] = value;
	}

	@Override
	public VolatileByteArray createArray( final int numEntities )
	{
		return new VolatileByteArray( numEntities, true );
	}

	@Override
	public byte[] getCurrentStorageArray()
	{
		return data;
	}
}
