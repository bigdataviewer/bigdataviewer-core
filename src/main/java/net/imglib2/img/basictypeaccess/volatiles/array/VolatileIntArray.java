package net.imglib2.img.basictypeaccess.volatiles.array;

import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileIntAccess;

/**
 * A {@link ShortArray} with an {@link #isValid()} flag.
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class VolatileIntArray extends AbstractVolatileArray< VolatileIntArray > implements VolatileIntAccess
{
	private static final long serialVersionUID = -5626240246651573531L;
	
	protected int[] data;

	public VolatileIntArray( final int numEntities, final boolean isValid )
	{
		super( isValid );
		this.data = new int[ numEntities ];
	}

	public VolatileIntArray( final int[] data, final boolean isValid )
	{
		super( isValid );
		this.data = data;
	}

	@Override
	public int getValue( final int index )
	{
		return data[ index ];
	}

	@Override
	public void setValue( final int index, final int value )
	{
		data[ index ] = value;
	}

	@Override
	public VolatileIntArray createArray( final int numEntities )
	{
		return new VolatileIntArray( numEntities, true );
	}

	@Override
	public int[] getCurrentStorageArray()
	{
		return data;
	}
}
