package bdv.img.n5;

public class BdvN5Format
{
	public static final String DOWNSAMPLING_FACTORS_KEY = "downsamplingFactors";
	public static final String DATA_TYPE_KEY = "dataType";

	public static String getPathName( final int setupId )
	{
		return String.format( "setup%d", setupId );
	}

	public static String getPathName( final int setupId, final int timepointId )
	{
		return String.format( "setup%d/timepoint%d", setupId, timepointId );
	}

	public static String getPathName( final int setupId, final int timepointId, final int level )
	{
		return String.format( "setup%d/timepoint%d/s%d", setupId, timepointId, level );
	}
}
