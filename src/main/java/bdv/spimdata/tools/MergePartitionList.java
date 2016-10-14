/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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

	/**
	 * Return map from setup id to {@link ExportMipmapInfo} for the
	 * {@link Hdf5ImageLoader} of the specified
	 * {@link AbstractSequenceDescription}.
	 *
	 * @param seq
	 *            a sequence.
	 * @return map from setup id to {@link ExportMipmapInfo}.
	 * @throws IllegalArgumentException
	 *             if the specified {@link AbstractSequenceDescription} does not
	 *             have an {@link Hdf5ImageLoader}.
	 */
	public static Map< Integer, ExportMipmapInfo > getHdf5PerSetupExportMipmapInfos( final AbstractSequenceDescription< ?, ?, ? > seq )
			throws IllegalArgumentException
	{
		final Hdf5ImageLoader imgLoader = getHdf5ImageLoader( seq );
		final HashMap< Integer, ExportMipmapInfo > perSetupMipmapInfo = new HashMap<>();
		for ( final int setupId : seq.getViewSetups().keySet() )
		{
			final MipmapInfo info = imgLoader.getSetupImgLoader( setupId ).getMipmapInfo();;
			perSetupMipmapInfo.put( setupId, new ExportMipmapInfo( Util.castToInts( info.getResolutions() ), info.getSubdivisions() ) );
		}
		return perSetupMipmapInfo;
	}

	/**
	 * Get the {@link Partition} list for the {@link Hdf5ImageLoader} of the
	 * specified {@link AbstractSequenceDescription}. If the dataset is not
	 * partitioned, a single partition containing the full dataset is created.
	 *
	 * @param seq
	 *            a sequence.
	 * @return partition list of the specified sequence.
	 * @throws IllegalArgumentException
	 *             if the specified {@link AbstractSequenceDescription} does not
	 *             have an {@link Hdf5ImageLoader}.
	 */
	public static ArrayList< Partition > getPartitions( final AbstractSequenceDescription< ?, ?, ? > seq )
			throws IllegalArgumentException
	{
		// create partition list for existing dataset
		final Hdf5ImageLoader imgLoader = getHdf5ImageLoader( seq );
		final ArrayList< Partition > partitions = new ArrayList<>( imgLoader.getPartitions() );
		if ( partitions.isEmpty() )
		{
			// maps every existing timepoint id to itself
			final Map< Integer, Integer > timepointIdentityMap = new HashMap<>();
			for ( final TimePoint tp : seq.getTimePoints().getTimePointsOrdered() )
				timepointIdentityMap.put( tp.getId(), tp.getId() );

			// maps every existing setup id to itself
			final Map< Integer, Integer > setupIdentityMap = new HashMap<>();
			for ( final int s : seq.getViewSetups().keySet() )
				setupIdentityMap.put( s, s );

			// add one partition for the unpartitioned existing dataset
			partitions.add( new Partition( imgLoader.getHdf5File().getAbsolutePath(), timepointIdentityMap, setupIdentityMap ) );
		}
		return partitions;
	}

	/**
	 * Get the {@link Hdf5ImageLoader} of a {@link AbstractSequenceDescription},
	 * or throw an exception if the sequence doesn't have an
	 * {@link Hdf5ImageLoader}.
	 *
	 * @param seq
	 *            a sequence.
	 * @return the {@link Hdf5ImageLoader} of the sequence.
	 * @throws IllegalArgumentException
	 *             if the specified {@link AbstractSequenceDescription} does not
	 *             have an {@link Hdf5ImageLoader}.
	 */
	public static Hdf5ImageLoader getHdf5ImageLoader( final AbstractSequenceDescription< ?, ?, ? > seq )
			throws IllegalArgumentException
	{
		final BasicImgLoader imgLoader = seq.getImgLoader();
		if ( imgLoader instanceof Hdf5ImageLoader )
			return ( Hdf5ImageLoader ) imgLoader;
		else
			throw new IllegalArgumentException( "invalid ImgLoader class " + imgLoader.getClass() + " (expected Hdf5ImageLoader)" );
	}
}
