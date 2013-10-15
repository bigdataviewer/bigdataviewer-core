package viewer.hdf5.img;

import net.imglib2.display.nativevolatile.VolatileShortAccess;
import net.imglib2.img.basictypeaccess.array.ShortArray;

public class VolatileShortArray extends ShortArray implements VolatileShortAccess
{
	private final boolean isValid;

	@Override
	public boolean isValid()
	{
		return isValid;
	}

	public VolatileShortArray( final int numEntities, final boolean isValid )
	{
		super( numEntities );
		this.isValid = isValid;
	}

	public VolatileShortArray( final short[] data, final boolean isValid )
	{
		super( data );
		this.isValid = isValid;
	}
}
