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

import bdv.viewer.RequestRepaint;
import java.util.concurrent.RejectedExecutionException;

/**
 * Thread to repaint display.
 */
public class PainterThread extends Thread implements RequestRepaint
{
	public interface Paintable
	{
		/**
		 * This is called by the painter thread to repaint the display.
		 */
		void paint();
	}

	private final Paintable paintable;

	private boolean pleaseRepaint;

	public PainterThread( final Paintable paintable )
	{
		this( null, "PainterThread", paintable );
	}

	public PainterThread( final ThreadGroup group, final Paintable paintable )
	{
		this( group, "PainterThread", paintable );
	}

	public PainterThread( final ThreadGroup group, final String name, final Paintable paintable )
	{
		super( group, name );
		this.paintable = paintable;
		this.pleaseRepaint = false;
	}

	@Override
	public void run()
	{
		while ( !isInterrupted() )
		{
			final boolean b;
			synchronized ( this )
			{
				b = pleaseRepaint;
				pleaseRepaint = false;
			}
			if ( b )
				try
				{
					paintable.paint();
				}
				catch ( final RejectedExecutionException e )
				{
					// this happens when the rendering threadpool
					// is killed before the painter thread.
				}
			synchronized ( this )
			{
				try
				{
					if ( !pleaseRepaint )
						wait();
				}
				catch ( final InterruptedException e )
				{
					break;
				}
			}
		}
	}

	/**
	 * Request repaint. This will trigger a call to {@link Paintable#paint()}
	 * from the {@link PainterThread}.
	 */
	@Override
	public void requestRepaint()
	{
		synchronized ( this )
		{
//			DebugHelpers.printStackTrace( 15 );
			pleaseRepaint = true;
			notify();
		}
	}
}
