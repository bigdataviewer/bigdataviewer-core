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
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JComponent;
import org.scijava.listeners.Listeners;

/*
 * A {@link JComponent} that uses {@link OverlayRenderer OverlayRenderers}
 * to render a canvas displayed on screen.
 * <p>
 * {@link InteractiveDisplayCanvas} also owns a {@link TransformEventHandler},
 * which is registered to listen to mouse and keyboard events if it implements
 * {@link MouseListener}, etc.
 * <p>
 * Moreover, {@link InteractiveDisplayCanvas} is a transform event multi-caster.
 * It receives {@link TransformListener#transformChanged(Object)
 * transformChanged} events (usually from its {@link TransformEventHandler}) and
 * propagates them to all registered listeners.
 *
 * @param <A>
 *            transform type
 *
 * @author Tobias Pietzsch
 */
public class InteractiveDisplayCanvas< A > extends JComponent
{
	/**
	 * Mouse/Keyboard handler that manipulates the view transformation.
	 */
	private TransformEventHandler< A > handler;

	/**
	 * Listeners that we have to notify about view transformation changes.
	 */
	final private CopyOnWriteArrayList< TransformListener< A > > transformListeners;
	final private Listeners.List< OverlayRenderer > overlayRenderers;

	/**
	 * Create a new {@link InteractiveDisplayCanvas} with initially no
	 * {@link OverlayRenderer OverlayRenderers} and no {@link TransformListener
	 * TransformListeners}. A {@link TransformEventHandler} is instantiated
	 * using the given factory, and registered for mouse and key events if it
	 * implements the appropriate interfaces ({@link MouseListener} etc.)
	 *
	 * @param width
	 *            preferred component width.
	 * @param height
	 *            preferred component height.
	 * @param transformEventHandlerFactory
	 *            factory to create a {@link TransformEventHandler} appropriate
	 *            for our transform type A.
	 */
	public InteractiveDisplayCanvas( final int width, final int height, final TransformEventHandlerFactory< A > transformEventHandlerFactory )
	{
		super();
		setPreferredSize( new Dimension( width, height ) );
		setFocusable( true );

		this.overlayRenderers = new Listeners.SynchronizedList<>( r -> r.setCanvasSize( getWidth(), getHeight() ) );
		this.transformListeners = new CopyOnWriteArrayList< >();

		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				final int w = getWidth();
				final int h = getHeight();
				if ( handler != null )
					handler.setCanvasSize( w, h, true );
				overlayRenderers.list.forEach( r -> r.setCanvasSize( w, h ) );
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

		handler = transformEventHandlerFactory.create( this::transformChanged );
		handler.setCanvasSize( width, height, false );
		addHandler( handler );
	}

	/**
	/**
	 * Add a {@link TransformListener} to notify about view transformation
	 * changes.
	 *
	 * @param listener
	 *            the transform listener to add.
	 */
//	@Override
	public void addTransformListener( final TransformListener< A > listener )
	{
		transformListeners.add( listener );
	}

	/**
	 * Remove a {@link TransformListener}.
	 *
	 * @param listener
	 *            the transform listener to remove.
	 */
//	@Override
	public void removeTransformListener( final TransformListener< A > listener )
	{
		transformListeners.remove( listener );
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
		if ( KeyListener.class.isInstance( h ) )
			addKeyListener( ( KeyListener ) h );

		if ( MouseMotionListener.class.isInstance( h ) )
			addMouseMotionListener( ( MouseMotionListener ) h );

		if ( MouseListener.class.isInstance( h ) )
			addMouseListener( ( MouseListener ) h );

		if ( MouseWheelListener.class.isInstance( h ) )
			addMouseWheelListener( ( MouseWheelListener ) h );

		if ( FocusListener.class.isInstance( h ) )
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
		if ( KeyListener.class.isInstance( h ) )
			removeKeyListener( ( KeyListener ) h );

		if ( MouseMotionListener.class.isInstance( h ) )
			removeMouseMotionListener( ( MouseMotionListener ) h );

		if ( MouseListener.class.isInstance( h ) )
			removeMouseListener( ( MouseListener ) h );

		if ( MouseWheelListener.class.isInstance( h ) )
			removeMouseWheelListener( ( MouseWheelListener ) h );

		if ( FocusListener.class.isInstance( h ) )
			removeFocusListener( ( FocusListener ) h );
	}

	/**
	 * Get the {@link TransformEventHandler} that handles mouse and key events
	 * to update our view transform.
	 *
	 * @return handles mouse and key events to update the view transform.
	 */
//	@Override
	public TransformEventHandler< A > getTransformEventHandler()
	{
		return handler;
	}

	/**
	 * Set the {@link TransformEventHandler} that handles mouse and key events
	 * to update our view transform.
	 *
	 * @param transformEventHandler
	 *            handler to use
	 */
//	@Override
	public synchronized void setTransformEventHandler( final TransformEventHandler< A > transformEventHandler )
	{
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
