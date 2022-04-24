package bdv.viewer.render;

import bdv.viewer.OverlayRenderer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;

/**
 * Overlay to show how the display is tiled for rendering.
 * (for debugging purposes.)
 */
public class DebugTilingOverlay implements OverlayRenderer
{
	private volatile Tiles tiling;

	private volatile boolean showTiles = true;

	private volatile long renderTime;

	private volatile double renderTimePerPixelAndSourceAverage;

	public DebugTilingOverlay( final MultiResolutionRenderer renderer )
	{
		renderer.debugTileOverlay = this;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Tiles tiling = this.tiling;
		if ( tiling != null )
		{
			final List< Tile > tiles = tiling.tiles;

			if ( showTiles )
			{
				final double s = 1.0 / tiling.scale;
				final int offsetX = tiling.offsetX;
				final int offsetY = tiling.offsetY;
				if ( tiles != null )
				{
					g.setColor( Color.GREEN );
					g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
					int i = 0;
					for ( Tile tile : tiles )
					{
						final int x0 = ( int ) ( ( offsetX + tile.tileMinX() ) * s );
						final int y0 = ( int ) ( ( offsetY + tile.tileMinY() ) * s );
						final int x1 = ( int ) ( ( offsetX + tile.tileMaxX() + 1 ) * s );
						final int y1 = ( int ) ( ( offsetY + tile.tileMaxY() + 1 ) * s );
						g.drawLine( x0, y0, x1, y0 );
						g.drawLine( x1, y0, x1, y1 );
						g.drawLine( x1, y1, x0, y1 );
						g.drawLine( x0, y1, x0, y0 );
						g.drawString( String.format( "%d, %d", i++, tile.sources().size() ), x0 + 5, y0 + 17 );
					}
				}
			}

			int maxSourcesPerTile = 0;
			if ( tiles != null )
				for ( Tile tile : tiles )
					maxSourcesPerTile = Math.max( maxSourcesPerTile, tile.sources().size() );

			g.setColor( Color.WHITE );
			g.setFont( new Font( "Monospaced", Font.BOLD, 14 ) );
			g.drawString( String.format( "%d tiles", tiles.size() ), ( int ) g.getClipBounds().getWidth() - 180, ( int ) g.getClipBounds().getHeight() - 80 );
			g.drawString( String.format( "%d max sources per tile", maxSourcesPerTile ), ( int ) g.getClipBounds().getWidth() - 180, ( int ) g.getClipBounds().getHeight() - 60 );
			g.drawString( String.format( "%.1f ms", renderTime / 1_000_000.0 ), ( int ) g.getClipBounds().getWidth() - 180, ( int ) g.getClipBounds().getHeight() - 40 );
			g.drawString( String.format( "%.1f ns", renderTimePerPixelAndSourceAverage ), ( int ) g.getClipBounds().getWidth() - 180, ( int ) g.getClipBounds().getHeight() - 20 );
		}
	}

	public void setTiling( final List< Tile > tiles, final double scale, final int offsetX, final int offsetY )
	{
		this.tiling = new Tiles( tiles, scale, offsetX, offsetY );
	}

	public boolean getShowTiles()
	{
		return showTiles;
	}

	public void setShowTiles( final boolean showTiles )
	{
		this.showTiles = showTiles;
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
	}

	public void setRenderTime( final long rendertime )
	{
		this.renderTime = rendertime;
	}

	public void setRenderTimePerPixelAndSource( final double renderTimePerPixelAndSourceAverage )
	{
		this.renderTimePerPixelAndSourceAverage = renderTimePerPixelAndSourceAverage;
	}

	public static class Tiles
	{
		final List< Tile > tiles;
		final double scale;
		final int offsetX;
		final int offsetY;

		public Tiles( final List< Tile > tiles, final double scale, final int offsetX, final int offsetY )
		{
			this.tiles = tiles;
			this.scale = scale;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
	}
}
