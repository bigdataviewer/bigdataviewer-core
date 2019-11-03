package bdv.viewer.state.r;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

// TODO: move to Util
public class WrappedList< E > implements List< E >
{
	private final List< E > list;

	public WrappedList( List< E > list )
	{
		this.list = list;
	}

	@Override
	public int size()
	{
		return list.size();
	}

	@Override
	public boolean isEmpty()
	{
		return list.isEmpty();
	}

	@Override
	public boolean contains( final Object o )
	{
		return list.contains( o );
	}

	@Override
	public Iterator< E > iterator()
	{
		return list.iterator();
	}

	@Override
	public Object[] toArray()
	{
		return list.toArray();
	}

	@Override
	public < T > T[] toArray( final T[] a )
	{
		return list.toArray( a );
	}

	@Override
	public boolean add( final E e )
	{
		return list.add( e );
	}

	@Override
	public boolean remove( final Object o )
	{
		return list.remove( o );
	}

	@Override
	public boolean containsAll( final Collection< ? > c )
	{
		return list.containsAll( c );
	}

	@Override
	public boolean addAll( final Collection< ? extends E > c )
	{
		return list.addAll( c );
	}

	@Override
	public boolean addAll( final int index, final Collection< ? extends E > c )
	{
		return list.addAll( index, c );
	}

	@Override
	public boolean removeAll( final Collection< ? > c )
	{
		return list.removeAll( c );
	}

	@Override
	public boolean retainAll( final Collection< ? > c )
	{
		return list.retainAll( c );
	}

	@Override
	public void clear()
	{
		list.clear();
	}

	@Override
	public E get( final int index )
	{
		return list.get( index );
	}

	@Override
	public E set( final int index, final E element )
	{
		return list.set( index, element );
	}

	@Override
	public void add( final int index, final E element )
	{
		list.add( index, element );
	}

	@Override
	public E remove( final int index )
	{
		return list.remove( index );
	}

	@Override
	public int indexOf( final Object o )
	{
		return list.indexOf( o );
	}

	@Override
	public int lastIndexOf( final Object o )
	{
		return list.lastIndexOf( o );
	}

	@Override
	public ListIterator< E > listIterator()
	{
		return list.listIterator();
	}

	@Override
	public ListIterator< E > listIterator( final int index )
	{
		return list.listIterator( index );
	}

	@Override
	public List< E > subList( final int fromIndex, final int toIndex )
	{
		return list.subList( fromIndex, toIndex );
	}

	@Override
	public String toString()
	{
		return list.toString();
	}

	@Override
	public int hashCode()
	{
		return list.hashCode();
	}

	@Override
	public boolean equals( final Object obj )
	{
		return list.equals( obj );
	}
}
