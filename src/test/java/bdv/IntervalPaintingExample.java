/*-
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
package bdv;

import bdv.export.ProgressWriterConsole;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.Interval;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.util.Intervals;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

public class IntervalPaintingExample
{
	public static void main( final String[] args ) throws SpimDataException
	{
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_full.xml";
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final BigDataViewer bdv = BigDataViewer.open(
				fn,
				new File( fn ).getName(),
				new ProgressWriterConsole(),
				ViewerOptions.options() );
		new IntervalPaintingExample( bdv.getViewerFrame() );
	}

	public IntervalPaintingExample( final ViewerFrame viewerFrame )
	{
		this.viewer = viewerFrame.getViewerPanel();
		viewer.getDisplay().overlays().add( new Overlay() );

		final Behaviours behaviours = new Behaviours( new InputTriggerConfig(), "bdv" );
		behaviours.behaviour( new RepaintIntervalBehaviour(), "repaint interval", "SPACE" );
		behaviours.install( viewerFrame.getTriggerbindings(), "repaint" );
	}

	private final ViewerPanel viewer;

	private Interval repaintInterval;

	private final int halfw = 30;

	private final int halfh = 30;

	private synchronized void repaint( final int x, final int y )
	{
		if ( x < 0 || y < 0 )
			repaintInterval = null;
		else
			repaintInterval = Intervals.createMinMax( x - halfw, y - halfh, x + halfw, y + halfh );

		viewer.getDisplay().repaint();
	}

	private synchronized void drawOverlay( final Graphics2D g )
	{
		if ( repaintInterval != null )
		{
			g.setColor( Color.green );
			draw( g, Intervals.expand( repaintInterval, 10 ) );
			g.setColor( Color.red );
			draw( g, Intervals.expand( repaintInterval, -10 ) );
		}
	}

	private static void draw( final Graphics2D g, final Interval interval )
	{
		final int x = ( int ) interval.min( 0 );
		final int y = ( int ) interval.min( 1 );
		final int w = ( int ) interval.dimension( 0 );
		final int h = ( int ) interval.dimension( 1 );
		g.drawRect( x, y, w, h );
	}

	private class RepaintIntervalBehaviour implements DragBehaviour
	{
		@Override
		public void init( final int x, final int y )
		{
			repaint( x, y );
		}

		@Override
		public void drag( final int x, final int y )
		{
			repaint( x, y );
		}

		@Override
		public void end( final int x, final int y )
		{
			repaint( -1, -1 );
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
}
