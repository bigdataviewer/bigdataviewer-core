/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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

import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import javax.swing.JFrame;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.ByteType;
import org.junit.Test;

import static org.junit.Assume.assumeFalse;

public class BdvHandlePanelGarbageCollectionTest
{
	@Test
	public void testBdv() throws InterruptedException
	{
		assumeFalse( GraphicsEnvironment.isHeadless() );

		for ( int i = 0; i < 8; i++ )
			showAndCloseJFrame( this::addBdvHandlePanel );
	}

	@Test( expected = OutOfMemoryError.class )
	public void testMemoryExhaustion()
	{
		assumeFalse( GraphicsEnvironment.isHeadless() );

		for ( int i = 0; i < 8; i++ )
			showJFrame( this::addBdvHandlePanel );
	}

	private void showAndCloseJFrame( final Consumer< JFrame > componentAdder )
	{
		final JFrame frame = ( showJFrame( componentAdder ) );
		try
		{
			Thread.sleep( 100 );
		}
		catch ( final InterruptedException ignored )
		{
		}
		closeJFrame( frame );
		System.gc();
	}

	private JFrame showJFrame( final Consumer< JFrame > componentAdder )
	{
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setSize( 500, 500 );
		componentAdder.accept( frame );
		frame.setVisible( true );
		return frame;
	}

	private void closeJFrame( final JFrame frame )
	{
		frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ) );
	}

	private void addBdvHandlePanel( final JFrame frame )
	{
		final BdvHandle handle = new BdvHandlePanel( null, Bdv.options() );
		BdvFunctions.show( dummyLargeImage(), "Image", BdvOptions.options().addTo( handle ) );
		frame.add( handle.getSplitPanel() );
		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosed( final WindowEvent e )
			{
				handle.close();
			}
		} );
	}

	private RandomAccessibleInterval< ByteType > dummyLargeImage()
	{
		// NB: The image is intended to use on tenth of the maximal memory.
		final byte[] array = new byte[ boundedConvertToInt( Runtime.getRuntime().maxMemory() / 5 ) ];
		return ArrayImgs.bytes( array, 10, 10 );
	}

	private int boundedConvertToInt( final long dataSize )
	{
		return ( int ) Math.max( Integer.MIN_VALUE, Math.min( Integer.MAX_VALUE, dataSize ) );
	}
}
