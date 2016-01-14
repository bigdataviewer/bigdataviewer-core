/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

class Hdf5BlockWriterThread extends Thread implements IHDF5Access
{
	private final IHDF5Access hdf5Access;

	private static interface Hdf5Task
	{
		public void run( final IHDF5Access hdf5Access );
	}

	private final BlockingQueue< Hdf5BlockWriterThread.Hdf5Task > queue;

	private volatile boolean shutdown;

	public Hdf5BlockWriterThread( final IHDF5Access hdf5Access, final int queueLength )
	{
		this.hdf5Access = hdf5Access;
		queue = new ArrayBlockingQueue< Hdf5BlockWriterThread.Hdf5Task >( queueLength );
		shutdown = false;
		setName( "HDF5BlockWriterQueue" );
	}

	public Hdf5BlockWriterThread( final File hdf5File, final int queueLength )
	{
		final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5File );
		IHDF5Access hdf5Access;
		try
		{
			hdf5Access = new HDF5AccessHack( hdf5Writer );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			hdf5Access = new HDF5Access( hdf5Writer );
		}
		this.hdf5Access = hdf5Access;
		queue = new ArrayBlockingQueue< Hdf5BlockWriterThread.Hdf5Task >( queueLength );
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
				final Hdf5BlockWriterThread.Hdf5Task task = queue.poll( 10, TimeUnit.MILLISECONDS );
				if ( task != null )
					task.run( hdf5Access );
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

	@Override
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

	@Override
	public IHDF5Writer getIHDF5Writer()
	{
		return hdf5Access.getIHDF5Writer();
	}

	@Override
	public void writeMipmapDescription( final int setupIdPartition, final ExportMipmapInfo mipmapInfo )
	{
		put( new WriteMipmapDescriptionTask( setupIdPartition, mipmapInfo ) );
	}

	@Override
	public void createAndOpenDataset( final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features )
	{
		put( new CreateAndOpenDatasetTask( path, dimensions, cellDimensions, features ) );
	}

	@Override
	public void writeBlockWithOffset( final short[] data, final long[] blockDimensions, final long[] offset )
	{
		put( new WriteBlockWithOffsetTask( data, blockDimensions, offset ) );
	}

	@Override
	public void closeDataset()
	{
		put( new CloseDatasetTask() );
	}

	private boolean put( final Hdf5BlockWriterThread.Hdf5Task task )
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

	private static class WriteMipmapDescriptionTask implements Hdf5BlockWriterThread.Hdf5Task
	{
		private final int setupIdPartition;

		private final ExportMipmapInfo mipmapInfo;

		public WriteMipmapDescriptionTask( final int setupIdPartition, final ExportMipmapInfo mipmapInfo )
		{
			this.setupIdPartition = setupIdPartition;
			this.mipmapInfo = mipmapInfo;

		}

		@Override
		public void run( final IHDF5Access hdf5Access )
		{
			hdf5Access.writeMipmapDescription( setupIdPartition, mipmapInfo );
		}
	}

	private static class CreateAndOpenDatasetTask implements Hdf5BlockWriterThread.Hdf5Task
	{
		private final String path;

		private final long[] dimensions;

		private final int[] cellDimensions;

		private final HDF5IntStorageFeatures features;

		public CreateAndOpenDatasetTask( final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features )
		{
			this.path = path;
			this.dimensions = dimensions;
			this.cellDimensions = cellDimensions;
			this.features = features;
		}

		@Override
		public void run( final IHDF5Access hdf5Access )
		{
			hdf5Access.createAndOpenDataset( path, dimensions, cellDimensions, features );
		}
	}

	private static class WriteBlockWithOffsetTask implements Hdf5BlockWriterThread.Hdf5Task
	{
		private final short[] data;

		private final long[] blockDimensions;

		private final long[] offset;

		public WriteBlockWithOffsetTask( final short[] data, final long[] blockDimensions, final long[] offset )
		{
			this.data = data;
			this.blockDimensions = blockDimensions;
			this.offset = offset;
		}

		@Override
		public void run( final IHDF5Access hdf5Access )
		{
			hdf5Access.writeBlockWithOffset( data, blockDimensions, offset );
		}
	}

	private static class CloseDatasetTask implements Hdf5BlockWriterThread.Hdf5Task
	{
		@Override
		public void run( final IHDF5Access hdf5Access )
		{
			hdf5Access.closeDataset();
		}
	}
}
