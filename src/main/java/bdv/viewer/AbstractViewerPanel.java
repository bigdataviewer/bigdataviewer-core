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
package bdv.viewer;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import net.imglib2.Positionable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.util.Affine3DHelpers;
import bdv.viewer.animate.AbstractTransformAnimator;
import bdv.viewer.animate.OverlayAnimator;
import bdv.viewer.animate.RotationAnimator;

/**
 * Abstract base class for {@link ViewerPanel}. It mostly serves as an interface
 * for tools like {@code BookmarksEditor}, to abstract from the specific {@code
 * ViewerPanel} implementation. (This allows for re-using these tools in
 * BigVolumeViewer.)
 *
 * @author Tobias Pietzsch
 */
public abstract class AbstractViewerPanel extends JPanel implements RequestRepaint
{
	/**
	 * Keeps track of the current mouse coordinates, which are used to provide
	 * the current global position (see {@link #getGlobalMouseCoordinates(RealPositionable)}).
	 */
	protected final MouseCoordinateListener mouseCoordinates;

	public AbstractViewerPanel( final LayoutManager layout, final boolean isDoubleBuffered )
	{
		super( layout, isDoubleBuffered );
		mouseCoordinates = new MouseCoordinateListener();
	}

	public AbstractViewerPanel( final LayoutManager layout )
	{
		this( layout, true );
	}

	public abstract InputTriggerConfig getInputTriggerConfig();

	/**
	 * Get the viewer canvas.
	 *
	 * @return the viewer canvas.
	 */
	public abstract InteractiveDisplay getDisplay();

	/**
	 * Get the AWT {@code Component} of the viewer canvas.
	 *
	 * @return the viewer canvas.
	 */
	public abstract Component getDisplayComponent();

	/**
	 * Add a new {@link OverlayAnimator} to the list of animators. The animation
	 * is immediately started. The new {@link OverlayAnimator} will remain in
	 * the list of animators until it {@link OverlayAnimator#isComplete()}.
	 *
	 * @param animator
	 *            animator to add.
	 */
	public abstract void addOverlayAnimator( OverlayAnimator animator );

	/**
	 * Get the ViewerState. This can be directly used for modifications, e.g.,
	 * adding/removing sources etc. See {@link SynchronizedViewerState} for
	 * thread-safety considerations.
	 */
	public abstract ViewerState state();

	/**
	 * Add/remove {@code TransformListener}s to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been painted
	 * with the viewer transform used to render that image.
	 * <p>
	 * This happens immediately after that image is painted onto the screen,
	 * before any overlays are painted.
	 */
	public abstract Listeners< TransformListener< AffineTransform3D > > renderTransformListeners();

	/**
	 * Add/remove {@code TransformListener}s to notify about viewer transformation
	 * changes. Listeners will be notified <em>before</em> calling
	 * {@link #requestRepaint()} so they have the chance to interfere.
	 */
	public abstract Listeners< TransformListener< AffineTransform3D > > transformListeners();

	public abstract void setTransformAnimator( AbstractTransformAnimator animator );

	/**
	 * Display the specified message in a text overlay for a short time.
	 *
	 * @param msg
	 *            String to display. Should be just one line of text.
	 */
	public abstract void showMessage( String msg );

	private final static double c = Math.cos( Math.PI / 4 );

	/**
	 * The planes which can be aligned with the viewer coordinate system: XY,
	 * ZY, and XZ plane.
	 */
	public enum AlignPlane
	{
		XY( 2, new double[] { 1, 0, 0, 0 } ),
		ZY( 0, new double[] { c, 0, -c, 0 } ),
		XZ( 1, new double[] { c, c, 0, 0 } );

		/**
		 * rotation from the xy-plane aligned coordinate system to this plane.
		 */
		public final double[] qAlign; // TODO: should be private/package private

		/**
		 * Axis index. The plane spanned by the remaining two axes will be
		 * transformed to the same plane by the computed rotation and the
		 * "rotation part" of the affine source transform.
		 * @see Affine3DHelpers#extractApproximateRotationAffine(AffineTransform3D, double[], int)
		 */
		public final int coerceAffineDimension; // TODO: should be private/package private

		private AlignPlane( final int coerceAffineDimension, final double[] qAlign )
		{
			this.coerceAffineDimension = coerceAffineDimension;
			this.qAlign = qAlign;
		}
	}

	/**
	 * Align the XY, ZY, or XZ plane of the local coordinate system of the
	 * currently active source with the viewer coordinate system.
	 *
	 * @param plane
	 *            to which plane to align.
	 */
	public void align( final AlignPlane plane )
	{
		final Source< ? > source = state().getCurrentSource().getSpimSource();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( state().getCurrentTimepoint(), 0, sourceTransform );

		final double[] qSource = new double[ 4 ];
		Affine3DHelpers.extractRotationAnisotropic( sourceTransform, qSource );

		final double[] qTmpSource = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, plane.coerceAffineDimension );
		LinAlgHelpers.quaternionMultiply( qSource, plane.qAlign, qTmpSource );

		final double[] qTarget = new double[ 4 ];
		LinAlgHelpers.quaternionInvert( qTmpSource, qTarget );

		final AffineTransform3D transform = state().getViewerTransform();
		final double centerX;
		final double centerY;
		synchronized ( mouseCoordinates )
		{
			if ( mouseCoordinates.isMouseInsidePanel() )
			{
				centerX = mouseCoordinates.getX();
				centerY = mouseCoordinates.getY();
			}
			else
			{
				centerY = getDisplayComponent().getHeight() / 2.0;
				centerX = getDisplayComponent().getWidth() / 2.0;
			}
		}
		setTransformAnimator( new RotationAnimator( transform, centerX, centerY, qTarget, 300 ) );
	}

	/**
	 * Set {@code gPos} to the current mouse coordinates transformed into the
	 * global coordinate system.
	 *
	 * @param gPos
	 *            is set to the current global coordinates.
	 */
	public void getGlobalMouseCoordinates( final RealPositionable gPos )
	{
		assert gPos.numDimensions() == 3;
		final RealPoint lPos = new RealPoint( 3 );
		mouseCoordinates.getMouseCoordinates( lPos );
		state().getViewerTransform().applyInverse( gPos, lPos );
	}

	/**
	 * Set {@code p} to the current mouse coordinates wrt to the display component.
	 *
	 * @param p
	 *            is set to the current mouse coordinates.
	 */
	public void getMouseCoordinates( final Positionable p )
	{
		assert p.numDimensions() == 2;
		mouseCoordinates.getMouseCoordinates( p );
	}

	protected void onMouseMoved() {}

	protected class MouseCoordinateListener implements MouseMotionListener, MouseListener
	{
		private int x;

		private int y;

		private boolean isInside;

		public synchronized void getMouseCoordinates( final Positionable p )
		{
			p.setPosition( x, 0 );
			p.setPosition( y, 1 );
		}

		@Override
		public synchronized void mouseDragged( final MouseEvent e )
		{
			x = e.getX();
			y = e.getY();
		}

		@Override
		public synchronized void mouseMoved( final MouseEvent e )
		{
			x = e.getX();
			y = e.getY();
			onMouseMoved();
		}

		public synchronized int getX()
		{
			return x;
		}

		public synchronized int getY()
		{
			return y;
		}

		public synchronized boolean isMouseInsidePanel()
		{
			return isInside;
		}

		@Override
		public synchronized void mouseEntered( final MouseEvent e )
		{
			isInside = true;
		}

		@Override
		public synchronized void mouseExited( final MouseEvent e )
		{
			isInside = false;
		}

		@Override
		public void mouseClicked( final MouseEvent e )
		{}

		@Override
		public void mousePressed( final MouseEvent e )
		{}

		@Override
		public void mouseReleased( final MouseEvent e )
		{}
	}

	@Override
	public boolean requestFocusInWindow()
	{
		return getDisplayComponent().requestFocusInWindow();
	}
}
