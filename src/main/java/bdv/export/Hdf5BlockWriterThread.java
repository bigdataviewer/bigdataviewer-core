/*
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
package bdv.export;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;

class Hdf5BlockWriterThread extends Thread
{
	private final HDF5Access hdf5Access;

	private final BlockingQueue< Runnable > queue;

	private volatile boolean shutdown;

	// TODO: remove, seems unused
	@Deprecated
	public Hdf5BlockWriterThread( final HDF5Access hdf5Access, final int queueLength )
	{
		this.hdf5Access = hdf5Access;
		queue = new ArrayBlockingQueue<>( queueLength );
		shutdown = false;
		setName( "HDF5BlockWriterQueue" );
	}

	public Hdf5BlockWriterThread( final File hdf5File, final int queueLength )
	{
		final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5File );
		hdf5Access = new HDF5Access( hdf5Writer );
		queue = new ArrayBlockingQueue<>( queueLength );
		shutdown = false;
		setName( "HDF5BlockWriterQueue" );
	}

	public Hdf5BlockWriterThread( final String hdf5FilePath, final int queueLength )
	{
		this( new File( hdf5FilePath), queueLength );
	}

	@Override
	public void run()
	{
		while ( ! ( shutdown && queue.isEmpty() ) )
		{
			try
			{
				final Runnable task = queue.poll( 10, TimeUnit.MILLISECONDS );
				if ( task != null )
					task.run();
				if ( queue.isEmpty() )
					synchronized ( emptyMonitor )
					{
						emptyMonitor.notifyAll();
					}
			}
			catch ( final InterruptedException e )
			{}
		}
	}

	private final Object emptyMonitor = new Object();

	public void waitUntilEmpty()
	{
		synchronized ( emptyMonitor )
		{
			while ( !queue.isEmpty() )
				try
				{
					emptyMonitor.wait();
				}
				catch ( final InterruptedException e )
				{}
		}
	}

	public void close()
	{
		shutdown = true;
		try
		{
			join();
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
		hdf5Access.close();
	}

	// this is for sharing with Hdf5ImageLoader for loopback loader when exporting
	public IHDF5Writer getIHDF5Writer()
	{
		return hdf5Access.getIHDF5Writer();
	}

	public void writeMipmapDescription( final int setupIdPartition, final ExportMipmapInfo mipmapInfo )
	{
		put( () -> hdf5Access.writeMipmapDescription( setupIdPartition, mipmapInfo ) );
	}

	public void writeDataType( final int setupIdPartition, final DataType dataType )
	{
		put( () -> hdf5Access.writeDataType( setupIdPartition, dataType ) );
	}

	public void createDataset( final String path, final long[] dimensions, final int[] cellDimensions, final DataType dataType, final Compression compression )
	{
		put( () -> hdf5Access.createDataset( path, dimensions, cellDimensions, dataType, compression ) );
	}

	public < T > void writeBlock(
			final String pathName,
			final DatasetAttributes datasetAttributes,
			final DataBlock< T > dataBlock )
	{
		put( () -> hdf5Access.writeBlock( pathName, datasetAttributes, dataBlock ) );
	}

	public void flush()
	{
		waitUntilEmpty();
	}

	private boolean put( final Runnable task )
	{
		try
		{
			queue.put( task );
			return true;
		}
		catch ( final InterruptedException e )
		{
			return false;
		}
	}
}
