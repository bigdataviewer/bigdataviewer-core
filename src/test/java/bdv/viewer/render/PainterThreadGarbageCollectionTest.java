/*
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
package bdv.viewer.render;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Make sure there are no memory leaks cause by {@link PainterThread}.
 */
public class PainterThreadGarbageCollectionTest
{

	/**
	 * This test makes sure that the {@link PainterThread} does not prevent
	 * the garbage collection of the {@link PainterThread.Paintable}.
	 * <p>
	 * The test will fail with an OutOfMemoryException, if the paintable
	 * object can not be garbage collection.
	 */
	@Test
	public void testGarbageCollection()
	{
		final WeakReference< MyPaintable > paintable = new WeakReference<>( new MyPaintable() );
		new PainterThread( paintable.get() ).start();

		while ( true )
		{
			byte[] hundredMegaBytes = new byte[ 1024 * 1024 * 100 ];
			MyPaintable p = paintable.get();
			if ( p == null )
				return; // Success! The "paintable" was garbage collected.
			else
				p.addMemory( hundredMegaBytes ); // Increase pressure to garbage collect "paintable".
		}
	}

	private static class MyPaintable implements PainterThread.Paintable
	{

		private final List< byte[] > memory = new ArrayList<>();

		@Override
		public void paint()
		{
			System.out.println( "paint()" );
		}

		/**
		 * This method allows to add more memory (a large array) to
		 * the {@link MyPaintable} instance. This memory can only be
		 * freed if the {@link MyPaintable} gets garbage collected.
		 */
		public void addMemory( byte[] array )
		{
			memory.add( array );
		}
	}
}
