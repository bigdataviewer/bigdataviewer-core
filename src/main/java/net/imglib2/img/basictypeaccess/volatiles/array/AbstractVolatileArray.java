package net.imglib2.img.basictypeaccess.volatiles.array;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;

/**
 * A {@link ShortArray} with an {@link #isValid()} flag.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public abstract class AbstractVolatileArray< T extends AbstractVolatileArray< T > > implements ArrayDataAccess< T >, VolatileAccess
{
	private static final long serialVersionUID = -3233057138272085300L;
	
	protected final boolean isValid;

	public AbstractVolatileArray( final boolean isValid )
	{
		this.isValid = isValid;
	}
	
	@Override
	public boolean isValid()
	{
		return isValid;
	}
}
