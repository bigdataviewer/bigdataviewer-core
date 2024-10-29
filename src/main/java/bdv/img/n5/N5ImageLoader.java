/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
package bdv.img.n5;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.cache.SharedQueue;
import bdv.img.cache.SimpleCacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.util.ConstantRandomAccessible;
import bdv.util.MipmapTransforms;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.blocks.SubArrayCopy;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.DataAccess;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class N5ImageLoader implements ViewerImgLoader, MultiResolutionImgLoader
{
	private final URI n5URI;
	protected final N5Properties n5properties;

	// TODO: it would be good if this would not be needed
	//       find available setups from the n5
	private final AbstractSequenceDescription< ?, ?, ? > seq;

	/**
	 * Maps setup id to {@link SetupImgLoader}.
	 */
	private final Map< Integer, SetupImgLoader< ?, ? > > setupImgLoaders = new HashMap<>();

	public N5ImageLoader( final URI n5URI, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this.n5URI = n5URI;
		this.seq = sequenceDescription;
		this.n5properties = createN5PropertiesInstance();
	}

	public N5ImageLoader( final File n5File, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this( n5File.toURI(), sequenceDescription );
	}

	public N5ImageLoader( final N5Reader n5Reader, final URI n5URI, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this( n5URI, sequenceDescription );
		n5 = n5Reader;
	}

	public URI getN5URI()
	{
		return n5URI;
	}

	public File getN5File()
	{
		return new File( n5URI );
	}

	/**
	 * @return a class that creates the pathnames for the setupId, timePointId and multiresolution levels
	 */
	public N5Properties createN5PropertiesInstance() { return new BdvN5Format(); }

	private volatile boolean isOpen = false;
	private SharedQueue createdSharedQueue;
	private VolatileGlobalCellCache cache;
	private N5Reader n5;


	private int requestedNumFetcherThreads = -1;
	private SharedQueue requestedSharedQueue;

	@Override
	public synchronized void setNumFetcherThreads( final int n )
	{
		requestedNumFetcherThreads = n;
	}

	@Override
	public void setCreatedSharedQueue( final SharedQueue createdSharedQueue )
	{
		requestedSharedQueue = createdSharedQueue;
	}

	private void open()
	{
		if ( !isOpen )
		{
			synchronized ( this )
			{
				if ( isOpen )
					return;

				try
				{
					if ( n5 == null )
					{
						n5 = new N5FSReader( getN5File().getAbsolutePath() );
					}

					int maxNumLevels = 0;
					final List< ? extends BasicViewSetup > setups = seq.getViewSetupsOrdered();
					for ( final BasicViewSetup setup : setups )
					{
						final int setupId = setup.getId();
						final SetupImgLoader< ?, ? > setupImgLoader = createSetupImgLoader( setupId );
						setupImgLoaders.put( setupId, setupImgLoader );
						maxNumLevels = Math.max( maxNumLevels, setupImgLoader.numMipmapLevels() );
					}

					final int numFetcherThreads = requestedNumFetcherThreads >= 0
							? requestedNumFetcherThreads
							: Math.max( 1, Runtime.getRuntime().availableProcessors() );
					final SharedQueue queue = requestedSharedQueue != null
							? requestedSharedQueue
							: ( createdSharedQueue = new SharedQueue( numFetcherThreads, maxNumLevels ) );
					cache = new VolatileGlobalCellCache( queue );
				}
				catch ( final IOException e )
				{
					throw new RuntimeException( e );
				}

				isOpen = true;
			}
		}
	}

	/**
	 * Clear the cache. Images that were obtained from
	 * this loader before {@link #close()} will stop working. Requesting images
	 * after {@link #close()} will cause the n5 to be reopened (with a
	 * new cache).
	 */
	public void close()
	{
		if ( isOpen )
		{
			synchronized ( this )
			{
				if ( !isOpen )
					return;

				if ( createdSharedQueue != null )
					createdSharedQueue.shutdown();
				cache.clearCache();

				createdSharedQueue = null;
				isOpen = false;
			}
		}
	}

	@Override
	public SetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		open();
		return setupImgLoaders.get( setupId );
	}

	private < T extends NativeType< T >, V extends Volatile< T > & NativeType< V > > SetupImgLoader< T, V > createSetupImgLoader( final int setupId ) throws IOException
	{
		final DataType dataType;
		try
		{
			dataType = n5properties.getDataType( n5, setupId );
		}
		catch ( final N5Exception e )
		{
			throw new IOException( e );
		}
		return new SetupImgLoader<>( setupId, Cast.unchecked( DataTypeProperties.of( dataType ) ) );
	}

	@Override
	public CacheControl getCacheControl()
	{
		open();
		return cache;
	}

	public class SetupImgLoader< T extends NativeType< T >, V extends Volatile< T > & NativeType< V > >
			extends AbstractViewerSetupImgLoader< T, V >
			implements MultiResolutionSetupImgLoader< T >
	{
		private final int setupId;

		private final double[][] mipmapResolutions;

		private final AffineTransform3D[] mipmapTransforms;

		public SetupImgLoader( final int setupId, final DataTypeProperties< T, V, ?, ? > props ) throws IOException
		{
			this(setupId, props.type(), props.volatileType() );
		}

		public SetupImgLoader( final int setupId, final T type, final V volatileType ) throws IOException
		{
			super( type, volatileType );
			this.setupId = setupId;
			try
			{
				mipmapResolutions = n5properties.getMipmapResolutions( n5, setupId );
			}
			catch ( final N5Exception e )
			{
				throw new IOException( e );
			}
			mipmapTransforms = new AffineTransform3D[ mipmapResolutions.length ];
			for ( int level = 0; level < mipmapResolutions.length; level++ )
				mipmapTransforms[ level ] = MipmapTransforms.getMipmapTransformDefault( mipmapResolutions[ level ] );
		}

		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return prepareCachedImage( timepointId, level, LoadingStrategy.BUDGETED, volatileType );
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return prepareCachedImage( timepointId, level, LoadingStrategy.BLOCKING, type );
		}

		@Override
		public Dimensions getImageSize( final int timepointId, final int level )
		{
			try
			{
				final String pathName = n5properties.getPath( setupId, timepointId, level );
				final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
				return new FinalDimensions( attributes.getDimensions() );
			}
			catch( final RuntimeException e )
			{
				return null;
			}
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return mipmapResolutions;
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return mipmapTransforms;
		}

		@Override
		public int numMipmapLevels()
		{
			return mipmapResolutions.length;
		}

		@Override
		public VoxelDimensions getVoxelSize( final int timepointId )
		{
			return null;
		}

		/**
		 * Create a {@link CellImg} backed by the cache.
		 */
		private < T extends NativeType< T > > RandomAccessibleInterval< T > prepareCachedImage( final int timepointId, final int level, final LoadingStrategy loadingStrategy, final T type )
		{
			try
			{
				final String pathName = n5properties.getPath( setupId, timepointId, level );
				final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
				final long[] dimensions = attributes.getDimensions();
				final int[] cellDimensions = attributes.getBlockSize();
				final CellGrid grid = new CellGrid( dimensions, cellDimensions );

				final int priority = numMipmapLevels() - 1 - level;
				final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );

				final SimpleCacheArrayLoader< ? > loader = createCacheArrayLoader( n5, pathName );
				return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
			}
			catch ( final IOException | N5Exception e )
			{
				System.err.println( String.format(
						"image data for timepoint %d setup %d level %d could not be found.",
						timepointId, setupId, level ) );
				return Views.interval(
						new ConstantRandomAccessible<>( type.createVariable(), 3 ),
						new FinalInterval( 1, 1, 1 ) );
			}
		}
	}

	private static class N5CacheArrayLoader< T, A extends DataAccess > implements SimpleCacheArrayLoader< A >
	{
		private final N5Reader n5;
		private final String pathName;
		private final DatasetAttributes attributes;
		private final IntFunction< T > createPrimitiveArray;
		private final Function< T, A > createVolatileArrayAccess;
		private final SubArrayCopy.Typed< T, T > subArrayCopy;

		N5CacheArrayLoader( final N5Reader n5, final String pathName, final DatasetAttributes attributes,
				final DataTypeProperties< ?, ?, T, A > dataTypeProperties )
		{
			this( n5, pathName, attributes, dataTypeProperties.createPrimitiveArray(), dataTypeProperties.createVolatileArrayAccess(),
					SubArrayCopy.forPrimitiveType( dataTypeProperties.type().getNativeTypeFactory().getPrimitiveType() ) );
		}

		N5CacheArrayLoader( final N5Reader n5, final String pathName, final DatasetAttributes attributes,
				final IntFunction< T > createPrimitiveArray,
				final Function< T, A > createVolatileArrayAccess,
				final SubArrayCopy.Typed< T, T > subArrayCopy )
		{

			this.n5 = n5;
			this.pathName = pathName;
			this.attributes = attributes;
			this.createPrimitiveArray = createPrimitiveArray;
			this.createVolatileArrayAccess = createVolatileArrayAccess;
			this.subArrayCopy = subArrayCopy;
		}

		@Override
		public A loadArray( final long[] gridPosition, final int[] cellDimensions ) throws IOException
		{
			final DataBlock< T > dataBlock;
			try
			{
				dataBlock = Cast.unchecked( n5.readBlock( pathName, attributes, gridPosition ) );
			}
			catch ( final N5Exception e )
			{
				throw new IOException( e );
			}
			if ( dataBlock != null && Arrays.equals( dataBlock.getSize(), cellDimensions ) )
			{
				return createVolatileArrayAccess.apply( dataBlock.getData() );
			}
			else
			{
				final T data = createPrimitiveArray.apply( ( int ) Intervals.numElements( cellDimensions ) );
				if ( dataBlock != null )
				{
					final T src = dataBlock.getData();
					final int[] srcDims = dataBlock.getSize();
					final int[] pos = new int[ srcDims.length ];
					final int[] size = new int[ srcDims.length ];
					Arrays.setAll( size, d -> Math.min( srcDims[ d ], cellDimensions[ d ] ) );
					subArrayCopy.copy( src, srcDims, pos, data, cellDimensions, pos, size );
				}
				return createVolatileArrayAccess.apply( data );
			}
		}
	}

	public static SimpleCacheArrayLoader< ? > createCacheArrayLoader( final N5Reader n5, final String pathName ) throws IOException
	{
		final DatasetAttributes attributes;
		try
		{
			attributes = n5.getDatasetAttributes( pathName );
		}
		catch ( final N5Exception e )
		{
			throw new IOException( e );
		}
		return new N5CacheArrayLoader<>( n5, pathName, attributes, DataTypeProperties.of( attributes.getDataType() ) );
	}
}
