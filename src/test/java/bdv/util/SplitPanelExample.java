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

import bdv.ui.splitpanel.SplitPanel;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import java.util.Random;
import java.util.function.Consumer;

import static bdv.ui.BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD;

public class SplitPanelExample
{

	public static void main( final String[] args ) throws Exception
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final Random random = new Random();

		final ArrayImg< ARGBType, IntArray > img = ArrayImgs.argbs( 100, 100, 100 );
		img.forEach( t -> t.set( random.nextInt() & 0xFF003F00 ) );
		final Bdv bdv2D = BdvFunctions.show( img, "greens", Bdv.options().is2D() );

		int delayInMilliseconds = 2000;

		SplitPanel splitPanel = bdv2D.getBdvHandle().getSplitPanel();

		Consumer<String> logger = (str) -> {
			System.out.println(str);
			bdv2D.getBdvHandle().getViewerPanel().showMessage(str);
		};

		Thread.sleep(delayInMilliseconds);

		logger.accept("Split panel expansion.");
		splitPanel.setCollapsed(false); // Expands the split Panel

		Thread.sleep(delayInMilliseconds);

		logger.accept("Split panel divider size change. NOT WORKING");
		splitPanel.setDividerSize(50); // TODO : fix divider size change not being triggered

		Thread.sleep(delayInMilliseconds);

		logger.accept("Forcing update through collapse / expansion.");
		splitPanel.setCollapsed(true); // Expands the split Panel
		splitPanel.setCollapsed(false); // Expands the split Panel

		Thread.sleep(delayInMilliseconds);

		logger.accept("Divider location change : 50/50");
		splitPanel.setDividerLocation(0.5);

		Thread.sleep(delayInMilliseconds);

		logger.accept("Collapsing 'Groups' card.");
		bdv2D.getBdvHandle().getCardPanel().setCardExpanded(DEFAULT_SOURCEGROUPS_CARD, false);

		Thread.sleep(delayInMilliseconds);

		logger.accept("Expanding 'Groups' card");
		bdv2D.getBdvHandle().getCardPanel().setCardExpanded(DEFAULT_SOURCEGROUPS_CARD, true);

		Thread.sleep(delayInMilliseconds);

		logger.accept("Removing 'Groups' card");
		bdv2D.getBdvHandle().getCardPanel().removeCard(DEFAULT_SOURCEGROUPS_CARD);

		Thread.sleep(delayInMilliseconds);

		logger.accept("Adding 'Dummy' Card");
		bdv2D.getBdvHandle().getCardPanel().addCard("Dummy", "Dummy",new JPanel(), true);

	}
}
