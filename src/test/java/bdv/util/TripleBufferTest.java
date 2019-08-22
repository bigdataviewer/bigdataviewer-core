package bdv.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TripleBufferTest {

	@Test
	public void testSimple() {
		TripleBuffer<ModifiableString> db = new TripleBuffer<>(ModifiableString::new);
		db.getWritableBuffer().set("Hello");
		db.doneWriting();
		assertEquals("Hello", db.getReadableBuffer().get());
	}

	@Test
	public void testWriteTwice() {
		TripleBuffer<ModifiableString> db = new TripleBuffer<>(ModifiableString::new);
		db.getWritableBuffer().set("Hello");
		db.doneWriting();
		db.getWritableBuffer().set("World");
		db.doneWriting();
		assertEquals("World", db.getReadableBuffer().get());
	}

	@Test
	public void testReadTwice() {
		TripleBuffer<ModifiableString> db = new TripleBuffer<>(ModifiableString::new);
		db.getWritableBuffer().set("Hello");
		db.doneWriting();
		assertEquals("Hello", db.getReadableBuffer().get());
		assertEquals("Hello", db.getReadableBuffer().get());
	}

	@Test
	public void testThreeBuffers() {
		TripleBuffer<ModifiableString> db = new TripleBuffer<>(ModifiableString::new);
		db.getWritableBuffer().set("1");
		db.doneWriting();
		db.getReadableBuffer();
		db.getWritableBuffer().set("2");
		db.doneWriting();
		db.getReadableBuffer();
		db.getWritableBuffer().set("3");
		db.doneWriting();
		db.getReadableBuffer();
		assertEquals("1", db.getWritableBuffer().get());
	}

	private static class ModifiableString {

		private String value = "";

		public String get() {
			return value;
		}

		public void set(String value) {
			this.value = value;
		}
	}
}
