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

//	public static void main( final String[] args )
//	{
//		final File hdf5CellsFile = new File( "/home/tobias/workspace/data/fast fly/111010_weber/e012-cells-mipmap2.h5" );
//		final IHDF5SimpleWriter writer = HDF5Factory.open( hdf5CellsFile );
//		final double[][] dres = new double[ resolutions.length ][];
//		for ( int l = 0; l < resolutions.length; ++l )
//		{
//			dres[ l ] = new double[ resolutions[ l ].length ];
//			for ( int d = 0; d < resolutions[ l ].length; ++d )
//				dres[ l ][ d ] = resolutions[ l ][ d ];
//		}
//		writer.writeDoubleMatrix( "resolutions", dres );
//		writer.writeIntMatrix( "subdivisions", subdivisions );
//		writer.close();
//	}
}
