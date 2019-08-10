package bdv.viewer.render;

import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.ARGBType;

import java.awt.image.BufferedImage;

class MyRenderOutputImage implements RenderOutputImage<BufferedImage> {

	private final ARGBScreenImage screenImage;

	public MyRenderOutputImage(int width, int height) {
		screenImage = new ARGBScreenImage(width, height);
	}

	public MyRenderOutputImage(int width, int heihgt, IntArray update) {
		screenImage = new ARGBScreenImage(width, heihgt, update);
	}

	@Override
	public int width() {
		return (int) screenImage.dimension(0);
	}

	@Override
	public int height() {
		return (int) screenImage.dimension( 1 );
	}

	@Override
	public ArrayImg<ARGBType, IntAccess> asArrayImg() {
		return (ArrayImg) screenImage;
	}

	@Override
	public BufferedImage unwrap() {
		return screenImage.image();
	}
}
