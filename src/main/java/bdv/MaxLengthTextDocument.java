package bdv;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class MaxLengthTextDocument extends PlainDocument
{
	private final int maxChars;

	/**
	 * @param maxChars
	 *            maximum characters permitted.
	 */
	public MaxLengthTextDocument( final int maxChars )
	{
		this.maxChars = maxChars;
	}

	@Override
	public void insertString( final int offs, final String str, final AttributeSet a ) throws BadLocationException
	{
		if ( str != null && ( getLength() + str.length() <= maxChars ) )
			super.insertString( offs, str, a );
	}
}
