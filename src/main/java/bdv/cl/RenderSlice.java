package bdv.cl;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.swing.JFrame;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.display.screenimage.awt.UnsignedByteAWTScreenImage;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.overlay.BufferedImageOverlayRenderer;
import net.imglib2.ui.util.GuiUtil;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import bdv.cl.BlockTexture.Block;
import bdv.cl.BlockTexture.BlockKey;
import bdv.cl.FindRequiredBlocks.RequiredBlocks;
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
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEvent.ProfilingCommand;
import com.jogamp.opencl.CLEventList;
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

	private CLKernel slice;

	private CLBuffer< FloatBuffer > transformMatrix;

	private CLBuffer< IntBuffer > sizes;

	public RenderSlice( final Hdf5ImageLoader imgLoader )
	{
		this.imgLoader = imgLoader;
		final int[] blockSize = imgLoader.getMipmapInfo( 0 ).getSubdivisions()[ 0 ]; // TODO

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
					System.out.println( "using " + dev.getName() );
					break;
				}
			}
			queue = device.createCommandQueue( Mode.PROFILING_MODE );

			final CLProgram program = cl.createProgram( this.getClass().getResourceAsStream( "slice2.cl" ) ).build();
			slice = program.createCLKernel( "slice" );

			transformMatrix = cl.createFloatBuffer( 12, Mem.READ_ONLY, Mem.ALLOCATE_BUFFER );
			sizes = cl.createIntBuffer( 8, Mem.READ_ONLY, Mem.ALLOCATE_BUFFER );

			final int[] gridSize = BlockTexture.findSuitableGridSize( blockSize, 500 );
			blockTexture = new BlockTexture( gridSize, blockSize, queue );
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

		if ( blockTexture != null && ! blockTexture.isReleased() )
			blockTexture.release();

		if ( transformMatrix != null && ! transformMatrix.isReleased() )
			transformMatrix.release();

		if ( sizes != null && ! sizes.isReleased() )
			sizes.release();

		if ( cl != null && ! cl.isReleased() )
			cl.release();
	}

	public void renderSlice( final ViewerState viewerState, final int width, final int height )
	{
		final int dimZ = 100;
		System.out.println();
		final Source< ? > source = viewerState.getSources().get( 0 ).getSpimSource(); // TODO
		final int timepoint = 0; // TODO
		final int timepointId = 0; // TODO
		final int setupId = 0; // TODO
		final int mipmapIndex = 0; // TODO
		final int[] blockSize = imgLoader.getMipmapInfo( setupId ).getSubdivisions()[ 0 ]; // TODO

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		viewerState.getViewerTransform( sourceToScreen );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, mipmapIndex, sourceTransform );
		sourceToScreen.concatenate( sourceTransform );

		long t = System.currentTimeMillis();
		final RequiredBlocks requiredBlocks = getRequiredBlocks( sourceToScreen, width, height, dimZ, new ViewId( timepointId, setupId ) );
		t = System.currentTimeMillis() - t;
		System.out.println( "getRequiredBlocks: " + t + " ms" );
		t = System.currentTimeMillis();
		int nnn = 0;
		for ( final int[] cellPos : requiredBlocks.cellPositions )
		{
			final BlockKey key = new BlockKey( cellPos );
			if ( ! blockTexture.contains( key ) )
			{
				blockTexture.put( key, getCellData( cellPos ) );
				nnn++;
			}
		}
		t = System.currentTimeMillis() - t;
		System.out.println( "upload " + nnn + " blocks: " + t + " ms" );

		final int[] lookupDims = new int[ 3 ];
		final int[] maxCell = requiredBlocks.maxCell;
		final int[] minCell = requiredBlocks.minCell;
		for ( int d = 0; d < 3; ++d )
			lookupDims[ d ] = maxCell[ d ] - minCell[ d ] + 1;

		final CLBuffer< ShortBuffer > blockLookup = cl.createShortBuffer( 4 * ( int ) Intervals.numElements( lookupDims ), Mem.READ_ONLY, Mem.ALLOCATE_BUFFER );
		final ByteBuffer bytes = queue.putMapBuffer( blockLookup, Map.WRITE, true );
		final ShortBuffer shorts = bytes.asShortBuffer();
		for ( final int[] cellPos : requiredBlocks.cellPositions )
		{
			final BlockKey key = new BlockKey( cellPos );
			final Block block = blockTexture.get( key );
			final int[] blockPos;
			if ( block != null )
				blockPos = block.getBlockPos();
			else
				blockPos = new int[] { 0, 0, 0 };
			final int i = 4 * IntervalIndexer.positionWithOffsetToIndex( cellPos, lookupDims, minCell );
			for ( int d = 0; d < 3; ++d )
				shorts.put( i + d, ( short ) ( blockPos[ d ] * blockSize[ d ] ) );
			shorts.put( i + 3, ( short ) 0 );
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
				1, 0, 0, - minCell[ 0 ] * blockSize[ 0 ],
				0, 1, 0, - minCell[ 1 ] * blockSize[ 1 ],
				0, 0, 1, - minCell[ 2 ] * blockSize[ 2 ] );
		screenToShiftedSource.concatenate( sourceToScreen.inverse() );
		final AffineTransform3D shiftedSourceToBlock = new AffineTransform3D();
		shiftedSourceToBlock.set(
				1.0 / blockSize[ 0 ], 0, 0, 0,
				0, 1.0 / blockSize[ 1 ], 0, 0,
				0, 0, 1.0 / blockSize[ 2 ], 0 );
		screenToShiftedSource.preConcatenate( shiftedSourceToBlock );
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
        for ( int i = 0; i < 1; ++i )
        {
			final CLEventList eventList = new CLEventList( 1 );
			slice.rewind()
				.putArg( transformMatrix )
				.putArg( sizes )
				.putArg( blockLookup )
				.putArg( blockTexture.get() )
				.putArg( renderTarget );
			queue.put2DRangeKernel( slice,
					globalWorkOffsetX, globalWorkOffsetY, globalWorkSizeX,
					globalWorkSizeY, localWorkSizeX, localWorkSizeY,
					eventList );
			queue.putReadImage( renderTarget, true ).finish();

	        final CLEvent event = eventList.getEvent( 0 );
	        final long start = event.getProfilingInfo( ProfilingCommand.START );
	        final long end = event.getProfilingInfo( ProfilingCommand.END );
	        System.out.println( "event t = " + ( ( end - start ) / 1000000.0 ) + " ms" );
        }

		if ( data == null )
			data = new byte[ width * height ];

		renderTarget.getBuffer().get( data );
		show( data, width, height );
		renderTarget.release();

		///////////////////

		blockLookup.release();
	}

	private byte[] data;
	private InteractiveDisplayCanvasComponent< AffineTransform2D > display;

	private void show( final byte[] data, final int width, final int height )
	{
		if ( display != null )
		{
			display.repaint();
			return;
		}

		final UnsignedByteAWTScreenImage screenImage = new UnsignedByteAWTScreenImage( ArrayImgs.unsignedBytes( data, width, height ) );
		final BufferedImage bufferedImage = screenImage.image();

		final BufferedImageOverlayRenderer target = new BufferedImageOverlayRenderer();
		target.setBufferedImage( bufferedImage );
		display = new InteractiveDisplayCanvasComponent< AffineTransform2D >( width, height, FixedTransformEventHandler2D.factory() );
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

	private RequiredBlocks getRequiredBlocks( final AffineTransform3D sourceToScreen, final int w, final int h, final int dd, final ViewId view )
	{
		final CachedCellImg< ?, ? > cellImg = (bdv.img.cache.CachedCellImg< ?, ? > ) imgLoader.getImage( view, 0 );
		final int[] cellDimensions = new int[ 3 ];
		cellImg.getCells().cellDimensions( cellDimensions );
		final long[] imgDimensions = new long[ 3 ];
		cellImg.dimensions( imgDimensions );
		return FindRequiredBlocks.getRequiredBlocks( sourceToScreen, w, h, dd, cellDimensions, imgDimensions );
	}
}
