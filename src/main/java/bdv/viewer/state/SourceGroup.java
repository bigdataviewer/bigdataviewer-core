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
package bdv.viewer.state;

import java.util.Collections;
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
	protected final SortedSet< Integer > sourceIds;

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

	public SourceGroup( final String name )
	{
		sourceIds = Collections.synchronizedSortedSet( new TreeSet<>() );
		this.name = name;
		isActive = true;
		isCurrent = false;
	}

	protected SourceGroup( final SourceGroup g )
	{
		synchronized ( g.sourceIds )
		{
			sourceIds = Collections.synchronizedSortedSet( new TreeSet<>( g.sourceIds ) );
		}
		name = g.name;
		isActive = g.isActive;
		isCurrent = g.isCurrent;
	}

	public SourceGroup copy()
	{
		return new SourceGroup( this );
	}

	public void addSource( final int sourceId )
	{
		sourceIds.add( sourceId );
	}

	public void removeSource( final int sourceId )
	{
		sourceIds.remove( sourceId );
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
		this.isActive = isActive;
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
		this.isCurrent = isCurrent;
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o )
			return true;
		if ( !( o instanceof SourceGroup ) )
			return false;

		final SourceGroup that = ( SourceGroup ) o;

		if ( !name.equals( that.name ) )
			return false;
		if ( isActive != that.isActive )
			return false;
		if ( isCurrent != that.isCurrent )
			return false;
		return sourceIds.equals( that.sourceIds );
	}

	@Override
	public int hashCode()
	{
		int result = sourceIds.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + ( isActive ? 1 : 0 );
		result = 31 * result + ( isCurrent ? 1 : 0 );
		return result;
	}
}
