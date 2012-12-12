package viewer.hdf5.img;

import net.imglib2.img.cell.AbstractCell;

public class Hdf5Cell< A > extends AbstractCell< A >
{
	public Hdf5Cell( final int[] dimensions, final long[] min, final A data )
	{
		super( dimensions, min );
		this.data = data;
	}

	private final A data;

	@Override
	public A getData()
	{
		return data;
	}
}