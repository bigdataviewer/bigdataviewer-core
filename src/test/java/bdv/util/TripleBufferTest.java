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
