package bdv.viewer.state.revised;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.imglib2.realtransform.AffineTransform3D;

public class PlaygroundReadOnly
{
	public static class SourceGroupHandle
	{
	}

	/**
	 * Snapshot of the state of a source group.
	 * Read-Only.
	 * Has a SourceGroupHandle that allows to identify the group across snapshots.
	 */
	public interface SourceGroup
	{
		String getName();

		/**
		 * Get the sources in this group.
		 *
		 * @return unmodifiable set of sources.
		 */
		Set< SourceAndConverter< ? > > getSources();

		/**
		 * Used internally.
		 * The handle is used to identify corresponding source groups across viewer state snapshots.
		 */
		SourceGroupHandle getSourceGroupHandle();

		boolean isActive();

		boolean isCurrent();

	}

	interface SourceGroups_ReadOnly extends List< SourceGroup >
	{
		// boolean contains(Object);
		// --> ViewerState.contains(SourceGroup)

		// int size();
		// --> ViewerState.numSourceGroups();

		// int indexOf(Object);
		// --> ViewerState.getIndex( SourceGroupHandle group );

		// SourceGroup get(int index);
		// --> SourceGroup getGroupAt( int index );

		SourceGroup get( SourceGroupHandle handle );

		// Get the current SourceGroup, or null if this list doesn't contain the current SourceGroup
		SourceGroup getCurrent();

		SourceGroups_ReadOnly getActive();

		SourceGroups_ReadOnly getContaining( SourceAndConverter< ? > source );

		// might still keep this, altough get(handle).isActive() exists
//		boolean isActive( SourceGroupHandle handle );

		// might still keep this, altough get(handle).isCurrent() exists
//		boolean isCurrent( SourceGroupHandle handle );
	}

	interface Sources_ReadOnly extends List< SourceAndConverter< ? > >
	{
		// boolean contains(Object);
		// --> ViewerState.contains(SourceAndConverter)

		// int size();
		// --> ViewerState.numSources();

		SourceAndConverter< ? > getCurrent();

		Sources_ReadOnly getActive();

		Sources_ReadOnly getVisible();

		// might also move/dulicate this to ViewerState_ReadOnly
		boolean isActive( SourceAndConverter< ? > source );

		// might also move/dulicate this to ViewerState_ReadOnly
		boolean isCurrent( SourceAndConverter< ? > source );

		// might also move/dulicate this to ViewerState_ReadOnly
		boolean isVisible( SourceAndConverter< ? > source );
	}

	public interface ViewerState_ReadOnly
	{
		Comparator< SourceAndConverter< ? > > sourceOrder();

		// returns a copy
		default AffineTransform3D getViewerTransform()
		{
			final AffineTransform3D t = new AffineTransform3D();
			getViewerTransform( t );
			return t;
		}

		void getViewerTransform( AffineTransform3D t );

		Interpolation getInterpolation();

		DisplayMode getDisplayMode();

		int getNumTimepoints();

		int getCurrentTimepoint();



		// --- SourceGroups ----------------------------------------

		SourceGroups_ReadOnly getGroups();
//		List< SourceGroup > getOrderedGroups();
//		boolean contains( SourceGroup group );
//		int numSourceGroups();
//		int getIndex( SourceGroupHandle group );
//		SourceGroup getGroupAt( int index );
//		List< SourceAndConverter< ? > > getSourcesIn( SourceGroupHandle group );


		/**
		 * Would be useful to have.
		 * It could be maintained explicitly and then would for sure be quicker than
		 * getSourceGroups().stream().filter(g -> g.contains(source)).collect(Collectors.toSet())
		 *
		 * This might also move to the SourceGroups_ReadOnly interface as
		 *     SourceGroups_ReadOnly containing(SourceAndConverter< ? >)
		 *     SourceGroups_ReadOnly getContaining(SourceAndConverter< ? >)
		 *
		 * state.groups().containing(state.currentSource())
		 * state.getGroups().getContaining(state.getCurrentSource())
		 */
//		SourceGroups_ReadOnly getSourceGroupsContaining( SourceAndConverter< ? > source );

//		SourceGroup getCurrentGroup();

		// might still keep this, altough getGroups().get(handle).isActive() exists
//		boolean isActive( SourceGroupHandle handle );
		// might still keep this, altough getGroups().get(handle).isCurrent() exists
//		boolean isCurrent( SourceGroupHandle handle );


		// --- Sources ---------------------------------------------

		Sources_ReadOnly getSources();
//		List< SourceAndConverter< ? > > getOrderedSources();
//		boolean contains( SourceAndConverter< ? > source );
//		int numSources();

//		boolean groupConstainsSource( SourceGroupHandle group, SourceAndConverter< ? > source );

//		SourceAndConverter< ? > getCurrentSource();
//		boolean isActive( SourceAndConverter< ? > source );
//		boolean isCurrent( SourceAndConverter< ? > source );
//		boolean isVisible( SourceAndConverter< ? > source );
//		List< SourceAndConverter< ? > > getVisibleSources();
	}
}
