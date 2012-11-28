package viewer.hdf5.img;

import net.imglib2.img.cell.AbstractCell;

public class Hdf5Cell< A > extends AbstractCell< A >
{
	public interface CellLoader< A >
	{
		public A loadCell( final int[] dimensions, final long[] min, final int entitiesPerPixel );
	}

	private final A data;

	public Hdf5Cell( final CellLoader< A > creator, final int[] dimensions, final long[] min, final int entitiesPerPixel )
	{
		super( dimensions, min );
		this.data = creator.loadCell( dimensions, min, entitiesPerPixel );
	}

	@Override
	public A getData()
	{
		return data;
	}
}
