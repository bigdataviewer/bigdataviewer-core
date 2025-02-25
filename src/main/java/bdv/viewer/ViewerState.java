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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.listeners.Listeners;

/**
 * Reading and writing the BigDataViewer state:
 * <ul>
 * <li>interpolation and display mode</li>
 * <li>contained sources and source groups</li>
 * <li>activeness / currentness of sources and groups</li>
 * <li>current timepoint and transformation</li>
 * </ul>
 *
 * @author Tobias Pietzsch
 */
public interface ViewerState
{
	/**
	 * Get a snapshot of this ViewerState.
	 *
	 * @return unmodifiable copy of the current state
	 */
	ViewerState snapshot();

	/**
	 * {@code ViewerStateChangeListener}s can be added/removed here,
	 * and will be notified about changes to this ViewerState.
	 */
	Listeners< ViewerStateChangeListener > changeListeners();

	/**
	 * Get the interpolation method.
	 *
	 * @return interpolation method
	 */
	Interpolation getInterpolation();

	/**
	 * Set the interpolation method (optional operation).
	 *
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	void setInterpolation( Interpolation interpolation );

	/**
	 * Get the current {@code DisplayMode}.
	 * <ul>
	 * <li>In {@code DisplayMode.SINGLE} only the current source is visible.</li>
	 * <li>In {@code DisplayMode.GROUP} the sources in the current group are visible.</li>
	 * <li>In {@code DisplayMode.FUSED} all active sources are visible.</li>
	 * <li>In {@code DisplayMode.FUSEDROUP} the sources in all active groups are visible.</li>
	 * </ul>
	 *
	 * @return the current display mode
	 */
	DisplayMode getDisplayMode();

	/**
	 * Set the {@link DisplayMode} (optional operation).
	 *
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	void setDisplayMode( DisplayMode mode );

	/**
	 * Get the number of timepoints.
	 *
	 * @return the number of timepoints
	 */
	int getNumTimepoints();

	/**
	 * Set the number of timepoints (optional operation).
	 * <p>
	 * If {@link #getCurrentTimepoint()} current timepoint} is
	 * {@code >= n}, it will be adjusted to {@code n-1}.
	 *
	 * @throws IllegalArgumentException
	 *     if {@code n < 1}.
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	void setNumTimepoints( int n );

	/**
	 * Get the current timepoint.
	 *
	 * @return current timepoint (index)
	 */
	int getCurrentTimepoint();

	/**
	 * Set the current timepoint (optional operation).
	 *
	 * @throws IllegalArgumentException
	 *     if {@code t >= getNumTimepoints()} or {@code t < 0}.
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	void setCurrentTimepoint( int t );

	/**
	 * Get the viewer transform.
	 *
	 * @param transform
	 *     is set to the viewer transform
	 */
	void getViewerTransform( AffineTransform3D transform );

	/**
	 * Get the viewer transform.
	 *
	 * @return a copy of the current viewer transform
	 */
	default AffineTransform3D getViewerTransform()
	{
		final AffineTransform3D transform = new AffineTransform3D();
		getViewerTransform( transform );
		return transform;
	}

	/**
	 * Set the viewer transform (optional operation).
	 *
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	void setViewerTransform( AffineTransform3D transform );

	// --------------------
	// --    sources     --
	// --------------------

	/**
	 * Get the list of sources. The returned {@code List} reflects changes to
	 * the viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the list of sources
	 */
	List< SourceAndConverter< ? > > getSources();

	/**
	 * Get the current source. (May return {@code null} if there is no current
	 * source)
	 *
	 * @return the current source
	 */
	SourceAndConverter< ? > getCurrentSource();

	/**
	 * Returns {@code true} if {@code source} is the current source. Equivalent
	 * to {@code (getCurrentSource() == source)}.
	 *
	 * @param source
	 *     the source. Passing {@code null} checks whether no source is current.
	 * @return {@code true} if {@code source} is the current source
	 */
	boolean isCurrentSource( SourceAndConverter< ? > source );

	/**
	 * Make {@code source} the current source (optional operation). Returns
	 * {@code true}, if current source changes as a result of the call. Returns
	 * {@code false}, if {@code source} is already the current source.
	 *
	 * @param source
	 *     the source to make current. Passing {@code null} clears the current
	 *     source.
	 *
	 * @return {@code true}, if current source changed as a result of the call
	 *
	 * @throws IllegalArgumentException
	 *     if {@code source} is not contained in the state (and not
	 *     {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean setCurrentSource( SourceAndConverter< ? > source );

	/**
	 * Get the set of active sources. The returned {@code Set} reflects changes
	 * to the viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the set of active sources
	 */
	Set< SourceAndConverter< ? > > getActiveSources();

	/**
	 * Check whether the given {@code source} is active.
	 *
	 * @return {@code true}, if {@code source} is active
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 * @throws IllegalArgumentException
	 *     if {@code source} is not contained in the state (and not
	 *     {@code null}).
	 */
	boolean isSourceActive( SourceAndConverter< ? > source );

	/**
	 * Set {@code source} active or inactive (optional operation).
	 * <p>
	 * Returns {@code true}, if source activity changes as a result of the call.
	 * Returns {@code false}, if {@code source} is already in the desired
	 * {@code active} state.
	 *
	 * @return {@code true}, if source activity changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 * @throws IllegalArgumentException
	 *     if {@code source} is not contained in the state (and not
	 *     {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean setSourceActive( SourceAndConverter< ? > source, boolean active );

	/**
	 * Set all sources in {@code collection} active or inactive (optional
	 * operation).
	 * <p>
	 * Returns {@code true}, if source activity changes as a result of the call.
	 * Returns {@code false}, if all sources were already in the desired
	 * {@code active} state.
	 *
	 * @return {@code true}, if source activity changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 * @throws IllegalArgumentException
	 *     if any element of {@code collection} is not contained in the state.
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean setSourcesActive( Collection< ? extends SourceAndConverter< ? > > collection, boolean active );

	/**
	 * Check whether the given {@code source} is visible.
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
	boolean isSourceVisible( SourceAndConverter< ? > source );

	/**
	 * Check whether the given {@code source} is both visible and provides image
	 * data for the current timepoint.
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
	 * @return {@code true}, if {@code source} is both visible and present
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 * @throws IllegalArgumentException
	 *     if {@code source} is not contained in the state (and not
	 *     {@code null}).
	 */
	boolean isSourceVisibleAndPresent( SourceAndConverter< ? > source );

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
	Set< SourceAndConverter< ? > > getVisibleSources();

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
	Set< SourceAndConverter< ? > > getVisibleAndPresentSources();

	/**
	 * Check whether the state contains the {@code source}.
	 *
	 * @return {@code true}, if {@code source} is in the list of sources.
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 */
	boolean containsSource( SourceAndConverter< ? > source );

	/**
	 * Add {@code source} to the state (optional operation). Returns
	 * {@code true}, if the source is added. Returns {@code false}, if the
	 * source is already present.
	 * <p>
	 * If {@code source} is added and no other source was current, then
	 * {@code source} is made current
	 *
	 * @return {@code true}, if list of sources changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code source == null}
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean addSource( SourceAndConverter< ? > source );

	/**
	 * Add all sources in {@code collection} to the state (optional operation).
	 * Returns {@code true}, if at least one source was added. Returns
	 * {@code false}, if all sources were already present.
	 * <p>
	 * If any sources are added and no other source was current, then the first
	 * added sources will be made current.
	 *
	 * @return {@code true}, if list of sources changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean addSources( Collection< ? extends SourceAndConverter< ? > > collection );

	/**
	 * Remove {@code source} from the state (optional operation).
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
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean removeSource( SourceAndConverter< ? > source );

	/**
	 * Remove all sources in {@code collection} from the state (optional
	 * operation). Returns {@code true}, if at least one source was removed.
	 * Returns {@code false}, if none of the sources was present.
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
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean removeSources( Collection< ? extends SourceAndConverter< ? > > collection );

	// TODO addSource with index
	// TODO addSources with index

	/**
	 * Remove all sources from the state (optional operation).
	 *
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	void clearSources();

	/**
	 * Returns a {@link Comparator} that compares sources according to the order
	 * in which they occur in the sources list. (Sources that do not occur in
	 * the list are ordered before any source in the list).
	 */
	Comparator< SourceAndConverter< ? > > sourceOrder();

	// --------------------
	// --     groups     --
	// --------------------

	/**
	 * Get the list of groups. The returned {@code List} reflects changes to the
	 * viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the list of groups
	 */
	List< SourceGroup > getGroups();

	/**
	 * Get the current group. (May return {@code null} if there is no current
	 * group)
	 *
	 * @return the current group
	 */
	SourceGroup getCurrentGroup();

	/**
	 * Returns {@code true} if {@code group} is the current group. Equivalent to
	 * {@code (getCurrentGroup() == group)}.
	 *
	 * @return {@code true} if {@code group} is the current group
	 */
	boolean isCurrentGroup( SourceGroup group );

	/**
	 * Make {@code group} the current group (optional operation). Returns
	 * {@code true}, if current group changes as a result of the call. Returns
	 * {@code false}, if {@code group} is already the current group.
	 *
	 * @param group
	 *     the group to make current
	 * @return {@code true}, if current group changed as a result of the call.
	 *
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean setCurrentGroup( SourceGroup group );

	/**
	 * Get the set of active groups. The returned {@code Set} reflects changes
	 * to the viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the set of active groups
	 */
	Set< SourceGroup > getActiveGroups();

	/**
	 * Check whether the given {@code group} is active.
	 *
	 * @return {@code true}, if {@code group} is active
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 */
	boolean isGroupActive( SourceGroup group );

	/**
	 * Set {@code group} active or inactive (optional operation).
	 * <p>
	 * Returns {@code true}, if group activity changes as a result of the call.
	 * Returns {@code false}, if {@code group} is already in the desired
	 * {@code active} state.
	 *
	 * @return {@code true}, if group activity changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean setGroupActive( SourceGroup group, boolean active );

	/**
	 * Set all groups in {@code collection} active or inactive (optional
	 * operation).
	 * <p>
	 * Returns {@code true}, if group activity changes as a result of the call.
	 * Returns {@code false}, if all groups were already in the desired
	 * {@code active} state.
	 *
	 * @return {@code true}, if group activity changed as a result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 * @throws IllegalArgumentException
	 *     if any element of {@code collection} is not contained in the state.
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean setGroupsActive( Collection< ? extends SourceGroup > collection, boolean active );

	/**
	 * Get the name of a {@code group}.
	 *
	 * @return name of the group, may be {@code null}
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 */
	String getGroupName( final SourceGroup group );

	/**
	 * Set the {@code name} of a {@code group} (optional operation).
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	void setGroupName( SourceGroup group, String name );

	/**
	 * Check whether the state contains the {@code group}.
	 *
	 * @return {@code true}, if {@code group} is in the list of groups.
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 */
	boolean containsGroup( SourceGroup group );

	/**
	 * Add {@code group} to the state (optional operation). Returns
	 * {@code true}, if the group is added. Returns {@code false}, if the group
	 * is already present.
	 * <p>
	 * If {@code group} is added and no other group was current, then
	 * {@code group} is made current
	 *
	 * @return {@code true}, if list of groups changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean addGroup( SourceGroup group );

	/**
	 * Add all groups in {@code collection} to the state (optional operation).
	 * Returns {@code true}, if at least one group was added. Returns
	 * {@code false}, if all groups were already present.
	 * <p>
	 * If any groups are added and no other group was current, then the first
	 * added groups will be made current.
	 *
	 * @return {@code true}, if list of groups changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean addGroups( Collection< ? extends SourceGroup > collection );

	/**
	 * Remove {@code group} from the state (optional operation).
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
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean removeGroup( SourceGroup group );

	/**
	 * Remove all groups in {@code collection} from the state (optional
	 * operation). Returns {@code true}, if at least one group was removed.
	 * Returns {@code false}, if none of the groups was present.
	 * <p>
	 * If the current group was removed, then the first group in the remaining
	 * list of groups is made current (if it exists).
	 *
	 * @return {@code true}, if list of groups changed as a result of the call.
	 *
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean removeGroups( Collection< ? extends SourceGroup > collection );

	/**
	 * Add {@code source} to {@code group} (optional operation).
	 * <p>
	 * Returns {@code true}, if {@code source} was added to {@code group}.
	 * Returns {@code false}, if {@code source} was already contained in
	 * {@code group}. or either of {@code source} and {@code group} is not valid
	 * (not in the BDV sources/groups list).
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a
	 * result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code source == null} or {@code group == null}
	 * @throws IllegalArgumentException
	 *     if either of {@code source} and {@code group} is not contained in the
	 *     state (and not {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean addSourceToGroup( SourceAndConverter< ? > source, SourceGroup group );

	/**
	 * Add all sources in {@code collection} to {@code group} (optional
	 * operation).
	 * <p>
	 * Returns {@code true}, if at least one source was added to {@code group}.
	 * Returns {@code false}, if all sources were already contained in
	 * {@code group}.
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a
	 * result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code group == null} or {@code collection == null} or any element
	 *     of {@code collection} is {@code null}.
	 * @throws IllegalArgumentException
	 *     if {@code group} or any element of {@code collection} is is not
	 *     contained in the state (and not {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean addSourcesToGroup( Collection< ? extends SourceAndConverter< ? > > collection, SourceGroup group );

	/**
	 * Remove {@code source} from {@code group} (optional operation).
	 * <p>
	 * Returns {@code true}, if {@code source} was removed from {@code group}.
	 * Returns {@code false}, if {@code source} was not contained in
	 * {@code group},
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a
	 * result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code source == null} or {@code group == null}
	 * @throws IllegalArgumentException
	 *     if either of {@code source} and {@code group} is not contained in the
	 *     state (and not {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean removeSourceFromGroup( SourceAndConverter< ? > source, SourceGroup group );

	/**
	 * Remove all sources in {@code collection} from {@code group} (optional
	 * operation).
	 * <p>
	 * Returns {@code true}, if at least one source was removed from
	 * {@code group}. Returns {@code false}, if none of the sources were
	 * contained in {@code group}.
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a
	 * result of the call
	 *
	 * @throws NullPointerException
	 *     if {@code group == null} or {@code collection == null} or any element
	 *     of {@code collection} is {@code null}.
	 * @throws IllegalArgumentException
	 *     if {@code group} or any element of {@code collection} is is not
	 *     contained in the state (and not {@code null}).
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	boolean removeSourcesFromGroup( Collection< ? extends SourceAndConverter< ? > > collection, SourceGroup group );

	/**
	 * Get the set sources in {@code group}. The returned {@code Set} reflects
	 * changes to the viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the set of sources in {@code group}
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 */
	Set< SourceAndConverter< ? > > getSourcesInGroup( SourceGroup group );

	/**
	 * Remove all groups from the state (optional operation).
	 *
	 * @throws UnsupportedOperationException
	 *     if the operation is not supported by this ViewerState
	 */
	void clearGroups();

	/**
	 * Returns a {@link Comparator} that compares groups according to the order
	 * in which they occur in the groups list. (Groups that do not occur in the
	 * list are ordered before any group in the list).
	 */
	Comparator< SourceGroup > groupOrder();
}
