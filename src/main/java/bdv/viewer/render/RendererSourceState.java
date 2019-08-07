package bdv.viewer.render;

import bdv.util.MipmapTransforms;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;

public class RendererSourceState< T > extends SourceAndConverter< T > {

	private final Interpolation interpolation;
	private final AffineTransform3D viewerTransform;
	private final int currentTimepoint;

	public RendererSourceState(SourceAndConverter<T> soc, Interpolation interpolation, AffineTransform3D viewerTransfrom, int currentTimepoint ) {
		super(soc);
		this.interpolation = interpolation;
		this.viewerTransform = viewerTransfrom;
		this.currentTimepoint = currentTimepoint;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}

	@Override
	public RendererSourceState<? extends Volatile<T>> asVolatile() {
		SourceAndConverter<? extends Volatile<T>> v = super.asVolatile();
		return v == null ? null : new RendererSourceState<>(v, interpolation, viewerTransform, currentTimepoint);
	}

	public int getBestMipMapLevel(AffineTransform3D screenScaleTransform) {
		final AffineTransform3D screenTransform = new AffineTransform3D();
		screenTransform.set(viewerTransform);
		screenTransform.preConcatenate( screenScaleTransform );
		return MipmapTransforms.getBestMipMapLevel( screenTransform, getSpimSource(), currentTimepoint );
	}
}
