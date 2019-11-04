package bdv.viewer.state.r;

import bdv.viewer.SourceAndConverter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public interface SourceGroups extends List< SourceGroup >
{
	/**
	 * Get the current SourceGroup.
	 *
	 * @return the current SourceGroup, or {@code null} if this list doesn't contain the current SourceGroup
	 */
	SourceGroup getCurrent();

	/**
	 * @return active SourceGroups in this list
	 */
	Set< SourceGroup > getActive();

	Set< SourceAndConverter< ? > > getSourcesIn( SourceGroup group );

	Set< SourceGroup > getMatching( Predicate< SourceGroup > condition );

	String getName( SourceGroup group );

	/**
	 * This is equivalent to {@code getActive().contains(group)}.
	 */
	boolean isActive( SourceGroup group );

	boolean isCurrent( SourceGroup group );

	Comparator< SourceGroup > order();

	/**
	 * @return SourceGroups in this list containing source
	 */
	default Set< SourceGroup > getGroupsContaining( SourceAndConverter< ? > source )
	{
		return getMatching( g -> getSourcesIn( g ).contains( source ) );
	}

	// -- modification operations --

	/**
	 * Set the {@code name} of the specified {@code group} (optional operation)
	 *
	 * @throws UnsupportedOperationException
	 * 		if this SourceGroups list is unmodifiable
	 */
	void setName( SourceGroup group, String name );

	/**
	 * Set group active or inactive (optional operation).
	 * <p>
	 * This is equivalent to {@code getActive().add(group)} and {@code getActive().remove(group)} respectively.
	 *
	 * @return {@code true}, if group activity changed as a result of the call
	 *
	 * @throws UnsupportedOperationException
	 * 		if this SourceGroups list is unmodifiable
	 */
	boolean setActive( SourceGroup group, boolean active );

	/**
	 * Make {@code group} the current group (optional operation).
	 *
	 * @return {@code true}, if current source changed as a result of the call
	 *
	 * @throws UnsupportedOperationException
	 * 		if this SourceGroups list is unmodifiable.
	 */
	boolean makeCurrent( SourceGroup group );

	/**
	 * Add {@code source} to {@code group} (optional operation).
	 * <p>
	 * This is equivalent to {@code getSourcesIn(group).add(source)}.
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a result of the call
	 *
	 * @throws UnsupportedOperationException
	 * 		if this SourceGroups list is unmodifiable
	 */
	boolean addSourceToGroup( SourceAndConverter< ? > source, SourceGroup group );

	/**
	 * Remove {@code source} from {@code group} (optional operation).
	 * <p>
	 * This is equivalent to {@code getSourcesIn(group).remove(source)}.
	 *
	 * @return {@code true}, if set of sources in {@code group} changed as a result of the call
	 *
	 * @throws UnsupportedOperationException
	 * 		if this SourceGroups list is unmodifiable
	 */
	boolean removeSourceFromGroup( SourceAndConverter< ? > source, SourceGroup group );
}
