/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package bdv.tools.boundingbox;

import static bdv.tools.boundingbox.TransformedBoxOverlay.BoxDisplayMode.FULL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;

import org.scijava.listeners.ChangeListener;
import org.scijava.listeners.ListenableVar;

/**
 * A BDV {@link OverlayRenderer} showing a {@link TransformedBox}. Displays a
 * wireframe box and/or intersection with current viewer slice, with various
 * adjustable parameters.
 */
public class TransformedBoxOverlay implements OverlayRenderer, TransformListener< AffineTransform3D >
{
	private static final double DISTANCE_TOLERANCE = 20.;

	private static final double HANDLE_RADIUS = DISTANCE_TOLERANCE / 2.;

	/**
	 * Specifies whether to show 3D wireframe box ({@code FULL}), or only
	 * intersection with viewer plane ({@code SECTION}).
	 */
	public enum BoxDisplayMode
	{
		FULL, SECTION;
	}

	public interface HighlightedCornerListener
	{
		void highlightedCornerChanged();
	}

	private final TransformedBox bbSource;

	private final Color backColor = new Color( 0x00994499 );

	private final Color frontColor = Color.GREEN;

	private final Stroke normalStroke = new BasicStroke();

	private final Stroke intersectionStroke = new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 10f, 10f }, 0f );

	private final Color intersectionColor = Color.WHITE.darker();

	private Color intersectionFillColor = new Color( 0x88994499, true );

	private final AffineTransform3D viewerTransform;

	private final AffineTransform3D transform;

	final RenderBoxHelper renderBoxHelper;

	private final CornerHighlighter cornerHighlighter;

	private double sourceSize = 5000;

	private double perspective = 0.5;

	private int canvasWidth;

	private int canvasHeight;

	private final ListenableVar< BoxDisplayMode, ChangeListener > displayMode = ListenableVar.create( FULL );

	private boolean showCornerHandles = true;

	private boolean fillIntersection = true;

	private int cornerId = -1;

	private HighlightedCornerListener highlightedCornerListener;

	public TransformedBoxOverlay( final Interval interval )
	{
		this( new TransformedBox()
		{
			@Override
			public Interval getInterval()
			{
				return interval;
			}

			@Override
			public void getTransform( final AffineTransform3D transform )
			{
				transform.identity();
			}
		} );
	}

	public TransformedBoxOverlay( final TransformedBox bbSource )
	{
		this.bbSource = bbSource;

		viewerTransform = new AffineTransform3D();
		transform = new AffineTransform3D();
		renderBoxHelper = new RenderBoxHelper();
		cornerHighlighter = new CornerHighlighter( DISTANCE_TOLERANCE );
	}

	/**
	 * Sets the perspective value. {@code perspective < 0} means parallel
	 * projection.
	 *
	 * @param perspective
	 *            the perspective value.
	 */
	public void setPerspective( final double perspective )
	{
		this.perspective = perspective;
	}

	public void showCornerHandles( final boolean showCornerHandles )
	{
		this.showCornerHandles = showCornerHandles;
	}

	public void fillIntersection( final boolean fillIntersection )
	{
		this.fillIntersection = fillIntersection;
	}

	public boolean getFillIntersection()
	{
		return fillIntersection;
	}

	public void setIntersectionFillColor( final Color intersectionFillColor )
	{
		this.intersectionFillColor = intersectionFillColor;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;

		final GeneralPath front = new GeneralPath();
		final GeneralPath back = new GeneralPath();
		final GeneralPath intersection = new GeneralPath();

		final RealInterval interval = bbSource.getInterval();
		final double ox = canvasWidth / 2;
		final double oy = canvasHeight / 2;
		synchronized ( viewerTransform )
		{
			bbSource.getTransform( transform );
			transform.preConcatenate( viewerTransform );
		}
		renderBoxHelper.setPerspectiveProjection( perspective > 0 );
		renderBoxHelper.setDepth( perspective * sourceSize );
		renderBoxHelper.setOrigin( ox, oy );
		renderBoxHelper.setScale( 1 );
		renderBoxHelper.renderBox( interval, transform, front, back, intersection );

		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		if ( displayMode.get() == FULL )
		{
			graphics.setStroke( normalStroke );
			graphics.setPaint( backColor );
			graphics.draw( back );
		}

		if ( fillIntersection )
		{
			graphics.setPaint( intersectionFillColor );
			graphics.fill( intersection );
		}

		graphics.setPaint( intersectionColor );
		graphics.setStroke( intersectionStroke );
		graphics.draw( intersection );

		if ( displayMode.get() == FULL )
		{
			graphics.setStroke( normalStroke );
			graphics.setPaint( frontColor );
			graphics.draw( front );

			if ( showCornerHandles )
			{
				final int id = getHighlightedCornerIndex();
				if ( id >= 0 )
				{
					final double[] p = renderBoxHelper.projectedCorners[ id ];
					final Ellipse2D cornerHandle = new Ellipse2D.Double(
							p[ 0 ] - HANDLE_RADIUS,
							p[ 1 ] - HANDLE_RADIUS,
							2 * HANDLE_RADIUS, 2 * HANDLE_RADIUS );
					final double z = renderBoxHelper.corners[ cornerId ][ 2 ];
					final Color cornerColor = ( z > 0 ) ? backColor : frontColor;

					graphics.setColor( cornerColor );
					graphics.fill( cornerHandle );
					graphics.setColor( cornerColor.darker().darker() );
					graphics.draw( cornerHandle );
				}
			}
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.canvasWidth = width;
		this.canvasHeight = height;
	}

	@Override
	public void transformChanged( final AffineTransform3D t )
	{
		synchronized ( viewerTransform )
		{
			viewerTransform.set( t );
		}
	}

	/**
	 * Get/set {@code BoxDisplayMode}, which specifies whether to show 3D
	 * wireframe box ({@code FULL}), or only intersection with viewer plane
	 * ({@code SECTION}).
	 */
	public ListenableVar< BoxDisplayMode, ChangeListener > boxDisplayMode()
	{
		return displayMode;
	}

	/**
	 * Get the transformation from the local coordinate frame of the
	 * {@link TransformedBox} to viewer coordinates.
	 *
	 * @param t is set to the box-to-viewer transform.
	 */
	public void getBoxToViewerTransform( final AffineTransform3D t )
	{
		synchronized ( viewerTransform ) // not a typo, all transform modifications synchronize on viewerTransform
		{
			t.set( transform );
		}
	}

	/**
	 * Get the index of the highlighted corner (if any).
	 *
	 * @return corner index or {@code -1} if no corner is highlighted
	 */
	public int getHighlightedCornerIndex()
	{
		return cornerId;
	}

	/**
	 * Returns a {@code MouseMotionListener} that can be installed into a bdv
	 * (see {@code ViewerPanel.getDisplay().addHandler(...)}). If installed, it
	 * will notify a {@code HighlightedCornerListener} (see
	 * {@link #setHighlightedCornerListener(HighlightedCornerListener)}) when
	 * the mouse is over a corner of the box (with some tolerance)/
	 *
	 * @return a {@code MouseMotionListener} implementing mouse-over for box
	 *         corners
	 */
	public MouseMotionListener getCornerHighlighter()
	{
		return cornerHighlighter;
	}

	public void setHighlightedCornerListener( final HighlightedCornerListener highlightedCornerListener )
	{
		this.highlightedCornerListener = highlightedCornerListener;
	}

	/**
	 * Set the index of the highlighted corner.
	 *
	 * @param id
	 *            corner index, {@code -1} means that no corner is highlighted.
	 */
	private void setHighlightedCorner( final int id )
	{
		final int oldId = cornerId;
		cornerId = ( id >= 0 && id < RenderBoxHelper.numCorners ) ? id : -1;
		if ( cornerId != oldId && highlightedCornerListener != null && displayMode.get() == FULL )
			highlightedCornerListener.highlightedCornerChanged();
	}

	/**
	 * Sets the source size (estimated maximum dimension) in global coordinates.
	 * Used for setting up perspective projection.
	 *
	 * @param sourceSize
	 *            estimated source size in global coordinates
	 */
	public void setSourceSize( final double sourceSize )
	{
		this.sourceSize = sourceSize;
	}

	private class CornerHighlighter extends MouseMotionAdapter
	{
		private final double squTolerance;

		CornerHighlighter( final double tolerance )
		{
			squTolerance = tolerance * tolerance;
		}

		@Override
		public void mouseMoved( final MouseEvent e )
		{
			final int x = e.getX();
			final int y = e.getY();
			for ( int i = 0; i < RenderBoxHelper.numCorners; i++ )
			{
				final double[] corner = renderBoxHelper.projectedCorners[ i ];
				final double dx = x - corner[ 0 ];
				final double dy = y - corner[ 1 ];
				final double dr2 = dx * dx + dy * dy;
				if ( dr2 < squTolerance )
				{
					setHighlightedCorner( i );
					return;
				}
			}
			setHighlightedCorner( -1 );
		}
	}
}
