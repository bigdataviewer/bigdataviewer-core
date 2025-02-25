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
package bdv.util.benchmark;

import bdv.util.benchmark.RenderingSetup.Renderer;
import bdv.viewer.BasicViewerState;
import bdv.viewer.DisplayMode;
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
public class RenderingWithEmptyBenchmark
{
	public ViewerState state;
	public Renderer renderer;

	@Param({"1", "8"})
	public int numRenderingThreads;

	@Setup
	public void setup()
	{
		final int[] targetSize = { 1680, 997 };
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set(
				6.085785957730029, -1.8606114880683469, 0.0, 706.509797402479,
				1.8606114880683469, 6.085785957730029, 0.0, 35.890787275681475,
				0.0, 0.0, 6.36385620774353, -318.02826989221813 );

		final Random random = new Random( 1L );

		state = new BasicViewerState();
		{
			final SourceAndConverter< UnsignedByteType > soc = createSourceAndConverter( random, 0, 0, 0 );
			state.addSource( soc );
			state.setSourceActive( soc, true );
		}
		{
			final SourceAndConverter< UnsignedByteType > soc = createSourceAndConverter( random, 1, 90, 0 );
			state.addSource( soc );
			state.setSourceActive( soc, true );
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
				.include( RenderingWithEmptyBenchmark.class.getSimpleName() )
				.warmupIterations( 4 )
				.measurementIterations( 8 )
				.warmupTime( TimeValue.milliseconds( 500 ) )
				.measurementTime( TimeValue.milliseconds( 500 ) )
				.build();
		new Runner( opt ).run();

//		final RenderingWithEmptyBenchmark b = new RenderingWithEmptyBenchmark();
//		b.numRenderingThreads = 1;
//		b.setup();
//		b.bench();
//		b.renderer.writeResult( "/Users/pietzsch/Desktop/RenderingWithEmptyBenchmark.png" );
	}
}
