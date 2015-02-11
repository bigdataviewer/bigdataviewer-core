package bdv.img.hdf5;

/**
 * The dimensions of an image and a flag indicating whether that image
 * exists (can be loaded)
 *
 * @author Tobias Pietzsch
 */
public class DimsAndExistence
{
	private final long[] dimensions;

	private final boolean exists;

	public DimsAndExistence( final long[] dimensions, final boolean exists )
	{
		this.dimensions = dimensions;
		this.exists = exists;
	}

	public long[] getDimensions()
	{
		return dimensions;
	}

	public boolean exists()
	{
		return exists;
	}
}
