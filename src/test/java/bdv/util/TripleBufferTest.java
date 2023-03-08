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
package bdv.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test {@link TripleBuffer}.
 *
 * @author Matthias Arzt
 */
public class TripleBufferTest
{
	@Test
	public void testSimple()
	{
		final TripleBuffer< ModifiableString > db = new TripleBuffer<>( ModifiableString::new );
		db.getWritableBuffer().set( "Hello" );
		db.doneWriting();
		assertEquals( "Hello", db.getReadableBuffer().getBuffer().get() );
	}

	@Test
	public void testWriteTwice()
	{
		final TripleBuffer< ModifiableString > db = new TripleBuffer<>( ModifiableString::new );
		db.getWritableBuffer().set( "Hello" );
		db.doneWriting();
		db.getWritableBuffer().set( "World" );
		db.doneWriting();
		assertEquals( "World", db.getReadableBuffer().getBuffer().get() );
	}

	@Test
	public void testReadTwice()
	{
		final TripleBuffer< ModifiableString > db = new TripleBuffer<>( ModifiableString::new );
		db.getWritableBuffer().set( "Hello" );
		db.doneWriting();
		assertEquals( "Hello", db.getReadableBuffer().getBuffer().get() );
		assertEquals( "Hello", db.getReadableBuffer().getBuffer().get() );
	}

	@Test
	public void testThreeBuffers()
	{
		final TripleBuffer< ModifiableString > db = new TripleBuffer<>( ModifiableString::new );
		db.getWritableBuffer().set( "1" );
		db.doneWriting();
		db.getReadableBuffer();
		db.getWritableBuffer().set( "2" );
		db.doneWriting();
		db.getReadableBuffer();
		db.getWritableBuffer().set( "3" );
		db.doneWriting();
		db.getReadableBuffer();
		assertEquals( "1", db.getWritableBuffer().get() );
	}

	private static class ModifiableString
	{
		private String value = "";

		public String get()
		{
			return value;
		}

		public void set( final String value )
		{
			this.value = value;
		}
	}
}
