package bdv.viewer.state.r;

import bdv.util.Affine3DHelpers;
import bdv.util.WrappedList;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.listeners.Listeners;

import static bdv.viewer.state.r.ViewerStateChange.CURRENT_GROUP_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.CURRENT_SOURCE_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.CURRENT_TIMEPOINT_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.DISPLAY_MODE_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.GROUP_ACTIVITY_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.GROUP_NAME_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.INTERPOLATION_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.NUM_GROUPS_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.NUM_SOURCES_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.NUM_TIMEPOINTS_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.SOURCE_ACTIVITY_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.VIEWER_TRANSFORM_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.VISIBILITY_CHANGED;
import static gnu.trove.impl.Constants.DEFAULT_CAPACITY;
import static gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR;

/**
 * Maintains the BigDataViewer state and implements {@link ViewerState} to
 * expose query and modification methods. {@code ViewerStateChangeListener}s can
 * be registered and will be notified about various {@link ViewerStateChange
 * state changes}.
 * <p>
 * <em>This class is not thread-safe.</em>
 * </p>
 *
 * @author Tobias Pietzsch
 */
public class BasicViewerState implements ViewerState
{
	private final Listeners.List< ViewerStateChangeListener > listeners;

	/**
	 * The current number of available timepoints.
	 */
	private int numTimepoints;

	/**
	 * Which timepoint (index) is currently shown.
	 */
	private int currentTimepoint;

	/**
	 * Transforms global coordinates to viewer coordinates.
	 */
	private final AffineTransform3D viewerTransform;

	/**
	 * The current interpolation method.
	 */
	private Interpolation interpolation;

	/**
	 * The current display mode.
	 */
	private DisplayMode displayMode;

	// -- sources --

	private final List< SourceAndConverter< ? > > sources;

	private final List< SourceAndConverter< ? > > unmodifiableSources;

	private final Set< SourceAndConverter< ? > > activeSources;

	private final Set< SourceAndConverter< ? > > unmodifiableActiveSources;

	private SourceAndConverter< ? > currentSource;

	private final TObjectIntMap< SourceAndConverter< ? > > sourceIndices;

	private final Set< SourceAndConverter< ? > > previousVisibleSources;

	// -- groups --

	private final List< SourceGroup > groups;

	private final List< SourceGroup > unmodifiableGroups;

	private final Map< SourceGroup, GroupData > groupData;

	private final Set< SourceGroup > activeGroups;

	private final Set< SourceGroup > unmodifiableActiveGroups;

	private SourceGroup currentGroup;

	private final TObjectIntMap< SourceGroup > groupIndices;

	private static final int NO_ENTRY_VALUE = -1;

	/**
	 * Create an empty state without any sources or groups. Interpolation is
	 * initialized as {@code Interpolation.NEARESTNEIGHBOR}. Display mode is
	 * initialized as {@code DisplayMode.SINGLE}.
	 */
	public BasicViewerState()
	{
		listeners = new Listeners.List<>();
		numTimepoints = 0;
		currentTimepoint = 0;
		viewerTransform = new AffineTransform3D();
		interpolation = Interpolation.NEARESTNEIGHBOR;
		displayMode = DisplayMode.SINGLE;
		sources = new ArrayList<>();
		unmodifiableSources = new UnmodifiableSources();
		activeSources = new HashSet<>();
		unmodifiableActiveSources = Collections.unmodifiableSet( activeSources );
		sourceIndices = new TObjectIntHashMap<>( DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, NO_ENTRY_VALUE );
		previousVisibleSources = new HashSet<>();
		groups = new ArrayList<>();
		unmodifiableGroups = new UnmodifiableGroups();
		groupData = new HashMap<>();
		activeGroups = new HashSet<>();
		unmodifiableActiveGroups = Collections.unmodifiableSet( activeGroups );
		groupIndices = new TObjectIntHashMap<>( DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, NO_ENTRY_VALUE );
	}

	/**
	 * Create a copy of the given {@code ViewerState} (except for (@link
	 * #changeListeners()}, which are not copied).
	 */
	public BasicViewerState( final ViewerState other )
	{
		listeners = new Listeners.List<>();

		numTimepoints = other.getNumTimepoints();
		currentTimepoint = other.getCurrentTimepoint();
		viewerTransform = other.getViewerTransform();
		interpolation = other.getInterpolation();
		displayMode = other.getDisplayMode();

		sources = new ArrayList<>( other.getSources() );
		unmodifiableSources = new UnmodifiableSources();
		activeSources = new HashSet<>( other.getActiveSources() );
		unmodifiableActiveSources = Collections.unmodifiableSet( activeSources );
		currentSource = other.getCurrentSource();
		sourceIndices = new TObjectIntHashMap<>( DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, NO_ENTRY_VALUE );
		for ( int i = 0; i < sources.size(); ++i )
			sourceIndices.put( sources.get( i ), i );
		previousVisibleSources = new HashSet<>( other.getVisibleSources() );

		groups = new ArrayList<>( other.getGroups() );
		unmodifiableGroups = new UnmodifiableGroups();
		groupData = new HashMap<>();
		other.getGroups().forEach( group -> {
			final GroupData data = new GroupData();
			data.name = other.getGroupName( group );
			data.sources.addAll( other.getSourcesInGroup( group ) );
			groupData.put( group, data );
		} );
		activeGroups = new HashSet<>( other.getActiveGroups() );
		unmodifiableActiveGroups = Collections.unmodifiableSet( activeGroups );
		currentGroup = other.getCurrentGroup();
		groupIndices = new TObjectIntHashMap<>( DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, NO_ENTRY_VALUE );
		for ( int i = 0; i < groups.size(); ++i )
			groupIndices.put( groups.get( i ), i );
	}

	/**
	 * {@code ViewerStateChangeListener}s can be added/removed here.
	 */
	public Listeners< ViewerStateChangeListener > changeListeners()
	{
		return listeners;
	}

	/**
	 * Get a snapshot of this ViewerState.
	 *
	 * @return unmodifiable copy of the current state
	 */
	public ViewerState snapshot()
	{
		return new UnmodifiableViewerState( new BasicViewerState( this ) );
	}

	@Override
	public Interpolation getInterpolation()
	{
		return interpolation;
	}

	@Override
	public void setInterpolation( final Interpolation i )
	{
		if ( interpolation != i )
		{
			interpolation = i;
			notifyListeners( INTERPOLATION_CHANGED );
		}
	}

	@Override
	public DisplayMode getDisplayMode()
	{
		return displayMode;
	}

	@Override
	public void setDisplayMode( final DisplayMode mode )
	{
		if ( displayMode != mode )
		{
			displayMode = mode;
			notifyListeners( DISPLAY_MODE_CHANGED );
			checkVisibilityChanged();
		}
	}

	@Override
	public int getNumTimepoints()
	{
		return numTimepoints;
	}

	@Override
	public void setNumTimepoints( final int n )
	{
		if ( numTimepoints != n )
		{
			numTimepoints = n;
			notifyListeners( NUM_TIMEPOINTS_CHANGED );

			// TODO: Should the current timepoint also be changed to be < numTimePoints?
		}
	}

	@Override
	public int getCurrentTimepoint()
	{
		return currentTimepoint;
	}

	@Override
	public void setCurrentTimepoint( final int t )
	{
		if ( currentTimepoint != t )
		{
			currentTimepoint = t;
			notifyListeners( CURRENT_TIMEPOINT_CHANGED );
		}
	}

	@Override
	public void getViewerTransform( final AffineTransform3D t )
	{
		t.set( viewerTransform );
	}

	@Override
	public void setViewerTransform( final AffineTransform3D t )
	{
		if ( !Affine3DHelpers.equals( viewerTransform, t ) )
		{
			viewerTransform.set( t );
			notifyListeners( VIEWER_TRANSFORM_CHANGED );
		}
	}

	// --------------------
	// --    sources     --
	// --------------------

	/**
	 * Get the list of sources. The returned {@code List} reflects changes to
	 * the viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the list of sources
	 */
	@Override
	public List< SourceAndConverter< ? > > getSources()
	{
		return unmodifiableSources;
	}

	/**
	 * Get the current source. (May return {@code null} if there is no current
	 * source)
	 *
	 * @return the current source
	 */
	@Override
	public SourceAndConverter< ? > getCurrentSource()
	{
		return currentSource;
	}

	/**
	 * Returns {@code true} if {@code source} is the current source. Equivalent
	 * to {@code (getCurrentSource() == source)}.
	 *
	 * @param source
	 *     the source. Passing {@code null} checks whether no source is current.
	 * @return {@code true} if {@code source} is the current source
	 */
	@Override
	public boolean isCurrentSource( final SourceAndConverter< ? > source )
	{
		return Objects.equals( source, currentSource );
	}

	/**
	 * Make {@code source} the current source. Returns {@code true}, if current
	 * source changes as a result of the call. Returns {@code false}, if
	 * {@code source} is already the current source.
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
	 */
	@Override
	public boolean setCurrentSource( final SourceAndConverter< ? > source )
	{
		checkSourcePresentAllowNull( source );

		final boolean modified = !Objects.equals( currentSource, source );
		currentSource = source;
		if ( modified )
		{
			notifyListeners( CURRENT_SOURCE_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	/**
	 * Get the set of active sources. The returned {@code Set} reflects changes
	 * to the viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the set of active sources
	 */
	@Override
	public Set< SourceAndConverter< ? > > getActiveSources()
	{
		return unmodifiableActiveSources;
	}

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
	@Override
	public boolean isSourceActive( final SourceAndConverter< ? > source )
	{
		checkSourcePresent( source );

		return activeSources.contains( source );
	}

	/**
	 * Set {@code source} active or inactive.
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
	 */
	@Override
	public boolean setSourceActive( final SourceAndConverter< ? > source, final boolean active )
	{
		checkSourcePresent( source );

		final boolean modified = active ? activeSources.add( source ) : activeSources.remove( source );
		if ( modified )
		{
			notifyListeners( SOURCE_ACTIVITY_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	/**
	 * Set all sources in {@code collection} active or inactive.
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
	 */
	@Override
	public boolean setSourcesActive( final Collection< ? extends SourceAndConverter< ? > > collection, final boolean active )
	{
		checkSourcesPresent( collection );

		final boolean modified = active ? activeSources.addAll( collection ) : activeSources.removeAll( collection );
		if ( modified )
		{
			notifyListeners( SOURCE_ACTIVITY_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

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
	@Override
	public boolean isSourceVisible( final SourceAndConverter< ? > source )
	{
		checkSourcePresent( source );

		switch ( displayMode )
		{
		case SINGLE:
		default:
			return Objects.equals( currentSource, source );
		case GROUP:
			return currentGroup != null && groupData.get( currentGroup ).sources.contains( source );
		case FUSED:
			return isSourceActive( source );
		case FUSEDGROUP:
			for ( final SourceGroup group : activeGroups )
				if ( groupData.get( group ).sources.contains( source ) )
					return true;
			return false;
		}
	}

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
	@Override
	public boolean isSourceVisibleAndPresent( final SourceAndConverter< ? > source )
	{
		return isSourceVisible( source ) && source.getSpimSource().isPresent( currentTimepoint );
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
	 * visible.</li>
	 * </ul>
	 *
	 * @return the set of visible sources
	 */
	@Override
	public Set< SourceAndConverter< ? > > getVisibleSources()
	{
		final Set< SourceAndConverter< ? > > visible = new HashSet<>();
		switch ( displayMode )
		{
		case SINGLE:
			if ( currentSource != null )
				visible.add( currentSource );
			break;
		case GROUP:
			if ( currentGroup != null )
				visible.addAll( groupData.get( currentGroup ).sources );
			break;
		case FUSED:
			visible.addAll( activeSources );
			break;
		case FUSEDGROUP:
			for ( final SourceGroup group : activeGroups )
				visible.addAll( groupData.get( group ).sources );
			break;
		}
		return visible;
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
	public Set< SourceAndConverter< ? > > getVisibleAndPresentSources()
	{
		final Set< SourceAndConverter< ? > > visible = getVisibleSources();
		visible.removeIf( source -> !source.getSpimSource().isPresent( currentTimepoint ) );
		return visible;
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
	public boolean containsSource( final SourceAndConverter< ? > source )
	{
		if ( source == null )
			throw new NullPointerException();

		return sourceIndices.containsKey( source );
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
	public boolean addSource( final SourceAndConverter< ? > source )
	{
		if ( source == null )
			throw new NullPointerException();

		final boolean modified = !sourceIndices.containsKey( source );
		if ( modified )
		{
			final int nextIndex = sources.size();
			sources.add( source );
			sourceIndices.put( source, nextIndex );
			final boolean currentSourceChanged = ( currentSource == null );
			if ( currentSourceChanged )
				currentSource = source;

			notifyListeners( NUM_SOURCES_CHANGED );
			if ( currentSourceChanged )
				notifyListeners( CURRENT_SOURCE_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
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
	public boolean addSources( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		checkAllNonNull( collection );

		boolean modified = false;
		boolean currentSourceChanged = false;
		for ( final SourceAndConverter< ? > source : collection )
		{
			if ( sourceIndices.containsKey( source ) )
				continue;

			modified = true;
			final int nextIndex = sources.size();
			sources.add( source );
			sourceIndices.put( source, nextIndex );
			if ( currentSource == null )
			{
				currentSourceChanged = true;
				currentSource = source;
			}
		}
		if ( modified )
		{
			notifyListeners( NUM_SOURCES_CHANGED );
			if ( currentSourceChanged )
				notifyListeners( CURRENT_SOURCE_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
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
	public boolean removeSource( final SourceAndConverter< ? > source )
	{
		if ( source == null )
			throw new NullPointerException();

		final int removedIndex = sourceIndices.remove( source );
		final boolean modified = ( removedIndex != NO_ENTRY_VALUE );
		if ( modified )
		{
			sources.remove( removedIndex );
			for ( int i = removedIndex; i < sources.size(); ++i )
				sourceIndices.put( sources.get( i ), i );

			activeSources.remove( source );
			final boolean currentSourceChanged = source.equals( currentSource );
			if ( currentSourceChanged )
				currentSource = sources.isEmpty() ? null : sources.get( 0 );

			boolean sourceToGroupAssignmentChanged = false;
			for ( final GroupData groupData : groupData.values() )
				sourceToGroupAssignmentChanged |= groupData.sources.remove( source );

			notifyListeners( NUM_SOURCES_CHANGED );
			if ( currentSourceChanged )
				notifyListeners( CURRENT_SOURCE_CHANGED );
			if ( sourceToGroupAssignmentChanged )
				notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
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
	public boolean removeSources( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		checkAllNonNull( collection );

		final boolean modified = sources.removeAll( collection );
		final boolean currentSourceChanged = collection.contains( currentSource );

		if ( modified )
		{
			sourceIndices.clear();
			for ( int i = 0; i < sources.size(); ++i )
				sourceIndices.put( sources.get( i ), i );
			activeSources.removeAll( collection );

			boolean sourceToGroupAssignmentChanged = false;
			for ( final GroupData groupData : groupData.values() )
				sourceToGroupAssignmentChanged |= groupData.sources.removeAll( sources );

			if ( currentSourceChanged )
				currentSource = sources.isEmpty() ? null : sources.get( 0 );

			notifyListeners( NUM_SOURCES_CHANGED );
			if ( currentSourceChanged )
				notifyListeners( CURRENT_SOURCE_CHANGED );
			if ( sourceToGroupAssignmentChanged )
				notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}
		return modified;

	}

	// TODO addSource with index
	// TODO addSources with index

	/**
	 * Remove all sources from the state.
	 */
	@Override
	public void clearSources()
	{
		if ( sources.isEmpty() )
			return;

		sources.clear();
		sourceIndices.clear();
		activeSources.clear();

		boolean sourceToGroupAssignmentChanged = false;
		for ( final GroupData groupData : groupData.values() )
		{
			sourceToGroupAssignmentChanged |= !groupData.sources.isEmpty();
			groupData.sources.clear();
		}

		final boolean currentSourceChanged = ( currentSource != null );
		currentSource = null;

		notifyListeners( NUM_SOURCES_CHANGED );
		if ( currentSourceChanged )
			notifyListeners( CURRENT_SOURCE_CHANGED );
		if ( sourceToGroupAssignmentChanged )
			notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
		checkVisibilityChanged();
	}

	/**
	 * Returns a {@link Comparator} that compares sources according to the order
	 * in which they occur in the sources list. (Sources that do not occur in
	 * the list are ordered before any source in the list).
	 */
	@Override
	public Comparator< SourceAndConverter< ? > > sourceOrder()
	{
		return Comparator.comparingInt( sourceIndices::get );
	}

	// --------------------
	// --     groups     --
	// --------------------

	/**
	 * Get the list of groups. The returned {@code List} reflects changes to the
	 * viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the list of groups
	 */
	@Override
	public List< SourceGroup > getGroups()
	{
		return unmodifiableGroups;
	}

	/**
	 * Get the current group. (May return {@code null} if there is no current
	 * group)
	 *
	 * @return the current group
	 */
	@Override
	public SourceGroup getCurrentGroup()
	{
		return currentGroup;
	}

	/**
	 * Returns {@code true} if {@code group} is the current group. Equivalent to
	 * {@code (getCurrentGroup() == group)}.
	 *
	 * @return {@code true} if {@code group} is the current group
	 */
	@Override
	public boolean isCurrentGroup( final SourceGroup group )
	{
		return Objects.equals( group, currentGroup );
	}

	/**
	 * Make {@code group} the current group. Returns {@code true}, if current
	 * group changes as a result of the call. Returns {@code false}, if
	 * {@code group} is already the current group.
	 *
	 * @param group
	 *     the group to make current
	 * @return {@code true}, if current group changed as a result of the call.
	 *
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 */
	@Override
	public boolean setCurrentGroup( final SourceGroup group )
	{
		checkGroupPresentAllowNull( group );

		final boolean modified = !Objects.equals( currentGroup, group );
		currentGroup = group;
		if ( modified )
		{
			notifyListeners( CURRENT_GROUP_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	/**
	 * Get the set of active groups. The returned {@code Set} reflects changes
	 * to the viewer state. It is unmodifiable and not thread-safe.
	 *
	 * @return the set of active groups
	 */
	@Override
	public Set< SourceGroup > getActiveGroups()
	{
		return unmodifiableActiveGroups;
	}

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
	@Override
	public boolean isGroupActive( final SourceGroup group )
	{
		checkGroupPresent( group );

		return activeGroups.contains( group );
	}

	/**
	 * Set {@code group} active or inactive.
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
	 */
	@Override
	public boolean setGroupActive( final SourceGroup group, final boolean active )
	{
		checkGroupPresent( group );

		final boolean modified = active ? activeGroups.add( group ) : activeGroups.remove( group );
		if ( modified )
		{
			notifyListeners( GROUP_ACTIVITY_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	/**
	 * Set all groups in {@code collection} active or inactive.
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
	 */
	@Override
	public boolean setGroupsActive( final Collection< ? extends SourceGroup > collection, final boolean active )
	{
		checkGroupsPresent( collection );

		final boolean modified = active ? activeGroups.addAll( collection ) : activeGroups.removeAll( collection );
		if ( modified )
		{
			notifyListeners( GROUP_ACTIVITY_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

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
	@Override
	public String getGroupName( final SourceGroup group )
	{
		checkGroupPresent( group );

		return groupData.get( group ).name;
	}

	/**
	 * Set the {@code name} of a {@code group}.
	 *
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 */
	@Override
	public void setGroupName( final SourceGroup group, final String name )
	{
		checkGroupPresent( group );

		final GroupData data = groupData.get( group );
		if ( !Objects.equals( data.name, name ) )
		{
			data.name = name;
			notifyListeners( GROUP_NAME_CHANGED );
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
	public boolean containsGroup( final SourceGroup group )
	{
		if ( group == null )
			throw new NullPointerException();

		return groupIndices.containsKey( group );
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
	public boolean addGroup( final SourceGroup group )
	{
		if ( group == null )
			throw new NullPointerException();

		final boolean modified = !groupIndices.containsKey( group );
		if ( modified )
		{
			final int nextIndex = groups.size();
			groups.add( group );
			groupData.put( group, new GroupData() );
			groupIndices.put( group, nextIndex );
			final boolean currentGroupChanged = ( currentGroup == null );
			if ( currentGroupChanged )
				currentGroup = group;

			notifyListeners( NUM_GROUPS_CHANGED );
			if ( currentGroupChanged )
				notifyListeners( CURRENT_GROUP_CHANGED );
			// new group is empty, so visibility will not change
			// checkVisibilityChanged();
		}
		return modified;
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
	public boolean addGroups( final Collection< ? extends SourceGroup > collection )
	{
		checkAllNonNull( collection );

		boolean modified = false;
		boolean currentGroupChanged = false;
		for ( final SourceGroup group : collection )
		{
			if ( groupIndices.containsKey( group ) )
				continue;

			modified = true;
			final int nextIndex = groups.size();
			groups.add( group );
			groupData.put( group, new GroupData() );
			groupIndices.put( group, nextIndex );
			if ( currentGroup == null )
			{
				currentGroupChanged = true;
				currentGroup = group;
			}
		}
		if ( modified )
		{
			notifyListeners( NUM_GROUPS_CHANGED );
			if ( currentGroupChanged )
				notifyListeners( CURRENT_GROUP_CHANGED );
			// new groups are empty, so visibility will not change
			// checkVisibilityChanged();
		}
		return modified;
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
	public boolean removeGroup( final SourceGroup group )
	{
		if ( group == null )
			throw new NullPointerException();

		final int removedIndex = groupIndices.remove( group );
		final boolean modified = ( removedIndex != NO_ENTRY_VALUE );
		if ( modified )
		{
			groups.remove( group );
			for ( int i = removedIndex; i < groups.size(); ++i )
				groupIndices.put( groups.get( i ), i );

			groupData.remove( group );
			activeGroups.remove( group );
			final boolean currentGroupChanged = group.equals( currentGroup );
			if ( currentGroupChanged )
				currentGroup = groups.isEmpty() ? null : groups.get( 0 );

			notifyListeners( NUM_GROUPS_CHANGED );
			if ( currentGroupChanged )
				notifyListeners( CURRENT_GROUP_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
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
	public boolean removeGroups( final Collection< ? extends SourceGroup > collection )
	{
		checkAllNonNull( collection );

		final boolean modified = groups.removeAll( collection );
		final boolean currentGroupChanged = collection.contains( currentGroup );

		if ( modified )
		{
			groupIndices.clear();
			for ( int i = 0; i < groups.size(); ++i )
				groupIndices.put( groups.get( i ), i );
			groupData.keySet().removeAll( collection );
			activeGroups.removeAll( collection );

			if ( currentGroupChanged )
				currentGroup = groups.isEmpty() ? null : groups.get( 0 );

			notifyListeners( NUM_GROUPS_CHANGED );
			if ( currentGroupChanged )
				notifyListeners( CURRENT_GROUP_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	/**
	 * Add {@code source} to {@code group}.
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
	 */
	@Override
	public boolean addSourceToGroup( final SourceAndConverter< ? > source, final SourceGroup group )
	{
		checkSourcePresent( source );
		checkGroupPresent( group );

		final boolean modified = groupData.get( group ).sources.add( source );
		if ( modified )
		{
			notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	/**
	 * Add all sources in {@code collection} to {@code group}.
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
	 */
	@Override
	public boolean addSourcesToGroup( final Collection< ? extends SourceAndConverter< ? > > collection, final SourceGroup group )
	{
		checkSourcesPresent( collection );
		checkGroupPresent( group );

		final boolean modified = groupData.get( group ).sources.addAll( collection );
		if ( modified )
		{
			notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	/**
	 * Remove {@code source} from {@code group}.
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
	 */
	@Override
	public boolean removeSourceFromGroup( final SourceAndConverter< ? > source, final SourceGroup group )
	{
		checkSourcePresent( source );
		checkGroupPresent( group );

		final boolean modified = groupData.get( group ).sources.remove( source );
		if ( modified )
		{
			notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}

		return modified;
	}

	/**
	 * Remove all sources in {@code collection} from {@code group}.
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
	 */
	@Override
	public boolean removeSourcesFromGroup( final Collection< ? extends SourceAndConverter< ? > > collection, final SourceGroup group )
	{
		checkSourcesPresent( collection );
		checkGroupPresent( group );

		final boolean modified = groupData.get( group ).sources.removeAll( collection );
		if ( modified )
		{
			notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}

		return modified;
	}

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
	@Override
	public Set< SourceAndConverter< ? > > getSourcesInGroup( final SourceGroup group )
	{
		checkGroupPresent( group );

		return groupData.get( group ).unmodifiableSources;
	}

	/**
	 * Remove all groups from the state.
	 */
	@Override
	public void clearGroups()
	{
		if ( groups.isEmpty() )
			return;

		groups.clear();
		groupIndices.clear();
		activeGroups.clear();

		final boolean currentGroupChanged = ( currentGroup != null );
		currentGroup = null;

		notifyListeners( NUM_GROUPS_CHANGED );
		if ( currentGroupChanged )
			notifyListeners( CURRENT_GROUP_CHANGED );
		notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
		checkVisibilityChanged();
	}

	/**
	 * Returns a {@link Comparator} that compares groups according to the order
	 * in which they occur in the groups list. (Groups that do not occur in the
	 * list are ordered before any group in the list).
	 */
	@Override
	public Comparator< SourceGroup > groupOrder()
	{
		return Comparator.comparingInt( groupIndices::get );
	}

	// --------------------
	// --    helpers     --
	// --------------------

	private class GroupData
	{
		String name;

		final Set< SourceAndConverter< ? > > sources;

		final Set< SourceAndConverter< ? > > unmodifiableSources;

		GroupData()
		{
			name = null;
			sources = new HashSet<>();
			unmodifiableSources = Collections.unmodifiableSet( sources );
		}
	}

	private class UnmodifiableSources extends WrappedList< SourceAndConverter< ? > >
	{
		public UnmodifiableSources()
		{
			super( Collections.unmodifiableList( sources ) );
		}

		@Override
		public boolean contains( final Object o )
		{
			return sourceIndices.containsKey( o );
		}

		@Override
		public boolean containsAll( final Collection< ? > c )
		{
			return sourceIndices.keySet().containsAll( c );
		}

		@Override
		public int indexOf( final Object o )
		{
			return sourceIndices.get( o );
		}

		@Override
		public int lastIndexOf( final Object o )
		{
			return sourceIndices.get( o );
		}
	}

	private class UnmodifiableGroups extends WrappedList< SourceGroup >
	{
		public UnmodifiableGroups()
		{
			super( Collections.unmodifiableList( groups ) );
		}

		@Override
		public boolean contains( final Object o )
		{
			return groupIndices.containsKey( o );
		}

		@Override
		public boolean containsAll( final Collection< ? > c )
		{
			return groupIndices.keySet().containsAll( c );
		}

		@Override
		public int indexOf( final Object o )
		{
			return groupIndices.get( o );
		}

		@Override
		public int lastIndexOf( final Object o )
		{
			return groupIndices.get( o );
		}
	}

	/**
	 * @throws NullPointerException
	 *     if {@code source == null}
	 * @throws IllegalArgumentException
	 *     if {@code source} is not contained in the state (and not
	 *     {@code null}).
	 */
	private void checkSourcePresent( final SourceAndConverter< ? > source )
	{
		if ( source == null )
			throw new NullPointerException();
		if ( !sources.contains( source ) )
			throw new IllegalArgumentException();
	}

	/**
	 * @throws IllegalArgumentException
	 *     if {@code source} is not contained in the state (and not
	 *     {@code null}).
	 */
	private void checkSourcePresentAllowNull( final SourceAndConverter< ? > source )
	{
		if ( source != null && !sources.contains( source ) )
			throw new IllegalArgumentException();
	}

	/**
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 * @throws IllegalArgumentException
	 *     if any element of {@code collection} is not contained in the state.
	 */
	private void checkSourcesPresent( final Collection< ? extends SourceAndConverter< ? > > collection )
	{
		if ( collection == null )
			throw new NullPointerException();
		for ( final SourceAndConverter< ? > source : collection )
			checkSourcePresent( source );
	}

	/**
	 * @throws NullPointerException
	 *     if {@code group == null}
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 */
	private void checkGroupPresent( final SourceGroup group )
	{
		if ( group == null )
			throw new NullPointerException();
		if ( !groups.contains( group ) )
			throw new IllegalArgumentException();
	}

	/**
	 * @throws IllegalArgumentException
	 *     if {@code group} is not contained in the state (and not
	 *     {@code null}).
	 */
	private void checkGroupPresentAllowNull( final SourceGroup group )
	{
		if ( group != null && !groups.contains( group ) )
			throw new IllegalArgumentException();
	}

	/**
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 * @throws IllegalArgumentException
	 *     if any element of {@code collection} is not contained in the state.
	 */
	private void checkGroupsPresent( final Collection< ? extends SourceGroup > collection )
	{
		if ( collection == null )
			throw new NullPointerException();
		for ( final SourceGroup group : collection )
			checkGroupPresent( group );
	}

	/**
	 * @throws NullPointerException
	 *     if {@code collection == null} or any element of {@code collection} is
	 *     {@code null}.
	 */
	private void checkAllNonNull( final Collection< ? > collection )
	{
		if ( collection == null )
			throw new NullPointerException();
		for ( final Object e : collection )
			if ( e == null )
				throw new NullPointerException();
	}

	private void notifyListeners( final ViewerStateChange change )
	{
		listeners.list.forEach( l -> l.viewerStateChanged( change ) );
	}

	private void checkVisibilityChanged()
	{
		final Set< SourceAndConverter< ? > > visible = getVisibleSources();
		if ( !visible.equals( previousVisibleSources ) )
		{
			previousVisibleSources.clear();
			previousVisibleSources.addAll( visible );
			notifyListeners( VISIBILITY_CHANGED );
		}
	}
}
