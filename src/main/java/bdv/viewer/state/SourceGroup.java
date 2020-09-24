/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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

import bdv.viewer.ViewerState;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import bdv.viewer.DisplayMode;

/**
 * TODO
 *
 * @author Tobias Pietzsch
 */
@Deprecated
public class SourceGroup
{
	protected final SortedSet< Integer > sourceIds;

	private final ViewerState state;

	private final bdv.viewer.SourceGroup handle;

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

	public SourceGroup( final ViewerState state, final bdv.viewer.SourceGroup handle )
	{
		this.state = state;
		this.handle = handle;
		sourceIds = Collections.synchronizedSortedSet( new TreeSet<>() );
	}

	public SourceGroup( final String name )
	{
		this.state = null;
		this.handle = null;
		sourceIds = Collections.synchronizedSortedSet( new TreeSet<>() );
		this.name = name;
		isActive = true;
		isCurrent = false;
	}

	protected SourceGroup( final SourceGroup g )
	{
		this.state = null;
		this.handle = null;
		synchronized ( g.getSourceIds() )
		{
			sourceIds = Collections.synchronizedSortedSet( new TreeSet<>( g.getSourceIds() ) );
		}
		name = g.getName();
		isActive = g.isActive();
		isCurrent = g.isCurrent();
	}

	public SourceGroup copy()
	{
		return new SourceGroup( this );
	}

	public void addSource( final int sourceId )
	{
		if ( handle == null )
			sourceIds.add( sourceId );
		else
		{
			state.addSourceToGroup( state.getSources().get( sourceId ), handle );
			sourceIds.clear();
			state.getSourcesInGroup( handle ).forEach( source -> sourceIds.add( state.getSources().indexOf( source ) ) );
		}
	}

	public void removeSource( final int sourceId )
	{
		if ( handle == null )
			sourceIds.remove( sourceId );
		else
		{
			state.removeSourceFromGroup( state.getSources().get( sourceId ), handle );
			sourceIds.clear();
			state.getSourcesInGroup( handle ).forEach( source -> sourceIds.add( state.getSources().indexOf( source ) ) );
		}
	}

	public SortedSet< Integer > getSourceIds()
	{
		if ( handle != null )
		{
			sourceIds.clear();
			state.getSourcesInGroup( handle ).forEach( source -> sourceIds.add( state.getSources().indexOf( source ) ) );
		}
		return sourceIds;
	}

	public String getName()
	{
		if ( handle != null )
			name = state.getGroupName( handle );
		return name;
	}

	public void setName( final String name )
	{
		if ( handle != null )
			state.setGroupName( handle, name );
		this.name = name;
	}

	/**
	 * Is the group active (visible in fused mode)?
	 *
	 * @return whether the source is active.
	 */
	public boolean isActive()
	{
		if ( handle != null )
			isActive = state.isGroupActive( handle );
		return isActive;
	}

	/**
	 * Set the group active (visible in fused mode) or inactive.
	 */
	public void setActive( final boolean isActive )
	{
		if ( handle != null )
			state.setGroupActive( handle, isActive );
		this.isActive = isActive;
	}

	/**
	 * Is this group the current group?
	 *
	 * @return whether the group is current.
	 */
	public boolean isCurrent()
	{
		if ( handle != null )
			isCurrent = state.isCurrentGroup( handle );
		return isCurrent;
	}

	/**
	 * Set this group current (or not).
	 */
	public void setCurrent( final boolean isCurrent )
	{
		if ( handle != null && isCurrent )
			state.setCurrentGroup( handle );
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
