/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.viewer.state;

import net.imglib2.Volatile;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;

/**
 * Source with some attached state needed for rendering.
 */
@Deprecated
public class SourceState< T > extends SourceAndConverter< T >
{
	static class VolatileSourceState< T, V extends Volatile< T > > extends SourceState< V >
	{
		public VolatileSourceState( final SourceAndConverter< V > soc, final ViewerState owner, final SourceAndConverter< ? > handle )
		{
			super( soc, owner, handle );
		}

		public static < T, V extends Volatile< T > > VolatileSourceState< T, V > create( final SourceAndConverter< V > soc, final ViewerState owner, final SourceAndConverter< ? > handle )
		{
			if ( soc == null )
				return null;
			else
				return new VolatileSourceState<>( soc, owner, handle );
		}
	}

	final ViewerState owner;

	final SourceAndConverter< ? > handle;

	final VolatileSourceState< T, ? extends Volatile< T > > volatileSourceState;

	public SourceState( final SourceAndConverter< T > soc, final ViewerState owner )
	{
		super( soc );
		this.owner = owner;
		handle = soc;
		volatileSourceState = VolatileSourceState.create( soc.asVolatile(), owner, handle );
	}

	protected SourceState( final SourceAndConverter< T > soc, final ViewerState owner, final SourceAndConverter< ? > handle )
	{
		super( soc );
		this.owner = owner;
		this.handle = handle;
		volatileSourceState = VolatileSourceState.create( soc.asVolatile(), owner, handle );
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected SourceState( final SourceState< T > s, final ViewerState owner )
	{
		super( s );
		this.owner = owner;
		handle = s.handle;
		volatileSourceState = VolatileSourceState.create( s.volatileSourceAndConverter, owner, handle );
	}

	public SourceState< T > copy( final ViewerState owner )
	{
		return new SourceState<>( this, owner );
	}

	/**
	 * Is the source is active (visible in fused mode)?
	 *
	 * @return whether the source is active.
	 */
	public boolean isActive()
	{
		return owner.state.isSourceActive( handle );
	}

	/**
	 * Set the source active (visible in fused mode) or inactive
	 */
	public void setActive( final boolean isActive )
	{
		synchronized ( owner )
		{
			owner.state.setSourceActive( handle, isActive );
		}
	}

	/**
	 * Is this source the current source?
	 *
	 * @return whether the source is current.
	 */
	public boolean isCurrent()
	{
		return owner.state.isCurrentSource( handle );
	}

	/**
	 * Set this source current (or not).
	 */
	public void setCurrent( final boolean isCurrent )
	{
		synchronized ( owner )
		{
			owner.state.setCurrentSource( handle );
		}
	}

	/**
	 * Create a {@link SourceState} from a {@link SourceAndConverter}.
	 */
	public static < T > SourceState< T > create( final SourceAndConverter< T > soc, final ViewerState owner )
	{
		return new SourceState<>( soc, owner );
	}

	@Override
	public SourceState< ? extends Volatile< T > > asVolatile()
	{
		return volatileSourceState;
	}
}

