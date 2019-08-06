package bdv.viewer.render;

import bdv.viewer.Interpolation;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.List;

public interface RendererState {

	void getViewerTransform(AffineTransform3D t);

	Interpolation getInterpolation();

	int getCurrentTimepoint();

	List< ? extends RendererSourceState< ? > > getSources();

	List< Integer > getVisibleSourceIndices();

	int getBestMipMapLevel(AffineTransform3D screenScaleTransform, int sourceIndex);
}
