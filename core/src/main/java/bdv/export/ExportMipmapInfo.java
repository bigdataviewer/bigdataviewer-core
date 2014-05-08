package bdv.export;

import bdv.img.hdf5.Hdf5ImageLoader.MipmapInfo;
import bdv.img.hdf5.Util;

public class ExportMipmapInfo extends MipmapInfo
{
	public final int[][] intResolutions;

	public ExportMipmapInfo( final int[][] resolutions, final int[][] subdivisions )
	{
		super( Util.castToDoubles( resolutions ), null, subdivisions );
		this.intResolutions = resolutions;
	}

	public int[][] getExportResolutions()
	{
		return intResolutions;
	}
}
