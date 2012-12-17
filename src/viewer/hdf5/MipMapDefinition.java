package viewer.hdf5;

// TODO: store this info in the HDF5
public class MipMapDefinition
{
//  mipmap def 1
//	public static final int[][] resolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 1 } };
//	public static final int[][] subdivisions = { { 32, 32, 4 }, { 32, 32, 4 }, { 16, 16, 4 } };

//  mipmap def 2
	public static final int[][] resolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 2 } };
	public static final int[][] subdivisions = { { 32, 32, 4 }, { 16, 16, 8 }, { 8, 8, 8 } };

	public static final int numLevels = resolutions.length;
}
