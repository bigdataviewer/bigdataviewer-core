/*-
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
package bdv;

import static net.imglib2.view.fluent.RandomAccessibleIntervalView.Extension.zero;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.VolatileBlockSupplier;
import net.imglib2.algorithm.blocks.convert.Convert;
import net.imglib2.algorithm.blocks.util.UnaryOperatorType;
import net.imglib2.blocks.BlockInterval;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.blocks.VolatileArray;
import net.imglib2.blocks.VolatilePrimitiveBlocks;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class VolatileBlocksPlayground
{
	public static void main2( String[] args ) throws Exception
	{
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_resave.xml";
		final SpimDataMinimal spim = new XmlIoSpimDataMinimal().load( fn );
		final ViewerSetupImgLoader<UnsignedShortType, VolatileUnsignedShortType > setupImgLoader = Cast.unchecked( spim.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ) );

		final RandomAccessibleInterval< VolatileUnsignedShortType > vimg = setupImgLoader.getVolatileImage( 1, 0 );
		System.out.println( "vimg = " + vimg );
		System.out.println( "vimg.getType().getClass() = " + vimg.getType().getClass() );

		final PrimitiveBlocks< VolatileUnsignedShortType > vblocks = PrimitiveBlocks.of( vimg.view().extend( zero() ) );
		System.out.println( "vblocks = " + vblocks );

		final long[] pos = { -500, -500, 0 };
		final int[] size = { 200, 200, 10 };
		final short[] data = new short[ ( int ) Intervals.numElements( size ) ];
		vblocks.copy( pos, data, size );
	}


	public static void main3( String[] args ) throws Exception
	{
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_resave.xml";
		final SpimDataMinimal spim = new XmlIoSpimDataMinimal().load( fn );
		final ViewerSetupImgLoader<UnsignedShortType, VolatileUnsignedShortType > setupImgLoader = Cast.unchecked( spim.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ) );

		final RandomAccessibleInterval< UnsignedShortType > img = setupImgLoader.getImage( 1 );
		final PrimitiveBlocks< UnsignedShortType > blocks = PrimitiveBlocks.of( img );
		System.out.println( "blocks = " + blocks );

		final RandomAccessibleInterval< VolatileUnsignedShortType > vimg = setupImgLoader.getVolatileImage( 1, 0 );
		System.out.println( "vimg = " + vimg );
		System.out.println( "vimg.getType().getClass() = " + vimg.getType().getClass() );

		final PrimitiveBlocks< VolatileUnsignedShortType > vblocks = VolatilePrimitiveBlocks.of( vimg );
		System.out.println( "vblocks = " + vblocks );

		final long[] pos = { -50, -50, 0 };
		final int[] size = { 1000, 400, 50 };
//		final long[] pos = { 0, 0, 0 };
//		final int[] size = { 2, 2, 2 };
		final int len = ( int ) Intervals.numElements( size );

		for ( int i = 0; i < 2; ++i )
		{
			final short[] data = new short[ len ];
			final byte[] valid = new byte[ len ];
			vblocks.copy( pos, new VolatileArray<>(data, valid), size );
			System.out.println();
			for ( int j = 0; j < valid.length; j++ )
				if ( valid[ j ] == 0 )
					valid[ j ] = 2;
			show( size, data, valid);
		}
	}

	public static void main4( String[] args ) throws Exception
	{
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_resave.xml";
		final SpimDataMinimal spim = new XmlIoSpimDataMinimal().load( fn );
		final ViewerSetupImgLoader<UnsignedShortType, VolatileUnsignedShortType > setupImgLoader = Cast.unchecked( spim.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ) );

		final RandomAccessibleInterval< UnsignedShortType > img = setupImgLoader.getImage( 1 );
		final PrimitiveBlocks< UnsignedShortType > blocks = PrimitiveBlocks.of( img );
		System.out.println( "blocks = " + blocks );

		final RandomAccessibleInterval< VolatileUnsignedShortType > vimg = setupImgLoader.getVolatileImage( 1, 0 );
		System.out.println( "vimg = " + vimg );
		System.out.println( "vimg.getType().getClass() = " + vimg.getType().getClass() );

		final BlockSupplier< VolatileUnsignedShortType > vblocks = VolatileBlockSupplier.of( vimg );
		System.out.println( "vblocks = " + vblocks );

		final long[] pos = { -50, -50, 0 };
		final int[] size = { 1000, 400, 50 };
//		final long[] pos = { 0, 0, 0 };
//		final int[] size = { 2, 2, 2 };
		final int len = ( int ) Intervals.numElements( size );

		for ( int i = 0; i < 2; ++i )
		{
			final short[] data = new short[ len ];
			final byte[] valid = new byte[ len ];
			vblocks.copy(
					BlockInterval.wrap( pos, size ),
					new VolatileArray<>( data, valid ) );
			System.out.println();
			for ( int j = 0; j < valid.length; j++ )
				if ( valid[ j ] == 0 )
					valid[ j ] = 2;
			show( size, data, valid);
		}
	}

	public static void main( String[] args ) throws Exception
	{
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_resave.xml";
		final SpimDataMinimal spim = new XmlIoSpimDataMinimal().load( fn );
		final ViewerSetupImgLoader<UnsignedShortType, VolatileUnsignedShortType > setupImgLoader = Cast.unchecked( spim.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ) );

		final RandomAccessibleInterval< UnsignedShortType > img = setupImgLoader.getImage( 1 );
		final PrimitiveBlocks< UnsignedShortType > blocks = PrimitiveBlocks.of( img );
		System.out.println( "blocks = " + blocks );

		final RandomAccessibleInterval< VolatileUnsignedShortType > vimg = setupImgLoader.getVolatileImage( 1, 0 );
		System.out.println( "vimg = " + vimg );
		System.out.println( "vimg.getType().getClass() = " + vimg.getType().getClass() );


		VolatileUnsignedShortType t = vimg.getType();
		final UnaryOperatorType uot = UnaryOperatorType.of( t.get(), new VolatileFloatType().get() );
		System.out.println( "uot = " + uot );


		final BlockSupplier< VolatileFloatType > vblocks = VolatileBlockSupplier
				.of( vimg )
				.andThen( Convert.convert( new VolatileFloatType() ) );
		System.out.println( "vblocks = " + vblocks );

		final long[] pos = { -50, -50, 0 };
		final int[] size = { 1000, 400, 50 };
//		final long[] pos = { 0, 0, 0 };
//		final int[] size = { 2, 2, 2 };
		final int len = ( int ) Intervals.numElements( size );

		for ( int i = 0; i < 2; ++i )
		{
			final float[] data = new float[ len ];
			final byte[] valid = new byte[ len ];
			vblocks.copy(
					BlockInterval.wrap( pos, size ),
					new VolatileArray<>( data, valid ) );
			System.out.println();
			for ( int j = 0; j < valid.length; j++ )
				if ( valid[ j ] == 0 )
					valid[ j ] = 2;
			show( size, data, valid );
		}
	}

	private static void show( final int[] size, final short[] data, final byte[] valid )
	{
		final Img< UnsignedShortType > dataImg = ArrayImgs.unsignedShorts( data, Util.int2long( size ) );
		final Img< UnsignedByteType > validImg = ArrayImgs.unsignedBytes( valid, Util.int2long( size ) );

		final BdvSource dataSrc = BdvFunctions.show( dataImg, "image data" );
		final BdvSource validSrc = BdvFunctions.show( validImg, "validity mask", Bdv.options().addTo( dataSrc ) );

		dataSrc.setDisplayRange( 0, 5000 );
		dataSrc.setDisplayRangeBounds( 0, 5000 );
		validSrc.setDisplayRange( 0, 8 );
	}

	private static void show( final int[] size, final float[] data, final byte[] valid )
	{
		final Img< FloatType > dataImg = ArrayImgs.floats( data, Util.int2long( size ) );
		final Img< UnsignedByteType > validImg = ArrayImgs.unsignedBytes( valid, Util.int2long( size ) );

		final BdvSource dataSrc = BdvFunctions.show( dataImg, "image data" );
		final BdvSource validSrc = BdvFunctions.show( validImg, "validity mask", Bdv.options().addTo( dataSrc ) );

		dataSrc.setDisplayRange( 0, 5000 );
		dataSrc.setDisplayRangeBounds( 0, 5000 );
		validSrc.setDisplayRange( 0, 8 );
	}
}

