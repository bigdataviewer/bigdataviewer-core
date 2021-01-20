/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.type.numeric.ARGBType;

/**
 * Test {@link TripleBuffer}.
 *
 * @author Matthias Arzt
 */
public class TripleBufferDemo
{
	private static TripleBuffer< ARGBScreenImage > tripleBuffer = new TripleBuffer<>( () -> new ARGBScreenImage( 100, 100 ) );

	private static ImageComponent imageComponent = new ImageComponent();

	public static void main( final String... args ) throws InterruptedException
	{
		final JFrame frame = new JFrame();
		frame.setSize( 100, 100 );
		frame.add( imageComponent );
		frame.setVisible( true );

		new PainterThread().start();
	}

	private static class PainterThread extends Thread
	{
		@Override
		public void run()
		{
			try
			{
				while ( true )
				{
					for ( double r = 0; r < 2 * Math.PI; r += 0.01 )
					{
						renderCircle( r );
						Thread.sleep( 1 );
						imageComponent.repaint();
					}
				}
			}
			catch ( final InterruptedException e )
			{
				e.printStackTrace();
			}
		}

		private void renderCircle( final double r )
		{
			final ARGBScreenImage image = tripleBuffer.getWritableBuffer();
			image.forEach( pixel -> pixel.set( 0xff00ff00 ) );
			final ArrayCursor< ARGBType > cursor = image.cursor();
			while ( cursor.hasNext() )
			{
				final ARGBType pixel = cursor.next();
				final double x = cursor.getDoublePosition( 0 );
				final double y = cursor.getDoublePosition( 1 );
				pixel.set( ARGBType.rgba( Math.sin( x / 10 + r ) * 127 + 127, Math.cos( y / 10 + r ) * 127 + 127, 0.0, 255.0 ) );
			}
			tripleBuffer.doneWriting( image );
		}
	}

	private static class ImageComponent extends JComponent
	{
		@Override
		protected void paintComponent( final Graphics g )
		{
			final ARGBScreenImage image = tripleBuffer.getReadableBuffer().getBuffer();
			if ( image != null )
				g.drawImage( image.image(), 0, 0, getWidth(), getHeight(), null );
		}
	}
}

