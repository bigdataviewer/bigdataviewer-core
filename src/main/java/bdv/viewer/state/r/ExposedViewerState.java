package bdv.viewer.state.r;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import net.imglib2.realtransform.AffineTransform3D;

/*
 * Synchronized access to viewer state
 */
// TODO rename
public class ExposedViewerState
{
	private static final boolean DEBUG = true;

	private final IViewerState state;

	public ExposedViewerState( final IViewerState state )
	{
		this.state = state;
	}

	/**
	 * Returns a monitor object to synchronize on for locking the viewer state.
	 * This can be used for atomically performing a sequences of operations.
	 */
	public Object getMonitor()
	{
		return state;
	}

	public Interpolation getInterpolation()
	{
		synchronized ( state )
		{
			return state.getInterpolation();
		}
	}

	public void setInterpolation( final Interpolation interpolation )
	{
		synchronized ( state )
		{
			state.setInterpolation( interpolation );
		}
	}

	public DisplayMode getDisplayMode()
	{
		synchronized ( state )
		{
			return state.getDisplayMode();
		}
	}

	public void setDisplayMode( final DisplayMode mode )
	{
		synchronized ( state )
		{
			state.setDisplayMode( mode );
		}
	}

	public int getNumTimepoints()
	{
		synchronized ( state )
		{
			return state.getNumTimepoints();
		}
	}

	public void setNumTimepoints( final int n )
	{
		synchronized ( state )
		{
			state.setNumTimepoints( n );
		}
	}

	public int getCurrentTimepoint()
	{
		synchronized ( state )
		{
			return state.getCurrentTimepoint();
		}
	}

	public void setCurrentTimepoint( final int t )
	{
		synchronized ( state )
		{
			state.setCurrentTimepoint( t );
		}
	}

	public void getViewerTransform( final AffineTransform3D transform )
	{
		synchronized ( state )
		{
			state.getViewerTransform( transform );
		}
	}

	public AffineTransform3D getViewerTransform()
	{
		final AffineTransform3D transform = new AffineTransform3D();
		getViewerTransform( transform );
		return transform;
	}

	public void setViewerTransform( final AffineTransform3D t )
	{
		synchronized ( state )
		{
			state.setViewerTransform( t );
		}
	}

	// ---------------------
	// --     sources     --
	// ---------------------

	/**
	 * Get the current source.
	 * (May return {@code null} if there is no current source)
	 *
	 * @return the current source
	 */
	public SourceAndConverter< ? > getCurrentSource()
	{
		synchronized ( state )
		{
			return state.getSources().getCurrent();
		}
	}

	/**
	 * Returns {@code true} if {@code source} is the current source.
	 * Equivalent to {@code (getCurrentSource() == source)}.
	 *
	 * @return {@code true} if {@code source} is the current source
	 */
	public boolean isCurrent( SourceAndConverter< ? > source )
	{
		synchronized ( state )
		{
			return state.getSources().isCurrent( source );
		}
	}

	/**
	 * Make {@code source} the current source.
	 * Returns {@code true}, if current source changes as a result of the call.
	 * Returns {@code false}, if {@code source} is already the current source or is not valid (not in the BDV sources list).
	 *
	 * @param source
	 * 		the source to make current
	 * @return {@code true}, if current source changed as a result of the call.
	 */
	public boolean setCurrentSource( final SourceAndConverter< ? > source )
	{
		try
		{
			synchronized ( state )
			{
				return state.getSources().makeCurrent( source );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the set of active sources.
	 * <p>
	 * The returned {@code Set} is a copy. Changes to the set will not be reflected in the viewer state, and vice versa.
	 *
	 * @return the set of active sources
	 */
	public Set< SourceAndConverter< ? > > getActiveSources()
	{
		synchronized ( state )
		{
			return new HashSet<>( state.getSources().getActive() );
		}
	}

	/**
	 * Check whether the given {@code source} is active.
	 * Returns {@code true}, if the source is active.
	 * Returns {@code false}, if the source is inactive or is not valid (not in the BDV sources list).
	 *
	 * @return {@code true}, if {@code source} is active
	 */
	public boolean isActive( final SourceAndConverter< ? > source )
	{
		try
		{
			synchronized ( state )
			{
				return state.getSources().isActive( source );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Set {@code source} active or inactive.
	 * (Active sources are visible in {@code FUSED} display mode.)
	 * <p>
	 * Returns {@code true}, if source activity changes as a result of the call.
	 * Returns {@code false}, if {@code source} is already in the desired {@code active} state or is not valid (not in the BDV sources list).
	 *
	 * @return {@code true}, if source activity changed as a result of the call
	 */
	public boolean setActive( final SourceAndConverter< ? > source, final boolean active )
	{
		try
		{
			synchronized ( state )
			{
				return state.getSources().setActive( source, active );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the set of visible sources.
	 * <p>
	 * The returned {@code Set} is a copy. Changes to the set will not be reflected in the viewer state, and vice versa.
	 * <p>
	 * Whether a source is visible depends on the {@link #getDisplayMode() display mode}:
	 * <ul>
	 * <li>In {@code DisplayMode.SINGLE} only the current source is visible.</li>
	 * <li>In {@code DisplayMode.GROUP} the sources in the current group are visible.</li>
	 * <li>In {@code DisplayMode.FUSED} all active sources are visible.</li>
	 * <li>In {@code DisplayMode.FUSEDROUP} the sources in all active groups are visible.</li>
	 * </ul>
	 * Additionally, a source can only be visible if it provides image data for the {@link #getCurrentTimepoint() current time-point}.
	 *
	 * @return the set of visible sources
	 */
	public Set< SourceAndConverter< ? > > getVisibleSources()
	{
		synchronized ( state )
		{
			// NB At the moment, this already creates a new set.
			//    If the implementation changes, we might need
			//    to duplicate the set here.
			return state.getSources().getVisible();
		}
	}

	/**
	 * Check whether the given {@code source} is visible.
	 * Returns {@code true}, if the source is visible.
	 * Returns {@code false}, if the source is invisible or is not valid (not in the BDV sources list).
	 * <p>
	 * Whether a source is visible depends on the {@link #getDisplayMode() display mode}:
	 * <ul>
	 * <li>In {@code DisplayMode.SINGLE} only the current source is visible.</li>
	 * <li>In {@code DisplayMode.GROUP} the sources in the current group are visible.</li>
	 * <li>In {@code DisplayMode.FUSED} all active sources are visible.</li>
	 * <li>In {@code DisplayMode.FUSEDROUP} the sources in all active groups are visible.</li>
	 * </ul>
	 * Additionally, a source can only be visible if it provides image data for the {@link #getCurrentTimepoint() current time-point}.
	 *
	 * @return {@code true}, if {@code source} is active
	 */
	public boolean isVisible( final SourceAndConverter< ? > source )
	{
		try
		{
			synchronized ( state )
			{
				return state.getSources().isVisible( source );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * TODO
	 */
	public boolean addSource( final SourceAndConverter< ? > source )
	{
		synchronized ( state )
		{
			return state.getSources().add( source );
		}
	}

	/**
	 * TODO
	 */
	public boolean addSources( final Collection< ? extends SourceAndConverter< ? > > sources )
	{
		synchronized ( state )
		{
			return state.getSources().addAll( sources );
		}
	}

	/**
	 * TODO
	 */
	public boolean removeSource( final SourceAndConverter< ? > source )
	{
		synchronized ( state )
		{
			return state.getSources().remove( source );
		}
	}

	/**
	 * TODO
	 */
	public boolean removeSources( final Collection< ? extends SourceAndConverter< ? > > sources )
	{
		synchronized ( state )
		{
			return state.getSources().removeAll( sources );
		}
	}

	// TODO addSource with index
	// TODO addSources with index

	/**
	 * TODO
	 */
	public Comparator< SourceAndConverter< ? > > sourceOrder()
	{
		// TODO
		return null;
	}

	// --------------------
	// --     groups     --
	// --------------------

	/**
	 * Get the current group.
	 * (May return {@code null} if there is no current group)
	 *
	 * @return the current group
	 */
	public SourceGroup getCurrentGroup()
	{
		synchronized ( state )
		{
			return state.getGroups().getCurrent();
		}
	}

	/**
	 * Returns {@code true} if {@code group} is the current group.
	 * Equivalent to {@code (getCurrentGroup() == group)}.
	 *
	 * @return {@code true} if {@code group} is the current group
	 */
	public boolean isCurrent( SourceGroup group )
	{
		synchronized ( state )
		{
			return state.getGroups().isCurrent( group );
		}
	}

	/**
	 * Make {@code group} the current group.
	 * Returns {@code true}, if current group changes as a result of the call.
	 * Returns {@code false}, if {@code group} is already the current group or is not valid (not in the BDV groups list).
	 *
	 * @param group
	 * 		the group to make current
	 * @return {@code true}, if current group changed as a result of the call.
	 */
	public boolean setCurrentGroup( final SourceGroup group )
	{
		try
		{
			synchronized ( state )
			{
				return state.getGroups().makeCurrent( group );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the set of active groups.
	 * <p>
	 * The returned {@code Set} is a copy. Changes to the set will not be reflected in the viewer state, and vice versa.
	 *
	 * @return the set of active groups
	 */
	public Set< SourceGroup > getActiveGroups()
	{
		synchronized ( state )
		{
			return new HashSet<>( state.getGroups().getActive() );
		}
	}

	/**
	 * Check whether the given {@code group} is active.
	 * Returns {@code true}, if the group is active.
	 * Returns {@code false}, if the group is inactive or is not valid (not in the BDV groups list).
	 *
	 * @return {@code true}, if {@code group} is active
	 */
	public boolean isActive( final SourceGroup group )
	{
		try
		{
			synchronized ( state )
			{
				return state.getGroups().isActive( group );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Set {@code group} active or inactive.
	 * (Active groups are visible in {@code FUSEDGROUP} display mode.)
	 * <p>
	 * Returns {@code true}, if group activity changes as a result of the call.
	 * Returns {@code false}, if {@code group} is already in the desired {@code active} state or is not valid (not in the BDV groups list).
	 *
	 * @return {@code true}, if group activity changed as a result of the call
	 */
	public boolean setActive( final SourceGroup group, final boolean active )
	{
		try
		{
			synchronized ( state )
			{
				return state.getGroups().setActive( group, active );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * TODO
	 */
	public boolean addGroup( final SourceGroup group )
	{
		synchronized ( state )
		{
			return state.getGroups().add( group );
		}
	}

	/**
	 * TODO
	 */
	public boolean addGroups( final Collection< ? extends SourceGroup > groups )
	{
		synchronized ( state )
		{
			return state.getGroups().addAll( groups );
		}
	}

	/**
	 * TODO
	 */
	public boolean removeGroup( final SourceGroup group )
	{
		synchronized ( state )
		{
			return state.getGroups().remove( group );
		}
	}

	/**
	 * TODO
	 */
	public boolean removeGroups( final Collection< ? extends SourceGroup > groups )
	{
		synchronized ( state )
		{
			return state.getGroups().removeAll( groups );
		}
	}


	// TODO add/remove source to/from group
	/**
	 * Add {@code source} to {@code group}.
	 * <p>
	 * Returns {@code true}, if  {@code source} was added to {@code group}.
	 * Returns {@code false}, if {@code source} was already contained in {@code group},
	 * or either of {@code source} and {@code group} is not valid (not in the BDV sources/groups list).
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a result of the call
	 */
	public boolean addSourceToGroup( SourceAndConverter< ? > source, SourceGroup group )
	{
		try
		{
			synchronized ( state )
			{
				return state.getGroups().getSourcesIn( group ).add( source );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Remove {@code source} from {@code group}.
	 * <p>
	 * Returns {@code true}, if  {@code source} was removed from {@code group}.
	 * Returns {@code false}, if {@code source} was not contained in {@code group},
	 * or either of {@code source} and {@code group} is not valid (not in the BDV sources/groups list).
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a result of the call
	 */
	public boolean removeSourceFromGroup( SourceAndConverter< ? > source, SourceGroup group )
	{
		try
		{
			synchronized ( state )
			{
				return state.getGroups().getSourcesIn( group ).remove( source );
			}
		}
		catch ( IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	// TODO add/remove source collection to/from group
	// TODO get set of sources in group
	// TODO get name of group
	// TODO set name of group

	/**
	 * TODO
	 */
	public Comparator< SourceGroup > order()
	{
		// TODO
		return null;
	}



}
