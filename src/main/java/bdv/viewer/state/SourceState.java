package bdv.viewer.state;

import net.imglib2.Volatile;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;

/**
 * Source with some attached state needed for rendering.
 */
public class SourceState< T > extends SourceAndConverter< T >
{
	protected static class Data
	{
		/**
		 * Whether the source is active (visible in  {@link DisplayMode#FUSED} mode).
		 */
		protected boolean isActive;

		/**
		 * Whether the source is current.
		 */
		protected boolean isCurrent;

		public Data()
		{
			isActive = true;
			isCurrent = false;
		}

		protected Data( final Data d )
		{
			isActive = d.isActive;
			isCurrent = d.isCurrent;
		}

		public Data copy()
		{
			return new Data( this );
		}
	}

	static class VolatileSourceState< T, V extends Volatile< T > > extends SourceState< V >
	{
		public VolatileSourceState( final SourceAndConverter< V > soc, final ViewerState owner, final Data data )
		{
			super( soc, owner, data );
		}

		public static < T, V extends Volatile< T > > VolatileSourceState< T, V > create( final SourceAndConverter< V > soc, final ViewerState owner, final Data data )
		{
			if ( soc == null )
				return null;
			else
				return new VolatileSourceState< T, V >( soc, owner, data );
		}
	}

	final ViewerState owner;

	final Data data;

	final VolatileSourceState< T, ? extends Volatile< T > > volatileSourceState;

	public SourceState( final SourceAndConverter< T > soc, final ViewerState owner )
	{
		super( soc );
		data = new Data();
		this.owner = owner;
		volatileSourceState = VolatileSourceState.create( soc.asVolatile(), owner, data );
	}

	protected SourceState( final SourceAndConverter< T > soc, final ViewerState owner, final Data data )
	{
		super( soc );
		this.data = data;
		this.owner = owner;
		volatileSourceState = VolatileSourceState.create( soc.asVolatile(), owner, data );
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected SourceState( final SourceState< T > s, final ViewerState owner )
	{
		super( s );
		data = s.data.copy();
		this.owner = owner;
		volatileSourceState = VolatileSourceState.create( s.volatileSourceAndConverter, owner, data );
	}

	public SourceState< T > copy( final ViewerState owner )
	{
		return new SourceState< T >( this, owner );
	}

	/**
	 * Is the source is active (visible in fused mode)?
	 *
	 * @return whether the source is active.
	 */
	public boolean isActive()
	{
		return data.isActive;
	}

	/**
	 * Set the source active (visible in fused mode) or inactive
	 */
	public void setActive( final boolean isActive )
	{
		synchronized ( owner )
		{
			data.isActive = isActive;
		}
	}

	/**
	 * Is this source the current source?
	 *
	 * @return whether the source is current.
	 */
	public boolean isCurrent()
	{
		return data.isCurrent;
	}

	/**
	 * Set this source current (or not).
	 */
	public void setCurrent( final boolean isCurrent )
	{
		synchronized ( owner )
		{
			data.isCurrent = isCurrent;
		}
	}

	/**
	 * Create a {@link SourceState} from a {@link SourceAndConverter}.
	 */
	public static < T > SourceState< T > create( final SourceAndConverter< T > soc, final ViewerState owner )
	{
		return new SourceState< T >( soc, owner );
	}

	@Override
	public SourceState< ? extends Volatile< T > > asVolatile()
	{
		return volatileSourceState;
	}
}

