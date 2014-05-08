package bdv.img.hdf5;

import java.util.Collections;
import java.util.HashMap;
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
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
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
}
