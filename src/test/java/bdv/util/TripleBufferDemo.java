package bdv.util;

import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import java.awt.*;

/**
 * Test {@link TripleBuffer}.
 *
 * @author Matthias Arzt
 */
public class TripleBufferDemo {

	private static TripleBuffer<ARGBScreenImage> tripleBuffer = new TripleBuffer<>(
			() -> new ARGBScreenImage(100, 100));

	private static ImageComponent imageComponent = new ImageComponent();

	public static void main(String... args) throws InterruptedException {

		JFrame frame = new JFrame();
		frame.setSize(100, 100);
		frame.add(imageComponent);
		frame.setVisible(true);

		new PainterThread().start();
	}

	private static class PainterThread extends Thread {

		@Override
		public void run() {
			try {
				while (true) {
					for (double r = 0; r < 2 * Math.PI; r += 0.01) {
						renderCircle(r);
						Thread.sleep(1);
						imageComponent.repaint();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void renderCircle(double r) {
			ARGBScreenImage image = tripleBuffer.getWritableBuffer();
			image.forEach(pixel -> pixel.set(0xff00ff00));
			ArrayCursor<ARGBType> cursor = image.cursor();
			while (cursor.hasNext()) {
				ARGBType pixel = cursor.next();
				double x = cursor.getDoublePosition(0);
				double y = cursor.getDoublePosition(1);
				pixel.set(ARGBType.rgba(Math.sin(x / 10 + r) * 127 + 127, Math.cos(y / 10 + r) * 127 + 127, 0.0, 255.0));
			}
			tripleBuffer.doneWriting();
		}
	}

	private static class ImageComponent extends JComponent {

		@Override
		protected void paintComponent(Graphics g) {
			ARGBScreenImage image = tripleBuffer.getReadableBuffer();
			if(image != null)
				g.drawImage(image.image(), 0, 0, getWidth(), getHeight(), null );
		}
	}
}

