/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.imglib2.realtransform.AffineTransform3D;

import org.scijava.listeners.Listeners;

/**
 * Maintains the BigDataViewer state and implements {@link ViewerState} to
 * expose query and modification methods. {@code ViewerStateChangeListener}s can
 * be registered and will be notified about various {@link ViewerStateChange
 * state changes}.
 * <p>
 * All methods of this class are {@code synchronized}, so that every individual
 * change to the viewer state is atomic. {@code IllegalArgumentException}s
 * thrown by the wrapped {@code BasicViewerState} are silently swallowed, under
 * the assumption that they result from concurrent changes (for example another
 * thread might have removed the source that you are trying to make current).
 * <p>
 * To perform sequences of operations atomically explicit synchronization is
 * required. In particular, this is true when using the collections returned by
 * {@link #getSources()}, {@link #getActiveSources()}, {@link #getGroups()},
 * {@link #getActiveGroups()}, and {@link #getSourcesInGroup(SourceGroup)}.
 * These collections are backed by the ViewerState, they reflect changes, and
 * they are <em>not thread-safe</em>. It is possible to run into
 * {@code ConcurrentModificationException} when iterating them, etc.
 * <p>
 * Example where explicit synchronization is required:
 * <pre>{@code
 * List<SourceGroup> groupsContainingCurrentSource;
 * synchronized (state) {
 *     SourceAndConverter<?> currentSource = state.getCurrentSource();
 *     groupsContainingCurrentSource =
 *         state.getGroups().stream()
 *             .filter(g -> state.getSourcesInGroup(g).contains(currentSource))
 *             .collect(Collectors.toList());
 * }}</pre>
 * <p>
 * Alternatively, for read-only access, it is possible to (atomically) take an
 * unmodifiable {@link #snapshot()} of the current state.
 *
 * @author Tobias Pietzsch
 */
public class SynchronizedViewerState implements ViewerState
{
	private static final boolean DEBUG = false;

	private final ViewerState state;

	public SynchronizedViewerState( final ViewerState state )
	{
		this.state = state;
	}

	@Override
	public Listeners< ViewerStateChangeListener > changeListeners()
	{
		return state.changeListeners();
	}

	/**
	 * Get a snapshot of this ViewerState.
	 *
	 * @return unmodifiable copy of the current state
	 */
	@Override
	public synchronized ViewerState snapshot()
	{
		return state.snapshot();
	}

	@Override
	public synchronized Interpolation getInterpolation()
	{
		return state.getInterpolation();
	}

	@Override
	public synchronized void setInterpolation( final Interpolation interpolation )
	{
		state.setInterpolation( interpolation );
	}

	@Override
	public synchronized DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	@Override
	public synchronized void setDisplayMode( final DisplayMode mode )
	{
		state.setDisplayMode( mode );
	}

	@Override
	public synchronized int getNumTimepoints()
	{
		return state.getNumTimepoints();
	}

	@Override
	public synchronized void setNumTimepoints( final int n )
	{
		state.setNumTimepoints( n );
	}

	@Override
	public synchronized int getCurrentTimepoint()
	{
		return state.getCurrentTimepoint();
	}

	@Override
	public synchronized void setCurrentTimepoint( final int t )
	{
		try
		{
			state.setCurrentTimepoint( t );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
		}
	}

	@Override
	public synchronized void getViewerTransform( final AffineTransform3D transform )
	{
		state.getViewerTransform( transform );
	}

	@Override
	public synchronized void setViewerTransform( final AffineTransform3D transform )
	{
		state.setViewerTransform( transform );
	}

	// --------------------
	// --    sources     --
	// --------------------

	/**
	 * Get the list of sources. The returned {@code List} reflects changes to
	 * the viewer state. It is unmodifiable and <em>not thread-safe</em>.
	 * <p>
	 * The returned list is also a set, there are no duplicate entries.
	 *
	 * @return the list of sources
	 */
	@Override
	public synchronized List< SourceAndConverter< ? > > getSources()
	{
		return state.getSources();
	}

	/**
	 * Get the current source. (May return {@code null} if there is no current
	 * source)
	 *
	 * @return the current source
	 */
	@Override
	public synchronized SourceAndConverter< ? > getCurrentSource()
	{
		return state.getCurrentSource();
	}

	/**
	 * Returns {@code true} if {@code source} is the current source. Equivalent
	 * to {@code (getCurrentSource() == source)}.
	 *
	 * @param source
	 *     the source. Passing {@code null} checks whether no source is current.
	 *
	 * @return {@code true} if {@code source} is the current source
	 */
	@Override
	public synchronized boolean isCurrentSource( final SourceAndConverter< ? > source )
	{
		return state.isCurrentSource( source );
	}

	/**
	 * Make {@code source} the current source. Returns {@code true}, if current
	 * source changes as a result of the call. Returns {@code false}, if
	 * {@code source} is already the current source.
	 * <p>
	 * Also returns {@code false}, if {@code source} is not valid (for example
	 * because it has been removed from the state by a different thread).
	 *
	 * @param source
	 *     the source to make current. Passing {@code null} clears the current
	 *     source.
	 *
	 * @return {@code true}, if current source changed as a result of the call
	 */
	@Override
	public synchronized boolean setCurrentSource( final SourceAndConverter< ? > source )
	{
		try
		{
			return state.setCurrentSource( source );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the set of active sources. The returned {@code Set} reflects changes
	 * to the viewer state. It is unmodifiable and <em>not thread-safe</em>.
	 *
	 * @return the set of active sources
	 */
	@Override
	public synchronized Set< SourceAndConverter< ? > > getActiveSources()
	{
		return state.getActiveSources();
	}

	/**
	 * Check whether the given {@code source} is active.
	 *
	 * Returns {@code true}, if {@code source} is active. Returns {@code false},
	 * if {@code source} is inactive or not valid (for example because it has
	 * been removed from the state by a different thread).
	 *
	 * @return {@code true}, if {@code source} is active
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 */
	@Override
	public synchronized boolean isSourceActive( final SourceAndConverter< ? > source )
	{
		try
		{
			return state.isSourceActive( source );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Set {@code source} active or inactive.
	 * <p>
	 * Returns {@code true}, if source activity changes as a result of the call.
	 * Returns {@code false}, if {@code source} is already in the desired
	 * {@code active} state.
	 * <p>
	 * Also returns {@code false}, if {@code source} is not valid (for example
	 * because it has been removed from the state by a different thread).
	 *
	 * @return {@code true}, if source activity changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 */
	@Override
	public synchronized boolean setSourceActive( final SourceAndConverter< ? > source, final boolean active )
	{
		try
		{
			return state.setSourceActive( source, active );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Set all sources in {@code collection} active or inactive.
	 * <p>
	 * Returns {@code true}, if source activity changes as a result of the call.
	 * Returns {@code false}, if all sources were already in the desired
	 * {@code active} state.
	 * <p>
	 * Does nothing and returns {@code false}, if any source in
	 * {@code collection} is not valid (for example because it was removed from
	 * the state by a different thread).
	 *
	 * @return {@code true}, if source activity changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 */
	@Override
	public synchronized boolean setSourcesActive( final Collection< ? extends SourceAndConverter< ? > > collection, final boolean active )
	{
		try
		{
			return state.setSourcesActive( collection, active );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Check whether the given {@code source} is visible.
	 * <p>
	 * Returns {@code false}, if {@code source} is not valid (for example
	 * because it has been removed from the state by a different thread).
	 * <p>
	 * Whether a source is visible depends on the {@link #getDisplayMode()
	 * display mode}:
	 * <ul>
	 * <li>In {@code DisplayMode.SINGLE} only the current source is visible.</li>
	 * <li>In {@code DisplayMode.GROUP} the sources in the current group are visible.</li>
	 * <li>In {@code DisplayMode.FUSED} all active sources are visible.</li>
	 * <li>In {@code DisplayMode.FUSEDROUP} the sources in all active groups are visible.</li>
	 * </ul>
	 *
	 * @return {@code true}, if {@code source} is visible
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 * @throws IllegalArgumentException
	 *     if {@code source} is not contained in the state (and not
	 *     {@code null}).
	 */
	@Override
	public synchronized boolean isSourceVisible( final SourceAndConverter< ? > source )
	{
		try
		{
			return state.isSourceVisible( source );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Check whether the given {@code source} is both visible and provides image
	 * data for the current timepoint.
	 * <p>
	 * Returns {@code false}, if {@code source} is not valid (for example
	 * because it has been removed from the state by a different thread).
	 *
	 * Whether a source is visible depends on the {@link #getDisplayMode()
	 * display mode}:
	 * <ul>
	 * <li>In {@code DisplayMode.SINGLE} only the current source is visible.</li>
	 * <li>In {@code DisplayMode.GROUP} the sources in the current group are visible.</li>
	 * <li>In {@code DisplayMode.FUSED} all active sources are visible.</li>
	 * <li>In {@code DisplayMode.FUSEDROUP} the sources in all active groups are visible.</li>
	 * </ul>
	 * Additionally, the source must be {@link bdv.viewer.Source#isPresent(int)
	 * present}, i.e., provide image data for the {@link #getCurrentTimepoint()
	 * current timepoint}.
	 *
	 * @return {@code true}, if {@code source} is both visible and present
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 */
	@Override
	public synchronized boolean isSourceVisibleAndPresent( final SourceAndConverter< ? > source )
	{
		try
		{
			return state.isSourceVisibleAndPresent( source );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the set of visible sources.
	 * <p>
	 * The returned {@code Set} is a copy. Changes to the set will not be
	 * reflected in the viewer state, and vice versa.
	 * <p>
	 * Whether a source is visible depends on the {@link #getDisplayMode()
	 * display mode}:
	 * <ul>
	 * <li>In {@code DisplayMode.SINGLE} only the current source is visible.</li>
	 * <li>In {@code DisplayMode.GROUP} the sources in the current group are visible.</li>
	 * <li>In {@code DisplayMode.FUSED} all active sources are visible.</li>
	 * <li>In {@code DisplayMode.FUSEDROUP} the sources in all active groups are visible.</li>
	 * </ul>
	 *
	 * @return the set of visible sources
	 */
	@Override
	public synchronized Set< SourceAndConverter< ? > > getVisibleSources()
	{
		return state.getVisibleSources();
	}

	/**
	 * Get the set of visible sources that also provide image data for the
	 * current timepoint.
	 * <p>
	 * The returned {@code Set} is a copy. Changes to the set will not be
	 * reflected in the viewer state, and vice versa.
	 * <p>
	 * Whether a source is visible depends on the {@link #getDisplayMode()
	 * display mode}:
	 * <ul>
	 * <li>In {@code DisplayMode.SINGLE} only the current source is visible.</li>
	 * <li>In {@code DisplayMode.GROUP} the sources in the current group are visible.</li>
	 * <li>In {@code DisplayMode.FUSED} all active sources are visible.</li>
	 * <li>In {@code DisplayMode.FUSEDROUP} the sources in all active groups are visible.</li>
	 * </ul>
	 * Additionally, the source must be {@link bdv.viewer.Source#isPresent(int)
	 * present}, i.e., provide image data for the {@link #getCurrentTimepoint()
	 * current timepoint}.
	 *
	 * @return the set of sources that are both visible and present
	 */
	@Override
	public synchronized Set< SourceAndConverter< ? > > getVisibleAndPresentSources()
	{
		return state.getVisibleAndPresentSources();
	}

	/**
	 * Check whether the state contains the {@code source}.
	 *
	 * @return {@code true}, if {@code source} is in the list of sources.
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 */
	@Override
	public synchronized boolean containsSource( final SourceAndConverter< ? > source )
	{
		return state.containsSource( source );
	}

	/**
	 * Add {@code source} to the state. Returns {@code true}, if the source is
	 * added. Returns {@code false}, if the source is already present.
	 * <p>
	 * If {@code source} is added and no other source was current, then
	 * {@code source} is made current
	 *
	 * @return {@code true}, if list of sources changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 */
	@Override
	public synchronized boolean addSource( final SourceAndConverter< ? > source )
	{
		return state.addSource( source );
	}

	/**
	 * Add all sources in {@code collection} to the state. Returns {@code true},
	 * if at least one source was added. Returns {@code false}, if all sources
	 * were already present.
	 * <p>
	 * If any sources are added and no other source was current, then the first
	 * added sources will be made current.
	 *
	 * @return {@code true}, if list of sources changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 */
	@Override
	public synchronized boolean addSources( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		return state.addSources( collection );
	}

	/**
	 * Remove {@code source} from the state.
	 * <p>
	 * Returns {@code true}, if {@code source} was removed from the state.
	 * Returns {@code false}, if {@code source} was not contained in state.
	 * <p>
	 * The {@code source} is also removed from any groups that contained it. If
	 * {@code source} was current, then the first source in the list of sources
	 * is made current (if it exists).
	 *
	 * @return {@code true}, if list of sources changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 */
	@Override
	public synchronized boolean removeSource( final SourceAndConverter< ? > source )
	{
		return state.removeSource( source );
	}

	/**
	 * Remove all sources in {@code collection} from the state. Returns
	 * {@code true}, if at least one source was removed. Returns {@code false},
	 * if none of the sources was present.
	 * <p>
	 * Removed sources are also removed from any groups containing them. If the
	 * current source was removed, then the first source in the remaining list
	 * of sources is made current (if it exists).
	 *
	 * @return {@code true}, if list of sources changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 */
	@Override
	public synchronized boolean removeSources( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		return state.removeSources( collection );
	}

	// TODO addSource with index
	// TODO addSources with index

	/**
	 * Remove all sources from the state.
	 */
	@Override
	public synchronized void clearSources()
	{
		state.clearSources();
	}

	/**
	 * Returns a {@link Comparator} that compares sources according to the order
	 * in which they occur in the sources list. (Sources that do not occur in
	 * the list are ordered before any source in the list).
	 */
	@Override
	public synchronized Comparator< SourceAndConverter< ? > > sourceOrder()
	{
		return state.sourceOrder();
	}

	// --------------------
	// --     groups     --
	// --------------------

	/**
	 * Get the list of groups. The returned {@code List} reflects changes to the
	 * viewer state. It is unmodifiable and <em>not thread-safe</em>.
	 * <p>
	 * The returned list is also a set, there are no duplicate entries.
	 *
	 * @return the list of groups
	 */
	@Override
	public synchronized List< SourceGroup > getGroups()
	{
		return state.getGroups();
	}

	/**
	 * Get the current group. (May return {@code null} if there is no current
	 * group)
	 *
	 * @return the current group
	 */
	@Override
	public synchronized SourceGroup getCurrentGroup()
	{
		return state.getCurrentGroup();
	}

	/**
	 * Returns {@code true} if {@code group} is the current group. Equivalent to
	 * {@code (getCurrentGroup() == group)}.
	 *
	 * @param group
	 *     the group. Passing {@code null} checks whether no group is current.
	 *
	 * @return {@code true} if {@code group} is the current group
	 */
	@Override
	public synchronized boolean isCurrentGroup( final SourceGroup group )
	{
		return state.isCurrentGroup( group );
	}

	/**
	 * Make {@code group} the current group. Returns {@code true}, if current
	 * group changes as a result of the call. Returns {@code false}, if
	 * {@code group} is already the current group.
	 * <p>
	 * Also returns {@code false}, if {@code group} is not valid (for example
	 * because it has been removed from the state by a different thread).
	 *
	 * @param group
	 *     the group to make current. Passing {@code null} clears the current
	 *     group.
	 *
	 * @return {@code true}, if current group changed as a result of the call.
	 */
	@Override
	public synchronized boolean setCurrentGroup( final SourceGroup group )
	{
		try
		{
			return state.setCurrentGroup( group );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the set of active groups. The returned {@code Set} reflects changes
	 * to the viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the set of active groups
	 */
	@Override
	public synchronized Set< SourceGroup > getActiveGroups()
	{
		return state.getActiveGroups();
	}

	/**
	 * Check whether the given {@code group} is active.
	 *
	 * Returns {@code true}, if {@code group} is active. Returns {@code false},
	 * if {@code group} is inactive or not valid (for example because it has
	 * been removed from the state by a different thread).
	 *
	 * @return {@code true}, if {@code group} is active
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 */
	@Override
	public synchronized boolean isGroupActive( final SourceGroup group )
	{
		try
		{
			return state.isGroupActive( group );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Set {@code group} active or inactive.
	 * <p>
	 * Returns {@code true}, if group activity changes as a result of the call.
	 * Returns {@code false}, if {@code group} is already in the desired
	 * {@code active} state.
	 * <p>
	 * Also returns {@code false}, if {@code group} is not valid (for example
	 * because it has been removed from the state by a different thread).
	 *
	 * @return {@code true}, if group activity changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 */
	@Override
	public synchronized boolean setGroupActive( final SourceGroup group, final boolean active )
	{
		try
		{
			return state.setGroupActive( group, active );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Set all groups in {@code collection} active or inactive.
	 * <p>
	 * Returns {@code true}, if group activity changes as a result of the call.
	 * Returns {@code false}, if all groups were already in the desired
	 * {@code active} state.
	 * <p>
	 * Does nothing and returns {@code false}, if any group in
	 * {@code collection} is not valid (for example because it was removed from
	 * the state by a different thread).
	 *
	 * @return {@code true}, if group activity changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 */
	@Override
	public synchronized boolean setGroupsActive( final Collection< ? extends SourceGroup > collection, final boolean active )
	{
		try
		{
			return state.setGroupsActive( collection, active );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the name of a {@code group}.
	 * <p>
	 * Returns {@code null}, if {@code group} is not valid (for example because
	 * it has been removed from the state by a different thread), an empty set
	 * is returned.
	 *
	 * @return name of the group, may be {@code null}
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 */
	@Override
	public synchronized String getGroupName( final SourceGroup group )
	{
		try
		{
			return state.getGroupName( group );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return null;
		}
	}

	/**
	 * Set the {@code name} of a {@code group}.
	 * <p>
	 * Does nothing, if {@code group} is not valid (for example because it has
	 * been removed from the state by a different thread), an empty set is
	 * returned.
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 */
	@Override
	public synchronized void setGroupName( final SourceGroup group, final String name )
	{
		try
		{
			state.setGroupName( group, name );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
		}
	}

	/**
	 * Check whether the state contains the {@code group}.
	 *
	 * @return {@code true}, if {@code group} is in the list of groups.
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 */
	@Override
	public synchronized boolean containsGroup( final SourceGroup group )
	{
		return state.containsGroup( group );
	}

	/**
	 * Add {@code group} to the state. Returns {@code true}, if the group is
	 * added. Returns {@code false}, if the group is already present.
	 * <p>
	 * If {@code group} is added and no other group was current, then
	 * {@code group} is made current
	 *
	 * @return {@code true}, if list of groups changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 */
	@Override
	public synchronized boolean addGroup( final SourceGroup group )
	{
		return state.addGroup( group );
	}

	/**
	 * Add all groups in {@code collection} to the state. Returns {@code true},
	 * if at least one group was added. Returns {@code false}, if all groups
	 * were already present.
	 * <p>
	 * If any groups are added and no other group was current, then the first
	 * added groups will be made current.
	 *
	 * @return {@code true}, if list of groups changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 */
	@Override
	public synchronized boolean addGroups( final Collection< ? extends SourceGroup > collection )
	{
		return state.addGroups( collection );
	}

	/**
	 * Remove {@code group} from the state.
	 * <p>
	 * Returns {@code true}, if {@code group} was removed from the state.
	 * Returns {@code false}, if {@code group} was not contained in state.
	 * <p>
	 * If {@code group} was current, then the first group in the list of groups
	 * is made current (if it exists).
	 *
	 * @return {@code true}, if list of groups changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 */
	@Override
	public synchronized boolean removeGroup( final SourceGroup group )
	{
		return state.removeGroup( group );
	}

	/**
	 * Remove all groups in {@code collection} from the state. Returns
	 * {@code true}, if at least one group was removed. Returns {@code false},
	 * if none of the groups was present.
	 * <p>
	 * If the current group was removed, then the first group in the remaining
	 * list of groups is made current (if it exists).
	 *
	 * @return {@code true}, if list of groups changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 */
	@Override
	public synchronized boolean removeGroups( final Collection< ? extends SourceGroup > collection )
	{
		return state.removeGroups( collection );
	}

	/**
	 * Add {@code source} to {@code group}.
	 * <p>
	 * Returns {@code true}, if {@code source} was added to {@code group}.
	 * Returns {@code false}, if {@code source} was already contained in
	 * {@code group}. or either of {@code source} and {@code group} is not valid
	 * (not in the BDV sources/groups list).
	 * <p>
	 * Does nothing and returns {@code false}, if {@code source} or
	 * {@code group} are not valid (for example because they were removed from
	 * the state by a different thread).
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a
	 * result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code source == null} or {@code group == null}
	 */
	@Override
	public synchronized boolean addSourceToGroup( final SourceAndConverter< ? > source, final SourceGroup group )
	{
		try
		{
			return state.addSourceToGroup( source, group );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Add all sources in {@code collection} to {@code group}.
	 * <p>
	 * Returns {@code true}, if at least one source was added to {@code group}.
	 * Returns {@code false}, if all sources were already contained in
	 * {@code group}.
	 * <p>
	 * Does nothing and returns {@code false}, if {@code group} or any source in
	 * {@code collection} are not valid (for example because they were removed
	 * from the state by a different thread).
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a
	 * result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code group == null} or {@code collection == null} or any element
	 *     of {@code collection} is {@code null}.
	 */
	@Override
	public synchronized boolean addSourcesToGroup( final Collection< ? extends SourceAndConverter< ? > > collection, final SourceGroup group )
	{
		try
		{
			return state.addSourcesToGroup( collection, group );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Remove {@code source} from {@code group}.
	 * <p>
	 * Returns {@code true}, if {@code source} was removed from {@code group}.
	 * Returns {@code false}, if {@code source} was not contained in
	 * {@code group},
	 * <p>
	 * Does nothing and returns {@code false}, if {@code source} or
	 * {@code group} are not valid (for example because they were removed from
	 * the state by a different thread).
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a
	 * result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code source == null} or {@code group == null}
	 */
	@Override
	public synchronized boolean removeSourceFromGroup( final SourceAndConverter< ? > source, final SourceGroup group )
	{
		try
		{
			return state.removeSourceFromGroup( source, group );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Remove all sources in {@code collection} from {@code group}.
	 * <p>
	 * Returns {@code true}, if at least one source was removed from
	 * {@code group}. Returns {@code false}, if none of the sources were
	 * contained in {@code group}.
	 * <p>
	 * Does nothing and returns {@code false}, if {@code group} or any source in
	 * {@code collection} are not valid (for example because they were removed
	 * from the state by a different thread).
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a
	 * result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code group == null} or {@code collection == null} or any element
	 *     of {@code collection} is {@code null}.
	 */
	@Override
	public synchronized boolean removeSourcesFromGroup( final Collection< ? extends SourceAndConverter< ? > > collection, final SourceGroup group )
	{
		try
		{
			return state.removeSourcesFromGroup( collection, group );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the set sources in {@code group}. The returned {@code Set} reflects
	 * changes to the viewer state. It is unmodifiable and not thread-safe.
	 * <p>
	 * If {@code group} is not valid (for example because it has been removed
	 * from the state by a different thread), an empty set is returned.
	 *
	 * @return the set of sources in {@code group}
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 */
	@Override
	public synchronized Set< SourceAndConverter< ? > > getSourcesInGroup( final SourceGroup group )
	{
		try
		{
			return state.getSourcesInGroup( group );
		}
		catch ( final IllegalArgumentException e )
		{
			if ( DEBUG )
				e.printStackTrace();
			return Collections.emptySet();
		}
	}

	/**
	 * Remove all groups from the state.
	 */
	@Override
	public synchronized void clearGroups()
	{
		state.clearGroups();
	}

	/**
	 * Returns a {@link Comparator} that compares groups according to the order
	 * in which they occur in the groups list. (Groups that do not occur in the
	 * list are ordered before any group in the list).
	 */
	@Override
	public synchronized Comparator< SourceGroup > groupOrder()
	{
		return state.groupOrder();
	}

	/**
	 * Returns the wrapped {@code ViewerState}.
	 * <p>
	 * <em>When using this, explicit synchronization (on this
	 * {@code SynchronizedViewerState}) is required. PLEASE BE CAREFUL!</em>
	 */
	// TODO: REMOVE?
	public ViewerState getWrappedState()
	{
		return state;
	}
}
