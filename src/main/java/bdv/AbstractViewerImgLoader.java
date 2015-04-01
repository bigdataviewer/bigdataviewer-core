package bdv;

import net.imglib2.Volatile;

// TODO: Remove
public abstract class AbstractViewerImgLoader< T, V extends Volatile< T > > implements ViewerImgLoader
{
	protected final T type;

	protected final V volatileType;

	public AbstractViewerImgLoader( final T type, final V volatileType )
	{
		this.type = type;
		this.volatileType = volatileType;
	}
}
