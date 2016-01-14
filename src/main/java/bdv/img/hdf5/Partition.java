/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.img.hdf5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;

/**
 * Partition describes one partition file of a dataset that is split across
 * multiple hdf5 files. An aggregating hdf5 linking the partition files can be
 * created using the Partition information (without looking at the constituent
 * files).
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class Partition
{
	protected final String path;

	/**
	 * maps {@link TimePoint#getId() timepoint ids} of the full sequence to
	 * timepoint ids of the partition.
	 */
	private final Map< Integer, Integer > timepointIdSequenceToPartition;

	/**
	 * maps {@link BasicViewSetup#getId() setup ids} of the full sequence to
	 * setup ids of the partition.
	 */
	private final Map< Integer, Integer > setupIdSequenceToPartition;

	/**
	 * Create a new Partition description.
	 *
	 * @param path
	 *            path to the hdf5 file for this partition
	 * @param timepointIdsSequence
	 * @param setupIdsSequence
	 * @param timepointIdsPartition
	 * @param setupIdsPartition
	 */
	public Partition( final String path, final int[] timepointIdsSequence, final int[] setupIdsSequence, final int[] timepointIdsPartition, final int[] setupIdsPartition )
	{
		this.path = path;
		timepointIdSequenceToPartition = new HashMap< Integer, Integer >();
		setupIdSequenceToPartition = new HashMap< Integer, Integer >();

		for ( int i = 0; i < timepointIdsSequence.length; ++i )
			timepointIdSequenceToPartition.put( timepointIdsSequence[ i ], timepointIdsPartition[ i ] );

		for ( int i = 0; i < setupIdsSequence.length; ++i )
			setupIdSequenceToPartition.put( setupIdsSequence[ i ], setupIdsPartition[ i ] );
	}

	/**
	 * Create a new Partition description.
	 *
	 * @param path
	 *            path to the hdf5 file for this partition
	 * @param timepointIdSequenceToPartition
	 * @param setupIdSequenceToPartition
	 */
	public Partition( final String path, final Map< Integer, Integer > timepointIdSequenceToPartition, final Map< Integer, Integer > setupIdSequenceToPartition )
	{
		this.path = path;
		this.timepointIdSequenceToPartition = Collections.unmodifiableMap( timepointIdSequenceToPartition );
		this.setupIdSequenceToPartition = Collections.unmodifiableMap( setupIdSequenceToPartition );
	}

	/**
	 * Get the  path to the hdf5 file for this partition.
	 *
	 * @return path to the hdf5 file for this partition.
	 */
	public String getPath()
	{
		return path;
	}

	/**
	 * Get a map from {@link TimePoint#getId() timepoint ids} of the full
	 * sequence to timepoint ids of this partition.
	 */
	public Map< Integer, Integer > getTimepointIdSequenceToPartition()
	{
		return timepointIdSequenceToPartition;
	}

	/**
	 * Get a map from {@link BasicViewSetup#getId() setup ids} of the full
	 * sequence to setup ids of this partition.
	 */
	public Map< Integer, Integer > getSetupIdSequenceToPartition()
	{
		return setupIdSequenceToPartition;
	}

	/**
	 * Does this partition contain the given {@link ViewId view}
	 * (timepoint-setup pair)?
	 *
	 * @param viewId
	 *            view (timepoint and setup id wrt. the full sequence)
	 * @return whether this partition contains the given view.
	 */
	public boolean contains( final ViewId viewId )
	{
		return
				timepointIdSequenceToPartition.containsKey( viewId.getTimePointId() ) &&
				setupIdSequenceToPartition.containsKey( viewId.getViewSetupId() );
	}

	/**
	 * Split a sequence into partitions, each containing a specified number of
	 * timepoints and setups.
	 *
	 * @param timepoints
	 *            list of all {@link TimePoint}s.
	 * @param setups
	 *            list of all {@link BasicViewSetup}s.
	 * @param timepointsPerPartition
	 *            how many timepoints should each partition contain (if this is
	 *            &le;0, put do not split timepoints across partitions).
	 * @param setupsPerPartition
	 *            how many setups should each partition contain (if this is
	 *            &le;0, put do not split setups across partitions).
	 * @param basename
	 *            This is used to generate paths for the partitions. Partitions
	 *            are named "basename-TT-SS.h5" where TT and SS are the index of
	 *            the timepoint and setup batch, respectively.
	 * @return list of partitions.
	 */
	public static ArrayList< Partition > split(
			final List< TimePoint > timepoints,
			final List< ? extends BasicViewSetup > setups,
			final int timepointsPerPartition,
			final int setupsPerPartition,
			final String basename )
	{
		final String partitionFilenameFormat = basename + "-%02d-%02d.h5";
		final int numTimepoints = timepoints.size();
		final int numSetups = setups.size();

		final ArrayList< Integer > timepointSplits = new ArrayList< Integer >();
		timepointSplits.add( 0 );
		if ( timepointsPerPartition > 0 )
			for ( int t = timepointsPerPartition; t < numTimepoints; t += timepointsPerPartition )
				timepointSplits.add( t );
		timepointSplits.add( numTimepoints );

		final ArrayList< HashMap< Integer, Integer > > timepointMaps = new ArrayList< HashMap< Integer, Integer > >();
		for ( int i = 0; i < timepointSplits.size() - 1; ++i )
		{
			final HashMap< Integer, Integer > timepointIdSequenceToPartition = new HashMap< Integer, Integer >();
			for ( int t = timepointSplits.get( i ); t < timepointSplits.get( i + 1 ); ++t )
			{
				final int id = timepoints.get( t ).getId();
				timepointIdSequenceToPartition.put( id, id );
			}
			timepointMaps.add( timepointIdSequenceToPartition );
		}

		final ArrayList< Integer > setupSplits = new ArrayList< Integer >();
		setupSplits.add( 0 );
		if ( setupsPerPartition > 0 )
			for ( int s = setupsPerPartition; s < numSetups; s += setupsPerPartition )
				setupSplits.add( s );
		setupSplits.add( numSetups );

		final ArrayList< HashMap< Integer, Integer > > setupMaps = new ArrayList< HashMap< Integer, Integer > >();
		for ( int i = 0; i < setupSplits.size() - 1; ++i )
		{
			final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap< Integer, Integer >();
			for ( int s = setupSplits.get( i ); s < setupSplits.get( i + 1 ); ++s )
			{
				final int id = setups.get( s ).getId();
				setupIdSequenceToPartition.put( id, id );
			}
			setupMaps.add( setupIdSequenceToPartition );
		}

		final ArrayList< Partition > partitions = new ArrayList< Partition >();
		for ( int t = 0; t < timepointMaps.size(); ++t )
		{
			for ( int s = 0; s < setupMaps.size(); ++s )
			{
				final String path = String.format( partitionFilenameFormat, t, s );
				partitions.add( new Partition( path, timepointMaps.get( t ), setupMaps.get( s ) ) );
			}
		}

		return partitions;
	}
}
