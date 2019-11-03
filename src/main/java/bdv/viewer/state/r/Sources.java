package bdv.viewer.state.r;

import bdv.viewer.SourceAndConverter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

// TODO consider renaming to "SourceAndConverters"
// TODO javadoc
public interface Sources extends List< SourceAndConverter< ? > >
{
	/**
	 * @return the current source, or {@code null} if this list doesn't contain the current source
	 */
	SourceAndConverter< ? > getCurrent();

	/**
	 * @return active sources
	 */
	Set< SourceAndConverter< ? > > getActive();

	/**
	 * @return visible sources
	 */
	Set< SourceAndConverter< ? > > getVisible();

	boolean isActive( SourceAndConverter< ? > source );

	boolean isCurrent( SourceAndConverter< ? > source );

	boolean isVisible( SourceAndConverter< ? > source );

	Comparator< SourceAndConverter< ? > > order();

	// -- modification operations --

	/**
	 * Set source active or inactive (optional operation).
	 *
	 * Active sources are visible in {@code FUSED} display mode.
	 *
	 * @return {@code true}, if source activity changed as a result of the call
	 *
	 * @throws UnsupportedOperationException
	 * 		if this source list is unmodifiable
	 */
	boolean setActive( SourceAndConverter< ? > source, boolean active );

	/**
	 * Make {@code source} the current source (optional operation).
	 *
	 * @return {@code true}, if current source changed as a result of the call
	 *
	 * @throws UnsupportedOperationException
	 * 		if this source list is unmodifiable.
	 */
	boolean makeCurrent( SourceAndConverter< ? > source );
}
