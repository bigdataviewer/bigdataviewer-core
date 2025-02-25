/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.viewer.render;

import bdv.viewer.SourceAndConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.imglib2.Interval;

/**
 * A tile of the rendered image.
 * <p>
 * It has a 2D bounding box, and contains the list of {@code SourceBounds} of all
 * sources that are overlapping the tile (sources that have to be considered for
 * rendering).
 * <p>
 * {@link #splitCandidates(int)} computes a list of positions where it would make
 * sense to further split this tile.
 */
class Tile
{
	// sources that are visible in this tile
	private final List< SourceBounds > bounds;

	// sources that are visible in all tiles
	private final List< SourceAndConverter< ? > > alwaysVisibleSources;

	private final int tileMinX;
	private final int tileMinY;
	private final int tileMaxX;
	private final int tileMaxY;

	private List< SourceAndConverter< ? > > sources;

	Tile( final List< SourceBounds > bounds,
			final List< SourceAndConverter< ? > > alwaysVisibleSources,
			final int tileMinX,
			final int tileMinY,
			final int tileMaxX,
			final int tileMaxY )
	{
		this.bounds = bounds;
		this.alwaysVisibleSources = alwaysVisibleSources;
		this.tileMinX = tileMinX;
		this.tileMinY = tileMinY;
		this.tileMaxX = tileMaxX;
		this.tileMaxY = tileMaxY;
	}

	Tile( final List< SourceBounds > bounds,
			final List< SourceAndConverter< ? > > alwaysVisibleSources,
			final Interval interval )
	{
		this( bounds, alwaysVisibleSources,
				( int ) interval.min( 0 ),
				( int ) interval.min( 1 ),
				( int ) interval.max( 0 ),
				( int ) interval.max( 1 ) );
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder( "TileSplit{" );
		sb.append( "(" ).append( tileMinX );
		sb.append( ", " ).append( tileMinY );
		sb.append( ") -- (" ).append( tileMaxX );
		sb.append( ", " ).append( tileMaxY );
		sb.append( "), " );
		sb.append( "bounds=" ).append( bounds );
		sb.append( '}' );
		return sb.toString();
	}

	public List< SourceBounds > sourceBounds()
	{
		return bounds;
	}

	public List< SourceAndConverter< ? > > alwaysVisibleSources()
	{
		return alwaysVisibleSources;
	}

	public List< SourceAndConverter< ? > > sources()
	{
		if ( sources == null )
		{
			final List< SourceAndConverter< ? > > sources = new ArrayList<>( bounds.size() + alwaysVisibleSources.size() );
			for ( SourceBounds sourceBounds : bounds )
			{
				SourceAndConverter< ? > source = sourceBounds.source();
				sources.add( source );
			}
			sources.addAll( alwaysVisibleSources );
			this.sources = sources;
		}
		return sources;
	}

	public int tileMinX()
	{
		return tileMinX;
	}

	public int tileMinY()
	{
		return tileMinY;
	}

	public int tileMaxX()
	{
		return tileMaxX;
	}

	public int tileMaxY()
	{
		return tileMaxY;
	}

	public int tileSizeX()
	{
		return tileMaxX - tileMinX + 1;
	}

	public int tileSizeY()
	{
		return tileMaxY - tileMinY + 1;
	}

	/**
	 * Compute candidate coordinates along dimension {@code d}, where it would make
	 * sense to split this tile. These are all min and max+1 bounds (in dimension
	 * {@code d}) of sources in this tile, which fall inside the tile bounding box.
	 * <p>
	 * It makes sense to split at the min bound of a source, because then the
	 * source will not overlap with the lower tile (which has tileMaxD == bound).
	 * It makes sense to split at the max+1 bound of a source, because then the
	 * source will not overlap with the upper tile (which has tileMinD == bound).
	 * <p>
	 * The returned list of split candidates is ordered, does not contain duplicates,
	 * and only contains split positions that fall inside this tile's bounds.
	 *
	 * @param d
	 * 		the dimension along which to compute split candidates, {@code d==0} or {@code d==1}.
	 *
	 * @return ordered list of split coordinates without duplicates
	 */
	public int[] splitCandidates( final int d )
	{
		int[] candidates = new int[ 2 * bounds.size() ];
		int i = 0;
		if ( d == 0 )
		{
			for ( SourceBounds bounds : bounds )
			{
				i = addIfInBounds( bounds.minX(), candidates, i, tileMinX, tileMaxX );
				i = addIfInBounds( bounds.maxX() + 1, candidates, i, tileMinX, tileMaxX );
			}
		}
		else
		{
			for ( SourceBounds bounds : bounds )
			{
				i = addIfInBounds( bounds.minY(), candidates, i, tileMinY, tileMaxY );
				i = addIfInBounds( bounds.maxY() + 1, candidates, i, tileMinY, tileMaxY );
			}
		}
		final int length = i;
		Arrays.sort( candidates, 0, length );

		// remove duplicates
		int j = 0;
		for ( i = 0; i < length; i++ )
		{
			final int value = candidates[ i ];
			if ( j == 0 || candidates[ j - 1 ] != value )
				candidates[ j++ ] = value;
		}
		return Arrays.copyOf( candidates, j );
	}

	private static int addIfInBounds( final int value, final int[] splitCandidates, final int i, final int tileMin, final int tileMax )
	{
		if ( value <= tileMin || value > tileMax )
			return i;

		splitCandidates[ i ] = value;
		return i + 1;
	}
}
