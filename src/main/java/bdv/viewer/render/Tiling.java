/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class Tiling
{
	/**
	 * Do not subdivide tiles below (or equal to) this size
	 */
	public static int MIN_TILE_SIZE = 8 * 8;

	/**
	 * Subdivide tiles for parallelization until they have this size approximately
	 */
	public static int MAX_TILE_SIZE = 32 * 32;

	/**
	 * If per-source render tasks in AccumulateProjector contain less target
	 * pixels than this, do not fork them but run sequentially inline.
	 */
	public static int MIN_ACCUMULATE_FORK_SIZE = 8 * 8;

	/**
	 * Compute a list of {@code Tile}s that split the render area.
	 * <p>
	 * The algorithm for splitting is roughly as follows (but may change in the future).
	 * <pre>{@code
	 * Recursively split each tile as follows:
	 * Find a preferred splitting direction as the dimension in which the tile is larger.
	 * Try to split along the preferred direction: (assume X)
	 * 		create int[] splitCandidates, order, make unique, and clip to tile bounds in X
	 *     	if candidates not empty, select the middle element as the new clip bound
	 *     	create two new TileSplits:
	 *          lowerSplit with bounds
	 *          	lowerSplit.minX = tile.minX
	 *          	lowerSplit.maxX = bound - 1
	 *          upperSplit with bounds
	 *          	upperSplit.minX = bound
	 *          	upperSplit.maxX = tile.maxX
	 *     	for each source:
	 * 			if (source.maxX < bound)
	 *          	add to lowerSplit
	 * 			else if (source.minX >= bound
	 * 				add to upperSplit
	 * 			else
	 * 				add to both lowerSplit and upperSplit
	 * 		recursively call split(lowerSplit) and split(upperSplit)
	 *      merge the resulting TileSplit lists and return
	 * Tf candidates is empty, try splitting along the other direction (same steps as above)
	 * Tf candidates for the second direction are also empty, return singleton list with the unsplit tile.
	 * }</pre>
	 */
	public static List< Tile > findTiles( final VisibleSourcesOnScreenBounds onScreenBounds )
	{
		final List< SourceBounds > bounds = onScreenBounds.sourceBoundsForVisibleSource();
		final List< SourceAndConverter< ? > > alwaysVisibleSources = onScreenBounds.alwaysVisibleSources();
		final Tile tile = new Tile( bounds, alwaysVisibleSources, onScreenBounds.screenInterval() );
		final List< Tile > tiles = split( tile );
		return tiles;
	}

	/**
	 * Recursively split the given {@code tile}.
	 */
	private static List< Tile > split( final Tile tile )
	{
		final int tileSizeX = tile.tileSizeX();
		final int tileSizeY = tile.tileSizeY();
		if ( tileSizeX * tileSizeY <= MIN_TILE_SIZE )
			return Collections.singletonList( tile );

		final Function< Tile[], List< Tile >> splitRecursively = tiles -> {
			final List< Tile > result = new ArrayList<>( split( tiles[ 0 ] ) );
			result.addAll( split( tiles[ 1 ] ) );
			return result;
		};

		// find preferred dimension to split: try the larger dimension first ...
		int dimension = tileSizeX > tileSizeY ? 0 : 1;
		int[] candidates = tile.splitCandidates( dimension );
		if ( candidates.length > 0 )
		{
			final int bound = candidates[ candidates.length / 2 ];
			return splitRecursively.apply( split( tile, bound, dimension ) );
		}

		// ... then try the other dimension ...
		dimension = tileSizeX > tileSizeY ? 1 : 0;
		candidates = tile.splitCandidates( dimension );
		if ( candidates.length > 0 )
		{
			final int bound = candidates[ candidates.length / 2 ];
			return splitRecursively.apply( split( tile, bound, dimension ) );
		}

		return Collections.singletonList( tile );
	}

	/**
	 * For concurrent rendering, further split tiles until all tiles are below
	 * MAX_TILE_SIZE. (Approximate. Tiles may be above threshold, because we
	 * never split along X).
	 *
	 * @param tiles list of tiles split along source bounds
	 * @return list of tiles further split for concurrent rendering
	 */
	public static List< Tile > splitForRendering( List< Tile > tiles )
	{
		final List< Tile > result = new ArrayList<>();
		for ( final Tile tile : tiles )
			splitForTargetSizeY( tile, MAX_TILE_SIZE, result );
		return result;
	}

	/**
	 * Splits tile into a list of tiles that are below {@code targetSize}.
	 *
	 * Always splits along Y, so tiles may actually be a bit larger than {@code
	 * targetSize}, if a single line is already larger.
	 */
	private static void splitForTargetSizeY( final Tile tile, final int targetSize, final List< Tile > splitTiles )
	{
		final int tileSizeX = tile.tileSizeX();
		final int tileSizeY = tile.tileSizeY();
		final int size = tileSizeX * tileSizeY;
		if ( size < targetSize )
		{
			splitTiles.add( tile );
			return;
		}

		final double numTiles = Math.ceil( ( double ) size / targetSize );
		final int h = ( int ) Math.ceil( tileSizeY / numTiles );

		final List< SourceBounds > bounds = tile.sourceBounds();
		final List< SourceAndConverter< ? > > alwaysVisibleSources = tile.alwaysVisibleSources();
		final int minX = tile.tileMinX();
		final int maxX = tile.tileMaxX();

		for ( int y = 0; y < tileSizeY; y += h )
		{
			final int minY = tile.tileMinY() + y;
			final int maxY = Math.min( tile.tileMaxY(), minY + h - 1 );
			splitTiles.add( new Tile( bounds, alwaysVisibleSources, minX, minY, maxX, maxY ) );
		}
	}

	/**
	 * Splits tile into a list of tiles that are below {@code targetSize}.
	 *
	 * Recursively splits along
	 */
	private static void splitForTargetSize( final Tile tile, final int targetSize, final List< Tile > splitTiles )
	{
		final int tileSizeX = tile.tileSizeX();
		final int tileSizeY = tile.tileSizeY();
		final int size = tileSizeX * tileSizeY;
		if ( size < targetSize )
		{
			splitTiles.add( tile );
			return;
		}

		final int dimension;
		final int bound;
		if ( tileSizeX > tileSizeY )
		{
			dimension = 0;
			bound = tile.tileMinX() + tileSizeX / 2;
		}
		else
		{
			dimension = 1;
			bound = tile.tileMinY() + tileSizeY / 2;
		}
		final Tile[] tiles = split( tile, bound, dimension );
		splitForTargetSize( tiles[ 0 ], targetSize, splitTiles );
		splitForTargetSize( tiles[ 1 ], targetSize, splitTiles );
	}

	/**
	 * Split {@code tile} into lower and upper tiles, at the given {@code bound} in dimension {@code d}.
	 *
	 * @param tile
	 * 		the tile to split
	 * @param bound
	 * 		where to split. {@code bound} is the min of the upper tile. {@code bound - 1} is the max of the lower tile.
	 * @param d
	 * 		if {@code d==0} split along X axis, otherwise split along Y axis.
	 *
	 * @return a {@code Tile[2]} array containing the lower and upper tiles.
	 */
	private static Tile[] split( final Tile tile, final int bound, final int d )
	{
		final List< SourceBounds > lowerSourceBounds = new ArrayList<>();
		final List< SourceBounds > upperSourceBounds = new ArrayList<>();
		final ToIntFunction< SourceBounds > min = d == 0 ? SourceBounds::minX : SourceBounds::minY;
		final ToIntFunction< SourceBounds > max = d == 0 ? SourceBounds::maxX : SourceBounds::maxY;
		for ( SourceBounds bounds : tile.sourceBounds() )
		{
			if ( max.applyAsInt( bounds ) < bound )
			{
				lowerSourceBounds.add( bounds );
			}
			else if ( min.applyAsInt( bounds ) >= bound )
			{
				upperSourceBounds.add( bounds );
			}
			else
			{
				lowerSourceBounds.add( bounds );
				upperSourceBounds.add( bounds );
			}
		}

		final int lowerMinX = tile.tileMinX();
		final int lowerMinY = tile.tileMinY();
		final int lowerMaxX = d == 0 ? bound - 1 : tile.tileMaxX();
		final int lowerMaxY = d == 1 ? bound - 1 : tile.tileMaxY();

		final int upperMinX = d == 0 ? bound : tile.tileMinX();
		final int upperMinY = d == 1 ? bound : tile.tileMinY();
		final int upperMaxX = tile.tileMaxX();
		final int upperMaxY = tile.tileMaxY();

		return new Tile[] {
				new Tile( lowerSourceBounds, tile.alwaysVisibleSources(), lowerMinX, lowerMinY, lowerMaxX, lowerMaxY ),
				new Tile( upperSourceBounds, tile.alwaysVisibleSources(), upperMinX, upperMinY, upperMaxX, upperMaxY ) };
	}
}
