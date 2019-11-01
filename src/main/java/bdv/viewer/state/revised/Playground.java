package bdv.viewer.state.revised;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.imglib2.realtransform.AffineTransform3D;

public class Playground
{
	public static void main( String[] args )
	{
	}

	interface SourceGroupHandle
	{
	}

	// SOURCES
	interface ViewerStateX
	{
		// creates a new SourceGroup handle
		SourceGroupHandle createGroup( String initialName, Collection< ? extends SourceAndConverter< ? > > initialSources );

		// creates a new SourceGroup handle
		SourceGroupHandle createGroup( String initialName );

		boolean remove( SourceGroupHandle group );

		boolean contains( SourceGroupHandle group );

		// unmodifiable copy
		List< SourceGroupHandle > getOrderedGroups();





		void add( SourceAndConverter< ? > source );

		boolean remove( SourceAndConverter< ? > source );

		boolean contains( SourceAndConverter< ? > source );

		// unmodifiable copy
		List< SourceAndConverter< ? > > getOrderedSources();




		void addSourceToGroup( SourceAndConverter< ? > source, SourceGroupHandle group );

		void removeSourceFromGroup( SourceAndConverter< ? > source, SourceGroupHandle group );

		boolean groupConstainsSource( SourceGroupHandle group, SourceAndConverter< ? > source );

		// unmodifiable copy
		List< SourceGroupHandle > getGroupsContaining( SourceAndConverter< ? > source );

		// unmodifiable copy
		List< SourceAndConverter< ? > > getSourcesIn( SourceGroupHandle group );




		boolean isActive( SourceAndConverter< ? > source );

		void setActive( SourceAndConverter< ? > source, boolean active );

		boolean isCurrent( SourceAndConverter< ? > source );

		void makeCurrent( SourceAndConverter< ? > source );

		SourceAndConverter< ? > getCurrentSource();




		boolean isActive( SourceGroupHandle group );

		void setActive( SourceGroupHandle group, boolean active );

		boolean isCurrent( SourceGroupHandle group );

		void makeCurrent( SourceGroupHandle source );

		SourceGroupHandle getCurrentGroup();



		boolean isVisible( SourceAndConverter< ? > source );




		// -- maybe --

		int getIndex( SourceAndConverter< ? > source );

		int getIndex( SourceGroupHandle group );

		SourceAndConverter< ? > getSourceAt( int index );

		SourceGroupHandle getGroupAt( int index );

		void add( int index, SourceAndConverter< ? > source );

		SourceGroupHandle createGroup( int index, String initialName, Collection< ? extends SourceAndConverter< ? > > initialSources );

		SourceGroupHandle createGroup( int index, String initialName );

		void sortSources( Comparator< SourceAndConverter< ? > > comparator );

		void sortGroups( Comparator< SourceGroupHandle > comparator );
	}


	interface bla
	{

		/*
		 * Renderer state.
		 * (which sources to show, which interpolation method to use, etc.)
		 */

		// returns a copy
		AffineTransform3D getViewerTransform();
		void getViewerTransform( AffineTransform3D t );
		void setViewerTransform( AffineTransform3D t );

		Interpolation getInterpolation();
		void setInterpolation( Interpolation interpolation );

		void setDisplayMode( final DisplayMode mode );
		DisplayMode getDisplayMode();

		int getCurrentTimepoint();

		void setCurrentTimepoint( final int timepoint );

		int numSources();

		int numSourceGroups();

		List< SourceAndConverter< ? > > getVisibleSources();

		int getNumTimepoints();

		void setNumTimepoints( int numTimepoints );
	}
}
