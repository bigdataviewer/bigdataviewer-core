/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class ManySourcesExample
{
	private static Bdv addSource(final Bdv bdv, final Random random, final int i, final Img< UnsignedByteType > img, final int xOffset, final int yOffset) {
		AffineTransform3D transform = new AffineTransform3D();
		transform.translate( xOffset, yOffset, 0 );
		final BdvSource source = BdvFunctions.show( img, "img " + i,
				Bdv.options()
						.addTo( bdv )
						.preferredSize( 900, 900 )
						.numRenderingThreads( Runtime.getRuntime().availableProcessors() )
						.sourceTransform( transform ) );
		final ARGBType color = new ARGBType( random.nextInt() & 0xFFFFFF );
		source.setColor( color );
		return bdv == null ? source : bdv;
	}

	private static Img< UnsignedByteType > createImg( final Random random )
	{
		final long[] dim = { 100, 100, 100 };
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( dim );
		img.forEach( t -> t.set( 64 + random.nextInt( 128 ) ) );
		return img;
	}

	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final int[] numSources = { 5, 5 };
		final Random random = new Random( 1L );

		final List< Img< UnsignedByteType > > imgs = new ArrayList<>();
		for ( int i = 0; i < numSources[ 0 ] * numSources[ 1 ]; i++ )
			imgs.add( createImg( random ) );

		int i = 0;
		Bdv bdv = null;
		for ( int y = 0; y < numSources[ 1 ]; ++y )
		{
			for ( int x = 0; x < numSources[ 0 ]; ++x )
			{
				final int xOffset = 90 * x;
				final int yOffset = 90 * y;
				bdv = addSource( bdv, random, i, imgs.get( i ), xOffset, yOffset );
				i++;
			}
		}

//		final ViewerPanel viewer = bdv.getBdvHandle().getViewerPanel();
//		final DebugTilingOverlay tilingOverlay = viewer.showDebugTileOverlay();
//		final Runnable toggleShowTiles = () -> {
//			tilingOverlay.setShowTiles( !tilingOverlay.getShowTiles() );
//			viewer.getDisplay().repaint();
//		};
//
//
//		final InputTriggerConfig keyconf = viewer.getInputTriggerConfig();
//		Actions actions = new Actions( keyconf );
//		actions.install( bdv.getBdvHandle().getKeybindings(), "tile overlay" );
//		actions.runnableAction( toggleShowTiles, "toggle draw tiles", "T" );

//		// print current transform
//		viewer.state().changeListeners().add( change -> {
//			if ( change == ViewerStateChange.VIEWER_TRANSFORM_CHANGED )
//			{
//				final AffineTransform3D t = viewer.state().getViewerTransform();
//				System.out.println( "t = " + Arrays.toString( t.getRowPackedCopy() ) );
//			}
//		} );

	}



}
