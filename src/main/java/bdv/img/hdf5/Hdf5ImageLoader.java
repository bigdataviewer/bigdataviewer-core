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
package bdv.img.hdf5;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.cache.SharedQueue;
import bdv.img.MipmapInfo;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.n5.DataTypeProperties;
import bdv.util.ConstantRandomAccessible;
import bdv.util.MipmapTransforms;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.DataAccess;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import static bdv.img.hdf5.Util.getCellsPath;
import static bdv.img.hdf5.Util.getResolutionsPath;
import static bdv.img.hdf5.Util.getSubdivisionsPath;
import static bdv.img.hdf5.Util.memTypeId;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;
import static net.imglib2.util.Util.printCoordinates;

public class Hdf5ImageLoader implements ViewerImgLoader, MultiResolutionImgLoader
{
	private final File hdf5File;

	/**
	 * The {@link bdv.img.hdf5.Hdf5ImageLoader} can be constructed with an existing
	 * {@link IHDF5Reader} which if non-null will be used instead of creating a
	 * new one on {@link #hdf5File}.
	 *
	 * <p>
	 * <em>Note that {@link #close()} will not close the existingHdf5Reader!</em>
	 */
	private final IHDF5Reader existingHdf5Reader;

	/**
	 * List of partitions if the dataset is split across several files
	 */
	private final ArrayList< Partition > partitions;

	private final AbstractSequenceDescription< ?, ?, ? > seq;

	/**
	 * Maps setup id to {@link SetupImgLoader}.
	 */
	private final Map< Integer, SetupImgLoader > setupImgLoaders = new HashMap<>();

	private volatile boolean isOpen = false;
	private SharedQueue createdSharedQueue;
	private VolatileGlobalCellCache cache;
	private IHDF5Reader hdf5Reader;
	private HDF5Access hdf5Access;

	private int requestedNumFetcherThreads = -1;
	private SharedQueue requestedSharedQueue;

	/**
	 *
	 * @param hdf5File
	 * @param hdf5Partitions
	 * @param sequenceDescription
	 *            the {@link AbstractSequenceDescription}. When loading images,
	 *            this may be used to retrieve additional information for a
	 *            {@link ViewId}, such as setup name, {@link Angle},
	 *            {@link Channel}, etc.
	 */
	public Hdf5ImageLoader( final File hdf5File, final ArrayList< Partition > hdf5Partitions, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this( hdf5File, hdf5Partitions, sequenceDescription, false );
	}

	public Hdf5ImageLoader( final File hdf5File, final ArrayList< Partition > hdf5Partitions, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription, final boolean doOpen )
	{
		this( hdf5File, null, hdf5Partitions, sequenceDescription, doOpen );
	}

	protected Hdf5ImageLoader( final File hdf5File, final IHDF5Reader existingHdf5Reader, final ArrayList< Partition > hdf5Partitions, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription, final boolean doOpen )
	{
		this.existingHdf5Reader = existingHdf5Reader;
		this.hdf5File = hdf5File;
		this.seq = sequenceDescription;
		this.partitions = new ArrayList<>();
		if ( hdf5Partitions != null )
			this.partitions.addAll( hdf5Partitions );
		if ( doOpen )
			open();
	}

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
					hdf5Reader = ( existingHdf5Reader != null ) ? existingHdf5Reader : HDF5Factory.openForReading( hdf5File );
					hdf5Access = new HDF5Access( hdf5Reader );

					int maxNumLevels = 0;
					final List< ? extends BasicViewSetup > setups = seq.getViewSetupsOrdered();
					for ( final BasicViewSetup setup : setups )
					{
						final int setupId = setup.getId();
						final SetupImgLoader setupImgLoader = createSetupImgLoader( setupId );
						setupImgLoaders.put( setupId, setupImgLoader );
						maxNumLevels = Math.max( maxNumLevels, setupImgLoader.numMipmapLevels() );
					}

					final int numFetcherThreads = requestedNumFetcherThreads >= 0
							? requestedNumFetcherThreads
							: 1;
					final SharedQueue queue = requestedSharedQueue != null
							? requestedSharedQueue
							: ( createdSharedQueue = new SharedQueue( numFetcherThreads, maxNumLevels ) );
					cache = new VolatileGlobalCellCache( queue );
				}
				catch ( IOException e )
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
				hdf5Access.closeAllDataSets();

				// only close reader if we constructed it ourselves
				if ( existingHdf5Reader == null )
					hdf5Access.close();

				createdSharedQueue = null;
				isOpen = false;
			}
		}
	}

//	public void initCachedDimensionsFromHdf5( final boolean background ) // REMOVED

	public File getHdf5File()
	{
		return hdf5File;
	}

	public ArrayList< Partition > getPartitions()
	{
		return partitions;
	}

	@Override
	public CacheControl getCacheControl()
	{
		open();
		return cache;
	}

//	public Hdf5VolatileShortArrayLoader getShortArrayLoader() // REMOVED


	/**
	 * Checks whether the given image data is present in the hdf5. Missing data
	 * may be caused by missing partition files
	 *
	 * @return true, if the given image data is present.
	 */
	public boolean existsImageData( final ViewLevelId id )
	{
		return getDimsAndExistence( id ).exists();
	}

	public DimsAndExistence getDimsAndExistence( final ViewLevelId id )
	{
		open();
		return hdf5Access.getDimsAndExistence( Util.getCellsPath( id ) );
	}

	public void printMipmapInfo()
	{
		open();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{
			final int setupId = setup.getId();
			System.out.println( "setup " + setupId );
			final MipmapInfo mipmapInfo = getSetupImgLoader( setupId ).getMipmapInfo();
			final double[][] reslevels = mipmapInfo.getResolutions();
			final int[][] subdiv = mipmapInfo.getSubdivisions();
			final int numLevels = mipmapInfo.getNumLevels();
			System.out.println( "    resolutions:");
			for ( int level = 0; level < numLevels; ++level )
			{
				final double[] res = reslevels[ level ];
				System.out.println( "    " + level + ": " + printCoordinates( res ) );
			}
			System.out.println( "    subdivisions:");
			for ( int level = 0; level < numLevels; ++level )
			{
				final int[] res = subdiv[ level ];
				System.out.println( "    " + level + ": " + printCoordinates( res ) );
			}
			System.out.println( "    level sizes:" );
			final int timepointId = seq.getTimePoints().getTimePointsOrdered().get( 0 ).getId();
			for ( int level = 0; level < numLevels; ++level )
			{
				final DimsAndExistence dims = getDimsAndExistence( new ViewLevelId( timepointId, setupId, level ) );
				final long[] dimensions = dims.getDimensions();
				System.out.println( "    " + level + ": " + printCoordinates( dimensions ) );
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
		DataType dataType = tryGetDataType( setupId );
		final boolean legacyInt16 = ( dataType == null );
		if ( legacyInt16 )
			dataType = DataType.UINT16;
		return new SetupImgLoader<>( hdf5Reader, hdf5Access, setupId, Cast.unchecked( DataTypeProperties.of( dataType ) ), legacyInt16 );
	}

	private DataType tryGetDataType( final int setupId )
	{
		try
		{
			return DataType.fromString( hdf5Reader.string().getAttr( Util.getSetupPath( setupId ), "dataType" ) );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public class SetupImgLoader< T extends NativeType< T >, V extends Volatile< T > & NativeType< V > >
			extends AbstractViewerSetupImgLoader< T, V >
			implements MultiResolutionSetupImgLoader< T >
	{
		private final HDF5Access hdf5Access;

		private final DataTypeProperties< ?, ?, ?, ? > typeProps;

		private final int setupId;

		private final MipmapInfo mipmapInfo;

		private final boolean legacyInt16;

		public SetupImgLoader(
				final IHDF5Reader hdf5Reader, final HDF5Access hdf5Access,
				final int setupId,
				final DataTypeProperties< T, V, ?, ? > typeProps, final boolean legacyInt16 )
		{
			super( typeProps.type(), typeProps.volatileType() );
			this.hdf5Access = hdf5Access;
			this.typeProps = typeProps;
			this.setupId = setupId;
			final double[][] mipmapResolutions = hdf5Reader.readDoubleMatrix( getResolutionsPath( setupId ) ); // TODO: make part of HDF5AccessHack so that we don't need hdf5Reader here?
			final AffineTransform3D[] mipmapTransforms = new AffineTransform3D[ mipmapResolutions.length ];
			for ( int level = 0; level < mipmapResolutions.length; level++ )
				mipmapTransforms[ level ] = MipmapTransforms.getMipmapTransformDefault( mipmapResolutions[ level ] );
			final int[][] subdivisions = hdf5Reader.readIntMatrix( getSubdivisionsPath( setupId ) );
			this.mipmapInfo = new MipmapInfo( mipmapResolutions, mipmapTransforms, subdivisions );
			this.legacyInt16 = legacyInt16;
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

		public MipmapInfo getMipmapInfo()
		{
			return mipmapInfo;
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return mipmapInfo.getResolutions();
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return mipmapInfo.getTransforms();
		}

		@Override
		public int numMipmapLevels()
		{
			return mipmapInfo.getNumLevels();
		}

		@Override
		public Dimensions getImageSize( final int timepointId, final int level )
		{
			final String pathName = getCellsPath( timepointId, setupId, level );
			final DimsAndExistence dims = hdf5Access.getDimsAndExistence( pathName );
			if ( dims.exists() )
				return new FinalDimensions( dims.getDimensions() );
			else
				return null;
		}

		@Override
		public VoxelDimensions getVoxelSize( final int timepointId )
		{
			// the voxel size is not stored in the hdf5
			return null;
		}

		/**
		 * Create a {@link CellImg} backed by the cache.
		 */
		private < T extends NativeType< T > > RandomAccessibleInterval< T > prepareCachedImage( final int timepointId, final int level, final LoadingStrategy loadingStrategy, final T type )
		{
			final String pathName = getCellsPath( timepointId, setupId, level );
			final DimsAndExistence dims = hdf5Access.getDimsAndExistence( pathName );
			if ( !dims.exists() )
			{
				System.err.println( String.format(
						"image data for timepoint %d setup %d level %d could not be found. Partition file missing?",
						timepointId, setupId, level ) );
				return Views.interval(
						new ConstantRandomAccessible<>( type.createVariable(), 3 ),
						new FinalInterval( 1, 1, 1 ) );
			}

			final long[] dimensions = dims.getDimensions();
			final int[] cellDimensions = ( dims.getBlockSize() != null )
					? dims.getBlockSize()
					: mipmapInfo.getSubdivisions()[ level ];
			final CellGrid grid = new CellGrid( dimensions, cellDimensions );

			final int priority = mipmapInfo.getMaxLevel() - level;
			final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
			final CacheArrayLoader< ? > loader = new Hdf5CacheArrayLoader<>( hdf5Access, pathName, typeProps, legacyInt16 );
			return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
		}
	}

	private static class Hdf5CacheArrayLoader< T, A extends DataAccess > implements CacheArrayLoader< A >
	{
		private final HDF5Access hdf5Access;
		private final String pathName;
		private final DataType dataType;
		private final long memTypeId;
		private final Function< T, A > createVolatileArrayAccess;

		Hdf5CacheArrayLoader( final HDF5Access hdf5Access, final String pathName,
				final DataTypeProperties< ?, ?, T, A > typeProps, final boolean legacyInt16 )
		{
			this.hdf5Access = hdf5Access;
			this.pathName = pathName;
			this.dataType = typeProps.dataType();
			this.memTypeId = legacyInt16 ? H5T_NATIVE_INT16 : memTypeId( dataType );
			this.createVolatileArrayAccess = typeProps.createVolatileArrayAccess();
		}

		@Override
		public A loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
		{
			final DataBlock< T > dataBlock = Cast.unchecked( hdf5Access.readBlock( pathName, dataType, memTypeId, dimensions, min ) );
			return createVolatileArrayAccess.apply( dataBlock.getData() );
		}
	}


	// TODO: Support loading images fully (into ArrayImg/CellImg), when ImgLoaderHints.LOAD_COMPLETELY is given
	//       Old code below

	/*
	private RandomAccessibleInterval< UnsignedShortType > loadImageCompletely( final int timepointId, final int level )
	{
		open();

		final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
		if ( ! existsImageData( id ) )
		{
			System.err.println(	String.format(
					"image data for timepoint %d setup %d level %d could not be found. Partition file missing?",
					id.getTimePointId(), id.getViewSetupId(), id.getLevel() ) );
			return getMissingDataImage( id, type );
		}

		Img< UnsignedShortType > img = null;
		final DimsAndExistence dimsAndExistence = getDimsAndExistence( new ViewLevelId( timepointId, setupId, level ) );
		final long[] dimsLong = dimsAndExistence.exists() ? dimsAndExistence.getDimensions() : null;
		final int n = dimsLong.length;
		final int[] dimsInt = new int[ n ];
		final long[] min = new long[ n ];
		if ( Intervals.numElements( new FinalDimensions( dimsLong ) ) <= Integer.MAX_VALUE )
		{
			// use ArrayImg
			for ( int d = 0; d < dimsInt.length; ++d )
				dimsInt[ d ] = ( int ) dimsLong[ d ];
			short[] data = null;
			try
			{
				data = hdf5Access.readShortMDArrayBlockWithOffset( timepointId, setupId, level, dimsInt, min );
			}
			catch ( final InterruptedException e )
			{}
			img = ArrayImgs.unsignedShorts( data, dimsLong );
		}
		else
		{
			final int[] cellDimensions = computeCellDimensions(
					dimsLong,
					mipmapInfo.getSubdivisions()[ level ] );
			final CellImgFactory< UnsignedShortType > factory = new CellImgFactory<>( type, cellDimensions );
			@SuppressWarnings( "unchecked" )
			final CellImg< UnsignedShortType, ShortArray > cellImg = ( CellImg< UnsignedShortType, ShortArray > ) factory.create( dimsLong );
			final Cursor< Cell< ShortArray > > cursor = cellImg.getCells().cursor();
			while ( cursor.hasNext() )
			{
				final Cell< ShortArray > cell = cursor.next();
				final short[] dataBlock = cell.getData().getCurrentStorageArray();
				cell.dimensions( dimsInt );
				cell.min( min );
				try
				{
					final short[] data = hdf5Access.readShortMDArrayBlockWithOffset( timepointId, setupId, level, dimsInt, min );
					System.arraycopy( data, 0, dataBlock, 0, dataBlock.length );
				}
				catch ( final InterruptedException e )
				{}
			}
			img = cellImg;
		}
		return img;
	}

	private int[] computeCellDimensions( final long[] dimsLong, final int[] chunkSize )
	{
		final int n = dimsLong.length;

		final long[] dimsInChunks = new long[ n ];
		int elementsPerChunk = 1;
		for ( int d = 0; d < n; ++d )
		{
			dimsInChunks[ d ] = ( dimsLong[ d ] + chunkSize[ d ] - 1 ) / chunkSize[ d ];
			elementsPerChunk *= chunkSize[ d ];
		}

		final int[] cellDimensions = new int[ n ];
		long s = Integer.MAX_VALUE / elementsPerChunk;
		for ( int d = 0; d < n; ++d )
		{
			final long ns = s / dimsInChunks[ d ];
			if ( ns > 0 )
				cellDimensions[ d ] = chunkSize[ d ] * ( int ) ( dimsInChunks[ d ] );
			else
			{
				cellDimensions[ d ] = chunkSize[ d ] * ( int ) ( s % dimsInChunks[ d ] );
				for ( ++d; d < n; ++d )
					cellDimensions[ d ] = chunkSize[ d ];
				break;
			}
			s = ns;
		}
		return cellDimensions;
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		if ( Arrays.asList( hints ).contains( ImgLoaderHints.LOAD_COMPLETELY ) )
			return loadImageCompletely( timepointId, level );

		return prepareCachedImage( timepointId, level, LoadingStrategy.BLOCKING, type );
	}
	*/
}
