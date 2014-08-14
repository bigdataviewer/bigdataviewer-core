package bdv.img.trakem2;

import ij.measure.Calibration;
import ini.trakem2.ControlWindow;
import ini.trakem2.Project;
import ini.trakem2.display.LayerSet;
import ini.trakem2.persistence.Loader;

import java.awt.Rectangle;
import java.io.File;

import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.volatiles.VolatileARGBType;

import org.jdom2.Element;

import bdv.AbstractViewerImgLoader;
import bdv.img.cache.VolatileCell;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;

public class TrakEM2ImageLoader extends AbstractViewerImgLoader< ARGBType, VolatileARGBType >
{
	private Project project;
	private Loader loader;
	private LayerSet layerset;
	
	private int width;
	private int height;
	private int depth;
	
	private int tileWidth;
	private int tileHeight;
	
	private double zScale;

	private int numScales;

	private double[][] mipmapResolutions;

	private AffineTransform3D[] mipmapTransforms;

	private long[][] imageDimensions;

	private int[] zScales;

	private VolatileGlobalCellCache< VolatileIntArray > cache;

	public TrakEM2ImageLoader()
	{
		super( new ARGBType(), new VolatileARGBType() );
	}
	
	public void init( final Project project, final int tileWidth, final int tileHeight, final int numScales )
	{
		this.project = project;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.numScales = numScales;
		
		loader = project.getLoader();
		layerset = project.getRootLayerSet();
		layerset.setSnapshotsMode(1);
		final Rectangle box = layerset.get2DBounds();
		
		width = box.width;
		height = box.height;
		depth = layerset.getLayers().size();

		final Calibration calibration = layerset.getCalibration();
		zScale = calibration.pixelDepth / calibration.pixelWidth;
		
		mipmapResolutions = new double[ numScales ][];
		imageDimensions = new long[ numScales ][];
		mipmapTransforms = new AffineTransform3D[ numScales ];
		zScales = new int[ numScales ];
		for ( int l = 0; l < numScales; ++l )
		{
			final int sixy = 1 << l;
			int siz = Math.max( 1, ( int )Math.round( sixy / zScale ) );
			
			mipmapResolutions[ l ] = new double[] { sixy, sixy, siz };
			imageDimensions[ l ] = new long[] { width >> l, height >> l, depth / siz };
			zScales[ l ] = siz;
			
			final AffineTransform3D mipmapTransform = new AffineTransform3D();
			
			mipmapTransform.set( sixy, 0, 0 );
			mipmapTransform.set( sixy, 1, 1 );
			mipmapTransform.set( zScale * siz, 2, 2 );
			
			mipmapTransform.set( 0.5 * ( sixy - 1 ), 0, 3 );
			mipmapTransform.set( 0.5 * ( sixy - 1 ), 1, 3 );
			mipmapTransform.set( 0.5 * ( zScale * siz - 1 ), 2, 3 );
			
			mipmapTransforms[ l ] = mipmapTransform;
		}

		final int[] maxLevels = new int[] { numScales - 1 };
		cache = new VolatileGlobalCellCache< VolatileIntArray >(
				new TrakEM2VolatileIntArrayLoader( loader, layerset, zScales ), 1, 1, numScales, maxLevels, 10 );
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		final String projectPath = elem.getChildText( "projectPath" );
		final int tileWidth = Integer.parseInt( elem.getChildText( "tileWidth" ) );
		final int tileHeight = Integer.parseInt( elem.getChildText( "tileHeight" ) );
		final String numScalesString = elem.getChildText( "numScales" );
		if ( numScalesString == null )
			numScales = getNumScales( width, height, tileWidth, tileHeight );
		else
			numScales = Integer.parseInt( numScalesString );

		ControlWindow.setGUIEnabled( false );
		project = Project.openFSProject( projectPath, false);
		
		init( project, tileWidth, tileHeight, numScales );
	}

	final static public int getNumScales( long width, long height, final long tileWidth, final long tileHeight )
	{
		int i = 1;

		while ( ( width >>= 1 ) > tileWidth && ( height >>= 1 ) > tileHeight )
			++i;

		return i;
	}

	@Override
	public RandomAccessibleInterval< ARGBType > getImage( final View view, final int level )
	{
		final CellImg< ARGBType, VolatileIntArray, VolatileCell< VolatileIntArray > >  img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
		final ARGBType linkedType = new ARGBType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileARGBType > getVolatileImage( final View view, final int level )
	{
		final CellImg< VolatileARGBType, VolatileIntArray, VolatileCell< VolatileIntArray > >  img = prepareCachedImage( view, level, LoadingStrategy.VOLATILE );
		final VolatileARGBType linkedType = new VolatileARGBType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public double[][] getMipmapResolutions( final int setup )
	{
		return mipmapResolutions;
	}

	@Override
	public int numMipmapLevels( final int setup )
	{
		return numScales;
	}

	/**
	 * (Almost) create a {@link CellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link ARGBType} and {@link VolatileARGBType}.
	 */
	protected < T extends NativeType< T > > CellImg< T, VolatileIntArray, VolatileCell< VolatileIntArray > > prepareCachedImage( final View view, final int level, final LoadingStrategy loadingStrategy )
	{
		final long[] dimensions = imageDimensions[ level ];
		final int[] cellDimensions = new int[]{ tileWidth, tileHeight, 1 };

		final CellCache< VolatileIntArray > c = cache.new VolatileCellCache( view.getTimepointIndex(), view.getSetupIndex(), level, loadingStrategy );
		final VolatileImgCells< VolatileIntArray > cells = new VolatileImgCells< VolatileIntArray >( c, 1, dimensions, cellDimensions );
		final CellImg< T, VolatileIntArray, VolatileCell< VolatileIntArray > > img = new CellImg< T, VolatileIntArray, VolatileCell< VolatileIntArray > >( null, cells );
		return img;
	}

	@Override
	public VolatileGlobalCellCache< VolatileIntArray > getCache()
	{
		return cache;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setup )
	{
		return mipmapTransforms;
	}
}
