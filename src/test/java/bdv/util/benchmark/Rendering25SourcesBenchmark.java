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
package bdv.util.benchmark;

import bdv.util.benchmark.RenderingSetup.Renderer;
import bdv.viewer.BasicViewerState;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import static bdv.util.benchmark.RenderingSetup.createSourceAndConverter;
import static bdv.viewer.DisplayMode.FUSED;

@State( Scope.Thread )
@Fork( 1 )
public class Rendering25SourcesBenchmark
{
	public ViewerState state;
	public Renderer renderer;

	@Param({"1", "8"})
	public int numRenderingThreads;

	@Setup
	public void setup()
	{
		final int[] numSources = { 5, 5 };
		final int[] targetSize = { 1680, 997 };
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set(
				3.7078643166510905, -2.055302752278437, 0.0, 512.6446071237642,
				2.055302752278437, 3.7078643166510905, 0.0, -912.7996823819342,
				0.0, 0.0, 4.239401749565353, -208.1315727072766 );
		final Random random = new Random( 1L );

		state = new BasicViewerState();

		int i = 0;
		for ( int y = 0; y < numSources[ 1 ]; ++y )
		{
			for ( int x = 0; x < numSources[ 0 ]; ++x )
			{
				final int xOffset = 90 * x;
				final int yOffset = 90 * y;
				final SourceAndConverter< UnsignedByteType > soc = createSourceAndConverter( random, i, xOffset, yOffset );
				state.addSource( soc );
				state.setSourceActive( soc, true );
				i++;
			}
		}

		state.setDisplayMode( FUSED );
		state.setViewerTransform( viewerTransform );

		renderer = new Renderer( targetSize, numRenderingThreads );
	}

	@Benchmark
	@BenchmarkMode( Mode.AverageTime )
	@OutputTimeUnit( TimeUnit.MILLISECONDS )
	public void bench()
	{
		renderer.render( state );
	}

	public static void main( final String... args ) throws RunnerException, IOException
	{
		final Options opt = new OptionsBuilder()
				.include( Rendering25SourcesBenchmark.class.getSimpleName() )
				.warmupIterations( 4 )
				.measurementIterations( 8 )
				.warmupTime( TimeValue.milliseconds( 500 ) )
				.measurementTime( TimeValue.milliseconds( 500 ) )
				.build();
		new Runner( opt ).run();

//		final Rendering25SourcesBenchmark b = new Rendering25SourcesBenchmark();
//		b.numRenderingThreads = 1;
//		b.setup();
//		b.bench();
//		b.renderer.writeResult( "/Users/pietzsch/Desktop/Rendering25SourcesBenchmark.png" );
	}
}
