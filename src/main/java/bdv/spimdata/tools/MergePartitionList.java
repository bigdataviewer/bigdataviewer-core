package bdv.spimdata.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import bdv.export.ExportMipmapInfo;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.Partition;
import bdv.img.hdf5.Util;

public class MergePartitionList
{
	public static ArrayList< Partition > getMergedHdf5PartitionList( final AbstractSequenceDescription< ?, ?, ? > seq1, final AbstractSequenceDescription< ?, ?, ? > seq2 )
	{
		final ArrayList< Partition > partitions = getPartitions( seq1 );
		partitions.addAll( getPartitions( seq2 ) );
		return partitions;
	}

	public static Map< Integer, ExportMipmapInfo > getHdf5PerSetupExportMipmapInfos( final AbstractSequenceDescription< ?, ?, ? > seq )
	{
		final Hdf5ImageLoader imgLoader = getHdf5ImageLoader( seq );
		final HashMap< Integer, ExportMipmapInfo > perSetupMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
		for ( final int setupId : seq.getViewSetups().keySet() )
		{
			final MipmapInfo info = imgLoader.getMipmapInfo( setupId );
			perSetupMipmapInfo.put( setupId, new ExportMipmapInfo( Util.castToInts( info.getResolutions() ), info.getSubdivisions() ) );
		}
		return perSetupMipmapInfo;
	}

	private static ArrayList< Partition > getPartitions( final AbstractSequenceDescription< ?, ?, ? > seq )
	{
		// create partition list for existing dataset
		final Hdf5ImageLoader imgLoader = getHdf5ImageLoader( seq );
		final ArrayList< Partition > partitions = new ArrayList< Partition >( imgLoader.getPartitions() );
		if ( partitions.isEmpty() )
		{
			// maps every existing timepoint id to itself
			final Map< Integer, Integer > timepointIdentityMap = new HashMap< Integer, Integer >();
			for ( final TimePoint tp : seq.getTimePoints().getTimePointsOrdered() )
				timepointIdentityMap.put( tp.getId(), tp.getId() );

			// maps every existing setup id to itself
			final Map< Integer, Integer > setupIdentityMap = new HashMap< Integer, Integer >();
			for ( final int s : seq.getViewSetups().keySet() )
				setupIdentityMap.put( s, s );

			// add one partition for the unpartitioned existing dataset
			partitions.add( new Partition( imgLoader.getHdf5File().getAbsolutePath(), timepointIdentityMap, setupIdentityMap ) );
		}
		return partitions;
	}

	private static Hdf5ImageLoader getHdf5ImageLoader( final AbstractSequenceDescription< ?, ?, ? > seq )
	{
		final BasicImgLoader< ? > imgLoader = seq.getImgLoader();
		if ( imgLoader instanceof Hdf5ImageLoader )
			return ( Hdf5ImageLoader ) imgLoader;
		else
			throw new IllegalArgumentException( "invalid ImgLoader class " + imgLoader.getClass() + " (expected Hdf5ImageLoader)" );
	}
}
