package bdv.img.hdf5;

import static bdv.img.hdf5.Util.getResolutionsPath;
import static bdv.img.hdf5.Util.getSubdivisionsPath;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.NativeImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.cell.DefaultCell;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import bdv.AbstractViewerImgLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import bdv.util.ConstantRandomAccessible;
import bdv.util.MipmapTransforms;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class Hdf5ImageLoader extends AbstractViewerImgLoader< UnsignedShortType, VolatileUnsignedShortType > implements ImgLoader< UnsignedShortType >
{
	protected File hdf5File;

	protected IHDF5Access hdf5Access;

	protected VolatileGlobalCellCache< VolatileShortArray > cache;

	/**
	 * Description of available mipmap levels for each {@link BasicViewSetup}.
	 * Contains for each mipmap level, the subsampling factors and subdivision
	 * block sizes. The {@link HashMap} key is the setup id.
	 */
	protected final HashMap< Integer, MipmapInfo > perSetupMipmapInfo;

	/**
	 * List of partitions if the dataset is split across several files
	 */
	protected final ArrayList< Partition > partitions;

	protected int maxNumLevels;

	/**
	 * Maps {@link ViewLevelId} (timepoint, setup, level) to
	 * {@link DimsAndExistence}. Every entry is either null or the existence and
	 * dimensions of one image. This is filled in when an image is loaded for
	 * the first time.
	 */
	protected final HashMap< ViewLevelId, DimsAndExistence > cachedDimsAndExistence;

	protected final AbstractSequenceDescription< ?, ?, ? > sequenceDescription;

	/**
	 *
	 * @param hdf5File
	 * @param hdf5Partitions
	 * @param sequenceDescription
	 *            the {@link AbstractSequenceDescription}. When
	 *            {@link #getImage(ViewId) loading} images, this may be used to
	 *            retrieve additional information for a {@link ViewId}, such as
	 *            setup name, {@link Angle}, {@link Channel}, etc.
	 */
	public Hdf5ImageLoader( final File hdf5File, final ArrayList< Partition > hdf5Partitions, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this( hdf5File, hdf5Partitions, sequenceDescription, true );
	}

	public Hdf5ImageLoader( final File hdf5File, final ArrayList< Partition > hdf5Partitions, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription, final boolean doOpen )
	{
		super( new UnsignedShortType(), new VolatileUnsignedShortType() );
		this.hdf5File = hdf5File;
		perSetupMipmapInfo = new HashMap< Integer, MipmapInfo >();
		cachedDimsAndExistence = new HashMap< ViewLevelId, DimsAndExistence >();
		this.sequenceDescription = sequenceDescription;
		partitions = new ArrayList< Partition >();
		if ( hdf5Partitions != null )
			partitions.addAll( hdf5Partitions );
		if ( doOpen )
			open();
	}

	private boolean isOpen = false;

	private void open()
	{
		if ( ! isOpen )
		{
			synchronized ( this )
			{
				if ( isOpen )
					return;
				isOpen = true;

				final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( hdf5File );

				maxNumLevels = 0;
				perSetupMipmapInfo.clear();
				final List< ? extends BasicViewSetup > setups = sequenceDescription.getViewSetupsOrdered();
				for ( final BasicViewSetup setup : setups )
				{
					final int setupId = setup.getId();

					final double[][] resolutions = hdf5Reader.readDoubleMatrix( getResolutionsPath( setupId ) );
					final AffineTransform3D[] transforms = new AffineTransform3D[ resolutions.length ];
					for ( int level = 0; level < resolutions.length; level++ )
						transforms[ level ] = MipmapTransforms.getMipmapTransformDefault( resolutions[ level ] );
					final int[][] subdivisions = hdf5Reader.readIntMatrix( getSubdivisionsPath( setupId ) );

					if ( resolutions.length > maxNumLevels )
						maxNumLevels = resolutions.length;

					perSetupMipmapInfo.put( setupId, new MipmapInfo( resolutions, transforms, subdivisions ) );
				}

				cachedDimsAndExistence.clear();

				final List< TimePoint > timepoints = sequenceDescription.getTimePoints().getTimePointsOrdered();
				final int maxNumTimepoints = timepoints.get( timepoints.size() - 1 ).getId() + 1;
				final int maxNumSetups = setups.get( setups.size() - 1 ).getId() + 1;
				try
				{
					hdf5Access = new HDF5AccessHack( hdf5Reader );
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
					hdf5Access = new HDF5Access( hdf5Reader );
				}
				cache = new VolatileGlobalCellCache< VolatileShortArray >( new Hdf5VolatileShortArrayLoader( hdf5Access ), maxNumTimepoints, maxNumSetups, maxNumLevels, 1 );
			}
		}
	}

	public void initCachedDimensionsFromHdf5( final boolean background )
	{
		open();
		final long t0 = System.currentTimeMillis();
		final List< TimePoint > timepoints = sequenceDescription.getTimePoints().getTimePointsOrdered();
		final List< ? extends BasicViewSetup > setups = sequenceDescription.getViewSetupsOrdered();
		for ( final TimePoint timepoint : timepoints )
		{
			final int t = timepoint.getId();
			for ( final BasicViewSetup setup : setups )
			{
				final int s = setup.getId();
				final int numLevels = perSetupMipmapInfo.get( s ).getNumLevels();
				for ( int l = 0; l < numLevels; ++l )
					getDimsAndExistence( new ViewLevelId( t, s, l ) );
			}
			if ( background )
				synchronized ( this )
				{
					try
					{
						wait( 100 );
					}
					catch ( final InterruptedException e )
					{}
				}
		}
		final long t1 = System.currentTimeMillis() - t0;
		System.out.println( "initCachedDimensionsFromHdf5 : " + t1 + " ms" );
	}

	public File getHdf5File()
	{
		return hdf5File;
	}

	public ArrayList< Partition > getPartitions()
	{
		return partitions;
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view, final int level )
	{
		final ViewLevelId id = new ViewLevelId( view, level );
		if ( ! existsImageData( id ) )
		{
			System.err.println(	String.format(
					"image data for timepoint %d setup %d level %d could not be found. Partition file missing?",
					id.getTimePointId(), id.getViewSetupId(), id.getLevel() ) );
			return getMissingDataImage( id, new UnsignedShortType() );
		}
		final CachedCellImg< UnsignedShortType, VolatileShortArray >  img = prepareCachedImage( id, LoadingStrategy.BLOCKING );
		final UnsignedShortType linkedType = new UnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final ViewId view, final int level )
	{
		final ViewLevelId id = new ViewLevelId( view, level );
		if ( ! existsImageData( id ) )
		{
			System.err.println(	String.format(
					"image data for timepoint %d setup %d level %d could not be found. Partition file missing?",
					id.getTimePointId(), id.getViewSetupId(), id.getLevel() ) );
			return getMissingDataImage( id, new VolatileUnsignedShortType() );
		}
		final CachedCellImg< VolatileUnsignedShortType, VolatileShortArray >  img = prepareCachedImage( id, LoadingStrategy.BUDGETED );
		final VolatileUnsignedShortType linkedType = new VolatileUnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public VolatileGlobalCellCache< VolatileShortArray > getCache()
	{
		open();
		return cache;
	}

	@Override
	public double[][] getMipmapResolutions( final int setupId )
	{
		return getMipmapInfo( setupId ).getResolutions();
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setupId )
	{
		return getMipmapInfo( setupId ).getTransforms();
	}

	@Override
	public int numMipmapLevels( final int setupId )
	{
		return getMipmapInfo( setupId ).getNumLevels();
	}

	public MipmapInfo getMipmapInfo( final int setupId )
	{
		open();
		return perSetupMipmapInfo.get( setupId );
	}

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

	/**
	 * For images that are missing in the hdf5, a constant image is created.
	 * If the dimension of the missing image is present in {@link #cachedDimensions} then use that.
	 * Otherwise create a 1x1x1 image.
	 */
	protected < T > RandomAccessibleInterval< T > getMissingDataImage( final ViewLevelId id, final T constant )
	{
		final long[] d = getDimsAndExistence( id ).getDimensions();
		return Views.interval( new ConstantRandomAccessible< T >( constant, 3 ), new FinalInterval( d ) );
	}

	public DimsAndExistence getDimsAndExistence( final ViewLevelId id )
	{
		open();
		DimsAndExistence dims = cachedDimsAndExistence.get( id );
		if ( dims == null )
		{
			// pause Fetcher threads for 5 ms. There will be more calls to
			// getImageDimension() because this happens when a timepoint is
			// loaded, and all setups for the timepoint are loaded then. We
			// don't want to interleave this with block loading operations.
			cache.pauseFetcherThreadsFor( 5 );
			dims = hdf5Access.getDimsAndExistence( id );
			cachedDimsAndExistence.put( id, dims );
		}
		return dims;
	}

	/**
	 * (Almost) create a {@link CellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, VolatileShortArray > prepareCachedImage( final ViewLevelId id, final LoadingStrategy loadingStrategy )
	{
		open();
		final int timepointId = id.getTimePointId();
		final int setupId = id.getViewSetupId();
		final int level = id.getLevel();
		final MipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupId );

		final long[] dimensions = getDimsAndExistence( id ).getDimensions();
		final int[] cellDimensions = mipmapInfo.getSubdivisions()[ level ];

		final int priority = mipmapInfo.getMaxLevel() - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< VolatileShortArray > c = cache.new VolatileCellCache( timepointId, setupId, level, cacheHints );
		final VolatileImgCells< VolatileShortArray > cells = new VolatileImgCells< VolatileShortArray >( c, new Fraction(), dimensions, cellDimensions );
		final CachedCellImg< T, VolatileShortArray > img = new CachedCellImg< T, VolatileShortArray >( cells );
		return img;
	}

	public void printMipmapInfo()
	{
		open();
		for ( final BasicViewSetup setup : sequenceDescription.getViewSetupsOrdered() )
		{
			final int setupId = setup.getId();
			System.out.println( "setup " + setupId );
			final MipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupId );
			final double[][] reslevels = mipmapInfo.getResolutions();
			final int[][] subdiv = mipmapInfo.getSubdivisions();
			final int numLevels = mipmapInfo.getNumLevels();
			System.out.println( "    resolutions:");
			for ( int level = 0; level < numLevels; ++level )
			{
				final double[] res = reslevels[ level ];
				System.out.println( "    " + level + ": " + net.imglib2.util.Util.printCoordinates( res ) );
			}
			System.out.println( "    subdivisions:");
			for ( int level = 0; level < numLevels; ++level )
			{
				final int[] res = subdiv[ level ];
				System.out.println( "    " + level + ": " + net.imglib2.util.Util.printCoordinates( res ) );
			}
			System.out.println( "    level sizes:" );
			final int timepointId = sequenceDescription.getTimePoints().getTimePointsOrdered().get( 0 ).getId();
			for ( int level = 0; level < numLevels; ++level )
			{
				final DimsAndExistence dims = getDimsAndExistence( new ViewLevelId( timepointId, setupId, level ) );
				final long[] dimensions = dims.getDimensions();
				System.out.println( "    " + level + ": " + net.imglib2.util.Util.printCoordinates( dimensions ) );
			}
		}
	}

	public MonolithicImageLoader getMonolithicImageLoader()
	{
		return new MonolithicImageLoader();
	}

	public class MonolithicImageLoader implements ImgLoader< UnsignedShortType >
	{
		@Override
		public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view )
		{
			Img< UnsignedShortType > img = null;
			final int timepoint = view.getTimePointId();
			final int setup = view.getViewSetupId();
			final int level = 0;
			final Dimensions dims = getImageSize( view );
			final int n = dims.numDimensions();
			final long[] dimsLong = new long[ n ];
			dims.dimensions( dimsLong );
			final int[] dimsInt = new int[ n ];
			final long[] min = new long[ n ];
			if ( Intervals.numElements( dims ) <= Integer.MAX_VALUE )
			{
				// use ArrayImg
				for ( int d = 0; d < dimsInt.length; ++d )
					dimsInt[ d ] = ( int ) dimsLong[ d ];
				short[] data = null;
				try
				{
					data = hdf5Access.readShortMDArrayBlockWithOffset( timepoint, setup, level, dimsInt, min );
				}
				catch ( final InterruptedException e )
				{}
				img = ArrayImgs.unsignedShorts( data, dimsLong );
			}
			else
			{
				final int[] cellDimensions = computeCellDimensions(
						dimsLong,
						perSetupMipmapInfo.get( setup ).getSubdivisions()[ level ] );
				final CellImgFactory< UnsignedShortType > factory = new CellImgFactory< UnsignedShortType >( cellDimensions );
				@SuppressWarnings( "unchecked" )
				final CellImg< UnsignedShortType, ShortArray, DefaultCell< ShortArray > > cellImg =
					( CellImg< UnsignedShortType, ShortArray, DefaultCell< ShortArray > > ) factory.create( dimsLong, new UnsignedShortType() );
				final Cursor< DefaultCell< ShortArray > > cursor = cellImg.getCells().cursor();
				while ( cursor.hasNext() )
				{
					final DefaultCell< ShortArray > cell = cursor.next();
					final short[] dataBlock = cell.getData().getCurrentStorageArray();
					cell.dimensions( dimsInt );
					cell.min( min );
					try
					{
						hdf5Access.readShortMDArrayBlockWithOffset( timepoint, setup, level, dimsInt, min, dataBlock );
					}
					catch ( final InterruptedException e )
					{}
				}
				img = cellImg;
			}
			return img;
		}

		@Override
		public RandomAccessibleInterval< FloatType > getFloatImage( final ViewId view, final boolean normalize )
		{
			Img< FloatType > img = null;
			final int timepoint = view.getTimePointId();
			final int setup = view.getViewSetupId();
			final int level = 0;
			final Dimensions dims = getImageSize( view );
			final int n = dims.numDimensions();
			final long[] dimsLong = new long[ n ];
			dims.dimensions( dimsLong );
			final int[] dimsInt = new int[ n ];
			final long[] min = new long[ n ];
			if ( Intervals.numElements( dims ) <= Integer.MAX_VALUE )
			{
				// use ArrayImg
				for ( int d = 0; d < dimsInt.length; ++d )
					dimsInt[ d ] = ( int ) dimsLong[ d ];
				float[] data = null;
				try
				{
					data = hdf5Access.readShortMDArrayBlockWithOffsetAsFloat( timepoint, setup, level, dimsInt, min );
				}
				catch ( final InterruptedException e )
				{}
				img = ArrayImgs.floats( data, dimsLong );
			}
			else
			{
				final int[] cellDimensions = computeCellDimensions(
						dimsLong,
						perSetupMipmapInfo.get( setup ).getSubdivisions()[ level ] );
				final CellImgFactory< FloatType > factory = new CellImgFactory< FloatType >( cellDimensions );
				@SuppressWarnings( "unchecked" )
				final CellImg< FloatType, FloatArray, DefaultCell< FloatArray > > cellImg =
					( CellImg< FloatType, FloatArray, DefaultCell< FloatArray > > ) factory.create( dimsLong, new FloatType() );
				final Cursor< DefaultCell< FloatArray > > cursor = cellImg.getCells().cursor();
				while ( cursor.hasNext() )
				{
					final DefaultCell< FloatArray > cell = cursor.next();
					final float[] dataBlock = cell.getData().getCurrentStorageArray();
					cell.dimensions( dimsInt );
					cell.min( min );
					try
					{
						hdf5Access.readShortMDArrayBlockWithOffsetAsFloat( timepoint, setup, level, dimsInt, min, dataBlock );
					}
					catch ( final InterruptedException e )
					{}
				}
				img = cellImg;
			}

			if ( normalize )
				// normalize the image to 0...1
				normalize( img );

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
				}
				s = ns;
			}
			return cellDimensions;
		}

		@Override
		public UnsignedShortType getImageType()
		{
			return Hdf5ImageLoader.this.getImageType();
		}

		@Override
		public Dimensions getImageSize( final ViewId view )
		{
			return Hdf5ImageLoader.this.getImageSize( view );
		}

		@Override
		public VoxelDimensions getVoxelSize( final ViewId view )
		{
			return Hdf5ImageLoader.this.getVoxelSize( view );
		}

	}

//  ================================ mpicbg.spim.data.sequence.ImgLoader =============================== //

	@Override
	public RandomAccessibleInterval< FloatType > getFloatImage( final ViewId view, final boolean normalize )
	{
		final RandomAccessibleInterval< UnsignedShortType > ushortImg = getImage( view );

		// copy unsigned short img to float img
		final FloatType f = new FloatType();
		final Img< FloatType > floatImg = net.imglib2.util.Util.getArrayOrCellImgFactory( ushortImg, f ).create( ushortImg, f );

		// set up executor service
		final int numProcessors = Runtime.getRuntime().availableProcessors();
		final ExecutorService taskExecutor = Executors.newFixedThreadPool( numProcessors );
		final ArrayList< Callable< Void > > tasks = new ArrayList< Callable< Void > >();

		// set up all tasks
		final int numPortions = numProcessors * 2;
		final long threadChunkSize = floatImg.size() / numPortions;
		final long threadChunkMod = floatImg.size() % numPortions;

		for ( int portionID = 0; portionID < numPortions; ++portionID )
		{
			// move to the starting position of the current thread
			final long startPosition = portionID * threadChunkSize;

			// the last thread may has to run longer if the number of pixels cannot be divided by the number of threads
			final long loopSize = ( portionID == numPortions - 1 ) ? threadChunkSize + threadChunkMod : threadChunkSize;

			tasks.add( new Callable< Void >()
			{
				@Override
				public Void call() throws Exception
				{
					final Cursor< UnsignedShortType > in = Views.iterable( ushortImg ).localizingCursor();
					final RandomAccess< FloatType > out = floatImg.randomAccess();

					in.jumpFwd( startPosition );

					for ( long j = 0; j < loopSize; ++j )
					{
						final UnsignedShortType vin = in.next();
						out.setPosition( in );
						out.get().set( vin.getRealFloat() );
					}

					return null;
				}
			});
		}

		try
		{
			// invokeAll() returns when all tasks are complete
			taskExecutor.invokeAll( tasks );
			taskExecutor.shutdown();
		}
		catch ( final InterruptedException e )
		{
			return null;
		}

		if ( normalize )
			// normalize the image to 0...1
			normalize( floatImg );

		return floatImg;
	}

	/**
	 * normalize img to 0...1
	 */
	protected static void normalize( final IterableInterval< FloatType > img )
	{
		float currentMax = img.firstElement().get();
		float currentMin = currentMax;
		for ( final FloatType t : img )
		{
			final float f = t.get();
			if ( f > currentMax )
				currentMax = f;
			else if ( f < currentMin )
				currentMin = f;
		}

		final float scale = ( float ) ( 1.0 / ( currentMax - currentMin ) );
		for ( final FloatType t : img )
			t.set( ( t.get() - currentMin ) * scale );
	}

	@Override
	public Dimensions getImageSize( final ViewId view )
	{
		final ViewLevelId id = new ViewLevelId( view, 0 );
		final DimsAndExistence dims = getDimsAndExistence( id );
		if ( dims.exists() )
			return new FinalDimensions( dims.getDimensions() );
		else
			return null;
	}

	@Override
	public VoxelDimensions getVoxelSize( final ViewId view )
	{
		// the voxel size is not stored in the hdf5
		return null;
	}
}
