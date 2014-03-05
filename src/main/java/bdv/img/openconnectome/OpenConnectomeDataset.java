package bdv.img.openconnectome;

import java.io.Serializable;
import java.util.HashMap;

public class OpenConnectomeDataset implements Serializable
{
	private static final long serialVersionUID = -3203047697934754547L;

	public HashMap< String, int[] > cube_dimension;
	public HashMap< String, long[] > imagesize;
	public HashMap< String, long[] > isotropic_slicerange;
	public HashMap< String, Long > neariso_scaledown;
	public long[] resolutions;
	public long[] slicerange;
	public HashMap< String, Double > zscale;
	public HashMap< String, long[] > zscaled_slicerange;
}