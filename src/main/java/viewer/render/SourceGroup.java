package viewer.render;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * TODO
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SourceGroup
{
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

	public SourceGroup( final String name )
	{
		sourceIds = new TreeSet< Integer >();
		this.name = name;
		isActive = true;
		isCurrent = false;
	}

	public SourceGroup( final SourceGroup g )
	{
		sourceIds = new TreeSet< Integer >( g.sourceIds );
		name = g.name;
		isActive = g.isActive;
		isCurrent = g.isCurrent;
	}

	public SourceGroup copy()
	{
		return new SourceGroup( this );
	}

	public synchronized void addSource( final int sourceId )
	{
		sourceIds.add( sourceId );
	}

	public synchronized void removeSource( final int sourceId )
	{
		sourceIds.remove( sourceId );
	}

	public synchronized SortedSet< Integer > getSourceIds()
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
		this.isActive = isActive;
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
		this.isCurrent = isCurrent;
	}

}
