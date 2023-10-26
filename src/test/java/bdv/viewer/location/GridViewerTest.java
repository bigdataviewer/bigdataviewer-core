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

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.BiConsumer;

import mpicbg.spim.data.SpimDataException;

import bdv.BigDataViewer;
import bdv.cache.CacheControl;
import bdv.export.ProgressWriterConsole;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RealLocalizable;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.integer.IntType;

public class GridViewerTest {

	public static void main(String[] args)
			throws IOException, SpimDataException {

		final ArrayList<ConverterSetup> converterSetups = new ArrayList<>();
		final ArrayList<SourceAndConverter<?>> sources = new ArrayList<>();
		final CacheControl cache = new CacheControl.Dummy();
		final ProgressWriterConsole progressWriter = new ProgressWriterConsole();
		final ViewerOptions viewerOptions = ViewerOptions.options();

		final String[] sourceNames = {
				"Grid A",
				"Grid B has a name longer than 20 characters",
				"Grid C",
		};

		for (int i = 0; i < sourceNames.length; i++) {
			final RealARGBColorConverter<IntType> converter = RealARGBColorConverter.create(new IntType(), 0, 127);
			final ConverterSetup converterSetup = new RealARGBColorConverterSetup(i, converter);
			converterSetups.add(converterSetup);

			final int numDimensions = 3; // (i < 3) ? 3 : 2;
			final int size = 100 * (i + 1);
			final RealRandomAccessibleSource<IntType> source = buildGridSource(numDimensions, sourceNames[i], size);
			final SourceAndConverter<IntType> soc = new SourceAndConverter<>(source, converter);
			sources.add(soc);
		}

		BigDataViewer.open(converterSetups,
						   sources,
						   1,
						   cache,
						   "Viewer Example",
						   progressWriter,
						   viewerOptions);
	}

	public static BiConsumer<RealLocalizable, IntType> GRID_FUNCTION = (pos, pixels) -> {
		int i = 0;
		int xPos = (int) Math.round(pos.getDoublePosition(0));
		int yPos = (int) Math.round(pos.getDoublePosition(1));
		if ((xPos == 0) || (yPos == 0)) {
			i = 120;
		} else {
			if (xPos % 10 == 0) {
				i = 60;
			} else if (yPos % 10 == 0) {
				i = 30;
			}
		}
		pixels.set(i);
	};

	public static RealRandomAccessibleSource<IntType> buildGridSource(final int numDimensions,
																	  final String name,
																	  final int size) {
		final FunctionRealRandomAccessible<IntType> grid =
				new FunctionRealRandomAccessible<>(numDimensions, GRID_FUNCTION, IntType::new);

		final long[] min = new long[numDimensions];
		final long[] max = new long[numDimensions];
		for (int d = 0; d < numDimensions; d++) {
			min[d] = -size;
			max[d] = size;
		}

		return new RealRandomAccessibleSource<IntType>(grid, new IntType(), name) {
			private final Interval interval = new FinalInterval(min, max);
			@Override
			public Interval getInterval(final int t,
										final int level) {
				return interval;
			}
		};
	}
}
