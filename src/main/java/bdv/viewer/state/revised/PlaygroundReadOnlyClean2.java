package bdv.viewer.state.revised;

import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.state.SourceGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PlaygroundReadOnlyClean2
{
	/**
	 * Handle of a SourceGroup.
	 * The name and contents of a SourceGroup change over time.
	 */
	public static final class SourceGroup
	{}

	interface SourceGroups extends List< SourceGroup >
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

		/**
		 * @return SourceGroups in this list containing source
		 */
		default Set< SourceGroup > getGroupsContaining( SourceAndConverter< ? > source )
		{
			return getMatching( g -> getSourcesIn( g ).contains( source ) );
		}

		Set< SourceGroup > getMatching( Predicate< SourceGroup > condition );

		String getName( SourceGroup group );

		boolean isActive( SourceGroup group );

		boolean isCurrent( SourceGroup group );

		Comparator< SourceGroup > order();
	}

	interface Sources extends List< SourceAndConverter< ? > >
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
	}

	public static class DefaultSources extends WrappedList< SourceAndConverter< ? > > implements Sources
	{
		private final ArrayList< SourceAndConverter< ? > > sources;

		private final Supplier< Set< SourceAndConverter< ? > > > getVisible;

		private final Predicate< SourceAndConverter< ? > > isVisible;

		private final Set< SourceAndConverter< ? > > active;

		private final Set< SourceAndConverter< ? > > unmodifiableActive;

		private SourceAndConverter< ? > current;

		DefaultSources(
				final Supplier< Set< SourceAndConverter< ? > > > getVisible,
				final Predicate< SourceAndConverter< ? > > isVisible )
		{
			this( new ArrayList<>(), getVisible, isVisible );
		}

		DefaultSources( final ArrayList< SourceAndConverter< ? > > sources, final Supplier< Set< SourceAndConverter< ? > > > getVisible, final Predicate< SourceAndConverter< ? > > isVisible )
		{
			super( Collections.unmodifiableList( sources ) );
			this.sources = sources;
			this.getVisible = getVisible;
			this.isVisible = isVisible;
			active = new HashSet<>();
			unmodifiableActive = Collections.unmodifiableSet( active );
		}

		@Override
		public SourceAndConverter< ? > getCurrent()
		{
			return current;
		}

		@Override
		public Set< SourceAndConverter< ? > > getActive()
		{
			return unmodifiableActive;
		}

		@Override
		public Set< SourceAndConverter< ? > > getVisible()
		{
			return getVisible.get();
		}

		@Override
		public boolean isActive( final SourceAndConverter< ? > source )
		{
			return active.contains( source );
		}

		@Override
		public boolean isCurrent( final SourceAndConverter< ? > source )
		{
			return Objects.equals( source, current );
		}

		@Override
		public boolean isVisible( final SourceAndConverter< ? > source )
		{
			return isVisible.test( source );
		}

		@Override
		public Comparator< SourceAndConverter< ? > > order()
		{
			// TODO make more efficient
			return Comparator.comparingInt( sources::indexOf );
		}

		// -- modify --

		public void addSource( final SourceAndConverter< ? > source )
		{
			if ( source == null || sources.contains( source ) )
				throw new IllegalArgumentException();

			sources.add( source );
			if ( current == null )
				current = source;
		}

		public boolean removeSource( final SourceAndConverter< ? > source )
		{
			if ( source == null )
				return false;

			final boolean removed = sources.remove( source );
			if ( removed )
			{
				active.remove( source );
				if ( current.equals( source ) )
					current = sources.isEmpty() ? null : sources.get( 0 );
			}
			return removed;
		}

		public void makeCurrent( SourceAndConverter< ? > source )
		{
			if ( source != null && !sources.contains( source ) )
				throw new IllegalArgumentException();

			current = source;
		}

		public void setActive( SourceAndConverter< ? > source, boolean active )
		{
			if ( source != null && !sources.contains( source ) )
				throw new IllegalArgumentException();

			if ( active )
				this.active.add( source );
			else
				this.active.remove( source );
		}
	}

	public static class DefaultSourceGroups extends WrappedList< SourceGroup > implements SourceGroups
	{
		private final ArrayList< SourceGroup > groups;

		private final Map< SourceGroup, String > names;

		private final Map< SourceGroup, Set< SourceAndConverter< ? > > > contents;

		private final Set< SourceGroup > active;

		private final Set< SourceGroup > unmodifiableActive;

		private SourceGroup current;

		DefaultSourceGroups()
		{
			this( new ArrayList<>() );
		}

		private DefaultSourceGroups( ArrayList< SourceGroup > groups )
		{
			super( Collections.unmodifiableList( groups ) );
			this.groups = groups;

			names = new HashMap<>();
			contents = new HashMap<>();
			active = new HashSet<>();
			unmodifiableActive = Collections.unmodifiableSet( active );
		}

		@Override
		public SourceGroup getCurrent()
		{
			return current;
		}

		@Override
		public Set< SourceGroup > getActive()
		{
			return unmodifiableActive;
		}

		@Override
		public Set< SourceAndConverter< ? > > getSourcesIn( final SourceGroup group )
		{
			if ( group == null )
				return Collections.emptySet();

			// TODO: should this return unmodifiable set?
			//       and should the unmodifiable versions be maintained?
			//       --> one HashMap to class containing name, sources, unmodifiableSources
			return contents.get( group );
		}

		@Override
		public Set< SourceGroup > getMatching( final Predicate< SourceGroup > condition )
		{
			Set< SourceGroup > result = new HashSet<>();
			for ( SourceGroup group : groups )
				if ( condition.test( group ) )
					result.add( group );
			return result;
		}

		@Override
		public String getName( final SourceGroup group )
		{
			return names.get( group );
		}

		@Override
		public boolean isActive( final SourceGroup group )
		{
			return active.contains( group );
		}

		@Override
		public boolean isCurrent( final SourceGroup group )
		{
			return Objects.equals( group, current );
		}

		@Override
		public Comparator< SourceGroup > order()
		{
			// TODO make more efficient
			return Comparator.comparingInt( groups::indexOf );
		}

		// -- modify --

		public boolean removeSource( final SourceAndConverter< ? > source )
		{
			if ( source == null )
				return false;

			boolean removed = false;
			for ( Set< SourceAndConverter< ? > > sources : contents.values() )
				removed |= sources.remove( source );

			return removed;
		}

		public void addGroup( final SourceGroup group )
		{
			if ( group == null || groups.contains( group ) )
				throw new IllegalArgumentException();

			groups.add( group );
			names.put( group, "" );
			contents.put( group, new HashSet<>() );
			if ( current == null )
				current = group;
		}

		public boolean removeGroup( final SourceGroup group )
		{
			if ( group == null )
				return false;

			final boolean removed = groups.remove( group );
			if ( removed )
			{
				active.remove( group );
				names.remove( group );
				contents.remove( group );
				if ( current.equals( group ) )
					current = groups.isEmpty() ? null : groups.get( 0 );
			}
			return removed;
		}

		public void makeCurrent( SourceGroup group )
		{
			if ( group != null && !groups.contains( group ) )
				throw new IllegalArgumentException();

			current = group;
		}

		public void setActive( SourceGroup group, boolean active )
		{
			if ( group != null && !groups.contains( group ) )
				throw new IllegalArgumentException();

			if ( active )
				this.active.add( group );
			else
				this.active.remove( group );
		}

		public void addSourceToGroup( SourceAndConverter< ? > source, SourceGroup group )
		{
			if ( source == null || group == null )
				throw new IllegalArgumentException();

			final Set< SourceAndConverter< ? > > sources = contents.get( group );
			if ( sources == null )
				throw new IllegalArgumentException();

			sources.add( source );
		}

		public boolean removeSourceFromGroup( SourceAndConverter< ? > source, SourceGroup group )
		{
			if ( source == null || group == null )
				throw new IllegalArgumentException();

			final Set< SourceAndConverter< ? > > sources = contents.get( group );
			if ( sources == null )
				throw new IllegalArgumentException();

			return sources.remove( source );
		}
	}

	public static class ViewerState
	{
		private final DefaultSources sources = new DefaultSources( this::getVisible, this::isVisible );

		private final DefaultSourceGroups groups = new DefaultSourceGroups();

		private DisplayMode displayMode;

		private int currentTimepoint;

		private boolean isPresent( final SourceAndConverter< ? > source )
		{
			return source.getSpimSource().isPresent( currentTimepoint );
		}

		private boolean isVisible( final SourceAndConverter< ? > source )
		{
			switch ( displayMode )
			{
			case SINGLE:
			default:
				return sources.isCurrent( source );
			case GROUP:
				final SourceGroup currentGroup = groups.getCurrent();
				return currentGroup != null && groups.getSourcesIn( currentGroup ).contains( source );
			case FUSED:
				return sources.isActive( source );
			case FUSEDGROUP:
				for ( SourceGroup group : groups.getActive() )
					if ( groups.getSourcesIn( group ).contains( source ) )
						return true;
				return false;
			}
		}

		private Set< SourceAndConverter< ? > > getVisible()
		{
			final Set< SourceAndConverter< ? > > visible = new HashSet<>();
			switch ( displayMode )
			{
			case SINGLE:
				final SourceAndConverter< ? > currentSource = sources.getCurrent();
				if ( currentSource != null && isPresent( currentSource ))
					visible.add( currentSource );
				break;
			case GROUP:
				final SourceGroup currentGroup = groups.getCurrent();
				if ( currentGroup != null )
					for ( SourceAndConverter< ? > source : groups.getSourcesIn( currentGroup ) )
						if ( isPresent( source ) )
							visible.add( source );
				break;
			case FUSED:
				for ( SourceAndConverter< ? > source : sources.getActive() )
					if ( isPresent( source ) )
						visible.add( source );
				break;
			case FUSEDGROUP:
				for ( SourceGroup group : groups.getActive() )
					for ( SourceAndConverter< ? > source : groups.getSourcesIn( group ) )
						if ( isPresent( source ) )
							visible.add( source );
				break;
			}
			return visible;
		}
	}
}
