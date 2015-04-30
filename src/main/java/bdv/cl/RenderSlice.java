package bdv.cl;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import javax.swing.JFrame;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.display.screenimage.awt.UnsignedByteAWTScreenImage;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.kdtree.HyperPlane;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.overlay.BufferedImageOverlayRenderer;
import net.imglib2.ui.util.GuiUtil;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import bdv.cl.BlockTexture.BlockKey;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileCell;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.viewer.Source;
import bdv.viewer.state.ViewerState;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.ChannelOrder;
import com.jogamp.opencl.CLImageFormat.ChannelType;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Map;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLProgram;

public class RenderSlice
{
	private final Hdf5ImageLoader imgLoader;

	private CLContext cl;

	private CLCommandQueue queue;

	private BlockTexture blockTexture;

	private CLKernel slice4;

	private CLBuffer< FloatBuffer > transformMatrix;

	private CLBuffer< IntBuffer > sizes;

	public RenderSlice( final Hdf5ImageLoader imgLoader )
	{
		this.imgLoader = imgLoader;

		try
		{
			cl = CLContext.create();

			CLDevice device = null;
			for ( final CLDevice dev : cl.getDevices() )
			{
				if ( "GeForce GT 650M".equals( dev.getName() ) )
//				if ( "HD Graphics 4000".equals( dev.getName() ) )
				{
					device = dev;
					break;
				}
			}
			queue = device.createCommandQueue();

			final CLProgram program = cl.createProgram( this.getClass().getResourceAsStream( "slice.cl" ) ).build();
			slice4 = program.createCLKernel( "slice4" );

			transformMatrix = cl.createFloatBuffer( 12, Mem.READ_ONLY, Mem.ALLOCATE_BUFFER );
			sizes = cl.createIntBuffer( 8, Mem.READ_ONLY, Mem.ALLOCATE_BUFFER );
		}
		catch ( final Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void cleanUp()
	{
		if ( queue != null && ! queue.isReleased() )
			queue.release();

		if ( transformMatrix != null && ! transformMatrix.isReleased() )
			transformMatrix.release();

		if ( sizes != null && ! sizes.isReleased() )
			sizes.release();

		if ( cl != null && ! cl.isReleased() )
			cl.release();
	}

	public void renderSlice( final ViewerState viewerState, final int width, final int height )
	{
		System.out.println( "render slice" );

		final Source< ? > source = viewerState.getSources().get( 0 ).getSpimSource(); // TODO
		final int timepoint = 0; // TODO
		final int timepointId = 0; // TODO
		final int setupId = 0; // TODO
		final int mipmapIndex = 0; // TODO

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		viewerState.getViewerTransform( sourceToScreen );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, mipmapIndex, sourceTransform );
		sourceToScreen.concatenate( sourceTransform );

		final int[] blockSize = imgLoader.getMipmapInfo( setupId ).getSubdivisions()[ 0 ];

		final RequiredBlocks requiredBlocks = getRequiredBlocks( sourceToScreen, width, height, 100, new ViewId( timepointId, setupId ) );

		blockTexture = new BlockTexture( new int[] { 10, 10, 10 }, blockSize, queue );
		for ( final int[] cellPos : requiredBlocks.cellPositions )
		{
			final BlockKey key = new BlockKey( cellPos );
			if ( ! blockTexture.contains( key ) )
				blockTexture.put( key, getCellData( cellPos ) );
		}

		final int[] lookupDims = new int[ 3 ];
		final int[] maxCell = requiredBlocks.maxCell;
		final int[] minCell = requiredBlocks.minCell;
		for ( int d = 0; d < 3; ++d )
			lookupDims[ d ] = maxCell[ d ] - minCell[ d ] + 1;

		final CLBuffer< IntBuffer > blockLookup = cl.createIntBuffer( 4 * ( int ) Intervals.numElements( lookupDims ), Mem.READ_ONLY, Mem.ALLOCATE_BUFFER );

		final ByteBuffer bytes = queue.putMapBuffer( blockLookup, Map.WRITE, true );
		final IntBuffer ints = bytes.asIntBuffer();
		for ( final int[] cellPos : requiredBlocks.cellPositions )
		{
			final BlockKey key = new BlockKey( cellPos );
			final int[] blockPos = blockTexture.get( key ).getBlockPos();
			final int i = 4 * IntervalIndexer.positionWithOffsetToIndex( cellPos, lookupDims, minCell );
			for ( int d = 0; d < 3; ++d )
				ints.put( i + d, blockPos[ d ] * blockSize[ d ] );
			ints.put( i + 3, 0 );
		}
		queue.putUnmapMemory( blockLookup, bytes );
		queue.finish();

		///////////////////

		final CLImage2d< ByteBuffer > renderTarget = ( CLImage2d< ByteBuffer > ) cl.createImage2d(
				Buffers.newDirectByteBuffer( width * height ),
				width,
				height,
				new CLImageFormat( ChannelOrder.R, ChannelType.UNSIGNED_INT8 ),
				Mem.READ_WRITE );

		final AffineTransform3D screenToShiftedSource = new AffineTransform3D();
		screenToShiftedSource.set(
				1, 0, 0, - minCell[ 0 ] * blockSize[ 0 ] ,
				0, 1, 0, - minCell[ 1 ] * blockSize[ 1 ] ,
				0, 0, 1, - minCell[ 2 ] * blockSize[ 2 ] );
		screenToShiftedSource.concatenate( sourceToScreen.inverse() );
		for ( int r = 0; r < 3; ++r )
			for ( int c = 0; c < 4; ++c )
				transformMatrix.getBuffer().put( ( float ) screenToShiftedSource.get( r, c ) );
		transformMatrix.getBuffer().rewind();
		queue.putWriteBuffer( transformMatrix, true );

		sizes.getBuffer().put( blockSize );
		sizes.getBuffer().put( 1 );
		sizes.getBuffer().put( lookupDims );
		sizes.getBuffer().put( 1 );
		sizes.getBuffer().rewind();
		queue.putWriteBuffer( sizes, true );

		final long globalWorkOffsetX = 0;
		final long globalWorkOffsetY = 0;
		final long globalWorkSizeX = width;
		final long globalWorkSizeY = height;
        final long localWorkSizeX = 0;
        final long localWorkSizeY = 0;
        for ( int i = 0; i < 10; ++i )
        {
	        long t = System.currentTimeMillis();
			slice4.rewind().putArg( transformMatrix ).putArg( sizes ).putArg( blockLookup ).putArg( blockTexture.get() ).putArg( renderTarget );
			queue.put2DRangeKernel( slice4, globalWorkOffsetX, globalWorkOffsetY, globalWorkSizeX, globalWorkSizeY, localWorkSizeX, localWorkSizeY );
			queue.putReadImage( renderTarget, true ).finish();
	        t = System.currentTimeMillis() - t;
	        System.out.println( "t = " + t + " ms" );
        }

		final byte[] data = new byte[ width * height ];
		renderTarget.getBuffer().get( data );
		show( data, width, height );

		renderTarget.release();

		///////////////////

		blockTexture.release();
		blockLookup.release();
	}

	private void show( final byte[] data, final int width, final int height )
	{
		final UnsignedByteAWTScreenImage screenImage = new UnsignedByteAWTScreenImage( ArrayImgs.unsignedBytes( data, width, height ) );
		final BufferedImage bufferedImage = screenImage.image();

		final BufferedImageOverlayRenderer target = new BufferedImageOverlayRenderer();
		target.setBufferedImage( bufferedImage );
		final InteractiveDisplayCanvasComponent< AffineTransform2D > display =
				new InteractiveDisplayCanvasComponent< AffineTransform2D >( width, height, FixedTransformEventHandler2D.factory() );
		display.addOverlayRenderer( target );
		target.setCanvasSize( width, height );

		final GraphicsConfiguration gc = GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL );
		final JFrame frame = new JFrame( "ImgLib2", gc );
		frame.getRootPane().setDoubleBuffered( true );
		final Container content = frame.getContentPane();
		content.add( display, BorderLayout.CENTER );
		frame.pack();
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
	}

	private short[] getCellData( final int[] cellPos )
	{
		final int timepointId = 0; // TODO
		final int setupId = 0; // TODO
		final int level = 0; // TODO


		final VolatileGlobalCellCache< VolatileShortArray > cache = imgLoader.getCache();
		final CacheHints cacheHints = new CacheHints( LoadingStrategy.BLOCKING, 0, false );
		final VolatileGlobalCellCache< VolatileShortArray >.VolatileCellCache cellCache = cache.new VolatileCellCache( timepointId, setupId, level, cacheHints );

		final int[] blockSize = imgLoader.getMipmapInfo( setupId ).getSubdivisions()[ level ];
		final long[] blockSizeLong = Util.int2long( blockSize );
		final Dimensions imageSize = imgLoader.getImageSize( new ViewId( timepointId, setupId ), level );

		final int n = 3;
		final long[] dimensions = new long[ n ];
		final int[] cellDimensions = new int[ n ];
		final int[] numCells = new int[ n ];
		final int[] borderSize = new int[ n ];

		imageSize.dimensions( dimensions );
		System.arraycopy( blockSize, 0, cellDimensions, 0, n );
		for ( int d = 0; d < n; ++d )
		{
			numCells[ d ] = ( int ) ( ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1 );
			borderSize[ d ] = ( int ) ( dimensions[ d ] - ( numCells[ d ] - 1 ) * cellDimensions[ d ] );
		}

		// up to this point, the data should be stored in a per-setup-and-level lookup
		// ===========================================================================

		final long[] cellMin = new long[ n ];
		final int[] cellDims  = new int[ n ];

		boolean needsPadding = false;
		for ( int d = 0; d < n; ++d )
		{
			if ( cellPos[ d ] + 1 == numCells[ d ] )
			{
				needsPadding = true;
				cellDims[ d ] = borderSize[ d ];
			}
			else
				cellDims[ d ] = cellDimensions[ d ];
			cellMin[ d ] = cellPos[ d ] * cellDimensions[ d ];
		}
		final int index = IntervalIndexer.positionToIndex( cellPos, numCells );
		final VolatileCell< VolatileShortArray > cell = cellCache.load( index, cellDims, cellMin );
		final short[] cellData = cell.getData().getCurrentStorageArray();

		final short[] data;
		if ( needsPadding )
		{
			final int numElements = blockSize[ 0 ] * blockSize[ 1 ] * blockSize[ 2 ];
			data = new short[ numElements ];
			final Img< UnsignedShortType > cellDataImg = ArrayImgs.unsignedShorts( cellData, cellDims[ 0 ], cellDims[ 1 ], cellDims[ 2 ] );
			final Cursor< UnsignedShortType > in = cellDataImg.cursor();
			final Cursor< UnsignedShortType > out = Views.interval( ArrayImgs.unsignedShorts( data, blockSizeLong ), cellDataImg ).cursor();
			while ( in.hasNext() )
				out.next().set( in.next() );
		}
		else
			data = cellData;

		return data;
	}

	private static class RequiredBlocks
	{
		private final ArrayList< int[] > cellPositions;

		private final int[] minCell;

		private final int[] maxCell;

		public RequiredBlocks( final ArrayList< int[] > cellPositions, final int[] minCell, final int[] maxCell )
		{
			this.cellPositions = cellPositions;
			this.minCell = minCell;
			this.maxCell = maxCell;
		}
	}

	private RequiredBlocks getRequiredBlocks( final AffineTransform3D sourceToScreen, final int w, final int h, final int dd, final ViewId view )
	{
//		final CachedCellImg< ?, ? > cellImg = (bdv.img.cache.CachedCellImg< ?, ? > ) imgLoader.getVolatileImage( view, 0 );
		final CachedCellImg< ?, ? > cellImg = (bdv.img.cache.CachedCellImg< ?, ? > ) imgLoader.getImage( view, 0 );

		final ArrayList< int[] > requiredCells = new ArrayList< int[] >();

		final int[] cellDimensions = new int[ 3 ];
		cellImg.getCells().cellDimensions( cellDimensions );
		final long[] imgDimensions = new long[ 3 ];
		cellImg.dimensions( imgDimensions );

		// bounding box in source coordinates...
		final RealPoint[] screenCorners = new RealPoint[ 8 ];
		screenCorners[ 0 ] = new RealPoint( 0.0, 0.0, 0.0 );
		screenCorners[ 1 ] = new RealPoint(   w, 0.0, 0.0 );
		screenCorners[ 2 ] = new RealPoint(   w,   h, 0.0 );
		screenCorners[ 3 ] = new RealPoint( 0.0,   h, 0.0 );
		screenCorners[ 4 ] = new RealPoint( 0.0, 0.0,  dd );
		screenCorners[ 5 ] = new RealPoint(   w, 0.0,  dd );
		screenCorners[ 6 ] = new RealPoint(   w,   h,  dd );
		screenCorners[ 7 ] = new RealPoint( 0.0,   h,  dd );
		final RealPoint sourceCorner = new RealPoint( 3 );
		final double[] bbMin = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		final double[] bbMax = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
		for ( int i = 0; i < screenCorners.length; ++i )
		{
			sourceToScreen.applyInverse( sourceCorner, screenCorners[ i ] );
			for ( int d = 0; d < 3; ++d )
			{
				final double p = sourceCorner.getDoublePosition( d );
				if ( p < bbMin[ d ] )
					bbMin[ d ] = p;
				if ( p > bbMax[ d ] )
					bbMax[ d ] = p;
			}
		}

		// bounding box of potentially contained cells in cell grid coordinates
		final int[] minCell = new int[ 3 ];
		final int[] maxCell = new int[ 3 ];
		for ( int d = 0; d < 3; ++d )
			maxCell[ d ] = ( int ) ( ( imgDimensions[ d ] - 1 ) / cellDimensions[ d ] );
		for ( int d = 0; d < 3; ++d )
		{
			minCell[ d ] = Math.min( maxCell[ d ], Math.max( ( int ) bbMin[ d ] / cellDimensions[ d ] - 1, 0 ) );
			maxCell[ d ] = Math.max( 0, Math.min( ( int ) bbMax[ d ] / cellDimensions[ d ] + 1, maxCell[ d ] ) );
		}

		// planes bounding the volume, normals facing inwards...
		final HyperPlane[] sourcePlanes = new HyperPlane[ 6 ];
		sourcePlanes[ 0 ] = inverseTransform( new HyperPlane(  0,  0,  1,   0 ), sourceToScreen );
		sourcePlanes[ 1 ] = inverseTransform( new HyperPlane(  0,  0, -1, -dd ), sourceToScreen );
		sourcePlanes[ 2 ] = inverseTransform( new HyperPlane(  1,  0,  0,   0 ), sourceToScreen );
		sourcePlanes[ 3 ] = inverseTransform( new HyperPlane( -1,  0,  0,  -w ), sourceToScreen );
		sourcePlanes[ 4 ] = inverseTransform( new HyperPlane(  0,  1,  0,   0 ), sourceToScreen );
		sourcePlanes[ 5 ] = inverseTransform( new HyperPlane(  0, -1,  0,  -h ), sourceToScreen );

		// shift planes such that we only have to check the (0,0,0) corner of cells
		final double[] cellBBMin = new double[ 3 ];
		final double[] cellBBMax = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			cellBBMin[ d ] = -0.5;
			cellBBMax[ d ] = cellDimensions[ d ] - 0.5;
		}
		final double[] offset = new double[ 3 ];
		for ( int i = 0; i < sourcePlanes.length; ++i )
		{
			final double[] nn = sourcePlanes[ i ].getNormal();
			final double m = sourcePlanes[ i ].getDistance();
			for ( int d = 0; d < 3; ++d )
				offset[ d ] = ( nn[ d ] < 0 ) ? cellBBMin[ d ] : cellBBMax[ d ];
			sourcePlanes[ i ] = new HyperPlane( nn, m - LinAlgHelpers.dot( nn, offset ) );
		}

		// stupid implementation for now: check all planes for all cells...
		// TODO: make this more clever
		final IntervalIterator cellsIter = new IntervalIterator( minCell, maxCell );
A:		while( cellsIter.hasNext() )
		{
			cellsIter.fwd();
			final double[] pos = new double[ 3 ];
			cellsIter.localize( pos );
			for ( int d = 0; d < 3; ++d )
				pos[ d ] *= cellDimensions[ d ];
			for ( int i = 0; i < sourcePlanes.length; ++i )
			{
				final HyperPlane plane = sourcePlanes[ i ];
				if ( LinAlgHelpers.dot( pos, plane.getNormal() ) < plane.getDistance() )
					continue A;
			}

			final int[] cellPos = new int[ 3 ];
			cellsIter.localize( cellPos );
			requiredCells.add( cellPos );
		}

		return new RequiredBlocks( requiredCells, minCell, maxCell );
	}

	public static HyperPlane transform( final HyperPlane plane, final AffineTransform3D transform )
	{
		final double[] O = new double[ 3 ];
		final double[] tO = new double[ 3 ];
		LinAlgHelpers.scale( plane.getNormal(), plane.getDistance(), O );
		transform.apply( O, tO );

		final double[][] m = new double[3][3];
		for ( int r = 0; r < 3; ++r )
			for ( int c = 0; c < 3; ++c )
				m[r][c] = transform.inverse().get( c, r );
		final double[] tN = new double[ 3 ];
		LinAlgHelpers.mult( m, plane.getNormal(), tN );
		LinAlgHelpers.normalize( tN );
		final double td = LinAlgHelpers.dot( tN, tO );

		return new HyperPlane( tN, td );
	}

	public static HyperPlane inverseTransform( final HyperPlane plane, final AffineTransform3D transform )
	{
		final double[] O = new double[ 3 ];
		final double[] tO = new double[ 3 ];
		LinAlgHelpers.scale( plane.getNormal(), plane.getDistance(), O );
		transform.applyInverse( tO, O );

		final double[][] m = new double[3][3];
		for ( int r = 0; r < 3; ++r )
			for ( int c = 0; c < 3; ++c )
				m[r][c] = transform.get( c, r );
		final double[] tN = new double[ 3 ];
		LinAlgHelpers.mult( m, plane.getNormal(), tN );
		LinAlgHelpers.normalize( tN );
		final double td = LinAlgHelpers.dot( tN, tO );

		return new HyperPlane( tN, td );
	}
}
