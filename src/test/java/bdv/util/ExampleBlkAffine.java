/*-
 * #%L
 * BigDataViewer quick visualization API.
 * %%
 * Copyright (C) 2016 - 2024 BigDataViewer developers.
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

import java.util.Arrays;
import java.util.Random;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.render.DebugTilingOverlay;
import bdv.viewer.render.SimpleVolatileProjector;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class ExampleBlkAffine
{
	public static void main( final String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final Random random = new Random();

		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( 100, 100, 100 );
		img.forEach( t -> t.set( random.nextInt( 255 ) ) );
		final Bdv bdv = BdvFunctions.show( img, "ubyte" );

		final ViewerPanel viewer = bdv.getBdvHandle().getViewerPanel();
		final DebugTilingOverlay tilingOverlay = viewer.showDebugTileOverlay();
		final Runnable toggleShowTiles = () -> {
			tilingOverlay.setShowTiles( !tilingOverlay.getShowTiles() );
			viewer.getDisplay().repaint();
		};

		final InputTriggerConfig keyconf = viewer.getInputTriggerConfig();
		Actions actions = new Actions( keyconf );
		actions.install( bdv.getBdvHandle().getKeybindings(), "tile overlay" );
		actions.runnableAction( toggleShowTiles, "toggle draw tiles", "T" );

		actions.runnableAction( () -> {
			SimpleVolatileProjector.DEBUG_USE_BLK_AFFINE = !SimpleVolatileProjector.DEBUG_USE_BLK_AFFINE;
			System.out.println( "SimpleVolatileProjector.DEBUG_USE_BLK_AFFINE = " + SimpleVolatileProjector.DEBUG_USE_BLK_AFFINE );
		}, "toggle blk transform", "B" );

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
