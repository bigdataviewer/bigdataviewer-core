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
		public VolatileSourceState( final SourceAndConverter< V > soc, final Data data )
		{
			super( soc, data );
		}

		public static < T, V extends Volatile< T > > VolatileSourceState< T, V > create( final SourceAndConverter< V > soc, final Data data )
		{
			if ( soc == null )
				return null;
			return new VolatileSourceState< T, V >( soc, data );
		}
	}

	final Data data;

	final VolatileSourceState< T, ? extends Volatile< T > > volatileSourceState;

	public SourceState( final SourceAndConverter< T > soc )
	{
		super( soc );
		data = new Data();
		volatileSourceState = VolatileSourceState.create( soc.asVolatile(), data );
	}

	protected SourceState( final SourceAndConverter< T > soc, final Data data )
	{
		super( soc );
		this.data = data;
		volatileSourceState = VolatileSourceState.create( soc.asVolatile(), data );
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected SourceState( final SourceState< T > s )
	{
		super( s );
		data = s.data.copy();
		volatileSourceState = VolatileSourceState.create( s.volatileSourceAndConverter, data );
	}

	public SourceState< T > copy()
	{
		return new SourceState< T >( this );
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
		data.isActive = isActive;
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
		data.isCurrent = isCurrent;
	}

	/**
	 * Create a {@link SourceState} from a {@link SourceAndConverter}.
	 */
	public static < T > SourceState< T > create( final SourceAndConverter< T > soc )
	{
		return new SourceState< T >( soc );
	}

	@Override
	public SourceState< ? extends Volatile< T > > asVolatile()
	{
		return volatileSourceState;
	}
}

