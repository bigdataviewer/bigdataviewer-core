/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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
package net.imglib2.ui;

import bdv.TransformEventHandler;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import javax.swing.JComponent;
import org.scijava.listeners.Listeners;

/*
 * A {@link JComponent} that uses {@link OverlayRenderer OverlayRenderers}
 * to render a canvas displayed on screen.
 * <p>
 * {@code InteractiveDisplayCanvas} has a {@code TransformEventHandler} that is notified when the component size is changed.
 * <p>
 * {@link #addHandler}/{@link #removeHandler} provide simplified that implement {@code MouseListener}, {@code KeyListener}, etc can be
 *
 * @param <A>
 *            transform type
 *
 * @author Tobias Pietzsch
 */
public class InteractiveDisplayCanvas extends JComponent
{
	/**
	 * Mouse/Keyboard handler that manipulates the view transformation.
	 */
	private TransformEventHandler handler;

	/**
	 * To draw this component, {@link OverlayRenderer#drawOverlays} is invoked for each renderer.
	 */
	final private Listeners.List< OverlayRenderer > overlayRenderers;

	/**
	 * Create a new {@code InteractiveDisplayCanvas}.
	 *
	 * @param width
	 *            preferred component width.
	 * @param height
	 *            preferred component height.
	 */
	public InteractiveDisplayCanvas( final int width, final int height )
	{
		super();
		setPreferredSize( new Dimension( width, height ) );
		setFocusable( true );

		overlayRenderers = new Listeners.SynchronizedList<>( r -> r.setCanvasSize( getWidth(), getHeight() ) );

		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				final int w = getWidth();
				final int h = getHeight();
				// NB: Update of overlayRenderers needs to happen before update of handler
				// Otherwise repaint might start before the render target receives the size change.
				overlayRenderers.list.forEach( r -> r.setCanvasSize( w, h ) );
				if ( handler != null )
					handler.setCanvasSize( w, h, true );
				// enableEvents( AWTEvent.MOUSE_MOTION_EVENT_MASK );
			}
		} );

		addMouseListener( new MouseAdapter()
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				requestFocusInWindow();
			}
		} );
	}

	/**
	 * OverlayRenderers can be added/removed here.
	 * {@link OverlayRenderer#drawOverlays} is invoked for each renderer (in the order they were added).
	 */
	public Listeners< OverlayRenderer > overlays()
	{
		return overlayRenderers;
	}

	/**
	 * Add new event handler. Depending on the interfaces implemented by
	 * <code>handler</code> calls {@link Component#addKeyListener(KeyListener)},
	 * {@link Component#addMouseListener(MouseListener)},
	 * {@link Component#addMouseMotionListener(MouseMotionListener)},
	 * {@link Component#addMouseWheelListener(MouseWheelListener)}.
	 *
	 * @param h handler to remove
	 */
//	@Override
	public void addHandler( final Object h )
	{
		if ( h instanceof KeyListener )
			addKeyListener( ( KeyListener ) h );

		if ( h instanceof MouseMotionListener )
			addMouseMotionListener( ( MouseMotionListener ) h );

		if ( h instanceof MouseListener )
			addMouseListener( ( MouseListener ) h );

		if ( h instanceof MouseWheelListener )
			addMouseWheelListener( ( MouseWheelListener ) h );

		if ( h instanceof FocusListener )
			addFocusListener( ( FocusListener ) h );
	}

	/**
	 * Remove an event handler. Add new event handler. Depending on the
	 * interfaces implemented by <code>handler</code> calls
	 * {@link Component#removeKeyListener(KeyListener)},
	 * {@link Component#removeMouseListener(MouseListener)},
	 * {@link Component#removeMouseMotionListener(MouseMotionListener)},
	 * {@link Component#removeMouseWheelListener(MouseWheelListener)}.
	 *
	 * @param h handler to remove
	 */
//	@Override
	public void removeHandler( final Object h )
	{
		if ( h instanceof KeyListener )
			removeKeyListener( ( KeyListener ) h );

		if ( h instanceof MouseMotionListener )
			removeMouseMotionListener( ( MouseMotionListener ) h );

		if ( h instanceof MouseListener )
			removeMouseListener( ( MouseListener ) h );

		if ( h instanceof MouseWheelListener )
			removeMouseWheelListener( ( MouseWheelListener ) h );

		if ( h instanceof FocusListener )
			removeFocusListener( ( FocusListener ) h );
	}

	/**
	 * Set the {@link TransformEventHandler} that will be notified when component is resized.
	 *
	 * @param transformEventHandler
	 *            handler to use
	 */
	public void setTransformEventHandler( final TransformEventHandler transformEventHandler )
	{
		if ( handler != null )
			removeHandler( handler );
		handler = transformEventHandler;
		handler.setCanvasSize( getWidth(), getHeight(), false );
		addHandler( handler );
	}

	@Override
	public void paintComponent( final Graphics g )
	{
		overlayRenderers.list.forEach( r -> r.drawOverlays( g ) );
	}

	// -- deprecated API --

	/**
	 * @deprecated Use {@code overlays().add(renderer)} instead
	 */
	@Deprecated
	public void addOverlayRenderer( final OverlayRenderer renderer )
	{
		overlayRenderers.add( renderer );
	}

	/**
	 * @deprecated Use {@code overlays().remove(renderer)} instead
	 */
	@Deprecated
	public void removeOverlayRenderer( final OverlayRenderer renderer )
	{
		overlayRenderers.remove( renderer );
	}
}
