package viewer.render;

import net.imglib2.Volatile;

public interface VolatileSource< T, V extends Volatile< T > > extends Source< V >
{
	public Source< T > nonVolatile();
}
