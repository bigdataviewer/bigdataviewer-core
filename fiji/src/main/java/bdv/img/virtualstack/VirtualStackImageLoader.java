package bdv.img.virtualstack;

import ij.ImagePlus;

import java.io.File;

import mpicbg.spim.data.ViewDescription;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

import org.jdom2.Element;

import bdv.AbstractViewerImgLoader;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;

/**
 * ImageLoader backed by a ImagePlus. The ImagePlus may be virtual and in
 * contrast to the imglib2 wrappers, we do not try to load all slices into
 * memory. Instead slices are stored in {@link VolatileGlobalCellCache}.
 *
 * Use {@link #createFloatInstance(ImagePlus)},
 * {@link #createUnsignedByteInstance(ImagePlus)} or
 * {@link #createUnsignedShortInstance(ImagePlus)} depending on the ImagePlus
 * pixel type. ARGB is currently not supported.
 *
 * @param <T> (non-volatile) pixel type
 * @param <V> volatile pixel type
 * @param <A> volatile array access type
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public abstract class VirtualStackImageLoader< T extends NativeType< T >, V extends Volatile< T > & NativeType< V >, A extends VolatileAccess >
		extends AbstractViewerImgLoader< T, V >
{
	public static VirtualStackImageLoader< FloatType, VolatileFloatType, VolatileFloatArray > createFloatInstance( final ImagePlus imp )
	{
		return new VirtualStackImageLoader< FloatType, VolatileFloatType, VolatileFloatArray >(
				imp, new VirtualStackVolatileFloatArrayLoader( imp ), new FloatType(), new VolatileFloatType() )
		{
			@Override
			protected void linkType( final CachedCellImg< FloatType, VolatileFloatArray > img )
			{
				img.setLinkedType( new FloatType( img ) );
			}

			@Override
			protected void linkVolatileType( final CachedCellImg< VolatileFloatType, VolatileFloatArray > img )
			{
				img.setLinkedType( new VolatileFloatType( img ) );
			}
		};
	}

	public static VirtualStackImageLoader< UnsignedShortType, VolatileUnsignedShortType, VolatileShortArray > createUnsignedShortInstance( final ImagePlus imp )
	{
		return new VirtualStackImageLoader< UnsignedShortType, VolatileUnsignedShortType, VolatileShortArray >(
				imp, new VirtualStackVolatileShortArrayLoader( imp ), new UnsignedShortType(), new VolatileUnsignedShortType() )
		{
			@Override
			protected void linkType( final CachedCellImg< UnsignedShortType, VolatileShortArray > img )
			{
				img.setLinkedType( new UnsignedShortType( img ) );
			}

			@Override
			protected void linkVolatileType( final CachedCellImg< VolatileUnsignedShortType, VolatileShortArray > img )
			{
				img.setLinkedType( new VolatileUnsignedShortType( img ) );
			}
		};
	}

	public static VirtualStackImageLoader< UnsignedByteType, VolatileUnsignedByteType, VolatileByteArray > createUnsignedByteInstance( final ImagePlus imp )
	{
		return new VirtualStackImageLoader< UnsignedByteType, VolatileUnsignedByteType, VolatileByteArray >(
				imp, new VirtualStackVolatileByteArrayLoader( imp ), new UnsignedByteType(), new VolatileUnsignedByteType() )
		{
			@Override
			protected void linkType( final CachedCellImg< UnsignedByteType, VolatileByteArray > img )
			{
				img.setLinkedType( new UnsignedByteType( img ) );
			}

			@Override
			protected void linkVolatileType( final CachedCellImg< VolatileUnsignedByteType, VolatileByteArray > img )
			{
				img.setLinkedType( new VolatileUnsignedByteType( img ) );
			}
		};
	}

	private static double[][] mipmapResolutions = new double[][] { { 1, 1, 1 } };

	private static AffineTransform3D[] mipmapTransforms = new AffineTransform3D[] { new AffineTransform3D() };

	private final VolatileGlobalCellCache< A > cache;

	private final long[] dimensions;

	private final int[] cellDimensions;

	protected VirtualStackImageLoader( final ImagePlus imp, final CacheArrayLoader< A > loader, final T type, final V volatileType )
	{
		super( type, volatileType );
		dimensions = new long[] { imp.getWidth(), imp.getHeight(), imp.getNSlices() };
		cellDimensions = new int[] { imp.getWidth(), imp.getHeight(), 1 };
		final int numTimepoints = imp.getNFrames();
		final int numSetups = imp.getNChannels();
		cache = new VolatileGlobalCellCache< A >( loader, numTimepoints, numSetups, 1, new int[] { 0 }, 1 );
	}

	protected abstract void linkType( CachedCellImg< T, A > img );

	protected abstract void linkVolatileType( CachedCellImg< V, A > img );

	@Override
	public RandomAccessibleInterval< T > getImage( final ViewDescription view, final int level )
	{
		final CachedCellImg< T, A > img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
		linkType( img );
		return img;
	}

	@Override
	public RandomAccessibleInterval< V > getVolatileImage( final ViewDescription view, final int level )
	{
		final CachedCellImg< V, A > img = prepareCachedImage( view, level, LoadingStrategy.BUDGETED );
		linkVolatileType( img );
		return img;
	}

	@Override
	public double[][] getMipmapResolutions( final int setup )
	{
		return mipmapResolutions;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setup )
	{
		return mipmapTransforms;
	}

	@Override
	public int numMipmapLevels( final int setup )
	{
		return 1;
	}

	@Override
	public VolatileGlobalCellCache< A > getCache()
	{
		return cache;
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * (Almost) create a {@link CachedCellImg} backed by the cache. The created
	 * image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type)
	 * linked type} before it can be used. The type should be either
	 * {@link ARGBType} and {@link VolatileARGBType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, A > prepareCachedImage( final ViewDescription view, final int level, final LoadingStrategy loadingStrategy )
	{
		final int priority = 0;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< A > c = cache.new VolatileCellCache( view.getTimepointIndex(), view.getSetupIndex(), level, cacheHints );
		final VolatileImgCells< A > cells = new VolatileImgCells< A >( c, 1, dimensions, cellDimensions );
		final CachedCellImg< T, A > img = new CachedCellImg< T, A >( cells );
		return img;
	}
}
