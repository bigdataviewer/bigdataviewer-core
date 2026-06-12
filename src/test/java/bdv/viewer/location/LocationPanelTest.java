/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
package bdv.viewer.location;

import javax.swing.JFrame;
import javax.swing.event.ChangeListener;

import net.imglib2.Interval;
import net.imglib2.util.Intervals;

public class LocationPanelTest {

	public static void main(String[] args) {

		JFrame frame = new JFrame("LocationPanelTest");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final Interval interval = Intervals.createMinMax(0, -111222333444L, -98765, 100, 66, 4321);
		final LocationPanel locationPanel = new LocationPanel(interval);

		final ChangeListener changeListener = e -> {
			final DimensionCoordinateComponents source = (DimensionCoordinateComponents) e.getSource();
			System.out.println("coordinateChangeListener: " + source.getPosition() + ", " + source.getDimension());
		};
		locationPanel.setDimensionValueChangeListener(changeListener);

		frame.setContentPane(locationPanel);

		//Display the window.
		frame.pack();
		frame.setVisible(true);

		locationPanel.setCenterPosition(new double[] {35.67, -111222333444.5, -222 });
	}

}
