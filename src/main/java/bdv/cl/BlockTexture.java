package bdv.cl;

import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;

import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLImage3d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.ChannelOrder;
import com.jogamp.opencl.CLImageFormat.ChannelType;
import com.jogamp.opencl.CLMemory.Mem;

public class BlockTexture
{
	public static class BlockKey
	{
		/**
		 * The cell grid coordinates of the source data.
		 */
		private final int[] cellPos;

		private final int hashcode;

		public BlockKey( final int[] cellGridPos )
		{
			this.cellPos = cellGridPos.clone();

			final long value = ( ( long ) cellPos[ 2 ] << 42 ) ^ cellPos[ 2 ] << 21 ^ ( long ) cellPos[ 2 ];
			hashcode = ( int ) ( value ^ ( value >>> 32 ) );
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj instanceof BlockKey )
			{
				final BlockKey b = ( BlockKey ) obj;
				return Arrays.equals ( cellPos, b.cellPos );
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return hashcode;
		}
	}

	public static class Block
	{
		/**
		 * The block grid coordinates of the data, i.e., where is the data
		 * stored in the blocks texture? This is to re-use that part of the
		 * texture.
		 */
		private final int[] blockPos;

		public Block()
		{
			blockPos = new int[ 3 ];
		}

		/**
		 * Get the grid coordinates of this block in the texture.
		 */
		public int[] getBlockPos()
		{
			return blockPos;
		}
	}

	private class LRUCache extends LinkedHashMap< BlockKey, Block >
	{
		private final int cacheSize;

		public LRUCache( final int numBlocks )
		{
			super( ( int ) ( numBlocks * 1.75f ), 0.75f, true );
			this.cacheSize = numBlocks;
		}

		/**
		 * Fills in the block grid coordinates and stores the {@code block} in
		 * the map with the given {@code key}. If the block grid is full, this
		 * will cause the least-recently used block do be removed from the map
		 * (it's grid coordinates will be re-used for the inserted block).
		 */
		@Override
		public Block put( final BlockKey key, final Block block )
		{
			if ( size() >= cacheSize )
			{
				final Block b = remove( this.keySet().iterator().next() );
				System.arraycopy( b.getBlockPos(), 0, block.getBlockPos(), 0, 3 );
			}
			else
			{
				IntervalIndexer.indexToPosition( size(), gridSize, block.getBlockPos() );
			}
			return super.put( key, block );
		}
	}

	private final int[] gridSize;

	private final int[] blockSize;

	private final LRUCache blocksCache;

	private final CLImage3d< ShortBuffer > blocksTexture;

	private final CLCommandQueue queue;

	private void writeBlock( final int[] blockPos, final short[] data )
	{
		blocksTexture.use( Buffers.newDirectShortBuffer( data ) );
		final int w = blockSize[ 0 ];
		final int h = blockSize[ 1 ];
		final int d = blockSize[ 2 ];
        final int x = blockPos[ 0 ] * w;
        final int y = blockPos[ 1 ] * h;
        final int z = blockPos[ 2 ] * d;
		queue.putWriteImage( blocksTexture, 0, 0, x, y, z, w, h, d, true );
	}

	@SuppressWarnings( "unchecked" )
	public BlockTexture( final int[] gridSize, final int[] blockSize, final CLCommandQueue queue )
	{
		this.gridSize = gridSize.clone();
		this.blockSize = blockSize.clone();
		this.queue = queue;
		final int numSlots = ( int ) Intervals.numElements( gridSize );
		blocksCache = new LRUCache( numSlots );
		blocksTexture = ( CLImage3d< ShortBuffer > ) queue.getContext().createImage3d(
				gridSize[ 0 ] * blockSize[ 0 ],
				gridSize[ 1 ] * blockSize[ 1 ],
				gridSize[ 2 ] * blockSize[ 2 ],
				new CLImageFormat( ChannelOrder.R, ChannelType.UNSIGNED_INT16 ),
				Mem.READ_ONLY );
	}

	public boolean contains( final BlockKey key )
	{
		return blocksCache.containsKey( key );
	}

	public Block get( final BlockKey key )
	{
		return blocksCache.get( key );
	}

	public void put( final BlockKey key, final short[] data )
	{
		final Block block = new Block();
		blocksCache.put( key, block );
		writeBlock( block.getBlockPos(), data );
	}

	public CLImage3d< ShortBuffer > get()
	{
		return blocksTexture;
	}

	public void release()
	{
		blocksTexture.release();
	}
}
