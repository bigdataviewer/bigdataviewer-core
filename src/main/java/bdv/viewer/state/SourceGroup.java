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
package bdv.viewer.state;

import java.util.SortedSet;
import java.util.TreeSet;

import bdv.viewer.DisplayMode;

/**
 * TODO
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class SourceGroup
{
	final ViewerState owner;

	protected final TreeSet< Integer > sourceIds;

	protected String name;

	/**
	 * Whether the group is active (visible in {@link DisplayMode#FUSED} mode).
	 */
	protected boolean isActive;

	/**
	 * Whether the group is current (only group visible in
	 * {@link DisplayMode#FUSED} mode).
	 */
	protected boolean isCurrent;

	public SourceGroup( final String name, final ViewerState owner )
	{
		this.owner = owner;
		sourceIds = new TreeSet< Integer >();
		this.name = name;
		isActive = true;
		isCurrent = false;
	}

	public SourceGroup( final SourceGroup g, final ViewerState owner )
	{
		this.owner = owner;
		sourceIds = new TreeSet< Integer >( g.sourceIds );
		name = g.name;
		isActive = g.isActive;
		isCurrent = g.isCurrent;
	}

	public SourceGroup copy( final ViewerState owner )
	{
		return new SourceGroup( this, owner );
	}

	public void addSource( final int sourceId )
	{
		synchronized ( owner )
		{
			sourceIds.add( sourceId );
		}
	}

	public void removeSource( final int sourceId )
	{
		synchronized ( owner )
		{
			sourceIds.remove( sourceId );
		}
	}

	public SortedSet< Integer > getSourceIds()
	{
		return sourceIds;
	}

	public String getName()
	{
		return name;
	}

	public void setName( final String name )
	{
		this.name = name;
	}

	/**
	 * Is the group active (visible in fused mode)?
	 *
	 * @return whether the source is active.
	 */
	public boolean isActive()
	{
		return isActive;
	}

	/**
	 * Set the group active (visible in fused mode) or inactive.
	 */
	public void setActive( final boolean isActive )
	{
		synchronized ( owner )
		{
			this.isActive = isActive;
		}
	}

	/**
	 * Is this group the current group?
	 *
	 * @return whether the group is current.
	 */
	public boolean isCurrent()
	{
		return isCurrent;
	}

	/**
	 * Set this group current (or not).
	 */
	public void setCurrent( final boolean isCurrent )
	{
		synchronized ( owner )
		{
			this.isCurrent = isCurrent;
		}
	}

}
