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
