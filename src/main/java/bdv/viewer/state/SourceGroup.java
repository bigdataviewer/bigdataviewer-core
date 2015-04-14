package bdv.viewer.state;

import java.util.SortedSet;
import java.util.TreeSet;

import bdv.viewer.DisplayMode;

/**
 * TODO
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class SourceGroup
{
	final ViewerState owner;

	protected final TreeSet< Integer > sourceIds;

	protected String name;

	/**
	 * Whether the group is active (visible in {@link DisplayMode#FUSED} mode).
	 */
	protected boolean isActive;

	/**
	 * Whether the group is current (only group visible in
	 * {@link DisplayMode#FUSED} mode).
	 */
	protected boolean isCurrent;

	public SourceGroup( final String name, final ViewerState owner )
	{
		this.owner = owner;
		sourceIds = new TreeSet< Integer >();
		this.name = name;
		isActive = true;
		isCurrent = false;
	}

	public SourceGroup( final SourceGroup g, final ViewerState owner )
	{
		this.owner = owner;
		sourceIds = new TreeSet< Integer >( g.sourceIds );
		name = g.name;
		isActive = g.isActive;
		isCurrent = g.isCurrent;
	}

	public SourceGroup copy( final ViewerState owner )
	{
		return new SourceGroup( this, owner );
	}

	public void addSource( final int sourceId )
	{
		synchronized ( owner )
		{
			sourceIds.add( sourceId );
		}
	}

	public void removeSource( final int sourceId )
	{
		synchronized ( owner )
		{
			sourceIds.remove( sourceId );
		}
	}

	public SortedSet< Integer > getSourceIds()
	{
		return sourceIds;
	}

	public String getName()
	{
		return name;
	}

	public void setName( final String name )
	{
		this.name = name;
	}

	/**
	 * Is the group active (visible in fused mode)?
	 *
	 * @return whether the source is active.
	 */
	public boolean isActive()
	{
		return isActive;
	}

	/**
	 * Set the group active (visible in fused mode) or inactive.
	 */
	public void setActive( final boolean isActive )
	{
		synchronized ( owner )
		{
			this.isActive = isActive;
		}
	}

	/**
	 * Is this group the current group?
	 *
	 * @return whether the group is current.
	 */
	public boolean isCurrent()
	{
		return isCurrent;
	}

	/**
	 * Set this group current (or not).
	 */
	public void setCurrent( final boolean isCurrent )
	{
		synchronized ( owner )
		{
			this.isCurrent = isCurrent;
		}
	}

}
