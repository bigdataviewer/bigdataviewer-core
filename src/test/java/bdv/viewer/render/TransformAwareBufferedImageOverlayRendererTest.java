package bdv.viewer.render;

import net.imglib2.FinalInterval;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.test.RandomImgs;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static net.imglib2.test.ImgLib2Assert.assertImageEquals;

public class TransformAwareBufferedImageOverlayRendererTest {

	@Test
	public void test() {
		TransformAwareBufferedImageOverlayRenderer renderer = new TransformAwareBufferedImageOverlayRenderer();
		renderer.setCanvasSize(10, 10);
		ARGBScreenImage image = new ARGBScreenImage(10, 10);
		AtomicInteger ai = new AtomicInteger();
		image.forEach(pixel -> pixel.set(0xff000000 + ai.incrementAndGet()));
		RenderResult result = new RenderResult(image, new AffineTransform3D(),
				0, 1.0, true, new FinalInterval(10, 10), new FinalInterval(10, 10),
				new FinalInterval(10, 10));
		renderer.setRenderResult(result);
		ARGBScreenImage screen = new ARGBScreenImage(10, 10);
		renderer.drawOverlays(screen.image().getGraphics());
		assertImageEquals(image, screen);
	}

	@Test
	public void test2() {
		TransformAwareBufferedImageOverlayRenderer renderer = new TransformAwareBufferedImageOverlayRenderer();
		renderer.setCanvasSize(10, 10);
		ARGBScreenImage image = new ARGBScreenImage(10, 10);
		AtomicInteger ai = new AtomicInteger();
		image.forEach(pixel -> pixel.set(0xff000000 + ai.incrementAndGet()));
		RenderResult result = new RenderResult(image, new AffineTransform3D(),
				0, 1.0, true, new FinalInterval(10, 10), new FinalInterval(10, 10),
				new FinalInterval(10, 10));
		renderer.setRenderResult(result);
		ARGBScreenImage image2 = new ARGBScreenImage(10, 10);
		RandomImgs.seed(42).randomize(image2);
		image2.forEach(pixel -> pixel.set(pixel.get() | 0xff000000));
		FinalInterval screenInterval = Intervals.createMinSize(2, 2, 3, 4);
		RenderResult result2 = new RenderResult(image2, new AffineTransform3D(),
				0, 1.0, false, screenInterval, screenInterval,
				screenInterval);
		renderer.setRenderResult(result2);
		ARGBScreenImage screen = new ARGBScreenImage(10, 10);
		renderer.drawOverlays(screen.image().getGraphics());
		assertImageEquals(Views.interval(image2,screenInterval), Views.interval(screen, screenInterval));
	}
}
