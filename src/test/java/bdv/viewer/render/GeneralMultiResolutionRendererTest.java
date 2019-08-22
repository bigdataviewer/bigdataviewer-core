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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.imglib2.test.ImgLib2Assert.assertImageEquals;
import static net.imglib2.test.ImgLib2Assert.assertIntervalEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link GeneralMultiResolutionRenderer}.
 */
public class GeneralMultiResolutionRendererTest {

	/**
	 * Render a 2d image, with the same size as the screen,
	 * and all transformations set to identity.
	 */
	@Test
	public void testPaintOneSourceWithoutTransformation() {
		SimpleRenderTarget display = new SimpleRenderTarget(100, 100);
		GeneralMultiResolutionRenderer renderer = createRenderer(display, new double[]{1.0});
		Img<ARGBType> image = RandomImgs.seed(42).nextImage(new ARGBType(), 100, 100);
		renderer.requestRepaint();
		renderer.paint(simpleRenderState(image));
		assertImageEquals(image, display.getResult().getImage());
	}

	/** Same as {@link #testPaintOneSourceWithoutTransformation()}, but only render a sub intervals. */
	@Test
	public void testPaintSubInterval() {
		// setup
		SimpleRenderTarget display = new SimpleRenderTarget(10, 10);
		GeneralMultiResolutionRenderer renderer = createRenderer(display, new double[]{1.0});
		RandomAccessibleInterval<ARGBType> image = RandomImgs.seed(42).nextImage(new ARGBType(), 10, 10);
		FinalInterval interval = Intervals.createMinSize(5, 6, 4, 2);
		// process
		renderer.requestRepaint(interval);
		renderer.paint(simpleRenderState(image));
		// test
		assertIntervalEquals(interval, display.getResult().getScreenInterval());
		assertRealIntervalEquals(interval, display.getResult().getScaledInterval());
		assertImageEquals(Views.interval(image, interval), Views.interval(display.getResult().getImage(), interval));
	}

	/** Same as {@link #testPaintOneSourceWithoutTransformation()}, but screen scale is 0.5. */
	@Test
	public void testPaintScaled() {
		// setup
		SimpleRenderTarget display = new SimpleRenderTarget(10, 10);
		GeneralMultiResolutionRenderer renderer = createRenderer(display, new double[]{0.5});
		RandomAccessibleInterval<ARGBType> image = createRampImage(new long[]{10, 10}, 10, 0);
		// process
		renderer.requestRepaint();
		renderer.paint(simpleRenderState(image));
		// test
		RandomAccessibleInterval<ARGBType> expected = createRampImage(new long[]{5, 5}, 20, 5);
		assertImageEquals(expected, display.getResult().getImage(), this::colorsSimilar);
	}

	private boolean colorsSimilar(ARGBType a, ARGBType b) {
		int va = a.get();
		int vb = b.get();
		return Math.abs( ARGBType.red(va) - ARGBType.red(vb) ) <= 1
				&& Math.abs( ARGBType.green(va) - ARGBType.green(vb) ) <= 1
				&& Math.abs( ARGBType.blue(va) - ARGBType.blue(vb) ) <= 1;
	}

	/** Same as {@link #testPaintScaled()}, but render only a sub interval. */
	@Test
	public void testPaintScaledSubInterval() {
		// setup
		SimpleRenderTarget display = new SimpleRenderTarget(10, 10);
		GeneralMultiResolutionRenderer renderer = createRenderer(display, new double[]{0.5});
		RandomAccessibleInterval<ARGBType> image = createRampImage(new long[]{10, 10}, 10, 0);
		FinalInterval interval = Intervals.createMinSize(5, 6, 4, 2);
		// process
		renderer.requestRepaint(interval);
		renderer.paint(simpleRenderState(image));
		// test
		RandomAccessibleInterval<ARGBType> expected = createRampImage(new long[]{5, 5}, 20, 5);
		Interval targetInterval = display.getResult().getPaddedScaledInterval();
		assertTrue(Intervals.contains(targetInterval, Intervals.createMinSize(2, 3, 2, 1)));
		assertImageEquals(Views.interval(expected, targetInterval), Views.interval(display.getResult().getImage(), targetInterval), this::colorsSimilar);
	}

	private RandomAccessibleInterval<ARGBType> createRampImage(long[] size, int slope, int offset) {
		return Converters.convert(
					(RandomAccessibleInterval<Localizable>) Views.interval(Localizables.randomAccessible(2), new FinalInterval(size)),
					(location, pixel) -> pixel.set(location.getIntPosition(0) * slope + offset), new ARGBType());
	}

	private GeneralMultiResolutionRenderer createRenderer(SimpleRenderTarget display, double[] screenScales) {
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

	/**
	 * Returns a {@link RenderState} that has one image as source.
	 * Viewer and source transformations are the identity transformation.
	 */
	private RenderState simpleRenderState(RandomAccessibleInterval<ARGBType> image) {
		RandomAccessibleInterval<ARGBType> image3d = Views.addDimension(image, 0, 0);
		Source<ARGBType> source = new SimpleSource(image3d);
		RenderSource<?> rendererStateSource = new RenderSource<>(new SourceAndConverter<>(source, new TypeIdentity<>()), Interpolation.NLINEAR);
		return new RenderState(new AffineTransform3D(), 0, Collections.singletonList(rendererStateSource));
	}

	private void assertRealIntervalEquals(RealInterval expected, RealInterval actual) {
		assertArrayEquals(Intervals.minAsDoubleArray(expected), Intervals.minAsDoubleArray(actual), 0.0);
		assertArrayEquals(Intervals.maxAsDoubleArray(expected), Intervals.maxAsDoubleArray(actual), 0.0);
	}

	private static class SimpleRenderTarget implements RenderTarget {

		private final int width, height;

		private RenderResult result;

		private SimpleRenderTarget(int width, int height) {
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

	private static class SimpleSource implements Source<ARGBType> {

		private final RandomAccessibleInterval<ARGBType> image;

		private SimpleSource(RandomAccessibleInterval<ARGBType> image) {
			this.image = image;
		}

		@Override
		public boolean isPresent(int t) {
			return true;
		}

		@Override
		public RandomAccessibleInterval<ARGBType> getSource(int t, int level) {
			return image;
		}

		@Override
		public RealRandomAccessible<ARGBType> getInterpolatedSource(int t, int level, Interpolation method) {
			return Views.interpolate(Views.extendZero(image), new NLinearInterpolatorFactory<>());
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
	}
}
