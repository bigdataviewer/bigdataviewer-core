package bdv.cl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImage3d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.ChannelOrder;
import com.jogamp.opencl.CLImageFormat.ChannelType;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Map;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLProgram;

import bdv.img.cache.CacheHints;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileCell;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Hdf5VolatileShortArrayLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import ij.ImageJ;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.IntervalIndexer;

public class JoclExample
{
	private final String fn = "/Users/pietzsch/workspace/data/111010_weber_full.xml";

	private final CLContext cl;

	private final CLDevice device;

	private final CLCommandQueue q;

	private final CLKernel slice1;

	private final CLKernel slice2;

	private final int[] blockDimensions = new int[] { 64, 64, 16 };

	private final int[] imageDimensions = new int[] { 958, 386, 44 };

	private final int nBlocks = 100;

	private CLImage3d< ShortBuffer > blocks;

	private CLBuffer<IntBuffer>  blockLookup;

	private CLImage2d< ByteBuffer > renderTarget;

	// 1.) set up context
	public JoclExample() throws IOException
	{
		cl = CLContext.create();

		CLDevice d = null;
		for ( final CLDevice dev : cl.getDevices() )
		{
			if ( "GeForce GT 650M".equals( dev.getName() ) )
			{
				d = dev;
				break;
			}
		}
		device = d;

		q = device.createCommandQueue();

		final CLProgram program = cl.createProgram( this.getClass().getResourceAsStream( "slice.cl" ) ).build();
		slice1 = program.createCLKernel( "slice1" );
		slice2 = program.createCLKernel( "slice2" );
	}

	// 2.) allocate 3d texture for blocks
	public void allocateBlockTexture()
	{
		final int[] imageSize = new int[ 3 ];
		imageSize[ 0 ] = blockDimensions[ 0 ];
		imageSize[ 1 ] = blockDimensions[ 1 ];
		imageSize[ 2 ] = blockDimensions[ 2 ] * nBlocks;
		blocks = ( CLImage3d< ShortBuffer > ) cl.createImage3d( imageSize[ 0 ], imageSize[ 1 ], imageSize[ 2 ], new CLImageFormat( ChannelOrder.R, ChannelType.UNSIGNED_INT16 ), Mem.READ_ONLY );
	}

	// 3.) function to write block to texture
	public void writeBlock( final short[] data, final int blockIndex )
	{
		final ShortBuffer b = Buffers.newDirectShortBuffer( data );
		blocks.use( b );
        final int originX = 0;
        final int originY = 0;
        final int originZ = blockIndex * blockDimensions[ 2 ];
        final int rangeX = blockDimensions[ 0 ];
        final int rangeY = blockDimensions[ 1 ];
        final int rangeZ = blockDimensions[ 2 ];
		q.putWriteImage( blocks, 0, 0, originX, originY, originZ, rangeX, rangeY, rangeZ, false ).finish();
	}

	// 4.) open h5 file and write some blocks to the texture
	public void readHDF5Blocks() throws SpimDataException
	{
		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();
		final SpimDataMinimal spimData = io.load( fn );
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		final Hdf5ImageLoader imgLoader = ( Hdf5ImageLoader ) seq.getImgLoader();
		final Hdf5VolatileShortArrayLoader loader = imgLoader.getShortArrayLoader();

		final VolatileGlobalCellCache cache = imgLoader.getCache();
		final CacheHints cacheHints = new CacheHints( LoadingStrategy.BLOCKING, 0, false );
		final VolatileGlobalCellCache.VolatileCellCache< VolatileShortArray > cellCache = cache.new VolatileCellCache< VolatileShortArray >( 1, 0, 0, cacheHints, loader );

		final int n = 3;
		final int[] dim = new int[] { 15, 7, 3 };
		final long[] cellGridPosition = new long[ n ];
		final long[] cellMin = new long[ n ];
		final int[] cellDims  = new int[ n ];

		int blockIndex = 0;
		cellGridPosition[ 2 ] = 1;
		for ( int y = 0; y < 6; ++y )
		{
			for ( int x = 0; x < 14; ++x )
			{
				cellGridPosition[ 0 ] = x;
				cellGridPosition[ 1 ] = y;

				final int index = IntervalIndexer.positionToIndex( cellGridPosition, dim );
				for ( int d = 0; d < n; ++d )
				{
					cellMin[ d ] = blockDimensions[ d ] * cellGridPosition[ d ];
					cellDims[ d ] = blockDimensions[ d ]; // TODO
				}
				final VolatileCell< VolatileShortArray > cell = cellCache.load( index, cellDims, cellMin );
				final short[] data = cell.getData().getCurrentStorageArray();
				writeBlock( data, blockIndex++ );
			}
		}
	}

	// 5.) allocate 2d render target
	public void allocateRenderTarget( final int width, final int height )
	{
		renderTarget = ( CLImage2d< ByteBuffer > ) cl.createImage2d( width, height, new CLImageFormat( ChannelOrder.R, ChannelType.UNSIGNED_INT8 ), Mem.READ_WRITE );
	}

	public void renderTest1( final int width, final int height )
	{
		final long globalWorkOffsetX = 0;
		final long globalWorkOffsetY = 0;
		final long globalWorkSizeX = width;
		final long globalWorkSizeY = height;
        final long localWorkSizeX = 0;
        final long localWorkSizeY = 0;

		final ByteBuffer b = Buffers.newDirectByteBuffer( width * height );
		renderTarget.use( b );

        slice1.putArg( blocks ).putArg( renderTarget );
		q.put2DRangeKernel( slice1, globalWorkOffsetX, globalWorkOffsetY, globalWorkSizeX, globalWorkSizeY, localWorkSizeX, localWorkSizeY ).putReadImage( renderTarget, true );

		final byte[] data = new byte[ b.remaining() ];
		b.get( data );

		ImageJFunctions.show( ArrayImgs.unsignedBytes( data, width, height ) );
	}

	public void renderTest2( final int width, final int height )
	{
		final long globalWorkOffsetX = 0;
		final long globalWorkOffsetY = 0;
		final long globalWorkSizeX = width;
		final long globalWorkSizeY = height;
        final long localWorkSizeX = 0;
        final long localWorkSizeY = 0;

		final ByteBuffer b = Buffers.newDirectByteBuffer( width * height );
		renderTarget.use( b );

		long t = System.currentTimeMillis();
        slice2.putArg( blockLookup ).putArg( blocks ).putArg( renderTarget );
		q.put2DRangeKernel( slice2, globalWorkOffsetX, globalWorkOffsetY, globalWorkSizeX, globalWorkSizeY, localWorkSizeX, localWorkSizeY ).putReadImage( renderTarget, true );
		t = System.currentTimeMillis() - t;
		System.out.println( "t=" + t + "ms" );

		final byte[] data = new byte[ b.remaining() ];
		b.get( data );

		ImageJFunctions.show( ArrayImgs.unsignedBytes( data, width, height ) );
	}

	// 6.) lookup texture for blocks
	public void allocateBlockLookupBuffer()
	{
		final int[] lookupSize = new int[ 3 ];
		int size = 4;
		for ( int d = 0; d < 3; ++d )
		{
			lookupSize[ d ] = imageDimensions[ d ] / blockDimensions[ d ];
			if ( lookupSize[ d ] * blockDimensions[ d ] < imageDimensions[ d ] )
				lookupSize[ d ] += 1;
			size *= lookupSize[ d ];
		}
		blockLookup = cl.createIntBuffer( size, Mem.READ_ONLY, Mem.ALLOCATE_BUFFER );
		final ByteBuffer bytes = q.putMapBuffer( blockLookup, Map.READ_WRITE, true );
		final IntBuffer ints = bytes.asIntBuffer();

		ints.rewind();
		for ( int z = 0; z < lookupSize[ 2 ]; ++z )
			for ( int y = 0; y < lookupSize[ 1 ]; ++y )
				for ( int x = 0; x < lookupSize[ 0 ]; ++x )
				{
					final int xStart = 0;
					final int yStart = 0;
					final int zStart = z == 1 ? ( y * 14 + x ) * blockDimensions[ 2 ] : 0;

					ints.put( xStart );
					ints.put( yStart );
					ints.put( zStart );
					ints.put( 0 );
				}
		ints.flip();
		q.putUnmapMemory( blockLookup, bytes );
	}

	// 7.) use lookup texture in kernel
	// 8.) thick slice rendering assuming all blocks are present

	final private Random rand = new Random();

	public short[] createRandomBlock()
	{
		final int size = blockDimensions[ 0 ] * blockDimensions[ 1 ] * blockDimensions[ 2 ];
		final short[] values = new short[ size ];
		for ( int i = 0; i < values.length; ++i )
			values[ i ] = ( short ) rand.nextInt( 255 );
		return values;
	}
	public static void main( final String[] args ) throws IOException, SpimDataException
	{
		ImageJ.main( args );

		final JoclExample example = new JoclExample();

		example.allocateBlockTexture();
		example.allocateBlockLookupBuffer();
		example.readHDF5Blocks();
//		final short[] values = example.createRandomBlock();
//		example.writeBlock( values, 0 );

//		example.allocateRenderTarget( 128, 128 );
//		example.renderTest2( 128, 128 );

		example.allocateRenderTarget( 896, 384 );
		example.renderTest2( 896, 384 );

		example.blocks.release();
		example.blockLookup.release();
		example.renderTarget.release();
		example.cl.release();
	}
}
