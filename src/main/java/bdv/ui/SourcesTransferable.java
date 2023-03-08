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
