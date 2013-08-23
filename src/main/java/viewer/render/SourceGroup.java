package viewer.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceGroup
{
	public interface UpdateListener
	{
		public void update();
	}

	protected final ArrayList< Integer > sourceIds;

	protected final ArrayList< UpdateListener > updateListeners;

	public SourceGroup()
	{
		sourceIds = new ArrayList< Integer >();
		updateListeners = new ArrayList< UpdateListener >();
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

	public synchronized List< Integer > getSourceIds()
	{
		return Collections.unmodifiableList( sourceIds );
	}

	protected void update()
	{
		for ( final UpdateListener l : updateListeners )
			l.update();
	}

	public synchronized void addUpdateListener( final UpdateListener l )
	{
		updateListeners.add( l );
	}

	public synchronized void removeUpdateListener( final UpdateListener l )
	{
		updateListeners.remove( l );
	}
}
