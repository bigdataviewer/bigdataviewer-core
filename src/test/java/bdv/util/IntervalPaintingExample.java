/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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

import bdv.viewer.OverlayRenderer;
import bdv.viewer.ViewerPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RealPositionable;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.position.transform.Round;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;
import static net.imglib2.cache.img.optional.CacheOptions.CacheType.BOUNDED;

/**
 * Shows a disk-cached checkerboard image.
 * A drag gesture with {@code SPACE} or {@code D} shortcut can be used to interactively draw 3D spheres into the image.
 * The new {@code ViewerPanel.requestRepaint(Interval)} method is used to selectively update the BDV for the modified regions.
 */
public class IntervalPaintingExample
{
	private static final int radius = 10;

	public static void main( final String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final int[] cellDimensions = new int[] { 64, 64, 64 };
		final long[] dimensions = new long[] { 640, 640, 640 };

		final DiskCachedCellImgOptions options = options()
				.cellDimensions( cellDimensions )
				.cacheType( BOUNDED )
				.maxCacheSize( 1000 );

		final CellLoader< ARGBType > loader = new CheckerboardLoader( new CellGrid( dimensions, cellDimensions ) );

		final Img< ARGBType > img = new DiskCachedCellImgFactory<>( new ARGBType(), options ).create(
				dimensions,
				loader );

		new IntervalPaintingExample( img );
	}

	private final ViewerPanel viewer;

	private final RandomAccess< Neighborhood< ARGBType > > sphere;

	private final RealPositionable roundpos;

	public IntervalPaintingExample( final Img< ARGBType > img )
	{
		final Bdv bdv = BdvFunctions.show( img, "IntervalPaintingExample" );

		viewer = bdv.getBdvHandle().getViewerPanel();
		sphere = new HyperSphereShape( radius ).neighborhoodsRandomAccessible( Views.extendZero( img ) ).randomAccess();
		roundpos = new Round<>( sphere );

		/*
		 * Install behaviour for painting into img with shortcut "D" or "SPACE"
		 */

		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "paint" );
		behaviours.behaviour( new PaintBehaviour(), "paint", "D", "SPACE" );

		/*
		 * Install overly to highlight painted region
		 */

		viewer.getDisplay().overlays().add( new Overlay() );
	}

	private Interval repaintInterval;

	private synchronized void drawOverlay( final Graphics2D g )
	{
		if ( repaintInterval != null )
		{
			g.setColor( Color.green );
			final int x = ( int ) repaintInterval.min( 0 );
			final int y = ( int ) repaintInterval.min( 1 );
			final int w = ( int ) repaintInterval.dimension( 0 );
			final int h = ( int ) repaintInterval.dimension( 1 );
			g.drawRect( x, y, w, h );
		}
	}

	private synchronized void draw( final int x, final int y )
	{
		if ( x < 0 || y < 0 )
		{
			repaintInterval = null;
			viewer.getDisplay().repaint();
		}
		else
		{
			viewer.displayToGlobalCoordinates( x, y, roundpos );
			sphere.get().forEach( t -> t.set( 0xFFFF0000 ) );

			final double scale = Affine3DHelpers.extractScale( viewer.state().getViewerTransform(), 0 );
			final int w = ( int ) Math.ceil( scale * radius ) + 3;
			repaintInterval = Intervals.createMinMax( x - w, y - w, x + w, y + w );
			viewer.requestRepaint( repaintInterval );
		}
	}

	private class PaintBehaviour implements DragBehaviour
	{
		@Override
		public void init( final int x, final int y )
		{
			draw( x, y );
		}

		@Override
		public void drag( final int x, final int y )
		{
			draw( x, y );
		}

		@Override
		public void end( final int x, final int y )
		{
			draw( -1, -1 );
		}
	}

	private class Overlay implements OverlayRenderer
	{
		@Override
		public void drawOverlays( final Graphics g )
		{
			drawOverlay( ( Graphics2D ) g );
		}

		@Override
		public void setCanvasSize( final int width, final int height )
		{
		}
	}

	static class CheckerboardLoader implements CellLoader< ARGBType >
	{
		private final CellGrid grid;

		public CheckerboardLoader( final CellGrid grid )
		{
			this.grid = grid;
		}

		@Override
		public void load( final SingleCellArrayImg< ARGBType, ? > cell ) throws Exception
		{
			final int n = grid.numDimensions();
			long sum = 0;
			for ( int d = 0; d < n; ++d )
				sum += cell.min( d ) / grid.cellDimension( d );
			final int color = ( sum % 2 == 0 ) ? 0xff000000 : 0xff008800;
			cell.forEach( t -> t.set( color ) );
		}
	}
}
