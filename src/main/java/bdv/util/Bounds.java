/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
package bdv.util;

/**
 * A range with {@code minBound <= maxBound}.
 * <p>
 * {@link #join(Bounds) join(other)} will derive a new {@code Bounds}, with
 * {@code minBound} the minimum of {@code this.getMinBound()} and
 * {@code other.getMinBound()} and so on.
 *
 * @author Tobias Pietzsch
 */
public final class Bounds
{
	private final double minBound;
	private final double maxBound;

	public Bounds( final double minBound, final double maxBound )
	{
		if ( minBound > maxBound )
			throw new IllegalArgumentException();

		this.minBound = minBound;
		this.maxBound = maxBound;
	}

	public double getMinBound()
	{
		return minBound;
	}

	public double getMaxBound()
	{
		return maxBound;
	}

	public Bounds join( final Bounds other )
	{
		final double newMinBound = Math.min( minBound, other.minBound );
		final double newMaxBound = Math.max( maxBound, other.maxBound );
		return new Bounds( newMinBound, newMaxBound );
	}

	@Override
	public String toString()
	{
		return "Bounds[ " + minBound + ", " + maxBound + " ]";
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o )
			return true;
		if ( o == null || getClass() != o.getClass() )
			return false;

		final Bounds that = ( Bounds ) o;

		if ( Double.compare( that.minBound, minBound ) != 0 )
			return false;
		return Double.compare( that.maxBound, maxBound ) == 0;
	}

	@Override
	public int hashCode()
	{
		int result;
		long temp;
		temp = Double.doubleToLongBits( minBound );
		result = ( int ) ( temp ^ ( temp >>> 32 ) );
		temp = Double.doubleToLongBits( maxBound );
		result = 31 * result + ( int ) ( temp ^ ( temp >>> 32 ) );
		return result;
	}
}
