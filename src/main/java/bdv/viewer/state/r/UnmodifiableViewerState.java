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
		return getSourcesInGroup( group );
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
