package bdv.viewer.state.r;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A {@link Set} wrapper that forwards all calls to another {@link Set}.
 *
 * @author Tobias Pietzsch
 */
// TODO: move to Util
public class WrappedSet< E > implements Set< E >
{
	private final Set< E > set;

	public WrappedSet( final Set< E > list )
	{
		this.set = list;
	}

	@Override
	public int size()
	{
		return set.size();
	}

	@Override
	public boolean isEmpty()
	{
		return set.isEmpty();
	}

	@Override
	public boolean contains( final Object o )
	{
		return set.contains( o );
	}

	@Override
	public Iterator< E > iterator()
	{
		return set.iterator();
	}

	@Override
	public Object[] toArray()
	{
		return set.toArray();
	}

	@Override
	public < T > T[] toArray( final T[] a )
	{
		return set.toArray( a );
	}

	@Override
	public boolean add( final E e )
	{
		return set.add( e );
	}

	@Override
	public boolean remove( final Object o )
	{
		return set.remove( o );
	}

	@Override
	public boolean containsAll( final Collection< ? > c )
	{
		return set.containsAll( c );
	}

	@Override
	public boolean addAll( final Collection< ? extends E > c )
	{
		return set.addAll( c );
	}

	@Override
	public boolean retainAll( final Collection< ? > c )
	{
		return set.retainAll( c );
	}

	@Override
	public boolean removeAll( final Collection< ? > c )
	{
		return set.removeAll( c );
	}

	@Override
	public void clear()
	{
		set.clear();
	}

	@Override
	public String toString()
	{
		return set.toString();
	}

	@Override
	public int hashCode()
	{
		return set.hashCode();
	}

	@Override
	public boolean equals( final Object obj )
	{
		return set.equals( obj );
	}
}
