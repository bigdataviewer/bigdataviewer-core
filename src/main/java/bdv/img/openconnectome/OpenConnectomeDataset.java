package bdv.img.openconnectome;

import java.io.Serializable;
import java.util.HashMap;

public class OpenConnectomeDataset implements Serializable
{
	private static final long serialVersionUID = 7020249341572881545L;

	public HashMap< String, int[] > cube_dimension;
	public String description;
	public HashMap< String, long[] > imagesize;
	public HashMap< String, Long > neariso_scaledown;
	public HashMap< String, long[] > offset;
	public long[] resolutions;
	public HashMap< String, double[] > voxelres;
}
