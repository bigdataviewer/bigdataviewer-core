package bdv.viewer.state.r;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Wraps another {@link ViewerState} and throws {@code UnsupportedOperationException} for all modification operations.
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

	public Interpolation getInterpolation()
	{
		return state.getInterpolation();
	}

	public void setInterpolation( final Interpolation i )
	{
		throw new UnsupportedOperationException();
	}

	public DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	public void setDisplayMode( final DisplayMode mode )
	{
		throw new UnsupportedOperationException();
	}

	public int getNumTimepoints()
	{
		return state.getNumTimepoints();
	}

	public void setNumTimepoints( final int n )
	{
		throw new UnsupportedOperationException();
	}

	public int getCurrentTimepoint()
	{
		return state.getCurrentTimepoint();
	}

	public void setCurrentTimepoint( final int t )
	{
		throw new UnsupportedOperationException();
	}

	public void getViewerTransform( final AffineTransform3D t )
	{
		state.getViewerTransform( t );
	}

	public void setViewerTransform( final AffineTransform3D t )
	{
		throw new UnsupportedOperationException();
	}

	public List< SourceAndConverter< ? > > getSources()
	{
		return state.getSources();
	}

	public SourceAndConverter< ? > getCurrentSource()
	{
		return state.getCurrentSource();
	}

	public boolean isCurrentSource( final SourceAndConverter< ? > source )
	{
		return state.isCurrentSource( source );
	}

	public boolean setCurrentSource( final SourceAndConverter< ? > source )
	{
		throw new UnsupportedOperationException();
	}

	public Set< SourceAndConverter< ? > > getActiveSources()
	{
		return state.getActiveSources();
	}

	public boolean isSourceActive( final SourceAndConverter< ? > source )
	{
		return state.isSourceActive( source );
	}

	public boolean setSourceActive( final SourceAndConverter< ? > source, final boolean active )
	{
		throw new UnsupportedOperationException();
	}

	public boolean setSourcesActive( final Collection< ? extends SourceAndConverter< ? > > collection, final boolean active )
	{
		throw new UnsupportedOperationException();
	}

	public boolean isSourceVisible( final SourceAndConverter< ? > source )
	{
		return state.isSourceVisible( source );
	}

	public boolean isSourceVisibleAndPresent( final SourceAndConverter< ? > source )
	{
		return state.isSourceVisibleAndPresent( source );
	}

	public Set< SourceAndConverter< ? > > getVisibleSources()
	{
		return state.getVisibleSources();
	}

	public Set< SourceAndConverter< ? > > getVisibleAndPresentSources()
	{
		return state.getVisibleAndPresentSources();
	}

	public boolean containsSource( final SourceAndConverter< ? > source )
	{
		return state.containsSource( source );
	}

	public boolean addSource( final SourceAndConverter< ? > source )
	{
		throw new UnsupportedOperationException();
	}

	public boolean addSources( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		throw new UnsupportedOperationException();
	}

	public boolean removeSource( final SourceAndConverter< ? > source )
	{
		throw new UnsupportedOperationException();
	}

	public boolean removeSources( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		throw new UnsupportedOperationException();
	}

	public void clearSources()
	{
		throw new UnsupportedOperationException();
	}

	public Comparator< SourceAndConverter< ? > > sourceOrder()
	{
		return state.sourceOrder();
	}

	public List< SourceGroup > getGroups()
	{
		return state.getGroups();
	}

	public SourceGroup getCurrentGroup()
	{
		return state.getCurrentGroup();
	}

	public boolean isCurrentGroup( final SourceGroup group )
	{
		return state.isCurrentGroup( group );
	}

	public boolean setCurrentGroup( final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	public Set< SourceGroup > getActiveGroups()
	{
		return state.getActiveGroups();
	}

	public boolean isGroupActive( final SourceGroup group )
	{
		return state.isGroupActive( group );
	}

	public boolean setGroupActive( final SourceGroup group, final boolean active )
	{
		throw new UnsupportedOperationException();
	}

	public boolean setGroupsActive( final Collection< ? extends SourceGroup > collection, final boolean active )
	{
		throw new UnsupportedOperationException();
	}

	public String getGroupName( final SourceGroup group )
	{
		return state.getGroupName( group );
	}

	public void setGroupName( final SourceGroup group, final String name )
	{
		throw new UnsupportedOperationException();
	}

	public boolean containsGroup( final SourceGroup group )
	{
		return state.containsGroup( group );
	}

	public boolean addGroup( final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	public boolean addGroups( final Collection< ? extends SourceGroup > collection )
	{
		throw new UnsupportedOperationException();
	}

	public boolean removeGroup( final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	public boolean removeGroups( final Collection< ? extends SourceGroup > collection )
	{
		throw new UnsupportedOperationException();
	}

	public boolean addSourceToGroup( final SourceAndConverter< ? > source, final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	public boolean addSourcesToGroup( final Collection< ? extends SourceAndConverter< ? > > collection, final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	public boolean removeSourceFromGroup( final SourceAndConverter< ? > source, final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	public boolean removeSourcesFromGroup( final Collection< ? extends SourceAndConverter< ? > > collection, final SourceGroup group )
	{
		throw new UnsupportedOperationException();
	}

	public Set< SourceAndConverter< ? > > getSourcesInGroup( final SourceGroup group )
	{
		return getSourcesInGroup( group );
	}

	public void clearGroups()
	{
		throw new UnsupportedOperationException();
	}

	public Comparator< SourceGroup > groupOrder()
	{
		return state.groupOrder();
	}
}
