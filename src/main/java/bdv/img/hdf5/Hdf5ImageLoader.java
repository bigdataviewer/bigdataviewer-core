package bdv.img.hdf5;

import static bdv.img.hdf5.Util.getResolutionsPath;
import static bdv.img.hdf5.Util.getSubdivisionsPath;
import static bdv.img.hdf5.Util.reorder;
import static mpicbg.spim.data.XmlHelpers.loadPath;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;

import mpicbg.spim.data.View;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.view.Views;

import org.jdom2.Element;

import bdv.ViewerImgLoader;
import bdv.img.cache.VolatileCell;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class Hdf5ImageLoader implements ViewerImgLoader
{
	protected File hdf5File;

	protected IHDF5Reader hdf5Reader;

	protected VolatileGlobalCellCache< VolatileShortArray > cache;

	protected final ArrayList< double[][] > perSetupMipmapResolutions;

	protected final ArrayList< int[][] > perSetupSubdivisions;

	/**
	 * List of partitions if the dataset is split across several files
	 */
	protected final ArrayList< Partition > partitions;

	protected int numTimepoints;

	protected int numSetups;

	protected int[] maxLevels;

	protected int maxNumLevels;

	/**
	 * An array of long[] arrays with {@link #numTimepoints} *
	 * {@link #numSetups} * {@link #maxNumLevels} entries. Every entry is either
	 * null or the dimensions of one image (identified by flattened index of
	 * level, setup, and timepoint). This is either loaded from XML if present
	 * or otherwise filled in when an image is loaded for the first time.
	 */
	protected long[][] cachedDimensions;

	/**
	 * An array of Booleans with {@link #numTimepoints} *
	 * {@link #numSetups} * {@link #maxNumLevels} entries. Every entry is either
	 * null or the existence of one image (identified by flattened index of
	 * level, setup, and timepoint). This is either loaded from XML if present
	 * or otherwise filled in when an image is loaded for the first time.
	 */
	protected Boolean[] cachedExistence;

	protected final boolean isCoarsestLevelBlocking = true;

	public Hdf5ImageLoader()
	{
		this( null );
	}

	public Hdf5ImageLoader( final ArrayList< Partition > hdf5Partitions )
	{
		hdf5File = null;
		hdf5Reader = null;
		cache = null;
		perSetupMipmapResolutions = new ArrayList< double[][] >();
		perSetupSubdivisions = new ArrayList< int[][] >();
		partitions = new ArrayList< Partition >();
		if ( hdf5Partitions != null )
			partitions.addAll( hdf5Partitions );
		maxLevels = null;
		cachedDimensions = null;
		cachedExistence = null;
	}

	public Hdf5ImageLoader( final File hdf5File, final ArrayList< Partition > hdf5Partitions )
	{
		this( hdf5File, hdf5Partitions, true );
	}

	public Hdf5ImageLoader( final File hdf5File, final ArrayList< Partition > hdf5Partitions, final boolean doOpen )
	{
		this.hdf5File = hdf5File;
		perSetupMipmapResolutions = new ArrayList< double[][] >();
		perSetupSubdivisions = new ArrayList< int[][] >();
		partitions = new ArrayList< Partition >();
		if ( hdf5Partitions != null )
			partitions.addAll( hdf5Partitions );
		if ( doOpen )
			open();
	}

	private void open()
	{
		hdf5Reader = HDF5Factory.openForReading( hdf5File );
		numTimepoints = hdf5Reader.readInt( "numTimepoints" );
		numSetups = hdf5Reader.readInt( "numSetups" );

		maxNumLevels = 0;
		maxLevels = new int[ numSetups ];
		perSetupMipmapResolutions.clear();
		perSetupSubdivisions.clear();
		for ( int setup = 0; setup < numSetups; ++setup )
		{
			final double [][] mipmapResolutions = hdf5Reader.readDoubleMatrix( getResolutionsPath( setup ) );
			perSetupMipmapResolutions.add( mipmapResolutions );
			if ( mipmapResolutions.length > maxNumLevels )
				maxNumLevels = mipmapResolutions.length;
			maxLevels[ setup ] = mipmapResolutions.length - 1;

			final int [][] subdivisions = hdf5Reader.readIntMatrix( getSubdivisionsPath( setup ) );
			perSetupSubdivisions.add( subdivisions );
		}

		cachedDimensions = new long[ numTimepoints * numSetups * maxNumLevels ][];
		cachedExistence = new Boolean[ numTimepoints * numSetups * maxNumLevels ];

		cache = new VolatileGlobalCellCache< VolatileShortArray >( new Hdf5VolatileShortArrayLoader( hdf5Reader ), numTimepoints, numSetups, maxNumLevels, maxLevels );
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		try
		{
			final String path = loadPath( elem, "hdf5", basePath ).toString();
			partitions.clear();
			for ( final Element p : elem.getChildren( "partition" ) )
				partitions.add( new Partition( p, basePath ) );
			hdf5File = new File( path );
			open();
			tryInitImageDimensions( elem );
//			initCachedDimensionsFromHdf5();
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public Element toXml( final File basePath )
	{
		final Element elem = new Element( "ImageLoader" );
		elem.setAttribute( "class", getClass().getCanonicalName() );
		elem.addContent( XmlHelpers.pathElement( "hdf5", hdf5File, basePath ) );
		for ( final Partition partition : partitions )
			elem.addContent( partition.toXml( basePath ) );
		return elem;
	}

	public void initCachedDimensionsFromHdf5( final boolean background )
	{
		final long t0 = System.currentTimeMillis();
		for ( int t = 0; t < numTimepoints; ++t )
		{
			for ( int s = 0; s < numSetups; ++s )
				for ( int l = 0; l <= maxLevels[ s ]; ++l )
					getImageDimension( t, s, l );
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

	public void tryInitImageDimensions( final Element elem )
	{
		final Element dimsElem = elem.getChild( "ImageDimensions" );
		if ( dimsElem == null )
			return;
		for ( final Element dimElem : dimsElem.getChildren( "dimension" ) )
		{
			final ImageDimension d = new ImageDimension( dimElem );
			final int index = getViewInfoCacheIndex( d.getTimepoint(), d.getSetup(), d.getLevel() );
			cachedDimensions[ index ] = d.getDimensions();
		}
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
	public RandomAccessibleInterval< FloatType > getImage( final View view )
	{
		throw new UnsupportedOperationException( "currently not used" );
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		return getUnsignedShortImage( view, 0 );
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view, final int level )
	{
		if ( ! existsImageData( view, level ) )
		{
			System.err.println( "image data for " + view.getBasename() + " level " + level + " could not be found. Partition file missing?" );
			return getMissingDataImage( view, level, new UnsignedShortType() );
		}
		final CellImg< UnsignedShortType, VolatileShortArray, VolatileCell< VolatileShortArray > >  img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
		final UnsignedShortType linkedType = new UnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileUnsignedShortImage( final View view, final int level )
	{
		if ( ! existsImageData( view, level ) )
		{
			System.err.println( "image data for " + view.getBasename() + " level " + level + " could not be found. Partition file missing?" );
			return getMissingDataImage( view, level, new VolatileUnsignedShortType() );
		}
//		final CellImg< VolatileUnsignedShortType, VolatileShortArray, Hdf5Cell< VolatileShortArray > >  img = prepareCachedImage( view, level, LoadingStrategy.VOLATILE );
		final CellImg< VolatileUnsignedShortType, VolatileShortArray, VolatileCell< VolatileShortArray > >  img = prepareCachedImage( view, level, LoadingStrategy.BUDGETED );
		final VolatileUnsignedShortType linkedType = new VolatileUnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	public VolatileGlobalCellCache< VolatileShortArray > getCache()
	{
		return cache;
	}

	@Override
	public double[][] getMipmapResolutions( final int setup )
	{
		return perSetupMipmapResolutions.get( setup );
	}

	public int[][] getSubdivisions( final int setup )
	{
		return perSetupSubdivisions.get( setup );
	}

	@Override
	public int numMipmapLevels( final int setup )
	{
		return getMipmapResolutions( setup ).length;
	}

	/**
	 * Checks whether the given image data is present in the hdf5. Missing data
	 * may be caused by missing partition files
	 *
	 * @return true, if the given image data is present.
	 */
	protected boolean existsImageData( final View view, final int level )
	{
		return existsImageData( view.getTimepointIndex(), view.getSetupIndex(), level );
	}

	/**
	 * Checks whether the given image data is present in the hdf5. Missing data
	 * may be caused by missing partition files
	 *
	 * @return true, if the given image data is present.
	 */
	protected boolean existsImageData( final int timepoint, final int setup, final int level )
	{
//		return timepoint <= 20;
		final int index = getViewInfoCacheIndex( timepoint, setup, level );
		if ( cachedExistence[ index ] == null )
		{
			boolean exists = false;
			final String cellsPath = Util.getCellsPath( timepoint, setup, level );
			cache.pauseFetcherThreads();
			long t0 = System.currentTimeMillis();
			long t1;
			long t2;
			final String prevCellsPath;
			synchronized ( hdf5Reader )
			{
				t1 = System.currentTimeMillis() - t0;
				t0 = System.currentTimeMillis();
				try {
					exists = hdf5Reader.exists( cellsPath );
				} catch ( final Exception e ) {
					exists = false;
				}
				t2 = System.currentTimeMillis() - t0;
				prevCellsPath = Hdf5VolatileShortArrayLoader.previousCellsPath;
				Hdf5VolatileShortArrayLoader.previousCellsPath = cellsPath;
			}
			if ( t1 > 5 )
			{
				final StringWriter sw = new StringWriter();
				sw.write( "(existsImageData) waited " + t1 + " ms for hdf5Reader lock\n" );
				sw.write( "after   " + prevCellsPath + "\n" );
				sw.write( "loading " + cellsPath + "\n" );
				final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
				final boolean found = true;
				if ( !found )
				{
					for ( final StackTraceElement elem : trace )
						sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
				}
				System.out.println( sw.toString() );
			}
			if ( t2 > 20 )
			{
				final StringWriter sw = new StringWriter();
				sw.write( "(existsImageData) waited " + t2 + " ms for hdf5 exists\n" );
				sw.write( "after   " + prevCellsPath + "\n" );
				sw.write( "loading " + cellsPath + "\n" );
				final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
				final boolean found = true;
				if ( !found )
				{
					for ( final StackTraceElement elem : trace )
						sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
				}
				System.out.println( sw.toString() );
			}
			cachedExistence[ index ] = new Boolean( exists );
		}
		return cachedExistence[ index ];
	}

	/**
	 * For images that are missing in the hdf5, a constant image is created.
	 * If the dimension of the missing image is present in {@link #cachedDimensions} then use that.
	 * Otherwise create a 1x1x1 image.
	 */
	protected < T > RandomAccessibleInterval< T > getMissingDataImage( final View view, final int level, final T constant )
	{
		final int t = view.getTimepointIndex();
		final int s = view.getSetupIndex();
		final int index = getViewInfoCacheIndex( t, s, level );
		long[] d = cachedDimensions[ index ];
		if ( d == null )
			d = new long[] { 1, 1, 1 };
		return Views.interval( new ConstantRandomAccessible< T >( constant, 3 ), new FinalInterval( d ) );
	}

	protected long[] getImageDimension( final int timepoint, final int setup, final int level )
	{
		final int index = getViewInfoCacheIndex( timepoint, setup, level );
		if ( cachedDimensions[ index ] == null )
		{
			final String cellsPath = Util.getCellsPath( timepoint, setup, level );
			final HDF5DataSetInformation info;
			if ( existsImageData( timepoint, setup, level ) )
			{
				cache.pauseFetcherThreads();
				long t0 = System.currentTimeMillis();
				long t1;
				long t2;
				final String prevCellsPath;
				synchronized ( hdf5Reader )
				{
					t1 = System.currentTimeMillis() - t0;
					t0 = System.currentTimeMillis();
					info = hdf5Reader.getDataSetInformation( cellsPath );
					t2 = System.currentTimeMillis() - t0;
					prevCellsPath = Hdf5VolatileShortArrayLoader.previousCellsPath;
					Hdf5VolatileShortArrayLoader.previousCellsPath = cellsPath;
				}
				if ( t1 > 5 )
				{
					final StringWriter sw = new StringWriter();
					sw.write( "(getImageDimension) waited " + t1 + " ms for hdf5Reader lock\n" );
					sw.write( "after   " + prevCellsPath + "\n" );
					sw.write( "loading " + cellsPath + "\n" );
					final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
					final boolean found = true;
					if ( !found )
					{
						for ( final StackTraceElement elem : trace )
							sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
					}
					System.out.println( sw.toString() );
				}
				if ( t2 > 20 )
				{
					final StringWriter sw = new StringWriter();
					sw.write( "(getImageDimension) waited " + t2 + " ms for hdf5 exists\n" );
					sw.write( "after   " + prevCellsPath + "\n" );
					sw.write( "loading " + cellsPath + "\n" );
					final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
					final boolean found = true;
					if ( !found )
					{
						for ( final StackTraceElement elem : trace )
							sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
					}
					System.out.println( sw.toString() );
				}
				cachedDimensions[ index ] = reorder( info.getDimensions() );
			}
			else
				cachedDimensions[ index ] = new long[] { 1, 1, 1 };
		}
		return cachedDimensions[ index ];
	}

	private int getViewInfoCacheIndex( final int timepoint, final int setup, final int level )
	{
		return level + maxNumLevels * ( setup + numSetups * timepoint );
	}

	/**
	 * (Almost) create a {@link CellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > CellImg< T, VolatileShortArray, VolatileCell< VolatileShortArray > > prepareCachedImage( final View view, final int level, final LoadingStrategy loadingStrategy )
	{
		if ( hdf5Reader == null )
			throw new RuntimeException( "no hdf5 file open" );

		final long[] dimensions = getImageDimension( view.getTimepointIndex(), view.getSetupIndex(), level );
		final int[] cellDimensions = perSetupSubdivisions.get( view.getSetupIndex() )[ level ];

		final CellCache< VolatileShortArray > c = cache.new Hdf5CellCache( view.getTimepointIndex(), view.getSetupIndex(), level, loadingStrategy );
		final VolatileImgCells< VolatileShortArray > cells = new VolatileImgCells< VolatileShortArray >( c, 1, dimensions, cellDimensions );
		final CellImg< T, VolatileShortArray, VolatileCell< VolatileShortArray > > img = new CellImg< T, VolatileShortArray, VolatileCell< VolatileShortArray > >( null, cells );
		return img;
	}
}
