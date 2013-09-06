package viewer.render;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class SourceGroup
{
	public interface UpdateListener
	{
		public void update();
	}

	protected final TreeSet< Integer > sourceIds;

	protected final SortedSet< Integer > unmodifiableSourceIds;

	protected final CopyOnWriteArrayList< UpdateListener > updateListeners;

	protected String name;

	protected boolean active;

	public SourceGroup()
	{
		sourceIds = new TreeSet< Integer >();
		unmodifiableSourceIds = Collections.unmodifiableSortedSet( sourceIds );
		updateListeners = new CopyOnWriteArrayList< UpdateListener >();
		name = "group"; // TODO
		active = true;
	}

	public synchronized void addSource( final int sourceId )
	{
		sourceIds.add( sourceId );
		update();
	}

	public synchronized void removeSource( final int sourceId )
	{
		sourceIds.remove( sourceId );
		update();
	}

	public synchronized SortedSet< Integer > getSourceIds()
	{
		return unmodifiableSourceIds;
	}

	public String getName()
	{
		return name;
	}

	public void setName( final String name )
	{
		this.name = name;
	}

	protected void update()
	{
		for ( final UpdateListener l : updateListeners )
			l.update();
	}

	public void addUpdateListener( final UpdateListener l )
	{
		updateListeners.add( l );
	}

	public void removeUpdateListener( final UpdateListener l )
	{
		updateListeners.remove( l );
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive( final boolean active )
	{
		this.active = active;
	}
}
