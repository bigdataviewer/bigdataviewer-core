package imaris;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Fraction;
import bdv.AbstractViewerImgLoader;
import bdv.img.cache.Cache;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import bdv.img.hdf5.DimsAndExistence;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.ViewLevelId;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class ImarisImageLoader extends AbstractViewerImgLoader< UnsignedByteType, VolatileUnsignedByteType >
{
	private IHDF5Access hdf5Access;

	private final MipmapInfo mipmapInfo;

	private final long[][] mipmapDimensions;

	private final File hdf5File;

	private final AbstractSequenceDescription< ?, ?, ? > sequenceDescription;

	private VolatileGlobalCellCache< VolatileByteArray > cache;

	/**
	 * Maps {@link ViewLevelId} (timepoint, setup, level) to
	 * {@link DimsAndExistence}. Every entry is either null or the existence and
	 * dimensions of one image. This is filled in when an image is loaded for
	 * the first time.
	 */
	private final HashMap< ViewLevelId, DimsAndExistence > cachedDimsAndExistence;

	public ImarisImageLoader( final File hdf5File, final MipmapInfo mipmapInfo, final long[][] mipmapDimensions, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		super( new UnsignedByteType(), new VolatileUnsignedByteType() );
		this.hdf5File = hdf5File;
		this.mipmapInfo = mipmapInfo;
		this.mipmapDimensions = mipmapDimensions;
		this.sequenceDescription = sequenceDescription;
		cachedDimsAndExistence = new HashMap< ViewLevelId, DimsAndExistence >();
	}

	private boolean isOpen = false;

	private void open()
	{
		if ( !isOpen )
		{
			synchronized ( this )
			{
				if ( isOpen )
					return;
				isOpen = true;

				final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( hdf5File );

				final List< TimePoint > timepoints = sequenceDescription.getTimePoints().getTimePointsOrdered();
				final List< ? extends BasicViewSetup > setups = sequenceDescription.getViewSetupsOrdered();

				final int maxNumTimepoints = timepoints.get( timepoints.size() - 1 ).getId() + 1;
				final int maxNumSetups = setups.get( setups.size() - 1 ).getId() + 1;
				final int maxNumLevels = mipmapInfo.getNumLevels();

				try
				{
					hdf5Access = new HDF5AccessHack( hdf5Reader );
				}
				catch ( final Exception e )
				{
					throw new RuntimeException( e );
				}
				cache = new VolatileGlobalCellCache< VolatileByteArray >( new ImarisVolatileByteArrayLoader( hdf5Access ), maxNumTimepoints, maxNumSetups, maxNumLevels, 1 );
			}
		}
	}

	@Override
	public RandomAccessibleInterval< UnsignedByteType > getImage( final ViewId view, final int level )
	{
		final ViewLevelId id = new ViewLevelId( view, level );
		final CachedCellImg< UnsignedByteType, VolatileByteArray > img = prepareCachedImage( id, LoadingStrategy.BLOCKING );
		final UnsignedByteType linkedType = new UnsignedByteType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedByteType > getVolatileImage( final ViewId view, final int level )
	{
		final ViewLevelId id = new ViewLevelId( view, level );
		final CachedCellImg< VolatileUnsignedByteType, VolatileByteArray > img = prepareCachedImage( id, LoadingStrategy.BUDGETED );
		final VolatileUnsignedByteType linkedType = new VolatileUnsignedByteType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	/**
	 * (Almost) create a {@link CellImg} backed by the cache. The created image
	 * needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked
	 * type} before it can be used. The type should be either
	 * {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, VolatileByteArray > prepareCachedImage( final ViewLevelId id, final LoadingStrategy loadingStrategy )
	{
		open();
		final int timepointId = id.getTimePointId();
		final int setupId = id.getViewSetupId();
		final int level = id.getLevel();
		final long[] dimensions = mipmapDimensions[ level ];
		final int[] cellDimensions = mipmapInfo.getSubdivisions()[ level ];

		final int priority = mipmapInfo.getMaxLevel() - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< VolatileByteArray > c = cache.new VolatileCellCache( timepointId, setupId, level, cacheHints );
		final VolatileImgCells< VolatileByteArray > cells = new VolatileImgCells< VolatileByteArray >( c, new Fraction(), dimensions, cellDimensions );
		final CachedCellImg< T, VolatileByteArray > img = new CachedCellImg< T, VolatileByteArray >( cells );
		return img;
	}

	@Override
	public double[][] getMipmapResolutions( final int setupId )
	{
		return mipmapInfo.getResolutions();
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setupId )
	{
		return mipmapInfo.getTransforms();
	}

	@Override
	public int numMipmapLevels( final int setupId )
	{
		return mipmapInfo.getNumLevels();
	}

	@Override
	public Cache getCache()
	{
		open();
		return cache;
	}
}
