package bdv.util;

import bdv.util.DoubleBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DoubleBufferTest {

	@Test
	public void testSimple() {
		DoubleBuffer<ModifiableString> db = new DoubleBuffer<>(ModifiableString::new);
		db.getWritableBuffer().set("Hello");
		db.doneWriting();
		assertEquals("Hello", db.getReadableBuffer().get());
	}

	@Test
	public void testWriteTwice() {
		DoubleBuffer<ModifiableString> db = new DoubleBuffer<>(ModifiableString::new);
		db.getWritableBuffer().set("Hello");
		db.doneWriting();
		db.getWritableBuffer().set("World");
		db.doneWriting();
		assertEquals("World", db.getReadableBuffer().get());
	}

	@Test
	public void testReadTwice() {
		DoubleBuffer<ModifiableString> db = new DoubleBuffer<>(ModifiableString::new);
		db.getWritableBuffer().set("Hello");
		db.doneWriting();
		assertEquals("Hello", db.getReadableBuffer().get());
		assertEquals("Hello", db.getReadableBuffer().get());
	}

	@Test
	public void testThreeBuffers() {
		DoubleBuffer<ModifiableString> db = new DoubleBuffer<>(ModifiableString::new);
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
