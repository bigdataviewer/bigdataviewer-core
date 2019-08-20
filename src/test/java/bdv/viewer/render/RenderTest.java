package bdv.viewer.render;

import bdv.cache.CacheControl;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.PainterThread;
import net.imglib2.util.Intervals;
import net.imglib2.util.Localizables;
import net.imglib2.view.Views;
import org.junit.Test;

import java.util.Collections;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.imglib2.test.ImgLib2Assert.assertImageEquals;
import static net.imglib2.test.ImgLib2Assert.assertIntervalEquals;
import static org.junit.Assert.assertArrayEquals;

public class RenderTest {

	@Test
	public void test() {
		SimpleRenderTraget display = new SimpleRenderTraget(100, 100);
		GeneralMultiResolutionRenderer renderer = createRenderer(display, new double[]{1.0});
		Img<ARGBType> image = RandomImgs.seed(42).nextImage(new ARGBType(), 100, 100);
		RenderSource<?> rendererStateSource = asRendererStateSource(image);
		renderer.requestRepaint();
		renderer.paint(new RenderState(new AffineTransform3D(), 0, Collections.singletonList(rendererStateSource)));
		assertImageEquals(image, display.getResult().getImage());
	}

	@Test
	public void testPaintSubInterval() {
		// setup
		SimpleRenderTraget display = new SimpleRenderTraget(10, 10);
		GeneralMultiResolutionRenderer renderer = createRenderer(display, new double[]{1.0});
		RandomAccessibleInterval<ARGBType> image = RandomImgs.seed(42).nextImage(new ARGBType(), 10, 10);
		RenderSource<?> rendererStateSource = asRendererStateSource(image);
		FinalInterval interval = Intervals.createMinSize(5, 6, 4, 2);
		// process
		renderer.requestRepaint(interval);
		renderer.paint(new RenderState(new AffineTransform3D(), 0, Collections.singletonList(rendererStateSource)));
		// test
		assertIntervalEquals(interval, display.getResult().getScreenInterval());
		assertRealIntervalEquals(interval, display.getResult().getScaledInterval());
		assertImageEquals(Views.interval(image, interval), Views.interval(display.getResult().getImage(), interval));
	}

	private void assertRealIntervalEquals(RealInterval expected, RealInterval actual) {
		assertArrayEquals(Intervals.minAsDoubleArray(expected), Intervals.minAsDoubleArray(actual), 0.0);
		assertArrayEquals(Intervals.maxAsDoubleArray(expected), Intervals.maxAsDoubleArray(actual), 0.0);
	}

	@Test
	public void testScaled() {
		SimpleRenderTraget display = new SimpleRenderTraget(10, 10);
		GeneralMultiResolutionRenderer renderer = createRenderer(display, new double[]{0.5});
		RandomAccessibleInterval<ARGBType> image = Converters.convert(
				(RandomAccessibleInterval<Localizable>) Views.interval(Localizables.randomAccessible(2), new FinalInterval(10, 10)),
				(location, pixel) -> pixel.set(location.getIntPosition(0)*10), new ARGBType());
		RenderSource<?> rendererStateSource = asRendererStateSource(image);
		renderer.requestRepaint();
		renderer.paint(new RenderState(new AffineTransform3D(), 0, Collections.singletonList(rendererStateSource)));
		RandomAccessibleInterval<ARGBType> expected = Converters.convert(
				(RandomAccessibleInterval<Localizable>) Views.interval(Localizables.randomAccessible(2), new FinalInterval(5, 5)),
				(location, pixel) -> pixel.set(location.getIntPosition(0) * 20 + 5), new ARGBType());
		assertImageEquals(expected, display.getResult().getImage(), this::colorsSimilar);
	}

	private boolean colorsSimilar(ARGBType a, ARGBType b) {
		int va = a.get();
		int vb = b.get();
		return Math.abs( ARGBType.red(va) - ARGBType.red(vb) ) <= 1
				&& Math.abs( ARGBType.green(va) - ARGBType.green(vb) ) <= 1
				&& Math.abs( ARGBType.blue(va) - ARGBType.blue(vb) ) <= 1;
	}

	@Test
	public void testScaledSubInterval() {
		// setup
		SimpleRenderTraget display = new SimpleRenderTraget(10, 10);
		GeneralMultiResolutionRenderer renderer = createRenderer(display, new double[]{0.5});
		RandomAccessibleInterval<ARGBType> image = Converters.convert(
				(RandomAccessibleInterval<Localizable>) Views.interval(Localizables.randomAccessible(2), new FinalInterval(10, 10)),
				(location, pixel) -> pixel.set(location.getIntPosition(0)*10), new ARGBType());
		RenderSource<?> rendererStateSource = asRendererStateSource(image);
		FinalInterval interval = Intervals.createMinSize(5, 6, 4, 2);
		// process
		renderer.requestRepaint(interval);
		renderer.paint(new RenderState(new AffineTransform3D(), 0, Collections.singletonList(rendererStateSource)));
		// test
		RandomAccessibleInterval<ARGBType> expected = Converters.convert(
				(RandomAccessibleInterval<Localizable>) Views.interval(Localizables.randomAccessible(2), new FinalInterval(5, 5)),
				(location, pixel) -> pixel.set(location.getIntPosition(0) * 20 + 5), new ARGBType());
		Interval targetInterval = display.getResult().getPaddedScaledInterval();
		assertImageEquals(Views.interval(expected, targetInterval), Views.interval(display.getResult().getImage(), targetInterval), this::colorsSimilar);
	}

	static String toString(RandomAccessibleInterval<ARGBType> image) {
		int n = image.numDimensions();
		if (n > 1) {
			StringJoiner joiner = new StringJoiner(",\n   ", "[\n   ", "\n]");
			for (long p = image.min(n - 1); p < image.max(n - 1); p++) {
				joiner.add(toString(Views.hyperSlice(image, n - 1, p)));
			}
			return joiner.toString();
		} else {
			StringJoiner joiner = new StringJoiner(", ", "[", "]");
			for (ARGBType pixel : Views.iterable(image))
				joiner.add(toString(pixel));
			return joiner.toString();
		}
	}

	private static String toString(ARGBType pixel) {
		return String.valueOf(pixel.get());
	}

	private RenderSource<?> asRendererStateSource(RandomAccessibleInterval<ARGBType> image) {
		RandomAccessibleInterval<ARGBType> image3d = Views.addDimension(image, 0, 0);
		Source<ARGBType> source = new Source<ARGBType>() {
			@Override
			public boolean isPresent(int t) {
				return true;
			}

			@Override
			public RandomAccessibleInterval<ARGBType> getSource(int t, int level) {
				return image3d;
			}

			@Override
			public RealRandomAccessible<ARGBType> getInterpolatedSource(int t, int level, Interpolation method) {
				return Views.interpolate(Views.extendZero(image3d), new NLinearInterpolatorFactory<>());
			}

			@Override
			public void getSourceTransform(int t, int level, AffineTransform3D transform) {
				transform.set(new AffineTransform3D());
			}

			@Override
			public ARGBType getType() {
				return new ARGBType();
			}

			@Override
			public String getName() {
				return "image";
			}

			@Override
			public VoxelDimensions getVoxelDimensions() {
				return new FinalVoxelDimensions("pixel", 1, 1, 1);
			}

			@Override
			public int getNumMipmapLevels() {
				return 1;
			}
		};
		return new RenderSource<>(
				new SourceAndConverter<>(source, new TypeIdentity<>()),
				Interpolation.NLINEAR
		);
	}

	private GeneralMultiResolutionRenderer createRenderer(SimpleRenderTraget display, double[] screenScales) {
		ViewerOptions.Values options = new ViewerOptions().values;
		PainterThread painterTread = new PainterThread(null);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		CacheControl cacheControl = new CacheControl.CacheControls();
		return new GeneralMultiResolutionRenderer(
				display, painterTread::requestRepaint, screenScales, options.getTargetRenderNanos(),
				options.getNumRenderingThreads(),
				executorService, options.isUseVolatileIfAvailable(), options.getAccumulateProjectorFactory(),
				cacheControl
		);
	}

	private static class SimpleRenderTraget implements RenderTarget {

		private final int width, height;

		private RenderResult result;

		private SimpleRenderTraget(int width, int height) {
			this.width = width;
			this.height = height;
		}

		@Override
		public void setRenderResult(RenderResult result) {
			this.result = result;
		}

		@Override
		public RandomAccessibleInterval<ARGBType> createOutputImage(int width, int height) {
			return ArrayImgs.argbs(width, height);
		}

		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}

		public RenderResult getResult() {
			return result;
		}
	}
}
