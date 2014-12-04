package bdv.img.hdf5;

import mpicbg.spim.data.sequence.ViewId;

public class ViewLevelId implements Comparable< ViewLevelId >
{
	/**
	 * The timepoint id (index).
	 */
	protected final int timepointId;

	/**
	 * The setup id (index within the timepoint).
	 */
	protected final int setupId;

	protected final int level;

	public ViewLevelId( final int timepointId, final int setupId, final int level )
	{
		this.timepointId = timepointId;
		this.setupId = setupId;
		this.level = level;
	}

	public ViewLevelId( final ViewId viewId, final int level )
	{
		this.timepointId = viewId.getTimePointId();
		this.setupId = viewId.getViewSetupId();
		this.level = level;
	}

	public int getTimePointId()
	{
		return timepointId;
	}

	public int getViewSetupId()
	{
		return setupId;
	}

	public int getLevel()
	{
		return level;
	}

	/**
	 * Two {@link ViewLevelId} are equal if they have the same
	 * {@link #getTimePointId() timepoint}, {@link #getViewSetupId() setup},
	 * and {@link #getLevel() level}. ids.
	 */
	@Override
	public boolean equals( final Object o )
	{
		if ( o != null && o instanceof ViewLevelId )
		{
			final ViewLevelId i = ( ViewLevelId ) o;
			if ( i.timepointId == timepointId &&
					i.setupId == setupId &&
					i.level == level )
				return true;
		}
		return false;
	}

	/**
	 * Order by ascending {@link #getTimePointId() timepoint} id, then
	 * {@link #getViewSetupId() setup} id, then {@link #getLevel() level}.
	 */
	@Override
	public int compareTo( final ViewLevelId o )
	{
		if ( timepointId == o.timepointId )
		{
			if ( setupId == o.setupId )
				return level - o.level;
			else
				return setupId - o.setupId;
		}
		else
			return timepointId - o.timepointId;
	}

	@Override
	public int hashCode()
	{
		// some non-colliding hash assuming we have that ids are below 2^15
		// for timepoint, 2^10 for setups, and 2^6 for levels
		return getLevel() + 1024 * ( getViewSetupId() + 32768 * getTimePointId() );
	}
}