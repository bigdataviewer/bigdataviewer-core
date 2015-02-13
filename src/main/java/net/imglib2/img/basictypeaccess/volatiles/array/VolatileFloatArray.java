package net.imglib2.img.basictypeaccess.volatiles.array;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileFloatAccess;

/**
 * A {@link FloatArray} with an {@link #isValid()} flag.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class VolatileFloatArray extends AbstractVolatileArray< VolatileFloatArray > implements VolatileFloatAccess
{
	private static final long serialVersionUID = 7277115124128878542L;

	protected float data[];

	public VolatileFloatArray( final int numEntities, final boolean isValid )
	{
		super( isValid );
		this.data = new float[ numEntities ];
	}

	public VolatileFloatArray( final float[] data, final boolean isValid )
	{
		super( isValid );
		this.data = data;
	}

	@Override
	public float getValue( final int index )
	{
		return data[ index ];
	}

	@Override
	public void setValue( final int index, final float value )
	{
		data[ index ] = value;
	}

	@Override
	public VolatileFloatArray createArray( final int numEntities )
	{
		return new VolatileFloatArray( numEntities, true );
	}

	@Override
	public float[] getCurrentStorageArray()
	{
		return data;
	}
}
