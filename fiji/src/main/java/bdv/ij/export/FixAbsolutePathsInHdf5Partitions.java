package bdv.ij.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import mpicbg.spim.data.SequenceDescription;

import org.jdom2.JDOMException;

import bdv.SequenceViewsLoader;
import bdv.export.WriteSequenceToHdf5;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;

/**
 * Older versions of multi-partition hdf5 export had a bug that caused absolute
 * paths to be used in the master hdf5 to link into the partitions. This may
 * cause problems if the data is moved around. {@link #fix(String)} can be used
 * to "repair" the master hdf5 and convert the absolute paths to relative paths.
 * Call {@link #fix(String)} with the path to the xml file of the dataset. A
 * fixed version of the master hdf5 with "FIXED" appended to the filename will
 * be written (the original master hdf5 will not be overwritten). To actually
 * use the fixed version rename it (remove the "FIXED" postfix).
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class FixAbsolutePathsInHdf5Partitions
{
	public static void fix( final String xmlFilename ) throws JDOMException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final SequenceViewsLoader loader = new SequenceViewsLoader( xmlFilename );
		final SequenceDescription seq = loader.getSequenceDescription();
		final Hdf5ImageLoader il = ( Hdf5ImageLoader) seq.getImgLoader();
		final String outfn = il.getHdf5File().getCanonicalPath() + "FIXED";
		final ArrayList< int[][] > perSetupResolutions = new ArrayList< int[][] >();
		final ArrayList< int[][] > perSetupSubdivisions = new ArrayList< int[][] >();
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
		{
			final double[][] dr = il.getMipmapResolutions( setup );
			final int[][] ir = new int[ dr.length ][ dr[0].length ];
			for ( int i = 0; i < dr.length; ++i )
				for ( int j = 0; j < dr[ 0 ].length; ++j )
					ir[i][j] = ( int ) dr[i][j];
			perSetupResolutions.add( ir );
			perSetupSubdivisions.add( il.getSubdivisions( setup ) );
		}
		final ArrayList< Partition > partitions = il.getPartitions();
		WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, perSetupResolutions, perSetupSubdivisions, partitions, new File( outfn ) );

		System.out.println( "fixed hdf5 master file written to " + outfn );
		System.out.println( "rename it to " + il.getHdf5File().getCanonicalPath() + " to use it." );
	}
//	public static void main( final String[] args ) throws Exception
//	{
//		fix( "/Users/pietzsch/Desktop/data/valia2/valia.xml" );
//	}
}
