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
package bdv.util;

import bdv.TransformEventHandler3D;
import java.util.Random;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

public class BehaviourBlockingExample
{
	public static void main( final String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final Random random = new Random();

		final ArrayImg< ARGBType, IntArray > img = ArrayImgs.argbs( 100, 100, 100 );
		img.forEach( t -> t.set( random.nextInt() & 0xFF00FF00 ) );
		Bdv bdv = BdvFunctions.show( img, "greens", Bdv.options() );

		// Add a BehaviourMap that maps all rotation behaviour names to a behaviour that does nothing.
		// This effectively blocks these rotation behaviours from being triggered.
		final BehaviourMap blockMap = new BehaviourMap();
		final Behaviour doNothing = new Behaviour() {};
		blockMap.put(TransformEventHandler3D.DRAG_ROTATE, doNothing );
		blockMap.put(TransformEventHandler3D.ROTATE_LEFT, doNothing );
		blockMap.put(TransformEventHandler3D.ROTATE_RIGHT, doNothing );
		blockMap.put(TransformEventHandler3D.DRAG_ROTATE_FAST, doNothing );
		blockMap.put(TransformEventHandler3D.ROTATE_LEFT_FAST, doNothing );
		blockMap.put(TransformEventHandler3D.ROTATE_RIGHT_FAST, doNothing );
		blockMap.put(TransformEventHandler3D.DRAG_ROTATE_SLOW, doNothing );
		blockMap.put(TransformEventHandler3D.ROTATE_LEFT_SLOW, doNothing );
		blockMap.put(TransformEventHandler3D.ROTATE_RIGHT_SLOW, doNothing );

		final String BLOCKING_MAP = "transform-blocking";
		final TriggerBehaviourBindings triggerbindings = bdv.getBdvHandle().getTriggerbindings();
		triggerbindings.addBehaviourMap( BLOCKING_MAP, blockMap );

		// The following restores the rotation behaviours (by removing the blocking map):
		//   triggerbindings.removeBehaviourMap( BLOCKING_MAP );

		// We can attach this to an action (triggered by pressing "R")
		Actions actions = new Actions( new InputTriggerConfig() );
		actions.install( bdv.getBdvHandle().getKeybindings(), "my-new-actions" );
		actions.runnableAction( () -> {
			triggerbindings.removeBehaviourMap( BLOCKING_MAP );
			bdv.getBdvHandle().getViewerPanel().showMessage( "Rotation behaviours restored" );
		}, "restore rotations", "R" );
	}
}
