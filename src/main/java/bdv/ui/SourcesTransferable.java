package bdv.ui;

import bdv.viewer.SourceAndConverter;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Drag and Drop {@code Transferable} for a collection of sources.
 */
public class SourcesTransferable implements Transferable
{
	public static final DataFlavor flavor = new DataFlavor( SourceList.class, DataFlavor.javaJVMLocalObjectMimeType );

	static final DataFlavor[] transferDataFlavors = { flavor };

	public static class SourceList
	{
		private final ArrayList< SourceAndConverter< ? > > sources;

		public List< SourceAndConverter< ? > > getSources()
		{
			return sources;
		}

		SourceList( final Collection< SourceAndConverter< ? > > sources )
		{
			this.sources = new ArrayList<>( sources );
		}
	}

	private final SourceList sources;

	public SourcesTransferable( final Collection< SourceAndConverter< ? > > sources )
	{
		this.sources = new SourceList( sources );
	}

	@Override
	public DataFlavor[] getTransferDataFlavors()
	{
		return transferDataFlavors;
	}

	@Override
	public boolean isDataFlavorSupported( final DataFlavor flavor )
	{
		return Objects.equals( flavor, this.flavor );
	}

	@Override
	public Object getTransferData( final DataFlavor flavor ) throws UnsupportedFlavorException, IOException
	{
		return sources;
	}
}
