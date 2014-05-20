package bdv.ij.export;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;

import org.jdom2.JDOMException;

import bdv.export.ExportMipmapInfo;
import bdv.export.WriteSequenceToHdf5;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Hdf5ImageLoader.MipmapInfo;
import bdv.img.hdf5.Partition;
import bdv.img.hdf5.Util;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

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
	public static void fix( final String xmlFilename ) throws JDOMException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException
	{
		final XmlIoSpimDataMinimal spimDataIo = new XmlIoSpimDataMinimal();
		final SpimDataMinimal spimData = spimDataIo.load( xmlFilename );
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		final Hdf5ImageLoader il = ( Hdf5ImageLoader) seq.getImgLoader();
		final String outfn = il.getHdf5File().getCanonicalPath() + "FIXED";
		final HashMap< Integer, ExportMipmapInfo > perSetupMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{
			final int setupId = setup.getId();
			final MipmapInfo info = il.getMipmapInfo( setupId );
			perSetupMipmapInfo.put( setupId, new ExportMipmapInfo(
					Util.castToInts( info.getResolutions() ),
					info.getSubdivisions() ) );
		}
		final ArrayList< Partition > partitions = il.getPartitions();
		WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, perSetupMipmapInfo, partitions, new File( outfn ) );

		System.out.println( "fixed hdf5 master file written to " + outfn );
		System.out.println( "rename it to " + il.getHdf5File().getCanonicalPath() + " to use it." );
	}
//	public static void main( final String[] args ) throws Exception
//	{
//		fix( "/Users/pietzsch/Desktop/data/valia2/valia.xml" );
//	}
}
