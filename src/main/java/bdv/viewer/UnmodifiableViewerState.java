/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
package bdv.viewer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.listeners.Listeners;

/**
 * Wraps another {@link ViewerState} and throws
 * {@code UnsupportedOperationException} for all modification operations.
 *
 * @author Tobias Pietzsch
 */
class UnmodifiableViewerState implements ViewerState
{
	private final ViewerState state;

	public UnmodifiableViewerState( final ViewerState state )
	{
		this.state = state;
	}

	@Override
	public ViewerState snapshot()
	{
		return new UnmodifiableViewerState( state.snapshot() );
	}

	@Override
	public Listeners< ViewerStateChangeListener > changeListeners()
	{
		return state.changeListeners();
	}

	@Override
	public Interpolation getInterpolation()
	{
		return state.getInterpolation();
	}

	@Override
	public void setInterpolation( final Interpolation i )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	@Override
	public void setDisplayMode( final DisplayMode mode )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumTimepoints()
	{
		return state.getNumTimepoints();
	}

	@Override
	public void setNumTimepoints( final int n )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getCurrentTimepoint()
	{
		return state.getCurrentTimepoint();
	}

	@Override
	public void setCurrentTimepoint( final int t )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void getViewerTransform( final AffineTransform3D t )
	{
		state.getViewerTransform( t );
	}

	@Override
	public void setViewerTransform( final AffineTransform3D t )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List< SourceAndConverter< ? > > getSources()
	{
		return state.getSources();
	}

	@Override
	public SourceAndConverter< ? > getCurrentSource()
	{
		return state.getCurrentSource();
	}

	@Override
	public boolean isCurrentSource( final SourceAndConverter< ? > source )
	{
		return state.isCurrentSource( source );
	}

	@Override
	public boolean setCurrentSource( final SourceAndConverter< ? > source )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set< SourceAndConverter< ? > > getActiveSources()
	{
		return state.getActiveSources();
	}

	@Override
	public boolean isSourceActive( final SourceAndConverter< ? > source )
	{
		return state.isSourceActive( source );
	}

	@Override
	public boolean setSourceActive( final SourceAndConverter< ? > source, final boolean active )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setSourcesActive( final Collection< ? extends SourceAndConverter< ? > > collection, final boolean active )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSourceVisible( final SourceAndConverter< ? > source )
	{
		return state.isSourceVisible( source );
	}

	@Override
	public boolean isSourceVisibleAndPresent( final SourceAndConverter< ? > source )
	{
		return state.isSourceVisibleAndPresent( source );
	}

	@Override
	public Set< SourceAndConverter< ? > > getVisibleSources()
	{
		return state.getVisibleSources();
	}

	@Override
	public Set< SourceAndConverter< ? > > getVisibleAndPresentSources()
	{
		return state.getVisibleAndPresentSources();
	}

	@Override
	public boolean containsSource( final SourceAndConverter< ? > source )
	{
		return state.containsSource( source );
	}

	@Override
	public boolean addSource( final SourceAndConverter< ? > source )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addSources( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeSource( final SourceAndConverter< ? > source )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeSources( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearSources()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Comparator< SourceAndConverter< ? > > sourceOrder()
	{
		return state.sourceOrder();
	}

	@Override
	public List< SourceGroup > getGroups()
	{
		return state.getGroups();
	}

	@Override
	public SourceGroup getCurrentGroup()
	{
		return state.getCurrentGroup();
	}

	@Override
	public boolean isCurrentGroup( final SourceGroup group )
	{
		return state.isCurrentGroup( group );
	}

	@Override
	public boolean setCurrentGroup( final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set< SourceGroup > getActiveGroups()
	{
		return state.getActiveGroups();
	}

	@Override
	public boolean isGroupActive( final SourceGroup group )
	{
		return state.isGroupActive( group );
	}

	@Override
	public boolean setGroupActive( final SourceGroup group, final boolean active )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setGroupsActive( final Collection< ? extends SourceGroup > collection, final boolean active )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getGroupName( final SourceGroup group )
	{
		return state.getGroupName( group );
	}

	@Override
	public void setGroupName( final SourceGroup group, final String name )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsGroup( final SourceGroup group )
	{
		return state.containsGroup( group );
	}

	@Override
	public boolean addGroup( final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addGroups( final Collection< ? extends SourceGroup > collection )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeGroup( final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeGroups( final Collection< ? extends SourceGroup > collection )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addSourceToGroup( final SourceAndConverter< ? > source, final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addSourcesToGroup( final Collection< ? extends SourceAndConverter< ? > > collection, final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeSourceFromGroup( final SourceAndConverter< ? > source, final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeSourcesFromGroup( final Collection< ? extends SourceAndConverter< ? > > collection, final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set< SourceAndConverter< ? > > getSourcesInGroup( final SourceGroup group )
	{
		return state.getSourcesInGroup( group );
	}

	@Override
	public void clearGroups()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Comparator< SourceGroup > groupOrder()
	{
		return state.groupOrder();
	}
}
