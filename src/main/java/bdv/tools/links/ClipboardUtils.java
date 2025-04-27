package bdv.tools.links;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClipboardUtils
{
	private static final Logger LOG = LoggerFactory.getLogger( ClipboardUtils.class );

	static void copyToClipboard( final String string )
	{
		final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents( new StringSelection( string ), null );
	}

	static String getFromClipboard()
	{
		final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		try
		{
			Transferable transferable = clipboard.getContents(DataFlavor.stringFlavor);
			if ( transferable != null )
			{
				return ( String ) transferable.getTransferData( DataFlavor.stringFlavor );
			}
		}
		catch ( UnsupportedFlavorException | IOException e )
		{
			LOG.debug( "Unable to retrieve string from system clipboard", e );
		}
		return null;
	}
}
